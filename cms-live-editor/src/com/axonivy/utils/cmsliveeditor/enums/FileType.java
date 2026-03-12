package com.axonivy.utils.cmsliveeditor.enums;

import org.apache.commons.lang3.StringUtils;

public enum FileType {
  PDF(".pdf", "pi pi-file-pdf cms-file-pdf-type"),
  EXCEL(".xls,.xlsx", "pi pi-file-excel cms-file-excel-type"),
  WORD(".doc,.docx", "pi pi-file-word cms-file-word-type"),
  IMAGE(".png,.jpg,.jpeg" ,"pi pi-image"),
  OTHERS("", "pi pi-file-o");

  FileType(String fileExtension, String iconClasses) {
    this.fileExtension = fileExtension;
    this.iconClasses = iconClasses;
  }

  private String fileExtension;
  private String iconClasses;

  public String getFileExtension() {
    return fileExtension;
  }

  public String getIconClasses() {
    return iconClasses;
  }

  public static FileType fromExtension(String extension) {
    if (StringUtils.isBlank(extension)) {
      return OTHERS;
    }

    extension = extension.toLowerCase();
    for (FileType type : FileType.values()) {
      if (type.fileExtension.contains(extension)) {
        return type;
      }
    }

    return OTHERS;
  }
}
