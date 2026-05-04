package com.axonivy.utils.cmsliveeditor.service;

import java.util.Map;

import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.enums.ExportType;
import com.axonivy.utils.cmsliveeditor.factory.CmsExporterFactory;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;

public class CmsDownloadService {

  private static CmsDownloadService instance;

  public static CmsDownloadService getInstance() {
    if (instance == null) {
      instance = new CmsDownloadService();
    }
    return instance;
  }

  /**
   * Export CMS to ZIP file
   *
   * Supports: - EXCEL → multiple Excel files zipped - YAML → multiple YAML files zipped
   */
  public StreamedContent exportCmsToZip(String projectName, String applicationName,
      Map<String, PmvCms> pmvCmsMap, ExportType type) throws Exception {
    return CmsExporterFactory.create(type).export(projectName, applicationName, pmvCmsMap);
  }
}
