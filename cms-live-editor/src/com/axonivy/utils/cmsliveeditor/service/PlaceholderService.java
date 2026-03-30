package com.axonivy.utils.cmsliveeditor.service;

import java.text.ChoiceFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

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
    if (selectedCms == null || CollectionUtils.isEmpty(selectedCms.getContents())) {
      return new ArrayList<>();
    }

    Set<String> normalizedLocaleTags = invalidLocales.stream().filter(Objects::nonNull)
        .map(tag -> Locale
            .forLanguageTag(tag.replace(CommonConstants.UNDERSCORE_CHARACTER, CommonConstants.HYPHEN_CHARACTER))
            .toLanguageTag())
        .collect(Collectors.toSet());

    return selectedCms.getContents().stream()
        .filter(cms -> cms.getLocale() != null && normalizedLocaleTags.contains(cms.getLocale().toLanguageTag()))
        .map(CmsContent::getIndex).distinct().sorted().toList();
  }

  public static boolean areFormatCompatible(String template, String message) {
    try {
      MessageFormat mf1 = new MessageFormat(template, Locale.ENGLISH);
      MessageFormat mf2 = new MessageFormat(message, Locale.ENGLISH);

      // Key fix: compare by argument index (not occurrence order)
      Format[] f1 = mf1.getFormatsByArgumentIndex();
      Format[] f2 = mf2.getFormatsByArgumentIndex();

      // Different number of argument indices
      if (f1.length != f2.length) {
        return false;
      }

      for (int i = 0; i < f1.length; i++) {
        String t1 = getFormatType(f1[i]);
        String t2 = getFormatType(f2[i]);

        if (!Objects.equals(t1, t2)) {
          return false;
        }
      }

      return true;

    } catch (Exception e) {
      // Invalid MessageFormat pattern
      return false;
    }
  }

  /**
   * Returns a detailed format type string to distinguish: - number vs date vs choice - integer vs currency vs percent -
   * date patterns
   */
  private static String getFormatType(Format format) {
    if (format == null) {
      return "String";
    }

    // ChoiceFormat (exact pattern matters)
    if (format instanceof ChoiceFormat cf) {
      return "ChoiceFormat:" + cf.toPattern();
    }

    // NumberFormat (distinguish integer/currency/percent)
    if (format instanceof DecimalFormat df) {
      return "NumberFormat:" + df.toPattern();
    }

    // DateFormat (distinguish patterns)
    if (format instanceof SimpleDateFormat sdf) {
      return "DateFormat:" + sdf.toPattern();
    }

    // Fallback (rare cases)
    return format.getClass().getName();
  }

  public static List<String> validateLocales(Map<String, SavedCms> cmsLocales) {
    if (cmsLocales == null || cmsLocales.isEmpty()) {
      return Collections.emptyList();
    }

    // Pick reference with most arguments (using MessageFormat instead of regex)
    String referenceOriginal =
        cmsLocales.values().stream().map(SavedCms::getOriginalContent).filter(s -> s != null && !s.isBlank())
            .max(Comparator.comparingInt(PlaceholderService::countArguments)).orElse(null);

    if (referenceOriginal == null || referenceOriginal.isBlank()) {
      return Collections.emptyList();
    }

    return cmsLocales.entrySet().stream().filter(e -> isEdited(e.getValue()))
        .filter(e -> !areFormatCompatible(referenceOriginal, e.getValue().getNewContent())).map(Map.Entry::getKey)
        .toList();
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
