package com.axonivy.utils.cmsliveeditor.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.cmsliveeditor.utils.FileSizeUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class FileSizeUtilsTest {

  private static final long BYTES_IN_ONE_HUNDRED_KB = 102400;
  private static final long BYTES_IN_ONE_KB = 1024;

  @Test
  public void testCalculateToKB() {
    assertEquals(0, FileSizeUtils.calculateToKB(0));
    assertEquals(1, FileSizeUtils.calculateToKB(BYTES_IN_ONE_KB));
    assertEquals(100, FileSizeUtils.calculateToKB(BYTES_IN_ONE_HUNDRED_KB));
  }
}

