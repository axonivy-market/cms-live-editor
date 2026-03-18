package com.axonivy.utils.cmsliveeditor.service;

import java.util.ArrayList;
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

  public static List<Cms> batchTranslate(List<Cms> entries, String sourceLang) {
    Ivy.log().error("String: " + sourceLang);
    List<Cms> results = new ArrayList<>();

    SourceLanguage sourceLanguageEnum = SourceLanguage.valueOf(sourceLang.toUpperCase());

    Options baseOptions = new Options();
    baseOptions.setSourceLang(sourceLanguageEnum);
    for (Cms cms : entries) {
      Cms cmsCopy = deepCopyCms(cms);
      if (cmsCopy.getContents() == null || cmsCopy.getContents().isEmpty() || cmsCopy.isFile()) {
        results.add(cmsCopy);
        continue;
      }

      // 1. Find source text
      String sourceText = null;
      for (CmsContent content : cmsCopy.getContents()) {
        if (sourceLang.equals(content.getLocale().getLanguage())) {
          sourceText = content.getContent();
          break;
        }
      }

      if (sourceText == null || sourceText.isBlank()) {
        results.add(cmsCopy);
        continue;
      }

      // 2. Translate each target
      for (CmsContent content : cmsCopy.getContents()) {

        if (sourceLang.equals(content.getLocale().getLanguage())) {
          continue;
        }

        if (content.getContent() != null && !content.getContent().isBlank()) {
          continue;
        }

        try {
          String langCode = content.getLocale().getLanguage().toUpperCase();

          TargetLanguage targetLang = TargetLanguage.valueOf(langCode);

          Options options = new Options();
          options.setSourceLang(sourceLanguageEnum);
          options.setTargetLang(targetLang);

          String translated = DeepLTranslationService.translate(sourceText, options);

          content.setContent(translated);

        } catch (Exception e) {
          System.err.println("Translate failed: " + sourceText);
        }
      }

      results.add(cmsCopy);
    }

    return results;
  }

  private static Cms deepCopyCms(Cms original) {

    Cms copy = new Cms();
    copy.setUri(original.getUri());
    copy.setPmvName(original.getPmvName());
    copy.setDifferentWithApplication(original.isDifferentWithApplication());
    copy.setFile(original.isFile());
    copy.setFileName(original.getFileName());
    copy.setFileExtension(original.getFileExtension());
    copy.setFileType(original.getFileType());

    if (original.getContents() != null) {
      List<CmsContent> contentCopies = new ArrayList<>();

      for (CmsContent c : original.getContents()) {
        CmsContent cc = new CmsContent();
        cc.setLocale(c.getLocale());
        cc.setContent(c.getContent());
        contentCopies.add(cc);
      }

      copy.setContents(contentCopies);
    }

    return copy;
  }
}
