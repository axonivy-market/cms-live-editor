package com.axonivy.utils.cmsliveeditor.utils;

import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.EXCEL_FILE_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.ZIP_CONTENT_TYPE;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.ZIP_FILE_NAME;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;
import com.axonivy.utils.cmsliveeditor.service.CmsContentLoader;

import ch.ivyteam.ivy.environment.Ivy;

public class CmsFileUtils {

  private static final Path BASE_PATH = Path.of("virtual-root").toAbsolutePath().normalize();

  private CmsFileUtils() {}

  public static Map<String, byte[]> collectCmsFiles(String projectName, PmvCms pmvCms) {
    if (pmvCms == null) {
      return new HashMap<>();
    }

    return pmvCms.getCmsList().stream().filter(Cms::isFile).peek(CmsContentLoader::loadFileContentOfCms)
        .flatMap(cms -> cms.getContents().stream()).filter(Objects::nonNull).filter(CmsContent::isFile)
        .map(content -> toZipEntry(projectName, content, BASE_PATH)).filter(Objects::nonNull)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
  }

  private static Map.Entry<String, byte[]> toZipEntry(String projectName, CmsContent content, Path basePath) {
    try {
      byte[] data = resolveFileContent(content);
      if (data == null || data.length == 0) {
        Ivy.log().error("File content not found for: " + content.getFileName());
        return null;
      }

      String uri = FileUtils.normalizeUri(content.getUri());
      String fileName = content.getFileName();

      if (!FileUtils.isValidFileName(fileName)) {
        Ivy.log().warn("Invalid filename: " + fileName);
        return null;
      }

      if (!FileUtils.isSafePath(basePath, uri)) {
        Ivy.log().warn("Blocked path traversal: " + uri);
        return null;
      }

      return new AbstractMap.SimpleEntry<>(FileUtils.buildNormalizedPath(projectName, uri), data);

    } catch (Exception e) {
      Ivy.log().error("Failed to prepare file: " + content.getFileName(), e);
      return null;
    }
  }

  private static byte[] resolveFileContent(CmsContent content) {
    if (content.getApplicationFileContent() != null && content.getApplicationFileSize() > 0) {
      return content.getApplicationFileContent();
    }
    return content.getFileContent();
  }

  public static void addPmvCmsFiles(String projectName, PmvCms pmvCms, Map<String, byte[]> cmsFiles) {
    Map<String, byte[]> files = collectCmsFiles(projectName, pmvCms);
    if (files != null) {
      cmsFiles.putAll(files);
    }
  }

  public static StreamedContent convertToZipYaml(String projectName, String applicationName, Map<String, String> files,
      Map<String, byte[]> cmsFiles) {
    try (var baos = new ByteArrayOutputStream(); var zipOut = new ZipOutputStream(baos)) {
      for (Entry<String, String> entry : files.entrySet()) {
        writeTextEntry(zipOut, entry.getKey(), entry.getValue());
      }
      writeCmsFileToZip(cmsFiles, zipOut);
      zipOut.finish();

      return buildStreamedContent(baos.toByteArray(), projectName, applicationName);
    } catch (IOException e) {
      Ivy.log().error("Error creating YAML zip", e);
      return null;
    }
  }

  public static StreamedContent convertToZip(String projectName, String applicationName,
      Map<String, Workbook> workbooks, Map<String, byte[]> files) throws Exception {
    try (var baos = new ByteArrayOutputStream(); var zipOut = new ZipOutputStream(baos)) {
      for (Entry<String, Workbook> entry : workbooks.entrySet()) {
        String fileName = String.format(EXCEL_FILE_NAME, entry.getKey());
        zipOut.putNextEntry(new ZipEntry(fileName));
        zipOut.write(convertWorkbookToByteArray(entry.getValue()));
        zipOut.closeEntry();
      }
      writeCmsFileToZip(files, zipOut);
      zipOut.finish();

      return buildStreamedContent(baos.toByteArray(), projectName, applicationName);
    } finally {
      closeWorkbooks(workbooks);
    }
  }

  public static byte[] convertWorkbookToByteArray(Workbook workbook) throws IOException {
    try (var outputStream = new ByteArrayOutputStream()) {
      workbook.write(outputStream);
      return outputStream.toByteArray();
    }
  }

  private static void writeTextEntry(ZipOutputStream zipOut, String name, String content) throws IOException {
    zipOut.putNextEntry(new ZipEntry(name));
    zipOut.write(content.getBytes(StandardCharsets.UTF_8));
    zipOut.closeEntry();
  }

  public static void writeCmsFileToZip(Map<String, byte[]> files, ZipOutputStream zipOut) throws IOException {
    for (Entry<String, byte[]> entry : files.entrySet()) {
      zipOut.putNextEntry(new ZipEntry(entry.getKey()));
      zipOut.write(entry.getValue());
      zipOut.closeEntry();
    }
  }

  public static void closeWorkbooks(Map<String, Workbook> workbooks) {
    workbooks.values().forEach(CmsFileUtils::closeWorkbook);
  }

  private static void closeWorkbook(Workbook workbook) {
    try {
      if (workbook != null) {
        workbook.close();
      }
    } catch (IOException e) {
      Ivy.log().error("Error closing workbook", e);
    }
  }

  private static StreamedContent buildStreamedContent(byte[] zipBytes, String projectName, String applicationName) {
    return DefaultStreamedContent.builder()
        .name(String.format(ZIP_FILE_NAME, Ivy.cms().co("/Labels/CMSDownload"), projectName, applicationName))
        .contentType(ZIP_CONTENT_TYPE).stream(() -> new ByteArrayInputStream(zipBytes)).build();
  }

}