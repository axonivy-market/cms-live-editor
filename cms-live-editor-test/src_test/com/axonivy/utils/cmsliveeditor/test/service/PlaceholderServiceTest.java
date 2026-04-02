package com.axonivy.utils.cmsliveeditor.test.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.SavedCms;
import com.axonivy.utils.cmsliveeditor.service.PlaceholderService;

public class PlaceholderServiceTest {

  private final PlaceholderService service = PlaceholderService.getInstance();

  @Test
  public void areMessagePatternsCompatibleShouldTreatBlankAsIncompatible() {
    assertFalse(service.areMessagePatternsCompatible("{0}", null));
    assertFalse(service.areMessagePatternsCompatible("{0}", ""));
    assertFalse(service.areMessagePatternsCompatible("{0}", "   "));
  }

  @Test
  public void areMessagePatternsCompatibleShouldIgnoreTextAndArgumentOrder() {
    assertTrue(service.areMessagePatternsCompatible("Hello {1} and {0}!", "Bonjour {0} et {1}!"));
  }

  @Test
  public void areMessagePatternsCompatibleShouldDetectMissingOrExtraArguments() {
    assertFalse(service.areMessagePatternsCompatible("{0} {1}", "{0}"));
    assertFalse(service.areMessagePatternsCompatible("{0}", "{0} {1}"));
  }

  @Test
  public void areMessagePatternsCompatibleShouldDetectFormatTypeChanges() {
    assertFalse(service.areMessagePatternsCompatible("{0} {1}", "{0,number} {1,date}"));
  }

  @Test
  public void areMessagePatternsCompatibleChoiceShouldIgnoreChoiceTextsButRequireSameStructure() {
    assertTrue(service.areMessagePatternsCompatible("{0,choice,0#no|1#yes}", "{0,choice,0#nein|1#ja}"));
    assertFalse(service.areMessagePatternsCompatible("{0,choice,0#no|1#yes}", "{0,choice,1#yes|0#no}"));
  }

  @Test
  public void areMessagePatternsCompatibleShouldDetectDifferentNumberStyle() {
    assertFalse(service.areMessagePatternsCompatible("{0,number,currency}", "{0,number,integer}"));
  }

  @Test
  void findInvalidLocaleIndicesShouldReturnMatchingIndices() {
    Cms cms = new Cms();

    CmsContent en = new CmsContent();
    en.setLocale(Locale.forLanguageTag("en"));
    en.setIndex(0);

    CmsContent fr = new CmsContent();
    fr.setLocale(Locale.forLanguageTag("fr"));
    fr.setIndex(1);

    cms.setContents(List.of(en, fr));

    List<Integer> result = service.findInvalidLocaleIndices(List.of("fr"), cms);
    assertEquals(List.of(1), result);
  }

  @Test
  void findInvalidLocaleIndicesShouldHandleUnderscoreLocales() {
    Cms cms = new Cms();

    CmsContent enUS = new CmsContent();
    enUS.setLocale(Locale.forLanguageTag("en-US"));
    enUS.setIndex(0);
    cms.setContents(List.of(enUS));

    List<Integer> result = service.findInvalidLocaleIndices(List.of("en_US"), cms);
    assertEquals(List.of(0), result);
  }

  @Test
  public void validateLocalesShouldFlagEditedLocalesNotMatchingStructure() {
    Map<String, SavedCms> cmsLocales = new LinkedHashMap<>();
    cmsLocales.put("en", createSavedCms("Hello {0} {1}", "Hello {0} {1}"));
    cmsLocales.put("fr", createSavedCms("Bonjour {0} {1}", "Bonjour {0}"));

    List<String> invalidLocales = service.validateLocales(cmsLocales);
    assertEquals(Set.of("fr"), new HashSet<>(invalidLocales));
  }

  @Test
  public void testValidateLocalesShouldReturnPlaceholderMismatchErrorsFirst() {
    Map<String, SavedCms> cmsLocales = new LinkedHashMap<>();
    cmsLocales.put("en", createSavedCms("Hello {0}", "Hello {0}"));
    cmsLocales.put("fr", createSavedCms("Bonjour {0}", "Bonjour"));
    cmsLocales.put("de", createSavedCms("Hallo {0}", "Hallo {0,invalid}"));

    List<String> invalidLocales = service.validateLocales(cmsLocales);
    assertEquals(Set.of("fr", "de"), new HashSet<>(invalidLocales));
  }

  @Test
  public void testValidateLocalesShouldReportMessageFormatErrorsOnlyForEditedLocales() {
    Map<String, SavedCms> cmsLocales = new LinkedHashMap<>();
    cmsLocales.put("en", createSavedCms("Hello {0}", "Hello {0,invalid}"));
    cmsLocales.put("fr", createSavedCms("Bonjour {0}", "Bonjour {0}"));

    List<String> invalidLocales = service.validateLocales(cmsLocales);
    assertEquals(Set.of("en"), new HashSet<>(invalidLocales));
  }

  @Test
  void testAreMessagePatternsCompatible() {
    assertTrue(service.areMessagePatternsCompatible("There {0,choice,0#no|1#one|1<{0,number,integer} files}",
        "Co {0,choice,0#here|1#there|1<{0,number,integer} files}"));
    assertFalse(service.areMessagePatternsCompatible("There {0,choice,1<{0,number,integer} files}",
        "There {0,choice,1<{0,number,currency} files}"));
    assertFalse(service.areMessagePatternsCompatible("There {0,choice,1<{0,number,integer} files}",
        "There {0,choice,1<files}"));
    assertFalse(service.areMessagePatternsCompatible("{0,choice,1#{1,choice,1<{1,number,integer}}}",
        "{0,choice,1#{1,choice,1<{1}}}"));
  }

  private static SavedCms createSavedCms(String original, String updated) {
    SavedCms cms = new SavedCms();
    cms.setOriginalContent(original);
    cms.setNewContent(updated);
    return cms;
  }
}
