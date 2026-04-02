package com.axonivy.utils.cmsliveeditor.service;

import java.text.Format;
import java.text.MessageFormat;
import java.util.*;
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

  public static List<String> validateLocales(Map<String, SavedCms> cmsLocales) {
    if (cmsLocales == null || cmsLocales.isEmpty()) {
      return new ArrayList<>();
    }

    String referenceOriginal = cmsLocales.values().stream()
        .map(SavedCms::getOriginalContent)
        .filter(s -> s != null && !s.isBlank())
        .max(Comparator.comparingInt(PlaceholderService::countArguments))
        .orElse(null);

    if (referenceOriginal == null || referenceOriginal.isBlank()) {
      return new ArrayList<>();
    }

    return cmsLocales.entrySet().stream()
        .filter(e -> isEdited(e.getValue()))
        .filter(e -> !areFormatCompatible(referenceOriginal,
            e.getValue().getNewContent()))
        .map(Map.Entry::getKey)
        .toList();
  }

  public static boolean areFormatCompatible(String template, String message) {
      return areFormatCompatibleStrict(template, message) && areFormatCompatibleRelax(template, message);
  }

  public static boolean areFormatCompatibleRelax(String template, String message) {
    try {
      MessageFormat mf1 = new MessageFormat(template, Locale.ENGLISH);
      MessageFormat mf2 = new MessageFormat(message, Locale.ENGLISH);

      Format[] f1 = mf1.getFormatsByArgumentIndex();
      Format[] f2 = mf2.getFormatsByArgumentIndex();

      if (f1.length != f2.length) {
        return false;
      }

      for (int i = 0; i < f1.length; i++) {
        Format format1 = f1[i];
        Format format2 = f2[i];

        // both null = ok
        if (format1 == null && format2 == null) {
          continue;
        }

        // one null = mismatch
        if (format1 == null || format2 == null) {
          return false;
        }

        // type mismatch
        if (!sameFormatType(format1, format2)) {
          return false;
        }
      }

      return true;

    } catch (Exception e) {
      return false;
    }
  }

  public static boolean areFormatCompatibleStrict(String template, String message) {
    try {
      MessageFormat mf1 = new MessageFormat(template, Locale.ENGLISH);
      MessageFormat mf2 = new MessageFormat(message, Locale.ENGLISH);

      Format[] f1 = mf1.getFormats();
      Format[] f2 = mf2.getFormats();

      if (f1.length != f2.length) {
        return false;
      }

      for (int i = 0; i < f1.length; i++) {
        Format format1 = f1[i];
        Format format2 = f2[i];

        if (format1 == null && format2 == null) continue;

        if (format1 == null || format2 == null) {
          return false;
        }

        if (!sameFormatType(format1, format2)) {
          return false;
        }
      }

      List<Integer> idx1 = extractArgumentIndices(template);
      List<Integer> idx2 = extractArgumentIndices(message);

      if (!idx1.equals(idx2)) {
        return false;
      }

      return true;

    } catch (Exception e) {
      return false;
    }
  }

  private static List<Integer> extractArgumentIndices(String pattern) {
    List<Integer> indices = new ArrayList<>();

    // Regex match {0}, {1,number}, {2,date,short}
    String regex = "\\{\\s*(\\d+)\\s*(?:,.*?)?\\}";
    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(pattern);

    while (matcher.find()) {
      indices.add(Integer.parseInt(matcher.group(1)));
    }

    return indices;
  }

  private static boolean sameFormatType(Format f1, Format f2) {
    if (f1 == null && f2 == null) return true;
    if (f1 == null || f2 == null) return false;

    if (!f1.getClass().equals(f2.getClass())) {
      return false;
    }

    if (f1 instanceof java.text.DecimalFormat d1 && f2 instanceof java.text.DecimalFormat d2) {
      return Objects.equals(d1.toPattern(), d2.toPattern());
    }

    if (f1 instanceof java.text.SimpleDateFormat d1 && f2 instanceof java.text.SimpleDateFormat d2) {
      return Objects.equals(d1.toPattern(), d2.toPattern());
    }

    return f1.equals(f2);
  }

  private static int countArguments(String pattern) {
    try {
      MessageFormat mf = new MessageFormat(pattern, Locale.ENGLISH);
      return mf.getFormatsByArgumentIndex().length;
    } catch (Exception e) {
      return -1;
    }
  }

  private static boolean isEdited(SavedCms cms) {
    return !Objects.equals(cms.getNewContent(), cms.getOriginalContent());
  }
}
