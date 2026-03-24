package com.axonivy.utils.cmsliveeditor.model;

public class Placeholder {
  private int index;
  private String format;
  private String style;

  public Placeholder(int index, String format, String style) {
    this.index = index;
    this.format = format;
    this.style = style;
  }

  public int getIndex() {
    return index;
  }

  public String getFormat() {
    return format;
  }

  public String getStyle() {
    return style;
  }
}
