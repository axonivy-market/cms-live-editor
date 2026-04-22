package com.axonivy.utils.cmsliveeditor.test.factory;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.factory.CmsExcelExporter;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class CmsExcelExporterTest {

  private final String PROJECT = "projectA";
  private final String APPLICATION = "TestApp";

  private Map<String, PmvCms> cmsMap;

  @BeforeEach
  void setup() {
    cmsMap = new HashMap<>();
    cmsMap.put(PROJECT, createMockPmvCms());
  }

  private PmvCms createMockPmvCms() {
    PmvCms pmv = new PmvCms(PROJECT, List.of(Locale.ENGLISH));

    Cms cms = new Cms();
    cms.setUri("uriA");

    CmsContent content = new CmsContent(0, Locale.ENGLISH, "Hello", "Hello");
    cms.setContents(List.of(content));

    pmv.setCmsList(List.of(cms));
    return pmv;
  }

  @Test
  void testCreateWorkbookFromPmvCms() {
    Workbook workbook = CmsExcelExporter.createWorkbookFromPmvCms(cmsMap.get(PROJECT));

    assertNotNull(workbook);
    assertEquals(1, workbook.getNumberOfSheets());
    assertEquals(2, workbook.getSheetAt(0).getPhysicalNumberOfRows());
  }

  @Test
  void testCollectWorkbooksAndCmsFiles() {
    Map<String, byte[]> files = new HashMap<>();

    Map<String, Workbook> result =
        CmsExcelExporter.collectWorkbooksAndCmsFiles(PROJECT, cmsMap, files);

    assertEquals(1, result.size());
    assertTrue(result.containsKey(PROJECT));
  }

  @Test
  void testExportShouldReturnZipWithExcel() throws Exception {
    CmsExcelExporter exporter = new CmsExcelExporter();

    StreamedContent result =
        exporter.export(PROJECT, APPLICATION, cmsMap);

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
    assertTrue(fileNames.get(0).endsWith("xlsx"));
  }
}