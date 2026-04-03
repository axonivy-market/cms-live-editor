package com.axonivy.utils.cmsliveeditor.model;

import java.io.Serializable;

import com.axonivy.utils.cmsliveeditor.enums.ExportType;

public class ExportOption implements Serializable {
  private static final long serialVersionUID = 1L;

  String label;
  String icon;
  ExportType type;

  public ExportOption(String label, String icon, ExportType type) {
    this.label = label;
    this.icon = icon;
    this.type = type;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public ExportType getType() {
    return type;
  }

  public void setType(ExportType type) {
    this.type = type;
  }
}
