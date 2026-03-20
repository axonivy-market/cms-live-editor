package com.axonivy.utils.cmsliveeditor.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;

public class CmsContentUtils {

  private CmsContentUtils() {
  }

  public static List<Locale> getLocalListFromCMS(List<Cms> cmsList) {
    return cmsList.stream().flatMap(c -> c.getContents().stream()).map(CmsContent::getLocale).filter(Objects::nonNull).distinct()
        .sorted(Comparator.comparing(l -> l.getDisplayLanguage(Locale.ENGLISH))).toList();
  }

  public static CmsContent getCmsContentByLocale(Cms cms, String localeTag) {
    if (cms == null || cms.getContents() == null || localeTag == null || localeTag.isBlank()) {
      return null;
    }
    String tag = localeTag.trim().replace('_', '-');
    return cms.getContents().stream().filter(c -> c.getLocale() != null && tag.equalsIgnoreCase(c.getLocale().toLanguageTag())).findFirst()
        .orElse(null);
  }

  public static String getContentByLocale(Cms cms, String localeTag) {
    CmsContent content = getCmsContentByLocale(cms, localeTag);
    return content != null ? content.getContent() : Strings.EMPTY;
  }

  public static List<Cms> getTranslatedCms(List<Cms> cmsList) {
    return cmsList.stream()
        .filter(cms -> cms.getContents() != null && cms.getContents().stream().anyMatch(c -> StringUtils.isNotBlank(c.getTranslatedContent())))
        .toList();
  }

  public static List<Locale> getExcludedLocales(List<Locale> locales, String excludedLocale) {
    if (locales == null || excludedLocale == null) {
      return List.of();
    }
    String selectedTag = excludedLocale.trim().replace('_', '-');
    return locales.stream().filter(l -> l != null && !selectedTag.equalsIgnoreCase(l.toLanguageTag())).toList();
  }

}
