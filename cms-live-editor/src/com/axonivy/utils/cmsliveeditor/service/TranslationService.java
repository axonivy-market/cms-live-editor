package com.axonivy.utils.cmsliveeditor.service;

import java.util.List;

import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.deepl.api.v2.client.SourceLanguage;
import com.deepl.api.v2.client.TargetLanguage;

import ch.ivyteam.ivy.environment.Ivy;
import deepl.translate.Options;

public class TranslationService {

  public static String translate(String text, String sourceLang, String targetLang) {
    TargetLanguage targetLanguageEnum = TargetLanguage.valueOf(targetLang);
    SourceLanguage sourceLanguageEnum = SourceLanguage.valueOf(sourceLang);
    Options translateOptions = new Options();
    translateOptions.setTargetLang(targetLanguageEnum);
    translateOptions.setSourceLang(sourceLanguageEnum);
    return DeepLTranslationService.translate(text, translateOptions);

  }

  public static void batchTranslate(List<Cms> entries, String sourceLang) {
    if (entries == null || entries.isEmpty() || sourceLang == null || sourceLang.isBlank()) {
      return;
    }

    String src = sourceLang.trim().toLowerCase(); // e.g. "en"
    Options options = new Options();

    try {
      options.setSourceLang(SourceLanguage.valueOf(src.toUpperCase()));
    } catch (Exception e) {
      Ivy.log().warn("Unsupported source language: " + sourceLang, e);
      return;
    }

    for (Cms cms : entries) {
      if (cms == null || cms.isFile() || cms.getContents() == null || cms.getContents().isEmpty()) {
        Ivy.log().error("Skip --");
        continue;
      }

      // find source text
      String sourceText = cms.getContents().stream().filter(c -> c.getLocale() != null && src.equalsIgnoreCase(c.getLocale().getLanguage()))
          .map(CmsContent::getContent).filter(t -> t != null && !t.isBlank()).findFirst().orElse(null);

      if (sourceText == null) {
        Ivy.log().error("sourceText == null");
        continue;
      }

      for (CmsContent content : cms.getContents()) {
        if (content == null || content.getLocale() == null) {
          Ivy.log().error("content == null || content.getLocale() == null");
          continue;
        }

        String target = content.getLocale().getLanguage().toUpperCase();

        // skip source language
        if (src.equalsIgnoreCase(content.getLocale().getLanguage())) {
          Ivy.log().error("src.equalsIgnoreCase(content.getLocale().getLanguage())");
          continue;
        }

        // only translate empty content
//        if (content.getContent() != null && !content.getContent().isBlank()) {
//          Ivy.log().error("content.getContent() != null && !content.getContent().isBlank()");
//          continue;
//        }

        try {
          options.setTargetLang(TargetLanguage.valueOf(target));
          String translated = DeepLTranslationService.translate(sourceText, options);
          content.setTranslatedContent(translated);
          content.setTranslated(true);
        } catch (Exception e) {
          Ivy.log().warn("Translate failed for target=" + target + ", source=" + src, e);
        }
      }
    }
  }
}
