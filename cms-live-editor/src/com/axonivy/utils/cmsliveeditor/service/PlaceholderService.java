package com.axonivy.utils.cmsliveeditor.service;

import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.cmsliveeditor.constants.CommonConstants;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.Placeholder;
import com.axonivy.utils.cmsliveeditor.model.SavedCms;

import ch.ivyteam.ivy.environment.Ivy;

public class PlaceholderService {
  private static PlaceholderService instance;

  public static PlaceholderService getInstance() {
    if (instance == null) {
      instance = new PlaceholderService();
    }
    return instance;
  }

  /**
   * Regex to match MessageFormat-style placeholders.
   *
   * Supported formats:
   *   {0}
   *   {1,number}
   *   {2,choice,0#no|1#yes}
   *
   * Groups:
   *   1 -> index (required)
   *   2 -> format (optional, e.g. number, choice)
   *   3 -> style (optional, e.g. currency, 0#no|1#yes)
   */
  private static final Pattern PLACEHOLDER_PATTERN =
      Pattern.compile("\\{(\\d+)(?:,\\s*([^,{}]+)(?:,\\s*([^{}]+))?)?\\}");
  private static final String CHOICE_FORMAT = "choice";

  /**
   * Find indices of CMS contents whose locale(s) are invalid.
   * Locale strings are normalized before comparison.
   */
  public List<Integer> findInvalidLocaleIndices(List<String> invalidLocales, Cms selectedCms) {
    if (selectedCms == null || selectedCms.getContents() == null || selectedCms.getContents().isEmpty()) {
      return new ArrayList<>();
    }

    Set<String> normalizedLocaleTags = invalidLocales.stream().filter(Objects::nonNull)
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
    // 1. Validate placeholder structure
    List<String> placeholderErrors = findMismatchLocales(cmsLocales);
    if (placeholderErrors != null && !placeholderErrors.isEmpty()) {
      return placeholderErrors;
    }

    // 2. Validate MessageFormat syntax
    return cmsLocales.entrySet().stream()
        .filter(entry -> {
          SavedCms cms = entry.getValue();
          return isEdited(cms) && validateMessageFormat(cms.getNewContent()) != null;
        })
        .map(Map.Entry::getKey)
        .toList();
  }

  public List<String> findMismatchLocales(Map<String, SavedCms> cmsLocales) {
    if (cmsLocales == null || cmsLocales.isEmpty()) {
      return new ArrayList<>();
    }

    List<Placeholder> originalPlaceholders = extractOriginalPlaceholders(cmsLocales);

    /** CASE 1: 
     *  Original has placeholder(s) 
     *  -> All edited locales must follow the same placeholder structure as the origin
     */
    if (!originalPlaceholders.isEmpty()) {
      return findMismatchWithOriginalPlaceholders(cmsLocales, originalPlaceholders);
    }

    /** CASE 2: 
     *  Original has no placeholder(s) 
     *  -> All edited locales must be consistent of placeholder structure
     */
    return findMismatchWithoutOriginalPlaceholders(cmsLocales);
  }

  private List<Placeholder> extractOriginalPlaceholders(Map<String, SavedCms> cmsLocales) {
    return cmsLocales.values().stream()
        .map(SavedCms::getOriginalContent)
        .filter(StringUtils::isNotBlank)
        .map(this::extractPlaceholders)
        .max(Comparator.comparingInt(List::size))
        .orElse(List.of());
  }

  private List<String> findMismatchWithOriginalPlaceholders(Map<String, SavedCms> cmsLocales,
      List<Placeholder> referencePlaceholders) {
    return cmsLocales.entrySet().stream()
        .filter(localeEntry -> isEdited(localeEntry.getValue()))
        .filter(localeEntry -> !hasSamePlaceholderStructure(referencePlaceholders,
            extractPlaceholders(localeEntry.getValue().getNewContent())))
        .map(Map.Entry::getKey)
        .toList();
  }

  private List<String> findMismatchWithoutOriginalPlaceholders(Map<String, SavedCms> cmsLocales) {
    List<Map.Entry<String, SavedCms>> entries = new ArrayList<>(cmsLocales.entrySet());

    List<List<Placeholder>> allNewPlaceholders = entries.stream()
        .map(localeEntry -> extractPlaceholders(localeEntry.getValue().getNewContent()))
        .toList();

    if (allNewPlaceholders.isEmpty()) {
      return new ArrayList<>();
    }

    // Count how many locales actually define placeholders
    long nonEmptyCount = allNewPlaceholders.stream()
        .filter(list -> !list.isEmpty())
        .count();

    // Mixed placeholder presence (some locales introduce placeholders while others do not) -> Invalid
    boolean hasMixedPresence = nonEmptyCount > 0 && nonEmptyCount < allNewPlaceholders.size();
    boolean hasTooFewWithPlaceholders = nonEmptyCount == 1;
    if (hasMixedPresence || hasTooFewWithPlaceholders) {
      return new ArrayList<>(cmsLocales.keySet());
    }

    // No new placeholders -> Valid
    if (nonEmptyCount == 0) {
      return List.of();
    }

    // All new contents have placeholders -> All locales must share the same placeholder structure
    return entries.stream()
        .filter(localeEntry -> !hasSamePlaceholderStructure(
            allNewPlaceholders.get(0),
            extractPlaceholders(localeEntry.getValue().getNewContent())))
        .map(Map.Entry::getKey)
        .toList();
  }

  public List<Placeholder> extractPlaceholders(String content) {
    if (StringUtils.isBlank(content)) {
      return new ArrayList<>();
    }

    Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
    List<Placeholder> placeholders = new ArrayList<>();

    while (matcher.find()) {
      int index = Integer.parseInt(matcher.group(1));
      String format = trimIfNotNull(matcher.group(2));
      String style = trimIfNotNull(matcher.group(3));

      if (CHOICE_FORMAT.equals(format) && style != null) {
        try {
          new ChoiceFormat(style);
        } catch (IllegalArgumentException e) {
          Ivy.log().error("#Invalid ChoiceFormat style detected",  e);
        }
      }

      placeholders.add(new Placeholder(index, format, style));
    }

    placeholders.sort(Comparator.comparingInt(Placeholder::getIndex));
    return placeholders;
  }

  /**
   * Compare placeholder structures between two contents.
   *
   * Rules:
   * - Same size
   * - Same index
   * - Same format
   * - Same style
   *     For "choice": use ChoiceFormat semantic comparison
   */
  public boolean hasSamePlaceholderStructure(List<Placeholder> originalPlaceholders,
      List<Placeholder> newPlaceholders) {

    if (originalPlaceholders.size() != newPlaceholders.size()) {
      return false;
    }

    for (int i = 0; i < originalPlaceholders.size(); i++) {
      Placeholder originalValue = originalPlaceholders.get(i);
      Placeholder newValue = newPlaceholders.get(i);

      if (originalValue.getIndex() != newValue.getIndex()) {
        return false;
      }

      if (!Objects.equals(originalValue.getFormat(), newValue.getFormat())) {
        return false;
      }

      if (CHOICE_FORMAT.equals(originalValue.getFormat())) {
        if (!isChoiceEqual(originalValue.getStyle(), newValue.getStyle())) {
          return false;
        }
      } else {
        if (!Objects.equals(originalValue.getStyle(), newValue.getStyle())) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Compare two ChoiceFormat styles semantically (not just string).
   */
  private boolean isChoiceEqual(String orginalStyle, String newStyle) {
    if (Objects.equals(orginalStyle, newStyle)) {
      return true;
    }
    if (orginalStyle == null || newStyle == null) {
      return false;
    }

    try {
      ChoiceFormat originalChoiceFormat = new ChoiceFormat(orginalStyle);
      ChoiceFormat newChoiceFormat = new ChoiceFormat(newStyle);

      return Arrays.equals(originalChoiceFormat.getLimits(), newChoiceFormat.getLimits())
          && Arrays.equals(originalChoiceFormat.getFormats(), newChoiceFormat.getFormats());

    } catch (IllegalArgumentException e) {
      Ivy.log().error("#Failed to compare ChoiceFormat", e);
      return false;
    }
  }

  private boolean isEdited(SavedCms cms) {
    return !Objects.equals(cms.getNewContent(), cms.getOriginalContent());
  }

  private String validateMessageFormat(String pattern) {
    try {
      new MessageFormat(pattern, Locale.ENGLISH);
      return null;
    } catch (IllegalArgumentException e) {
      return e.getMessage();
    }
  }

  private String trimIfNotNull(String value) {
    return value == null ? null : value.trim();
  }

}
