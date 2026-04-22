package com.axonivy.utils.cmsliveeditor.test.factory;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.factory.CmsYamlExporter;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class CmsYamlExporterTest {

  private final String PROJECT = "projectA";
  private final String APPLICATION = "TestApp";
  private final String YAML_FILE = "cms_en.yaml";

  private Map<String, PmvCms> cmsMap;

  @BeforeEach
  void setup() {
    cmsMap = new HashMap<>();
    cmsMap.put(PROJECT, createMockPmvCms());
  }

  private PmvCms createMockPmvCms() {
    PmvCms pmv = new PmvCms(PROJECT, List.of(Locale.ENGLISH));

    Cms cms = new Cms();
    cms.setUri("a/b");

    CmsContent content = new CmsContent(0, Locale.ENGLISH, "Hello", "Hello");
    cms.setContents(List.of(content));

    pmv.setCmsList(List.of(cms));
    return pmv;
  }

  @Test
  void testCollectYamlFilesAndCmsFiles() {
    Map<String, byte[]> files = new HashMap<>();

    Map<String, String> result =
        CmsYamlExporter.collectYamlFilesAndCmsFiles(PROJECT, cmsMap, files);

    assertEquals(1, result.size());
    assertTrue(result.containsKey(YAML_FILE));
  }

  @Test
  void testExportShouldReturnZipWithYaml() throws Exception {
    CmsYamlExporter exporter = new CmsYamlExporter();

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
    assertTrue(fileNames.contains(YAML_FILE));
  }
}