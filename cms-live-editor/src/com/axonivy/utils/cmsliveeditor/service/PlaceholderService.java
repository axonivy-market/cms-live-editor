package com.axonivy.utils.cmsliveeditor.service;

import java.text.ChoiceFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.axonivy.utils.cmsliveeditor.constants.CommonConstants;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.SavedCms;

public class PlaceholderService {

  private static final Pattern ARGUMENT_INDEX_PATTERN = Pattern.compile("\\{(\\d+)");
  private static PlaceholderService instance;

  public static PlaceholderService getInstance() {
    if (instance == null) {
      instance = new PlaceholderService();
    }
    return instance;
  }

  public List<Integer> findInvalidLanguageIndices(Cms selectedCms, Map<String, SavedCms> savedLocales) {
    if (selectedCms == null || !selectedCms.hasTextContents()) {
      return new ArrayList<>();
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

    if (cmsLocales == null || cmsLocales.isEmpty()) {
      return new ArrayList<>();
    }

    List<String> errorLocales = validateLocales(cmsLocales);
    if (errorLocales.isEmpty()) {
      return new ArrayList<>();
    }

    return mapLocalesToIndices(selectedCms, errorLocales);
  }

  public List<Integer> mapLocalesToIndices(Cms selectedCms, List<String> invalidLocales) {
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
    String originalValue = cmsLocales.values().stream()
        .map(SavedCms::getOriginalContent)
        .filter(value -> value != null && !value.isBlank())
        .max(Comparator.comparingInt(PlaceholderService::countArgumentNumbers))
        .orElse(null);
    
    List<Map.Entry<String, SavedCms>> editedLocales = cmsLocales.entrySet().stream()
        .filter(localeEntry -> isEdited(localeEntry.getValue())).toList();

    // CASE 1: original has NO placeholders → compare among edited locales
    if (originalValue == null || countArgumentNumbers(originalValue) <= 0) {
      return validateAmongEditedLocales(editedLocales, cmsLocales.entrySet());
    }

    // CASE 2: original has placeholders
    return findIncompatibleLocales(originalValue, editedLocales);
  }

  private List<String> validateAmongEditedLocales(List<Map.Entry<String, SavedCms>> editedLocales,
      Set<Entry<String, SavedCms>> allLocales) {
    if (editedLocales.isEmpty()) {
      return new ArrayList<>();
    }

    String referenceValue = editedLocales.stream()
        .map(entry -> entry.getValue().getNewContent())
        .filter(value -> value != null && !value.isBlank())
        .max(Comparator.comparingInt(PlaceholderService::countArgumentNumbers))
        .orElse(null);

    // No edited locale has placeholders → nothing to validate
    if (referenceValue == null || countArgumentNumbers(referenceValue) <= 0) {
      return new ArrayList<>();
    }

    // Validate ALL locales against the reference so unedited locales
    // missing the newly introduced placeholders are flagged too
    return findIncompatibleLocales(referenceValue, new ArrayList<>(allLocales));
  }

  private List<String> findIncompatibleLocales(String originalValue, List<Map.Entry<String, SavedCms>> editedLocales) {
    return editedLocales.stream()
        .filter(localeEntry -> !areMessagePatternsCompatible(originalValue, localeEntry.getValue().getNewContent()))
        .map(Map.Entry::getKey)
        .toList();
  }

  private List<Integer> extractArgumentIndices(String pattern) {
    List<Integer> indices = new ArrayList<>();

    Matcher matcher = ARGUMENT_INDEX_PATTERN.matcher(pattern);
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
      List<Integer> originalArgs = extractArgumentIndices(originalValue).stream().sorted().toList();
      List<Integer> newArgs = extractArgumentIndices(newValue).stream().sorted().toList();
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
    if (originalFormat == null || newFormat == null) {
      return originalFormat == newFormat;
    }

    if (!originalFormat.getClass().equals(newFormat.getClass())) {
      return false;
    }

    return switch (originalFormat) {
      case ChoiceFormat originalChoiceFormat -> areChoiceFormatsEquivalent(originalChoiceFormat,
          (ChoiceFormat) newFormat);
      case DecimalFormat originalDecimalFormat -> Objects.equals(originalDecimalFormat.toPattern(),
          ((DecimalFormat) newFormat).toPattern());
      case SimpleDateFormat originalDateFormat -> Objects.equals(originalDateFormat.toPattern(),
          ((SimpleDateFormat) newFormat).toPattern());
      default -> originalFormat.equals(newFormat);
    };
  }

  private boolean areChoiceFormatsEquivalent(ChoiceFormat originalChoiceFormat, ChoiceFormat newChoiceFormat) {
    if (!Arrays.equals(originalChoiceFormat.getLimits(), newChoiceFormat.getLimits())) {
      return false;
    }

    Object[] originalFormats = originalChoiceFormat.getFormats();
    Object[] newFormats = newChoiceFormat.getFormats();

    for (int i = 0; i < originalFormats.length; i++) {
      String originalPatternPart = String.valueOf(originalFormats[i]);
      String newPatternPart = String.valueOf(newFormats[i]);

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
