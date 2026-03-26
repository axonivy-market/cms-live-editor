package com.axonivy.utils.cmsliveeditor.test.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.service.DeepLTranslationService;
import com.axonivy.utils.cmsliveeditor.service.TranslationService;
import com.axonivy.utils.cmsliveeditor.utils.CmsContentUtils;

public class TranslationServiceTest {
  private MockedStatic<DeepLTranslationService> deepLMock;
  private MockedStatic<CmsContentUtils> cmsContentUtilsMock;

  @BeforeEach
  public void setup() {
    deepLMock = Mockito.mockStatic(DeepLTranslationService.class);
    cmsContentUtilsMock = Mockito.mockStatic(CmsContentUtils.class);
  }

  @AfterEach
  public void clear() {
    deepLMock.close();
    cmsContentUtilsMock.close();
  }

  @Test
  public void testTranslate() {
    String text = "Hello";
    String srcLang = "EN";
    String targetLang = "FR";
    String translatedText = "Bonjour";

    deepLMock.when(() -> DeepLTranslationService.translate(eq(text), any())).thenReturn(translatedText);

    String result = TranslationService.translate(text, srcLang, targetLang);

    assertEquals(translatedText, result);

    deepLMock.verify(() -> DeepLTranslationService.translate(eq(text), any()), times(1));
  }

  @Test
  public void testBatchTranslate() {
    String srcLang = "EN";
    String targetLang = "FR";
    String originalText = "Good Morning";
    String translatedText = "Bonjour";

    Cms cmsEntry = new Cms();
    List<CmsContent> contents = new ArrayList<>();
    CmsContent sourceContent = new CmsContent();
    sourceContent.setLocale(Locale.forLanguageTag(srcLang));
    sourceContent.setContent(originalText);
    contents.add(sourceContent);
    CmsContent targetContent = new CmsContent();
    targetContent.setLocale(Locale.forLanguageTag(targetLang));
    contents.add(targetContent);
    cmsEntry.setContents(contents);

    List<Cms> entries = new ArrayList<>();
    entries.add(cmsEntry);

    cmsContentUtilsMock.when(() -> CmsContentUtils.getContentByLocale(cmsEntry, srcLang)).thenReturn(originalText);
    cmsContentUtilsMock.when(() -> CmsContentUtils.getCmsContentByLocale(cmsEntry, targetLang)).thenReturn(targetContent);
    deepLMock.when(() -> DeepLTranslationService.translate(eq(originalText), any())).thenReturn(translatedText);

    TranslationService.batchTranslate(entries, srcLang, targetLang);

    assertEquals(translatedText, targetContent.getTranslatedContent());

    deepLMock.verify(() -> DeepLTranslationService.translate(eq(originalText), any()), times(1));
    cmsContentUtilsMock.verify(() -> CmsContentUtils.getContentByLocale(cmsEntry, srcLang), times(1));
  }
}
