package com.axonivy.utils.cmsliveeditor.utils;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.primefaces.model.file.UploadedFile;

import com.axonivy.utils.cmsliveeditor.constants.CommonConstants;
import com.axonivy.utils.cmsliveeditor.constants.FileConstants;

import ch.ivyteam.ivy.environment.Ivy;

public class FileUtils {

  public static long calculateToKB(long numberOfBytes) {
    return (long) Math.ceil(numberOfBytes / FileConstants.BYTE_IN_KB);
  }

  public static boolean isValidFileSize(long fileSize, long maxMBUploadFileSize) {
    return fileSize <= maxMBUploadFileSize * FileConstants.KB_IN_MB * FileConstants.BYTE_IN_KB;
  }

  public static long getMaxUploadedFileSize() {
    try {
      return Long.parseLong(Ivy.var().get("com.axonivy.utils.cmsliveeditor.MaxUploadedFileSize"));
    } catch (Exception e) {
      Ivy.log().error(e);
      return FileConstants.DEFAULT_VALID_SIZE_MB;
    }
  }

  public static String getFileExtension(UploadedFile file) {
    if (file == null) {
      return StringUtils.EMPTY;
    }
    String extension = StringUtils.EMPTY;
    String fileName = file.getFileName();
    if (StringUtils.isNotBlank(fileName)) {
      int lastDot = fileName.lastIndexOf(CommonConstants.DOT_CHARACTER);
      if (lastDot > 0 && lastDot < fileName.length() - 1) {
        extension = fileName.substring(lastDot + 1).toLowerCase();
      }
    }
    return extension;
  }

  public static String normalizeUri(String uri) {
    if (uri == null) {
      return Strings.EMPTY;
    }
    // Normalize
    uri = uri.replace("\\", CommonConstants.SLASH_CHARACTER);

    // remove ALL leading slashes
    while (uri.startsWith(CommonConstants.SLASH_CHARACTER)) {
      uri = uri.substring(1);
    }
    return uri;
  }

  public static boolean isValidFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return false;
    }

    return !(fileName.contains("..") || fileName.contains("/") || fileName.contains("\\"));
  }

  public static boolean isSafePath(Path basePath, String uri) {
    if (uri == null) {
      return false;
    }

    Path resolved = basePath.resolve(uri).normalize();
    return resolved.startsWith(basePath);
  }

  public static String buildNormalizedPath(String... parts) {
    String path = String.join("/", parts);

    path = path.replace("\\", "/").replaceAll("//+", "/");

    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }

    return path;
  }
}
