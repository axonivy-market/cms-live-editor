package com.axonivy.utils.cmsliveeditor.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.cmsliveeditor.constants.FileConstants;
import com.axonivy.utils.cmsliveeditor.utils.FileUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class FileUtilsTest {

  private static final long ONE_HUNDRED = 100;

  @Test
  public void testCalculateToKB() {
    assertEquals(0, FileUtils.calculateToKB(0));
    assertEquals(1, FileUtils.calculateToKB(FileConstants.BYTE_IN_KB));
    assertEquals(ONE_HUNDRED, FileUtils.calculateToKB(ONE_HUNDRED * FileConstants.BYTE_IN_KB));
  }

  @Test
  public void testIsValidFileSize() {
    long twoMBFileSize = 2 * FileConstants.KB_IN_MB * FileConstants.BYTE_IN_KB;
    assertEquals(false, FileUtils.isValidFileSize(twoMBFileSize, 1));
    long oneMBFileSize = 1 * FileConstants.KB_IN_MB * FileConstants.BYTE_IN_KB;
    assertEquals(true, FileUtils.isValidFileSize(oneMBFileSize, 1));
  }
}

