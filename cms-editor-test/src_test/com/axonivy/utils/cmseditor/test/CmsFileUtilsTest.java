package com.axonivy.utils.cmseditor.test;

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

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmseditor.model.Cms;
import com.axonivy.utils.cmseditor.model.CmsContent;
import com.axonivy.utils.cmseditor.model.PmvCms;
import com.axonivy.utils.cmseditor.utils.CmsFileUtils;

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
  public void testWriteCmsToZipStreamedContent() throws Exception {
    StreamedContent result = CmsFileUtils.writeCmsToZipStreamedContent(PROJECT_CMS_A, TEST_APPLICATION, cmsPmvMap);
    assertNotNull(result);
    assertEquals(CONTENT_TYPE, result.getContentType());
    assertTrue(result.getName().equals(String.format(DOWNLOAD_FILE_FORMAT, PROJECT_CMS_A, TEST_APPLICATION)));

    // Further checks to ensure the content is as expected
    try (ByteArrayInputStream bais = (ByteArrayInputStream) result.getStream().get();
        var zipInputStream = new java.util.zip.ZipInputStream(bais)) {
      java.util.zip.ZipEntry zipEntry = zipInputStream.getNextEntry();
      assertNotNull(zipEntry);
      assertEquals(String.join(DOT_CHARACTER, PROJECT_CMS_A, EXCEL_FILE_EXTENSION), zipEntry.getName());
    }
  }

  @Test
  public void testWriteCmsToZipStreamedContentForAllProjects() throws Exception {
    PmvCms pmvCmsB = createMockPmvCms(1, Locale.ENGLISH, "originContentB", "contentB", "UriB");
    cmsPmvMap.put(PROJECT_CMS_B, pmvCmsB);
    StreamedContent result = CmsFileUtils.writeCmsToZipStreamedContent("", TEST_APPLICATION, cmsPmvMap);
    assertNotNull(result);
    assertEquals(CONTENT_TYPE, result.getContentType());
    assertTrue(result.getName().equals(String.format(DOWNLOAD_FILE_FORMAT, ALL_PROJECTS, TEST_APPLICATION)));
    List<String> fileNames = new ArrayList<>();
    try (ByteArrayInputStream bais = (ByteArrayInputStream) result.getStream().get();
        var zipInputStream = new java.util.zip.ZipInputStream(bais)) {
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
  public void testConvertToZip() throws Exception {
    Map<String, Workbook> workbooks = new HashMap<>();
    Workbook workbook = new XSSFWorkbook();
    workbooks.put(PROJECT_CMS_A, workbook);
    StreamedContent result = CmsFileUtils.convertToZip(ALL_PROJECTS, TEST_APPLICATION, workbooks);
    assertNotNull(result);
    assertEquals(CONTENT_TYPE, result.getContentType());
    assertTrue(result.getName().equals(String.format(DOWNLOAD_FILE_FORMAT, ALL_PROJECTS, TEST_APPLICATION)));

    // Further checks to ensure the content is as expected
    try (ByteArrayInputStream bais = (ByteArrayInputStream) result.getStream().get();
        var zipInputStream = new java.util.zip.ZipInputStream(bais)) {
      java.util.zip.ZipEntry zipEntry = zipInputStream.getNextEntry();
      assertNotNull(zipEntry);
      assertEquals(String.join(DOT_CHARACTER, PROJECT_CMS_A, EXCEL_FILE_EXTENSION), zipEntry.getName());
    }
  }
}

