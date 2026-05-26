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
      String value1 = getCmsUri(cms1);
      String value2 = getCmsUri(cms2);
      int comparison = value1.compareToIgnoreCase(value2);
      return SortOrder.ASCENDING.equals(sortOrder) ? comparison : -comparison;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String getCmsUri(Cms cms) {
    return cms == null || StringUtils.isBlank(cms.getUri()) ? StringUtils.EMPTY : cms.getUri();
  }
}
