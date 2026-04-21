package com.axonivy.utils.cmsliveeditor.utils;

import static com.axonivy.utils.cmsliveeditor.constants.CommonConstants.BACKSLASH_CHARACTER;
import static com.axonivy.utils.cmsliveeditor.constants.CommonConstants.DOT_CHARACTER;
import static com.axonivy.utils.cmsliveeditor.constants.CommonConstants.DOUBLE_DOT;
import static com.axonivy.utils.cmsliveeditor.constants.CommonConstants.DOUBLE_SLASH_REGEX;
import static com.axonivy.utils.cmsliveeditor.constants.CommonConstants.LEADING_SLASH_REGEX;
import static com.axonivy.utils.cmsliveeditor.constants.CommonConstants.SLASH_CHARACTER;
import static com.axonivy.utils.cmsliveeditor.constants.CommonConstants.WINDOWS_DRIVE_REGEX;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.primefaces.model.file.UploadedFile;

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
      int lastDot = fileName.lastIndexOf(DOT_CHARACTER);
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
    uri = uri.replace(BACKSLASH_CHARACTER, SLASH_CHARACTER);

    uri = uri.replaceAll(LEADING_SLASH_REGEX, Strings.EMPTY);
    return uri;
  }

  public static boolean isValidFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return false;
    }

    return !(fileName.contains(DOUBLE_DOT) || fileName.contains(SLASH_CHARACTER)
        || fileName.contains(BACKSLASH_CHARACTER));
  }

  public static boolean isSafePath(Path basePath, String uri) {
    if (uri == null || uri.isBlank()) {
      return false;
    }

    uri = normalizeUri(uri);

    // ❗ Block Windows absolute path
    if (uri.matches(WINDOWS_DRIVE_REGEX)) {
      return false;
    }

    Path resolved = basePath.resolve(uri).normalize();
    return resolved.startsWith(basePath);
  }

  public static String buildNormalizedPath(String... parts) {
    String path = Arrays.stream(parts).filter(StringUtils::isNotBlank).collect(Collectors.joining(SLASH_CHARACTER));

    path = path.replace(BACKSLASH_CHARACTER, SLASH_CHARACTER).replaceAll(DOUBLE_SLASH_REGEX, SLASH_CHARACTER);

    if (path.endsWith(SLASH_CHARACTER)) {
      path = path.substring(0, path.length() - 1);
    }

    return path;
  }
}
