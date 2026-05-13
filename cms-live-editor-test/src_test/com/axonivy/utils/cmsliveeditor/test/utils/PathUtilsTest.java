package com.axonivy.utils.cmsliveeditor.test.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.cmsliveeditor.utils.PathUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class PathUtilsTest {

  // ===== Paths =====
  private static final String lONG_URI = "/com/axonivy/utils/demo/EmailFormat/name";
  private static final String SHORT_PATH_OF_lONG_URI = "../EmailFormat/name";
  private static final String SHORT_URI = "/Labels/Login";

  @Test
  public void testGetLastTwoPathSegmentsWithBlankInput() {
    assertEquals(StringUtils.EMPTY, PathUtils.getLastTwoPathSegments(null));
    assertEquals(StringUtils.EMPTY, PathUtils.getLastTwoPathSegments(StringUtils.EMPTY));
  }

  @Test
  public void testGetLastTwoPathSegmentsWithShortUriInput() {
    assertEquals(SHORT_URI, PathUtils.getLastTwoPathSegments(SHORT_URI));
  }

  @Test
  public void testGetLastTwoPathSegmentsWithLongUriInput() {
    assertEquals(SHORT_PATH_OF_lONG_URI, PathUtils.getLastTwoPathSegments(lONG_URI));
  }
}

