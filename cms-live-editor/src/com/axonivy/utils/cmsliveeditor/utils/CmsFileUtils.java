package com.axonivy.utils.cmsliveeditor.utils;

import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.ZIP_CONTENT_TYPE;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.ZIP_FILE_NAME;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;
import com.axonivy.utils.cmsliveeditor.service.CmsContentLoader;

import ch.ivyteam.ivy.environment.Ivy;

public class CmsFileUtils {

  private static final Path BASE_PATH = Path.of("export").toAbsolutePath().normalize();

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
      if (data == null) {
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

  // Get applicationFileContent if present, otherwise get the original one
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

  public static void writeCmsFileToZip(Map<String, byte[]> files, ZipOutputStream zipOut) throws IOException {
    for (Entry<String, byte[]> entry : files.entrySet()) {
      zipOut.putNextEntry(new ZipEntry(entry.getKey()));
      zipOut.write(entry.getValue());
      zipOut.closeEntry();
    }
  }

  public static StreamedContent buildStreamedContent(byte[] zipBytes, String projectName, String applicationName) {
    return DefaultStreamedContent.builder()
        .name(String.format(ZIP_FILE_NAME, Ivy.cms().co("/Labels/CMSDownload"), projectName, applicationName))
        .contentType(ZIP_CONTENT_TYPE).stream(() -> new ByteArrayInputStream(zipBytes)).build();
  }

}
