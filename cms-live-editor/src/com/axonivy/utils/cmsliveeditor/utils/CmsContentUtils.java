package com.axonivy.utils.cmsliveeditor.utils;

import static com.axonivy.utils.cmsliveeditor.constants.CommonConstants.HYPHEN;
import static com.axonivy.utils.cmsliveeditor.constants.CommonConstants.UNDER_SCORE;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;

public class CmsContentUtils {

  private CmsContentUtils() {
  }

  public static List<Locale> getLocalListFromCMS(List<Cms> cmsList) {
    if (CollectionUtils.isEmpty(cmsList)) {
      return List.of();
    }
    return cmsList.stream().flatMap(c -> c.getContents().stream()).map(CmsContent::getLocale).filter(Objects::nonNull).distinct()
        .sorted(Comparator.comparing(l -> l.getDisplayLanguage(Locale.ENGLISH))).toList();
  }

  public static CmsContent getCmsContentByLocale(Cms cms, String localeTag) {
    if (cms == null || cms.getContents() == null || StringUtils.isBlank(localeTag)) {
      return null;
    }
    String tag = localeTag.trim().replace(UNDER_SCORE, HYPHEN);
    return cms.getContents().stream().filter(c -> c.getLocale() != null && tag.equalsIgnoreCase(c.getLocale().toLanguageTag())).findFirst()
        .orElse(null);
  }

  public static String getContentByLocale(Cms cms, String localeTag) {
    CmsContent content = getCmsContentByLocale(cms, localeTag);
    return content != null ? content.getContent() : Strings.EMPTY;
  }

  public static List<Cms> getTranslatedCms(List<Cms> cmsList) {
    if (CollectionUtils.isEmpty(cmsList)) {
      return List.of();
    }
    return cmsList.stream()
        .filter(cms -> cms.getContents() != null && cms.getContents().stream().anyMatch(c -> StringUtils.isNotBlank(c.getTranslatedContent())))
        .toList();
  }

  public static List<Locale> getExcludedLocales(List<Locale> locales, String excludedLocale) {
    if (CollectionUtils.isEmpty(locales) || StringUtils.isBlank(excludedLocale)) {
      return List.of();
    }
    String selectedTag = excludedLocale.trim().replace(UNDER_SCORE, HYPHEN);
    return locales.stream().filter(l -> l != null && !selectedTag.equalsIgnoreCase(l.toLanguageTag())).toList();
  }

}
