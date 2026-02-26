package com.axonivy.utils.cmseditor.utils;

import com.axonivy.utils.cmseditor.constants.FileConstants;

public class FileSizeUtils {

  public static long calculateToKB(long numberOfBytes) {
    return (long) Math.ceil(numberOfBytes / FileConstants.BYTE_IN_KB);
  }
}
