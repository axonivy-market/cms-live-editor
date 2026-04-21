package com.axonivy.utils.cmsliveeditor.test.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.constants.CommonConstants;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;
import com.axonivy.utils.cmsliveeditor.utils.CmsFileUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class CmsFileUtilsTest {
  private final String PROJECT_A = "projectA";
  private final String PROJECT_B = "projectB";
  private final String APPLICATION = "TestApp";
  private final String YAML_FILE_NAME = "cms_en.yaml";
  private final String EXCEL_EXTENSION = "xlsx";

  private Map<String, PmvCms> cmsMap;

  @BeforeEach
  public void setUp() {
    cmsMap = new HashMap<>();
    cmsMap.put(PROJECT_A, createMockPmvCms("PmvA", "uriA", "contentA"));
  }

  private PmvCms createMockPmvCms(String pmvName, String uri, String content) {
    PmvCms pmvCms = new PmvCms(pmvName, Collections.singletonList(Locale.ENGLISH));

    Cms cms = new Cms();
    cms.setUri(uri);

    CmsContent cmsContent = new CmsContent(0, Locale.ENGLISH, content, content);
    cms.setContents(Collections.singletonList(cmsContent));

    pmvCms.setCmsList(Collections.singletonList(cms));
    return pmvCms;
  }

  @Test
  public void testCreateWorkbookFromPmvCms() {
    XSSFWorkbook workbook = CmsFileUtils.createWorkbookFromPmvCms(cmsMap.get(PROJECT_A));

    assertNotNull(workbook);
    assertEquals(1, workbook.getNumberOfSheets());
    assertEquals(2, workbook.getSheetAt(0).getPhysicalNumberOfRows()); // header + 1 row
  }

  @Test
  public void testCollectWorkbooksAndCmsFilesForSingleProject() {
    Map<String, byte[]> files = new HashMap<>();

    Map<String, Workbook> result = CmsFileUtils.collectWorkbooksAndCmsFiles(PROJECT_A, cmsMap, files);

    assertEquals(1, result.size());
    assertTrue(result.containsKey(PROJECT_A));
  }

  @Test
  public void testCollectWorkbooksAndCmsFilesForAllProjects() {
    cmsMap.put(PROJECT_B, createMockPmvCms("PmvB", "uriB", "contentB"));

    Map<String, byte[]> files = new HashMap<>();

    Map<String, Workbook> result = CmsFileUtils.collectWorkbooksAndCmsFiles("", cmsMap, files);

    assertEquals(2, result.size());
  }

  @Test
  public void testCollectYamlFilesAndCmsFilesForSingleProject() {
    Map<String, byte[]> files = new HashMap<>();

    Map<String, String> yamlFiles = CmsFileUtils.collectYamlFilesAndCmsFiles(PROJECT_A, cmsMap, files);

    assertEquals(1, yamlFiles.size());
    assertTrue(yamlFiles.containsKey(YAML_FILE_NAME));
  }

  @Test
  public void testCollectYamlFilesAndCmsFilesForAllProjects() {
    cmsMap.put(PROJECT_B, createMockPmvCms("PmvB", "uriB", "contentB"));

    Map<String, byte[]> files = new HashMap<>();

    Map<String, String> yamlFiles = CmsFileUtils.collectYamlFilesAndCmsFiles("", cmsMap, files);

    assertEquals(2, yamlFiles.size());

    assertTrue(
        yamlFiles.keySet().stream().anyMatch(name -> name.contains(PROJECT_A + CommonConstants.SLASH_CHARACTER)));
    assertTrue(
        yamlFiles.keySet().stream().anyMatch(name -> name.contains(PROJECT_B + CommonConstants.SLASH_CHARACTER)));
  }

  @Test
  public void testConvertToZipYaml() throws Exception {
    Map<String, String> yamlFiles = new HashMap<>();
    yamlFiles.put(YAML_FILE_NAME, "key: value");

    Map<String, byte[]> cmsFiles = new HashMap<>();

    StreamedContent result = CmsFileUtils.convertToZipYaml(PROJECT_A, APPLICATION, yamlFiles, cmsFiles);

    assertNotNull(result);

    List<String> fileNames = new ArrayList<>();

    try (ByteArrayInputStream bais = (ByteArrayInputStream) result.getStream().get();
        ZipInputStream zis = new ZipInputStream(bais)) {

      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (!entry.isDirectory()) {
          fileNames.add(entry.getName());
        }
      }
    }

    assertEquals(1, fileNames.size());
    assertTrue(fileNames.contains(YAML_FILE_NAME));
  }

  @Test
  public void testConvertToZipExcel() throws Exception {
    Map<String, Workbook> workbooks = new HashMap<>();
    workbooks.put(PROJECT_A, CmsFileUtils.createWorkbookFromPmvCms(cmsMap.get(PROJECT_A)));

    Map<String, byte[]> files = new HashMap<>();

    StreamedContent result = CmsFileUtils.convertToZip(PROJECT_A, APPLICATION, workbooks, files);

    assertNotNull(result);

    List<String> fileNames = new ArrayList<>();

    try (ByteArrayInputStream bais = (ByteArrayInputStream) result.getStream().get();
        ZipInputStream zis = new ZipInputStream(bais)) {

      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (!entry.isDirectory()) {
          fileNames.add(entry.getName());
        }
      }
    }

    assertEquals(1, fileNames.size());
    assertTrue(fileNames.get(0).endsWith(EXCEL_EXTENSION));
  }

  @Test
  public void testGetFileTypeByExtension() {
    var type = CmsFileUtils.getFileTypeByExtension("xlsx");
    assertNotNull(type);
  }
}
