package com.axonivy.utils.cmsliveeditor.test.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

  private final String YAML_FILE = "cms_en.yaml";
  private final String PROJECT_A = "projectA";
  private final String PROJECT_B = "projectB";
  private final String APPLICATION = "TestApp";

  private Map<String, PmvCms> cmsMap;

  @BeforeEach
  void setup() {
    cmsMap = new HashMap<>();
    cmsMap.put(PROJECT_A, createMockPmvCms(PROJECT_A));
    cmsMap.put(PROJECT_B, createMockPmvCms(PROJECT_B));
  }

  private PmvCms createMockPmvCms(String project) {
    PmvCms pmv = new PmvCms(project, List.of(Locale.ENGLISH));

    Cms cms = new Cms();
    cms.setUri("uriA");

    CmsContent content = new CmsContent(0, Locale.ENGLISH, "Hello", "Hello");
    cms.setContents(List.of(content));

    pmv.setCmsList(List.of(cms));
    return pmv;
  }

  @Test
  void testExportShouldReturnZipWithYaml() throws Exception {
    CmsYamlExporter exporter = new CmsYamlExporter();

    StreamedContent result =
        exporter.export(PROJECT_A, APPLICATION, cmsMap);

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

  @Test
  void testExportAllProjectsShouldReturnZipWithMultipleYamlFiles() throws Exception {
    CmsYamlExporter exporter = new CmsYamlExporter();
    StreamedContent result = exporter.export("", APPLICATION, cmsMap);
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
    assertEquals(2, fileNames.size());
    assertTrue(fileNames.stream().allMatch(name -> name.endsWith(".yaml")));
    assertTrue(fileNames.stream().anyMatch(name -> name.contains("projectA/")));
    assertTrue(fileNames.stream().anyMatch(name -> name.contains("projectB/")));
  }
}