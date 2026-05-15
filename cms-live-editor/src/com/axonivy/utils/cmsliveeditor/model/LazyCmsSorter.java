package com.axonivy.utils.cmsliveeditor.model;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.SortOrder;

public class LazyCmsSorter implements Comparator<Cms> {

  private SortOrder sortOrder;

  public LazyCmsSorter(SortOrder sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public int compare(Cms cms1, Cms cms2) {
    try {
      String value1 = (cms1 == null || StringUtils.isBlank(cms1.getUri())) ? StringUtils.EMPTY : cms1.getUri();
      String value2 = (cms2 == null || StringUtils.isBlank(cms2.getUri())) ? StringUtils.EMPTY : cms2.getUri();

      int value = value1.compareToIgnoreCase(value2);

      return SortOrder.ASCENDING.equals(sortOrder) ? value : -1 * value;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
