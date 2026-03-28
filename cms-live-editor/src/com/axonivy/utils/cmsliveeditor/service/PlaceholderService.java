package com.axonivy.utils.cmsliveeditor.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.cmsliveeditor.constants.CommonConstants;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.Placeholder;
import com.axonivy.utils.cmsliveeditor.model.SavedCms;

public class PlaceholderService {
  private static final PlaceholderService INSTANCE = new PlaceholderService();

  public static PlaceholderService getInstance() {
    return INSTANCE;
  }

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\d+)(?:,([^,}]+)(?:,([^}]+))?)?\\}");
  private static final String CHOICE_FORMAT = "choice";

  public List<Integer> findInvalidLocaleIndices(List<String> invalidLocales, Cms selectedCms) {
    if (selectedCms == null || CollectionUtils.isEmpty(selectedCms.getContents())) {
      return Collections.emptyList();
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
    // Placeholder structure validation
    List<String> placeholderErrors = findMismatchLocales(cmsLocales);
    if (!CollectionUtils.isEmpty(placeholderErrors)) {
      return placeholderErrors;
    }

    // MessageFormat validation
    return cmsLocales.entrySet().stream()
        .filter(e -> isEdited(e.getValue()))
        .filter(e -> validateMessageFormat(e.getValue().getNewContent()) != null)
        .map(Map.Entry::getKey)
        .toList();
  }

  public List<String> findMismatchLocales(Map<String, SavedCms> cmsLocales) {
    if (cmsLocales == null || cmsLocales.isEmpty()) {
      return Collections.emptyList();
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
        .orElse(Collections.emptyList());
  }

  private List<String> findMismatchWithOriginalPlaceholders(Map<String, SavedCms> cmsLocales,
      List<Placeholder> referencePlaceholders) {
    return cmsLocales.entrySet().stream()
        .filter(e -> isEdited(e.getValue()))
        .filter(e -> !hasSamePlaceholderStructure(referencePlaceholders,
            extractPlaceholders(e.getValue().getNewContent())))
        .map(Map.Entry::getKey)
        .toList();
  }

  private List<String> findMismatchWithoutOriginalPlaceholders(Map<String, SavedCms> cmsLocales) {
    List<Map.Entry<String, SavedCms>> entries = new ArrayList<>(cmsLocales.entrySet());

    List<List<Placeholder>> allNewPlaceholders = entries.stream()
        .map(e -> extractPlaceholders(e.getValue().getNewContent()))
        .toList();

    if (allNewPlaceholders.isEmpty()) {
      return Collections.emptyList();
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
      return Collections.emptyList();
    }

    // All new contents have placeholders -> All locales must share the same placeholder structure
    return entries.stream()
        .filter(e -> !hasSamePlaceholderStructure(
            allNewPlaceholders.get(0),
            extractPlaceholders(e.getValue().getNewContent())))
        .map(Map.Entry::getKey)
        .toList();
  }

  public List<Placeholder> extractPlaceholders(String content) {
    if (StringUtils.isBlank(content)) {
      return Collections.emptyList();
    }

    Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
    List<Placeholder> placeholders = new ArrayList<>();

    while (matcher.find()) {
      placeholders.add(new Placeholder(Integer.parseInt(matcher.group(1)), 
          matcher.group(2) == null ? null : matcher.group(2).trim(),
          matcher.group(3) == null ? null : matcher.group(3).trim()));
    }

    placeholders.sort(Comparator.comparingInt(Placeholder::getIndex));
    return placeholders;
  }

  public boolean hasSamePlaceholderStructure(List<Placeholder> originalPlaceholders,
      List<Placeholder> newPlaceholders) {
    if (originalPlaceholders.size() != newPlaceholders.size())
      return false;

    for (int i = 0; i < originalPlaceholders.size(); i++) {
      Placeholder originalPlaceholder = originalPlaceholders.get(i);
      Placeholder newPlaceholder = newPlaceholders.get(i);

      if (originalPlaceholder.getIndex() != newPlaceholder.getIndex()) {
        return false;
      }

      if (CHOICE_FORMAT.equals(originalPlaceholder.getFormat()) || CHOICE_FORMAT.equals(newPlaceholder.getFormat())) {
        if (!Objects.equals(originalPlaceholder.getFormat(), newPlaceholder.getFormat())
            || !isChoiceStyleEqual(originalPlaceholder.getStyle(), newPlaceholder.getStyle())) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean isChoiceStyleEqual(String originalStyle, String newStyle) {
    if (Objects.equals(originalStyle, newStyle)) {
      return true;
    }
    if (originalStyle == null || newStyle == null) {
      return false;
    }
    List<String> originalStyleParts =
        Arrays.stream(originalStyle.split(CommonConstants.VERTICAL_LINE_CHARACTER)).map(String::trim).sorted().toList();
    List<String> newStyleParts =
        Arrays.stream(newStyle.split(CommonConstants.VERTICAL_LINE_CHARACTER)).map(String::trim).sorted().toList();

    return originalStyleParts.equals(newStyleParts);
  }

  private boolean isEdited(SavedCms cms) {
    return !Objects.equals(cms.getNewContent(), cms.getOriginalContent());
  }

  private String validateMessageFormat(String pattern) {
    try {
      new java.text.MessageFormat(pattern, java.util.Locale.ENGLISH);
      return null;
    } catch (IllegalArgumentException e) {
      return e.getMessage();
    }
  }
}
