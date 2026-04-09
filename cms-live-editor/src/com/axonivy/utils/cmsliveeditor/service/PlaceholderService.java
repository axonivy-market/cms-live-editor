package com.axonivy.utils.cmsliveeditor.service;

import java.text.ChoiceFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.axonivy.utils.cmsliveeditor.constants.CommonConstants;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.SavedCms;

public class PlaceholderService {

  private static PlaceholderService instance;

  public static PlaceholderService getInstance() {
    if (instance == null) {
      instance = new PlaceholderService();
    }
    return instance;
  }

  public List<Integer> findInvalidLanguageIndices(Cms selectedCms, Map<String, SavedCms> savedLocales) {
    if (selectedCms == null || selectedCms.isFile()) {
      return List.of();
    }

    Map<String, SavedCms> cmsLocales = new HashMap<>();
    for (CmsContent content : selectedCms.getContents()) {
      String localeKey = content.getLocale().toString();
      if (savedLocales.containsKey(localeKey)) {
        cmsLocales.put(localeKey, savedLocales.get(localeKey));
      } else {
        cmsLocales.put(localeKey,
            new SavedCms(selectedCms.getUri(), localeKey, content.getOriginalContent(), content.getContent()));
      }
    }

    if (cmsLocales.isEmpty()) {
      return List.of();
    }

    List<String> errorLocales = validateLocales(cmsLocales);
    if (errorLocales.isEmpty()) {
      return List.of();
    }

    return findInvalidLocaleIndices(errorLocales, selectedCms);
  }

  public List<Integer> findInvalidLocaleIndices(List<String> invalidLocales, Cms selectedCms) {
    if (selectedCms == null || selectedCms.getContents() == null || selectedCms.getContents().isEmpty()) {
      return new ArrayList<>();
    }

    Set<String> normalizedLocaleTags = invalidLocales.stream()
        .filter(Objects::nonNull)
        .map(tag -> Locale
            .forLanguageTag(tag.replace(CommonConstants.UNDERSCORE_CHARACTER, CommonConstants.HYPHEN_CHARACTER))
            .toLanguageTag())
        .collect(Collectors.toSet());

    return selectedCms.getContents().stream()
        .filter(cms -> cms.getLocale() != null && normalizedLocaleTags.contains(cms.getLocale().toLanguageTag()))
        .map(CmsContent::getIndex)
        .distinct()
        .sorted()
        .toList();
  }

  public List<String> validateLocales(Map<String, SavedCms> cmsLocales) {
    if (cmsLocales == null || cmsLocales.isEmpty()) {
      return new ArrayList<>();
    }

    String originalValue = cmsLocales.values().stream()
        .map(SavedCms::getOriginalContent)
        .filter(value -> value != null && !value.isBlank())
        .max(Comparator.comparingInt(PlaceholderService::countArgumentNumbers))
        .orElse(null);

    // CASE 1: original has NO placeholders → compare among edited locales
    if (originalValue == null || countArgumentNumbers(originalValue) <= 0) {
      return validateAmongEditedLocales(cmsLocales);
    }

    // CASE 2: original has placeholders
    return cmsLocales.entrySet().stream()
        .filter(localeEntry -> isEdited(localeEntry.getValue()))
        .filter(localeEntry -> !areMessagePatternsCompatible(originalValue, localeEntry.getValue().getNewContent()))
        .map(Map.Entry::getKey)
        .toList();
  }

  private List<String> validateAmongEditedLocales(Map<String, SavedCms> cmsLocales) {
    List<Map.Entry<String, SavedCms>> editedLocales =
        cmsLocales.entrySet().stream().filter(entry -> isEdited(entry.getValue())).toList();
    if (editedLocales.isEmpty()) {
      return List.of();
    }

    List<Map.Entry<String, SavedCms>> entries = new ArrayList<>(cmsLocales.entrySet());
    List<List<Integer>> allArgs =
        entries.stream().map(entry -> extractArgumentIndices(entry.getValue().getNewContent())).toList();

    long nonEmptyCount = allArgs.stream().filter(set -> !set.isEmpty()).count();
    boolean hasMixedPresence = nonEmptyCount > 0 && nonEmptyCount < allArgs.size();
    boolean hasTooFewWithPlaceholders = nonEmptyCount == 1;

    if (hasMixedPresence || hasTooFewWithPlaceholders) {
      return editedLocales.stream().map(Map.Entry::getKey).toList();
    }

    if (nonEmptyCount == 0) {
      return List.of();
    }

    String base = editedLocales.get(0).getValue().getNewContent();
    return editedLocales.stream().filter(entry -> !areMessagePatternsCompatible(base, entry.getValue().getNewContent()))
        .map(Map.Entry::getKey).toList();
  }


  private List<Integer> extractArgumentIndices(String pattern) {
    List<Integer> indices = new ArrayList<>();

    Matcher matcher = Pattern.compile("\\{(\\d+)").matcher(pattern);
    while (matcher.find()) {
      indices.add(Integer.parseInt(matcher.group(1)));
    }

    return indices;
  }

  /**
   * Checks whether two MessageFormat patterns are structurally compatible.
   *
   * This ignores plain text differences (translations), but enforces:
   * - Same argument indices
   * - Same format types (number, date, etc.)
   * - Same format styles (e.g. integer vs currency)
   * - Same ChoiceFormat structure
   * - Same nested placeholder(s) (recursively)
   */
  public boolean areMessagePatternsCompatible(String originalValue, String newValue) {
    try {
      List<Integer> originalArgs = extractArgumentIndices(originalValue);
      List<Integer> newArgs = extractArgumentIndices(newValue);
      if (!originalArgs.equals(newArgs)) {
        return false;
      }

      MessageFormat originalMessageFormat = new MessageFormat(originalValue, Locale.ENGLISH);
      MessageFormat newMessageFormat = new MessageFormat(newValue, Locale.ENGLISH);
      return hasSameMessageFormatStructure(originalMessageFormat, newMessageFormat);

    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Compares two MessageFormat objects at the top level.
   *
   * Rules:
   * - They have the same number of arguments
   * - Each argument uses the same format type and structure
   */
  private boolean hasSameMessageFormatStructure(MessageFormat originalMessageFormat, MessageFormat newMessageFormat) {
    Format[] originalFormat = originalMessageFormat.getFormatsByArgumentIndex();
    Format[] newFormat = newMessageFormat.getFormatsByArgumentIndex();

    if (originalFormat.length != newFormat.length) {
      return false;
    }

    for (int i = 0; i < originalFormat.length; i++) {
      if (!hasSameFormatComponent(originalFormat[i], newFormat[i])) {
        return false;
      }
    }

    return true;
  }

  /**
   * Recursively compares two Format components.
   * 
   * Supported cases:
   * - ChoiceFormat → compares limits and recursively validates each branch
   * - DecimalFormat → compares numeric pattern (e.g. integer vs currency)
   * - SimpleDateFormat → compares date pattern
   * - Fallback → uses equals() to compare choiceFormats and choiceLimits
   *
   * Important:
   * - Text differences are ignored
   * - Structural or type changes are NOT allowed
   * - Nested MessageFormats inside ChoiceFormat are validated recursively
   */
  private boolean hasSameFormatComponent(Format originalFormat, Format newFormat) {
    if (originalFormat == null && newFormat == null) {
      return true;
    }
    if (originalFormat == null || newFormat == null) {
      return false;
    }

    if (!originalFormat.getClass().equals(newFormat.getClass())) {
      return false;
    }

    if (originalFormat instanceof ChoiceFormat originalChoiceFormat && newFormat instanceof ChoiceFormat newChoiceFormat) {
      return areChoiceFormatsEquivalent(originalChoiceFormat, newChoiceFormat);
    }

    if (originalFormat instanceof DecimalFormat originalDecimalFormat && newFormat instanceof DecimalFormat newDecimalFormat) {
      return Objects.equals(originalDecimalFormat.toPattern(), newDecimalFormat.toPattern());
    }

    if (originalFormat instanceof SimpleDateFormat originalDateFormat && newFormat instanceof SimpleDateFormat newDateFormat) {
      return Objects.equals(originalDateFormat.toPattern(), newDateFormat.toPattern());
    }

    return originalFormat.equals(newFormat);
  }

  private boolean areChoiceFormatsEquivalent(ChoiceFormat originalChoiceFormat, ChoiceFormat newChoiceFormat) {
    if (!Arrays.equals(originalChoiceFormat.getLimits(), newChoiceFormat.getLimits())) {
      return false;
    }

    Object[] originalChoiceFormats = originalChoiceFormat.getFormats();
    Object[] newChoiceFormats = newChoiceFormat.getFormats();

    if (originalChoiceFormats.length != newChoiceFormats.length) {
      return false;
    }

    for (int i = 0; i < originalChoiceFormats.length; i++) {
      String originalPatternPart = String.valueOf(originalChoiceFormats[i]);
      String newPatternPart = String.valueOf(newChoiceFormats[i]);

      MessageFormat originalNestedMessageFormat = new MessageFormat(originalPatternPart, Locale.ENGLISH);
      MessageFormat newNestedMessageFormat = new MessageFormat(newPatternPart, Locale.ENGLISH);

      if (!hasSameMessageFormatStructure(originalNestedMessageFormat, newNestedMessageFormat)) {
        return false;
      }
    }
    return true;
  }

  private static int countArgumentNumbers(String pattern) {
    try {
      MessageFormat messageFormat = new MessageFormat(pattern, Locale.ENGLISH);
      return messageFormat.getFormatsByArgumentIndex().length;
    } catch (Exception e) {
      return -1;
    }
  }

  private static boolean isEdited(SavedCms cms) {
    return !Objects.equals(cms.getNewContent(), cms.getOriginalContent());
  }
}
