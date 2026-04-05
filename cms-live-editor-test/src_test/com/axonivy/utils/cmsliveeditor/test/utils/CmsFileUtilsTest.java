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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.constants.CommonConstants;
import com.axonivy.utils.cmsliveeditor.enums.ExportType;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;
import com.axonivy.utils.cmsliveeditor.utils.CmsFileUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class CmsFileUtilsTest {
  private final String CONTENT_TYPE = "application/zip";
  private final String DOT_CHARACTER = ".";
  private final String TEST_APPLICATION = "TestApplication";
  private final String PROJECT_CMS_A = "projectCmsA";
  private final String PROJECT_CMS_B = "projectCmsB";
  private final String ALL_PROJECTS = "All";
  private final String EXCEL_FILE_EXTENSION = "xlsx";
  private final String YAML_FILE_NAME = "cms_en.yaml";
  private final String DOWNLOAD_FILE_FORMAT = "CMSDownload_%s_%s.zip";

  private Map<String, PmvCms> cmsPmvMap;

  @BeforeEach
  public void setUp() {
    cmsPmvMap = new HashMap<>();
    PmvCms pmvCmsA = createMockPmvCms(0, Locale.ENGLISH, "originContentA", "contentA", "UriA");
    cmsPmvMap.put(PROJECT_CMS_A, pmvCmsA);
  }

  private PmvCms createMockPmvCms(int index, Locale locale, String originalContent, String content, String uri) {
    PmvCms pmvCms = new PmvCms("PmvName", Collections.singletonList(locale));
    pmvCms.setCmsList(Collections.singletonList(createMockCms(index, locale, originalContent, content, uri)));
    return pmvCms;
  }

  private Cms createMockCms(int index, Locale locale, String originalContent, String content, String uri) {
    Cms cms = new Cms();
    cms.setUri(uri);
    CmsContent cmsContent = new CmsContent(index, locale, originalContent, content);
    cms.setContents(Collections.singletonList(cmsContent));
    return cms;
  }

  @Test
  public void testExportCmsToExcelZipForSingleProject() throws Exception {
    StreamedContent result = CmsFileUtils.exportCmsToZip(PROJECT_CMS_A, TEST_APPLICATION, cmsPmvMap, ExportType.EXCEL);

    assertNotNull(result);
    assertEquals(CONTENT_TYPE, result.getContentType());
    assertEquals(String.format(DOWNLOAD_FILE_FORMAT, PROJECT_CMS_A, TEST_APPLICATION), result.getName());

    try (ByteArrayInputStream bais = (ByteArrayInputStream) result.getStream().get();
        ZipInputStream zipInputStream = new ZipInputStream(bais)) {

      ZipEntry entry = zipInputStream.getNextEntry();
      assertNotNull(entry);
      assertEquals(PROJECT_CMS_A + DOT_CHARACTER + EXCEL_FILE_EXTENSION, entry.getName());
    }
  }

  @Test
  public void testExportCmsToExcelZipForAllProjects() throws Exception {
    cmsPmvMap.put(PROJECT_CMS_B, createMockPmvCms(1, Locale.ENGLISH, "originContentB", "contentB", "UriB"));

    StreamedContent result = CmsFileUtils.exportCmsToZip("", TEST_APPLICATION, cmsPmvMap, ExportType.EXCEL);

    assertNotNull(result);
    assertEquals(CONTENT_TYPE, result.getContentType());
    assertEquals(String.format(DOWNLOAD_FILE_FORMAT, ALL_PROJECTS, TEST_APPLICATION), result.getName());

    List<String> fileNames = new ArrayList<>();

    try (ByteArrayInputStream bais = (ByteArrayInputStream) result.getStream().get();
        ZipInputStream zipInputStream = new ZipInputStream(bais)) {

      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (!entry.isDirectory()) {
          fileNames.add(entry.getName());
        }
      }
    }

    assertEquals(2, fileNames.size());
    assertTrue(fileNames.contains(String.join(DOT_CHARACTER, PROJECT_CMS_A, EXCEL_FILE_EXTENSION)));
    assertTrue(fileNames.contains(String.join(DOT_CHARACTER, PROJECT_CMS_B, EXCEL_FILE_EXTENSION)));
  }

  @Test
  public void testExportCmsToYamlZipForSingleProject() throws Exception {
    StreamedContent result = CmsFileUtils.exportCmsToZip(PROJECT_CMS_A, TEST_APPLICATION, cmsPmvMap, ExportType.YAML);

    assertNotNull(result);
    assertEquals(CONTENT_TYPE, result.getContentType());

    try (ByteArrayInputStream bais = (ByteArrayInputStream) result.getStream().get();
        ZipInputStream zipInputStream = new ZipInputStream(bais)) {

      ZipEntry entry = zipInputStream.getNextEntry();
      assertNotNull(entry);

      assertEquals(YAML_FILE_NAME, entry.getName());
    }
  }

  @Test
  public void testExportCmsToYamlZipForAllProjects() throws Exception {
    cmsPmvMap.put(PROJECT_CMS_B, createMockPmvCms(1, Locale.ENGLISH, "originContentB", "contentB", "UriB"));

    StreamedContent result = CmsFileUtils.exportCmsToZip("", TEST_APPLICATION, cmsPmvMap, ExportType.YAML);

    assertNotNull(result);

    List<String> fileNames = new ArrayList<>();

    try (ByteArrayInputStream bais = (ByteArrayInputStream) result.getStream().get();
        ZipInputStream zipInputStream = new ZipInputStream(bais)) {

      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (!entry.isDirectory()) {
          fileNames.add(entry.getName());
        }
      }
    }

    assertEquals(2, fileNames.size());
    assertTrue(fileNames.contains(String.join(CommonConstants.SLASH_CHARACTER, PROJECT_CMS_A, YAML_FILE_NAME)));
    assertTrue(fileNames.contains(String.join(CommonConstants.SLASH_CHARACTER, PROJECT_CMS_B, YAML_FILE_NAME)));
  }
}
