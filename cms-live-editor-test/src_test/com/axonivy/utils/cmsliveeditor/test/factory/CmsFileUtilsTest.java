package com.axonivy.utils.cmsliveeditor.test.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.cmsliveeditor.enums.FileType;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;

class CmsFileUtilsTest {

  // ==================== collectCmsFiles ====================

  @Test
  void collectCmsFiles_nullPmvCms_returnsEmptyMap() {
    Map<String, byte[]> result = CmsFileUtils.collectCmsFiles("project", null);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void collectCmsFiles_noFileCmsEntries_returnsEmptyMap() {
    Cms textCms = buildTextCms("/Labels/Hello", "en", "Hello");
    PmvCms pmvCms = buildPmvCms("project", List.of(Locale.ENGLISH), List.of(textCms));

    Map<String, byte[]> result = CmsFileUtils.collectCmsFiles("project", pmvCms);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  // ==================== addPmvCmsToWorkbooks ====================

  @Test
  void addPmvCmsToWorkbooks_nullPmvCms_doesNotAddToWorkbooks() {
    Map<String, Workbook> workbooks = new HashMap<>();
    CmsFileUtils.addPmvCmsToWorkbooks("project", null, workbooks);
    assertTrue(workbooks.isEmpty());
  }

  @Test
  void addPmvCmsToWorkbooks_validPmvCms_addsWorkbook() {
    PmvCms pmvCms = buildPmvCms("project", List.of(Locale.ENGLISH), Collections.emptyList());
    Map<String, Workbook> workbooks = new HashMap<>();

    CmsFileUtils.addPmvCmsToWorkbooks("project", pmvCms, workbooks);

    assertEquals(1, workbooks.size());
    assertTrue(workbooks.containsKey("project"));
    assertNotNull(workbooks.get("project"));
  }

  // ==================== addPmvCmsFiles ====================

  @Test
  void addPmvCmsFiles_nullPmvCms_doesNotAddToCmsFiles() {
    Map<String, byte[]> cmsFiles = new HashMap<>();
    CmsFileUtils.addPmvCmsFiles("project", null, cmsFiles);
    assertTrue(cmsFiles.isEmpty());
  }

  @Test
  void addPmvCmsFiles_noFileCmsEntries_doesNotAddToCmsFiles() {
    PmvCms pmvCms = buildPmvCms("project", List.of(Locale.ENGLISH),
        List.of(buildTextCms("/Labels/Hello", "en", "Hello")));
    Map<String, byte[]> cmsFiles = new HashMap<>();

    CmsFileUtils.addPmvCmsFiles("project", pmvCms, cmsFiles);

    assertTrue(cmsFiles.isEmpty());
  }

  // ==================== getFileTypeByExtension ====================

  @Test
  void getFileTypeByExtension_png_returnsPngType() {
    FileType result = CmsFileUtils.getFileTypeByExtension("png");
    assertNotNull(result);
    assertEquals(FileType.fromExtension(".png"), result);
  }

  @Test
  void getFileTypeByExtension_upperCase_normalizedAndResolved() {
    FileType result = CmsFileUtils.getFileTypeByExtension("PNG");
    assertNotNull(result);
    assertEquals(FileType.fromExtension(".png"), result);
  }

  // ==================== Helpers ====================

  private PmvCms buildPmvCms(String name, List<Locale> locales, List<Cms> cmsList) {
    PmvCms pmvCms = new PmvCms();
    pmvCms.setPmvName(name);
    pmvCms.setLocales(locales);
    pmvCms.setCmsList(cmsList);
    return pmvCms;
  }

  private Cms buildTextCms(String uri, String language, String contentValue) {
    CmsContent content = new CmsContent();
    content.setLocale(Locale.forLanguageTag(language));
    content.setContent(contentValue);

    Cms cms = new Cms();
    cms.setUri(uri);
    cms.setContents(List.of(content));
    cms.setFile(false);
    return cms;
  }
}