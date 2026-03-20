package com.axonivy.utils.cmsliveeditor.service;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.utils.CmsContentUtils;
import com.deepl.api.v2.client.SourceLanguage;
import com.deepl.api.v2.client.TargetLanguage;

import ch.ivyteam.ivy.environment.Ivy;
import deepl.translate.Options;

public class TranslationService {

  public static String translate(String text, String srcLang, String targetLang) {
    TargetLanguage targetLanguageEnum = TargetLanguage.valueOf(targetLang);
    SourceLanguage sourceLanguageEnum = SourceLanguage.valueOf(srcLang);
    Options translateOptions = new Options();
    translateOptions.setTargetLang(targetLanguageEnum);
    translateOptions.setSourceLang(sourceLanguageEnum);
    return DeepLTranslationService.translate(text, translateOptions);

  }

  public static void batchTranslate(List<Cms> entries, String srcLang, String targetLang) {
    if (entries == null || entries.isEmpty() || StringUtils.isAnyBlank(srcLang, targetLang)) {
      return;
    }

    Options options = new Options();

    try {
      options.setSourceLang(SourceLanguage.valueOf(srcLang.toUpperCase(Locale.ENGLISH)));
    } catch (Exception e) {
      Ivy.log().warn("Unsupported source language: " + srcLang, e);
      return;
    }

    for (Cms cms : entries) {
      if (cms == null || cms.isFile() || cms.getContents() == null || cms.getContents().isEmpty()) {
        Ivy.log().debug("Skipping CMS (null/file/empty): " + (cms == null ? "null" : cms.getUri()));
        continue;
      }

      // find source text (first non-blank content matching source language)
      String sourceText = CmsContentUtils.getContentByLocale(cms, srcLang);

      if (StringUtils.isBlank(sourceText)) {
        Ivy.log().debug("No source text found for CMS: " + cms.getUri() + " sourceLang=" + srcLang);
        continue;
      }

      // translate once per targetLang for this CMS
      String translatedForTarget = null;
      String targetLower = targetLang.trim().toLowerCase(Locale.ENGLISH);

      // if target equals source, skip
      if (srcLang.equalsIgnoreCase(targetLower)) {
        Ivy.log().debug("Skipping translate because source == target for CMS: " + cms.getUri());
        continue;
      }

      try {
        options.setTargetLang(TargetLanguage.valueOf(targetLower.toUpperCase(Locale.ENGLISH)));
        translatedForTarget = DeepLTranslationService.translate(sourceText, options);
      } catch (Exception e) {
        Ivy.log().warn("Translate failed for CMS: " + cms.getUri() + " target=" + targetLower + ", source=" + srcLang, e);
        translatedForTarget = null;
        continue;
      }

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
