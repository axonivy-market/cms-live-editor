package com.axonivy.utils.cmsliveeditor.model;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.SortOrder;

public record LazyCmsSorter(SortOrder sortOrder) implements Comparator<Cms> {

  @Override
  public int compare(Cms cms1, Cms cms2) {
    try {
      String uriValueOfCms1 = getCmsUri(cms1);
      String uriValueOfCms2 = getCmsUri(cms2);
      int comparison = uriValueOfCms1.compareToIgnoreCase(uriValueOfCms2);
      return SortOrder.ASCENDING.equals(sortOrder) ? comparison : -comparison;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String getCmsUri(Cms cms) {
    return cms == null || StringUtils.isBlank(cms.getUri()) ? StringUtils.EMPTY : cms.getUri();
  }
}
