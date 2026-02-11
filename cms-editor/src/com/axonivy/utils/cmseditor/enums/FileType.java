package com.axonivy.utils.cmseditor.enums;

public enum FileType {
  PDF("pdf"), EXCEL("xls|xlsx"), WORD("doc|docx"), IMAGE("jpeg|jpg|png"), OTHERS("");

  FileType(String fileExtension) {
    this.fileExtension = fileExtension;
  }

  private String fileExtension;

  public String getFileExtension() {
    return fileExtension;
  }

  public void setFileExtension(String fileExtension) {
    this.fileExtension = fileExtension;
  }

}
