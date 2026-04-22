package com.axonivy.utils.cmsliveeditor.test.utils;

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

import org.junit.jupiter.api.Test;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;
import com.axonivy.utils.cmsliveeditor.utils.CmsFileUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class CmsFileUtilsTest {

  private final String PROJECT = "projectA";

  @Test
  void testCollectCmsFilesShouldReturnEmptyWhenNull() {
    Map<String, byte[]> result = CmsFileUtils.collectCmsFiles(PROJECT, null);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void testCollectCmsFilesShouldCollectValidFile() {
    PmvCms pmv = new PmvCms(PROJECT, List.of(Locale.ENGLISH));

    Cms cms = new Cms();
    cms.setFile(true);

    CmsContent content = new CmsContent();
    content.setFile(true);
    content.setUri("folder/file.txt");
    content.setFileName("file.txt");
    content.setFileContent("data".getBytes());

    cms.setContents(List.of(content));
    pmv.setCmsList(List.of(cms));

    Map<String, byte[]> result = CmsFileUtils.collectCmsFiles(PROJECT, pmv);

    assertEquals(1, result.size());
    assertTrue(result.keySet().iterator().next().contains("folder/file.txt"));
  }

  @Test
  void testCollectCmsFilesShouldSkipInvalidFileName() {
    PmvCms pmv = new PmvCms(PROJECT, List.of(Locale.ENGLISH));

    Cms cms = new Cms();
    cms.setFile(true);

    CmsContent content = new CmsContent();
    content.setFile(true);
    content.setUri("folder/file.txt");
    content.setFileName("../file.txt"); // invalid
    content.setFileContent("data".getBytes());

    cms.setContents(List.of(content));
    pmv.setCmsList(List.of(cms));

    Map<String, byte[]> result = CmsFileUtils.collectCmsFiles(PROJECT, pmv);

    assertTrue(result.isEmpty());
  }

  @Test
  void testCollectCmsFilesShouldBlockUnsafePath() {
    PmvCms pmv = new PmvCms(PROJECT, List.of(Locale.ENGLISH));

    Cms cms = new Cms();
    cms.setFile(true);

    CmsContent content = new CmsContent();
    content.setFile(true);
    content.setUri("../etc/passwd"); // unsafe
    content.setFileName("file.txt");
    content.setFileContent("data".getBytes());

    cms.setContents(List.of(content));
    pmv.setCmsList(List.of(cms));

    Map<String, byte[]> result = CmsFileUtils.collectCmsFiles(PROJECT, pmv);

    assertTrue(result.isEmpty());
  }

  @Test
  void testAddPmvCmsFilesShouldMergeFiles() {
    Map<String, byte[]> target = new HashMap<>();

    PmvCms pmv = new PmvCms(PROJECT, List.of(Locale.ENGLISH));

    Cms cms = new Cms();
    cms.setFile(true);

    CmsContent content = new CmsContent();
    content.setFile(true);
    content.setUri("file.txt");
    content.setFileName("file.txt");
    content.setFileContent("data".getBytes());

    cms.setContents(List.of(content));
    pmv.setCmsList(List.of(cms));

    CmsFileUtils.addPmvCmsFiles(PROJECT, pmv, target);

    assertEquals(1, target.size());
  }

  @Test
  void testWriteCmsFileToZip() throws Exception {
    Map<String, byte[]> files = new HashMap<>();
    files.put("file.txt", "data".getBytes());

    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos);

    CmsFileUtils.writeCmsFileToZip(files, zos);
    zos.close();

    List<String> names = new ArrayList<>();

    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        names.add(entry.getName());
      }
    }

    assertEquals(1, names.size());
    assertTrue(names.contains("file.txt"));
  }

  @Test
  void testBuildStreamedContent() throws Exception {
    byte[] data = "zipdata".getBytes();

    StreamedContent result = CmsFileUtils.buildStreamedContent(data, PROJECT, "app");

    assertNotNull(result);
    assertNotNull(result.getStream());

    ByteArrayInputStream stream = (ByteArrayInputStream) result.getStream().get();

    assertTrue(stream.readAllBytes().length > 0);
  }
}