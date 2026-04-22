package com.axonivy.utils.cmsliveeditor.test.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.utils.CmsZipUtils;

class CmsZipUtilsTest {

  // ==================== convertToZipYaml ====================

  @Test
  void convertToZipYaml_validFiles_returnsStreamedContent() {
    Map<String, String> yamlFiles = new HashMap<>();
    yamlFiles.put("cms_en.yaml", "Labels:\n  Hello: World\n");
    Map<String, byte[]> cmsFiles = Collections.emptyMap();

    StreamedContent result = CmsZipUtils.convertToZipYaml("project", "app", yamlFiles, cmsFiles);

    assertNotNull(result);
    assertNotNull(result.getStream());
  }

  @Test
  void convertToZipYaml_withCmsFiles_includesBothInZip() throws IOException {
    Map<String, String> yamlFiles = new HashMap<>();
    yamlFiles.put("cms_en.yaml", "Labels:\n  Hello: World\n");
    Map<String, byte[]> cmsFiles = new HashMap<>();
    cmsFiles.put("project/assets/logo.png", new byte[]{1, 2, 3});

    StreamedContent result = CmsZipUtils.convertToZipYaml("project", "app", yamlFiles, cmsFiles);

    assertNotNull(result);
    // verify zip contains both entries
    try (ZipInputStream zis = new ZipInputStream(result.getStream().get())) {
      int entryCount = 0;
      while (zis.getNextEntry() != null) {
        entryCount++;
      }
      assertTrue(entryCount >= 2);
    }
  }

  @Test
  void convertToZipYaml_emptyFiles_returnsStreamedContent() {
    StreamedContent result = CmsZipUtils.convertToZipYaml("project", "app",
        Collections.emptyMap(), Collections.emptyMap());
    assertNotNull(result);
  }

  // ==================== convertToZip ====================

  @Test
  void convertToZip_validWorkbooks_returnsStreamedContent() throws Exception {
    XSSFWorkbook workbook = new XSSFWorkbook();
    workbook.createSheet("CMS");
    Map<String, org.apache.poi.ss.usermodel.Workbook> workbooks = new HashMap<>();
    workbooks.put("project", workbook);

    StreamedContent result = CmsZipUtils.convertToZip("project", "app", workbooks, Collections.emptyMap());

    assertNotNull(result);
    assertNotNull(result.getStream());
  }

  @Test
  void convertToZip_withCmsFiles_includesBothInZip() throws Exception {
    XSSFWorkbook workbook = new XSSFWorkbook();
    workbook.createSheet("CMS");
    Map<String, org.apache.poi.ss.usermodel.Workbook> workbooks = new HashMap<>();
    workbooks.put("project", workbook);
    Map<String, byte[]> cmsFiles = new HashMap<>();
    cmsFiles.put("project/assets/logo.png", new byte[]{1, 2, 3});

    StreamedContent result = CmsZipUtils.convertToZip("project", "app", workbooks, cmsFiles);

    assertNotNull(result);
    try (ZipInputStream zis = new ZipInputStream(result.getStream().get())) {
      int entryCount = 0;
      while (zis.getNextEntry() != null) {
        entryCount++;
      }
      assertTrue(entryCount >= 2);
    }
  }

  @Test
  void convertToZip_emptyWorkbooks_returnsStreamedContent() throws Exception {
    StreamedContent result = CmsZipUtils.convertToZip("project", "app",
        Collections.emptyMap(), Collections.emptyMap());
    assertNotNull(result);
  }
}