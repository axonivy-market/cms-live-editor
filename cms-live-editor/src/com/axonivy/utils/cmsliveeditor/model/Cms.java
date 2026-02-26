package com.axonivy.utils.cmsliveeditor.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Cms implements Serializable {

  @Serial
  private static final long serialVersionUID = -88931664585615316L;

  private String uri;

  private List<CmsContent> contents;
  
  private String pmvName;
  
  private boolean isDifferentWithApplication;
  
  public boolean isDifferentWithApplication() {
    return isDifferentWithApplication;
  }

  public void setDifferentWithApplication(boolean isDifferentWithApplication) {
    this.isDifferentWithApplication = isDifferentWithApplication;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public List<CmsContent> getContents() {
    return contents;
  }

  public void setContents(List<CmsContent> contents) {
    this.contents = contents;
  }

  public void addContent(CmsContent content) {
    if (contents == null) {
      contents = new ArrayList<>();
    }
    contents.add(content);
  }

  public boolean isEditing() {
    return contents.stream().anyMatch(CmsContent::isEditing);
  }

  public String getPmvName() {
    return pmvName;
  }

  public void setPmvName(String pmvName) {
    this.pmvName = pmvName;
  }

}
