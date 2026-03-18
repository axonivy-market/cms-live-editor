package com.axonivy.utils.cmsliveeditor.utils;

import org.apache.commons.lang3.StringUtils;
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
}
