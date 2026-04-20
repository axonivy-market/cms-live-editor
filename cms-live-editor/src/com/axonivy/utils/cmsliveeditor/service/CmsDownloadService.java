package com.axonivy.utils.cmsliveeditor.service;

import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.EXCEL_FILE_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.ZIP_CONTENT_TYPE;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.ZIP_FILE_NAME;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.model.PmvCms;
import com.axonivy.utils.cmsliveeditor.utils.CmsFileUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class CmsDownloadService {

  public static StreamedContent writeCmsToZipStreamedContent(String projectName, String applicationName, Map<String, PmvCms> cmsPmvMap)
      throws Exception {

    var workbooks = new HashMap<String, Workbook>();
    var cmsFiles = new HashMap<String, byte[]>();

    if (StringUtils.isEmpty(projectName)) {
      projectName = Ivy.cms().co("/Labels/AllProjects");
      for (var entry : cmsPmvMap.entrySet()) {
        CmsFileUtils.addPmvCmsToWorkbooks(entry.getKey(), entry.getValue(), workbooks);
        CmsFileUtils.addPmvCmsFiles(entry.getKey(), entry.getValue(), cmsFiles);
      }
    } else {
      PmvCms pmvCms = cmsPmvMap.get(projectName);
      CmsFileUtils.addPmvCmsToWorkbooks(projectName, pmvCms, workbooks);
      CmsFileUtils.addPmvCmsFiles(projectName, pmvCms, cmsFiles);
    }

    return convertToZip(projectName, applicationName, workbooks, cmsFiles);
  }

  public static StreamedContent convertToZip(String projectName, String applicationName, Map<String, Workbook> workbooks,
      Map<String, byte[]> files) throws Exception {

    try (var baos = new ByteArrayOutputStream(); var zipOut = new ZipOutputStream(baos)) {

      // Excel files
      for (Entry<String, Workbook> entry : workbooks.entrySet()) {
        var fileName = String.format(EXCEL_FILE_NAME, entry.getKey());

        zipOut.putNextEntry(new ZipEntry(fileName));
        zipOut.write(CmsFileUtils.convertWorkbookToByteArray(entry.getValue()));
        zipOut.closeEntry();
      }

      // CMS files
      for (Map.Entry<String, byte[]> entry : files.entrySet()) {
        zipOut.putNextEntry(new ZipEntry(entry.getKey()));
        zipOut.write(entry.getValue());
        zipOut.closeEntry();
      }

      zipOut.close();

      byte[] zipBytes = baos.toByteArray();

      return DefaultStreamedContent.builder()
          .name(String.format(ZIP_FILE_NAME, Ivy.cms().co("/Labels/CMSDownload"), projectName, applicationName)).contentType(ZIP_CONTENT_TYPE)
          .stream(() -> new ByteArrayInputStream(zipBytes)).build();

    } finally {
      CmsFileUtils.closeWorkbooks(workbooks);
    }
  }
}
