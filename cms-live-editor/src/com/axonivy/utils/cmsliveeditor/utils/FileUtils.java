package com.axonivy.utils.cmsliveeditor.utils;

import com.axonivy.utils.cmsliveeditor.constants.FileConstants;

public class FileUtils {

  public static long calculateToKB(long numberOfBytes) {
    return (long) Math.ceil(numberOfBytes / FileConstants.BYTE_IN_KB);
  }

  public static boolean isValidFileSize(long fileSize, long maxMBUploadFileSize) {
    return fileSize <= maxMBUploadFileSize * FileConstants.KB_IN_MB * FileConstants.BYTE_IN_KB;
  }
}
