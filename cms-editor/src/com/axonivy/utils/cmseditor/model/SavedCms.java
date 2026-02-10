package com.axonivy.utils.cmseditor.model;

import java.io.Serial;
import java.io.Serializable;

public class SavedCms implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private String uri;

  private String locale;

  private String originalContent;

  private String newContent;
  
  private byte[] newFileContent;

  public SavedCms() {
    super();
  }

  public SavedCms(String uri, String locale, String originalContent, String newContent) {
    super();
    this.uri = uri;
    this.locale = locale;
    this.originalContent = originalContent;
    this.newContent = newContent;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public String getOriginalContent() {
    return originalContent;
  }

  public void setOriginalContent(String originalContent) {
    this.originalContent = originalContent;
  }

  public String getNewContent() {
    return newContent;
  }

  public void setNewContent(String newContent) {
    this.newContent = newContent;
  }

  public byte[] getNewFileContent() {
    return newFileContent;
  }

  public void setNewFileContent(byte[] newFileContent) {
    this.newFileContent = newFileContent;
  }

  @Override
  public String toString() {
    return String.format("[%s] [%s] [%s] [%s]", uri, locale, originalContent, newContent);
  }
}
