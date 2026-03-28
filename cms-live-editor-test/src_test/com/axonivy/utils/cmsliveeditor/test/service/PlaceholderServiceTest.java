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
import com.axonivy.utils.cmsliveeditor.model.Placeholder;
import com.axonivy.utils.cmsliveeditor.model.SavedCms;
import com.axonivy.utils.cmsliveeditor.service.PlaceholderService;

public class PlaceholderServiceTest {

  private final PlaceholderService service = PlaceholderService.getInstance();

  @Test
  public void testExtractPlaceholdersBlankShouldReturnEmpty() {
    assertTrue(service.extractPlaceholders(null).isEmpty());
    assertTrue(service.extractPlaceholders("").isEmpty());
    assertTrue(service.extractPlaceholders("   ").isEmpty());
  }

  @Test
  public void testExtractPlaceholdersShouldParseAndSortByIndex() {
    List<Placeholder> placeholders = service.extractPlaceholders("Hello {1} and {0}!");
    assertEquals(2, placeholders.size());
    assertEquals(0, placeholders.get(0).getIndex());
    assertEquals(1, placeholders.get(1).getIndex());
  }

  @Test
  public void testExtractPlaceholdersShouldTrimFormatAndStyle() {
    List<Placeholder> placeholders = service.extractPlaceholders("{0, choice , 0#no | 1#yes }");
    assertEquals(1, placeholders.size());
    assertEquals(0, placeholders.get(0).getIndex());
    assertEquals("choice", placeholders.get(0).getFormat());
    assertEquals("0#no | 1#yes", placeholders.get(0).getStyle());
  }

  @Test
  public void testHasSamePlaceholderStructureNonChoiceShouldIgnoreFormatAndStyle() {
    List<Placeholder> original = service.extractPlaceholders("{0} {1}");
    List<Placeholder> changedFormat = service.extractPlaceholders("{0,number} {1,date}");
    assertTrue(service.hasSamePlaceholderStructure(original, changedFormat));
  }

  @Test
  public void testHasSamePlaceholderStructureChoiceShouldRequireSameFormatAndStyleIgnoringOrder() {
    List<Placeholder> original = service.extractPlaceholders("{0,choice,0#no|1#yes}");
    List<Placeholder> reordered = service.extractPlaceholders("{0,choice,1#yes|0#no}");
    List<Placeholder> different = service.extractPlaceholders("{0,choice,0#no|1#maybe}");
    List<Placeholder> differentFormat = service.extractPlaceholders("{0,number,0#no|1#yes}");

    assertTrue(service.hasSamePlaceholderStructure(original, reordered));
    assertFalse(service.hasSamePlaceholderStructure(original, different));
    assertFalse(service.hasSamePlaceholderStructure(original, differentFormat));
  }

  @Test
  public void testFindInvalidLocaleIndicesShouldNormalizeUnderscoresAndReturnDistinctSortedIndices() {
    Cms cms = new Cms();

    CmsContent en1 = new CmsContent();
    en1.setIndex(2);
    en1.setLocale(Locale.forLanguageTag("en-US"));
    cms.addContent(en1);

    CmsContent de = new CmsContent();
    de.setIndex(1);
    de.setLocale(Locale.forLanguageTag("de-DE"));
    cms.addContent(de);

    CmsContent enDuplicateIndex = new CmsContent();
    enDuplicateIndex.setIndex(2);
    enDuplicateIndex.setLocale(Locale.forLanguageTag("en-US"));
    cms.addContent(enDuplicateIndex);

    List<Integer> invalidIndices = service.findInvalidLocaleIndices(List.of("en_US", null, "de-DE"), cms);
    assertEquals(List.of(1, 2), invalidIndices);
  }

  @Test
  public void testFindMismatchLocalesWithOriginalPlaceholdersShouldFlagEditedLocalesNotMatchingStructure() {
    Map<String, SavedCms> cmsLocales = new LinkedHashMap<>();
    cmsLocales.put("en", createSavedCms("Hello {0} {1}", "Hello {0} {1}"));
    cmsLocales.put("fr", createSavedCms("Bonjour {0} {1}", "Bonjour {0}"));

    List<String> mismatched = service.findMismatchLocales(cmsLocales);
    assertEquals(Set.of("fr"), new HashSet<>(mismatched));
  }

  @Test
  public void testFindMismatchLocalesWithoutOriginalPlaceholdersMixedPlaceholderPresenceShouldFlagAllLocales() {
    Map<String, SavedCms> cmsLocales = new LinkedHashMap<>();
    cmsLocales.put("en", createSavedCms("Hello", "Hello {0}"));
    cmsLocales.put("fr", createSavedCms("Bonjour", "Bonjour"));

    List<String> mismatched = service.findMismatchLocales(cmsLocales);
    assertEquals(Set.of("en", "fr"), new HashSet<>(mismatched));
  }

  @Test
  public void testFindMismatchLocalesWithoutOriginalPlaceholdersAllHavePlaceholdersShouldShareStructure() {
    Map<String, SavedCms> cmsLocales = new LinkedHashMap<>();
    cmsLocales.put("en", createSavedCms("Hello", "Hello {0}"));
    cmsLocales.put("de", createSavedCms("Hallo", "Hallo {0}"));
    cmsLocales.put("fr", createSavedCms("Bonjour", "Bonjour {1}"));

    List<String> mismatched = service.findMismatchLocales(cmsLocales);
    assertEquals(Set.of("fr"), new HashSet<>(mismatched));
  }

  @Test
  public void testValidateLocalesShouldReturnPlaceholderMismatchErrorsFirst() {
    Map<String, SavedCms> cmsLocales = new LinkedHashMap<>();
    cmsLocales.put("en", createSavedCms("Hello {0}", "Hello {0}"));
    cmsLocales.put("fr", createSavedCms("Bonjour {0}", "Bonjour"));
    cmsLocales.put("de", createSavedCms("Hallo {0}", "Hallo {0,invalid}"));

    List<String> invalidLocales = service.validateLocales(cmsLocales);
    assertEquals(Set.of("fr"), new HashSet<>(invalidLocales));
  }

  @Test
  public void testValidateLocalesShouldReportMessageFormatErrorsOnlyForEditedLocales() {
    Map<String, SavedCms> cmsLocales = new LinkedHashMap<>();
    cmsLocales.put("en", createSavedCms("Hello {0}", "Hello {0,invalid}"));
    cmsLocales.put("fr", createSavedCms("Bonjour {0}", "Bonjour {0}"));

    List<String> invalidLocales = service.validateLocales(cmsLocales);
    assertEquals(Set.of("en"), new HashSet<>(invalidLocales));
  }

  private static SavedCms createSavedCms(String original, String updated) {
    SavedCms cms = new SavedCms();
    cms.setOriginalContent(original);
    cms.setNewContent(updated);
    return cms;
  }
}
