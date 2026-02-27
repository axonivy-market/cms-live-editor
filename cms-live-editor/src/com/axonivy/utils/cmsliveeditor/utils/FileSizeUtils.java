package com.axonivy.utils.cmsliveeditor.utils;

import com.axonivy.utils.cmsliveeditor.constants.FileConstants;

public class FileSizeUtils {

  public static long calculateToKB(long numberOfBytes) {
    return (long) Math.ceil(numberOfBytes / FileConstants.BYTE_IN_KB);
  }
}
