package com.axonivy.utils.cmsliveeditor.test.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.utils.CmsContentUtils;

public class CmsContentUtilsTest {

  @Test
  public void testGetLocalListFromCMS() {
    Cms cms1 = new Cms();
    CmsContent c1 = new CmsContent();
    c1.setLocale(Locale.ENGLISH);
    c1.setContent("en content");

    CmsContent c2 = new CmsContent();
    c2.setLocale(Locale.FRENCH);
    c2.setContent("fr content");

    cms1.setContents(List.of(c1, c2));

    Cms cms2 = new Cms();
    CmsContent c3 = new CmsContent();
    c3.setLocale(Locale.ENGLISH);
    c3.setContent("another en content");

    cms2.setContents(List.of(c3));

    List<Locale> locales = CmsContentUtils.getLocalListFromCMS(List.of(cms1, cms2));

    assertNotNull(locales);
    assertEquals(2, locales.size());
    assertEquals(Locale.ENGLISH.getDisplayLanguage(Locale.ENGLISH), locales.get(0).getDisplayLanguage(Locale.ENGLISH));
    assertEquals(Locale.FRENCH.getDisplayLanguage(Locale.ENGLISH), locales.get(1).getDisplayLanguage(Locale.ENGLISH));
  }

  @Test
  public void testGetCmsContentByLocale() {
    Cms cms = new Cms();

    CmsContent enUS = new CmsContent();
    enUS.setLocale(Locale.ENGLISH);
    enUS.setTranslatedContent("US content");

    CmsContent de = new CmsContent();
    de.setLocale(Locale.GERMAN);
    de.setTranslatedContent("DE content");

    cms.setContents(List.of(enUS, de));

    CmsContent found = CmsContentUtils.getCmsContentByLocale(cms, "en");
    assertNotNull(found);
    assertEquals("US content", found.getTranslatedContent());

    CmsContent found2 = CmsContentUtils.getCmsContentByLocale(cms, "de");
    assertNotNull(found2);
    assertEquals("DE content", found2.getTranslatedContent());

    // missing locale returns null
    CmsContent missing = CmsContentUtils.getCmsContentByLocale(cms, "fr");
    assertNull(missing);
  }

  @Test
  public void testGetContentByLocale() {
    Cms cms = new Cms();
    cms.setContents(List.of()); // no contents

    String content = CmsContentUtils.getContentByLocale(cms, "en");
    assertNotNull(content);
    assertEquals("", content);
  }

  @Test
  public void testGetTranslatedCms() {
    Cms cms1 = new Cms();
    CmsContent a = new CmsContent();
    a.setLocale(Locale.ENGLISH);
    a.setContent("A");
    a.setTranslatedContent(""); // not translated
    cms1.setContents(List.of(a));

    Cms cms2 = new Cms();
    CmsContent b = new CmsContent();
    b.setLocale(Locale.FRENCH);
    b.setContent("B");
    b.setTranslatedContent("B translated"); // translated
    cms2.setContents(List.of(b));

    List<Cms> translated = CmsContentUtils.getTranslatedCms(List.of(cms1, cms2));
    assertNotNull(translated);
    assertEquals(1, translated.size());
    assertTrue(translated.contains(cms2));
    assertFalse(translated.contains(cms1));
  }

  @Test
  public void testGetExcludedLocales() {
    List<Locale> locales = List.of(Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN);

    List<Locale> filtered = CmsContentUtils.getExcludedLocales(locales, "fr");
    assertNotNull(filtered);
    // French should be excluded
    assertEquals(2, filtered.size());
    assertFalse(filtered.stream().anyMatch(l -> "fr".equalsIgnoreCase(l.toLanguageTag())));
  }
}
