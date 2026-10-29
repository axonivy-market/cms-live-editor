package com.axonivy.utils.cmsliveeditor.test.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.primefaces.model.SortOrder;

import com.axonivy.utils.cmsliveeditor.model.LazyCmsSorter;

public class LazyCmsSorterTest extends CmsBaseTest {

  @Test
  public void testCompareAscendingEqual() {
    LazyCmsSorter sorter = new LazyCmsSorter(SortOrder.ASCENDING);
    int result = sorter.compare(cmsA, cmsA);
    assertTrue(result == 0, "Same URIs should compare as equal");
  }

  @Test
  public void testCompareAscending() {
    LazyCmsSorter sorter = new LazyCmsSorter(SortOrder.ASCENDING);
    int result = sorter.compare(cmsA, cmsB);
    assertTrue(result < 0, "'" + URI_A + "' should come before '" + URI_B + "' in ascending order");
  }

  @Test
  public void testCompareAscendingWithNullCms() {
    LazyCmsSorter sorter = new LazyCmsSorter(SortOrder.ASCENDING);
    int result = sorter.compare(cmsA, cmsNullPath);
    assertTrue(result > 0, "Cms has null URI should come before '" + URI_A + "' in ascending order");
  }

  @Test
  public void testCompareBothNullEqual() {
    LazyCmsSorter sorter = new LazyCmsSorter(SortOrder.ASCENDING);
    int result = sorter.compare(cmsNullPath, cmsNullPath);
    assertTrue(result == 0, "Two null Cms entries should compare as equal");
  }

  @Test
  public void testCompareDescending() {
    LazyCmsSorter sorter = new LazyCmsSorter(SortOrder.DESCENDING);
    int result = sorter.compare(cmsB, cmsA);
    assertTrue(result < 0, "'" + URI_B + "' should come before '" + URI_A + "' in ascending order");
  }
}
