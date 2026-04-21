package com.axonivy.utils.cmsliveeditor.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.enums.ExportType;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;
import com.axonivy.utils.cmsliveeditor.utils.CmsFileUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class CmsDownloadService {

  /**
   * Export CMS to ZIP file
   *
   * Supports:
   * - EXCEL → multiple Excel files zipped
   * - YAML  → multiple YAML files zipped
   */
  public static StreamedContent exportCmsToZip(String projectName, String applicationName,
      Map<String, PmvCms> pmvCmsMap, ExportType type) throws Exception {
    String normalizedProjectName = StringUtils.isBlank(projectName) ? Ivy.cms().co("/Labels/AllProjects") : projectName;
    var cmsFiles = new HashMap<String, byte[]>();
    if (type == ExportType.EXCEL) {
      Map<String, Workbook> workbooks = CmsFileUtils.collectWorkbooksAndCmsFiles(normalizedProjectName, pmvCmsMap, cmsFiles);
      return CmsFileUtils.convertToZip(normalizedProjectName, applicationName, workbooks, cmsFiles);
    } else {
      Map<String, String> files = CmsFileUtils.collectYamlFilesAndCmsFiles(normalizedProjectName, pmvCmsMap, cmsFiles);
      return CmsFileUtils.convertToZipYaml(normalizedProjectName, applicationName, files, cmsFiles);
    }
  }

}