package com.axonivy.utils.cmsliveeditor.factory;

import com.axonivy.utils.cmsliveeditor.enums.ExportType;

public class CmsExporterFactory {

  private CmsExporterFactory() {}

  public static CmsExporter create(ExportType type) {
    return switch (type) {
      case EXCEL -> new CmsExcelExporter();
      case YAML -> new CmsYamlExporter();
    };
  }
}