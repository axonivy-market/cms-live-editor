package com.axonivy.utils.cmsliveeditor.service;

import java.util.List;
import java.util.Locale;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.utils.CmsContentUtils;
import com.deepl.api.v2.client.SourceLanguage;
import com.deepl.api.v2.client.TargetLanguage;

import deepl.translate.Options;

public class TranslationService {

  public static String translate(String text, String srcLang, String targetLang) {
    Options translateOptions = new Options();
    translateOptions.setTargetLang(TargetLanguage.fromValue(targetLang));
    translateOptions.setSourceLang(SourceLanguage.fromValue(srcLang));
    return DeepLTranslationService.translate(text, translateOptions);
  }

  public static void batchTranslate(List<Cms> entries, String srcLang, String targetLang) {
    if (CollectionUtils.isEmpty(entries) || StringUtils.isAnyBlank(srcLang, targetLang)) {
      return;
    }

    Options options = new Options();
    options.setSourceLang(SourceLanguage.fromValue(srcLang.toUpperCase(Locale.ENGLISH)));

    for (Cms cms : entries) {
      if (cms == null || cms.isFile() || CollectionUtils.isEmpty(cms.getContents())) {
        continue;
      }

      // find source text (first non-blank content matching source language)
      String sourceText = CmsContentUtils.getContentByLocale(cms, srcLang);

      if (StringUtils.isBlank(sourceText)) {
        continue;
      }

      // translate once per targetLang for this CMS
      String translatedForTarget = null;
      String targetLower = targetLang.trim().toLowerCase(Locale.ENGLISH);

      // if target equals source, skip
      if (srcLang.equalsIgnoreCase(targetLower)) {
        continue;
      }

      options.setTargetLang(TargetLanguage.fromValue(targetLower.toUpperCase(Locale.ENGLISH)));
      translatedForTarget = DeepLTranslationService.translate(sourceText, options);
      if (translatedForTarget == null) {
        continue;
      }
      CmsContent oldContent = CmsContentUtils.getCmsContentByLocale(cms, targetLang);
      if (oldContent == null) {
        continue;
      }
      oldContent.setTranslatedContent(translatedForTarget);
    }
  }
}
