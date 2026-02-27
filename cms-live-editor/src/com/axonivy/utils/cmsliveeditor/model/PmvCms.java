package com.axonivy.utils.cmsliveeditor.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.collections.CollectionUtils;

public class PmvCms implements Serializable {

  @Serial
  private static final long serialVersionUID = 4233153680327218576L;

  private String pmvName;

  private List<Locale> locales;

  private List<Cms> cmsList;

  public PmvCms(String pmvName, List<Locale> locales) {
    super();
    this.pmvName = pmvName;
    this.locales = locales;
  }

  public String getPmvName() {
    return pmvName;
  }

  public void setPmvName(String pmvName) {
    this.pmvName = pmvName;
  }

  public List<Locale> getLocales() {
    return locales;
  }

  public void setLocales(List<Locale> locales) {
    this.locales = locales;
  }

  public List<Cms> getCmsList() {
    return cmsList;
  }

  public void setCmsList(List<Cms> cmsList) {
    this.cmsList = cmsList;
  }

  public void addCms(Cms cms) {
    if (CollectionUtils.isEmpty(cmsList)) {
      cmsList = new ArrayList<>();
    }
    cmsList.add(cms);
  }

}
