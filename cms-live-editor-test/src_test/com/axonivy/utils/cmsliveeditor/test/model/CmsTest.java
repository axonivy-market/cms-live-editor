package com.axonivy.utils.cmsliveeditor.test.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class CmsTest extends CmsBaseTest {
  @Test
  public void testEqualsSameInstance() {
    assertEquals(cmsA, cmsA);
  }

  @Test
  public void testEqualsDifferentUri() {
    assertNotEquals(cmsA, cmsB);
  }

  @Test
  public void testEqualsNull() {
    assertNotEquals(cmsA, null);
  }

  @Test
  public void testEqualsDifferentType() {
    assertNotEquals(cmsA, URI_A);
  }

  @Test
  public void testEqualsOneNullUri() {
    assertNotEquals(cmsA, cmsNullPath);
  }

  @Test
  public void testHashCodeSameUri() {
    assertEquals(cmsA.hashCode(), cmsA.hashCode());
  }

  @Test
  public void testHashCodeDifferentUri() {
    assertNotEquals(cmsA.hashCode(), cmsB.hashCode());
  }
}
