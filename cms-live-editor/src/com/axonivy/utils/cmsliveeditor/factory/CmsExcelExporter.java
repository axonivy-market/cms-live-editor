package com.axonivy.utils.cmsliveeditor.factory;

import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.EXCEL_FILE_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.SHEET_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.URI_HEADER;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.model.PmvCms;
import com.axonivy.utils.cmsliveeditor.utils.CmsContentUtils;
import com.axonivy.utils.cmsliveeditor.utils.CmsFileUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class CmsExcelExporter implements CmsExporter {

  @Override
  public StreamedContent export(String projectName, String applicationName, Map<String, PmvCms> pmvCmsMap) {
    Map<String, byte[]> cmsFiles = new HashMap<>();
    Map<String, Workbook> workbooks = collectWorkbooksAndCmsFiles(projectName, pmvCmsMap, cmsFiles);
    try {
      return convertToZip(projectName, applicationName, workbooks, cmsFiles);
    } catch (Exception e) {
      Ivy.log().error("Error exporting Excel zip", e);
      return null;
    }
  }

  private XSSFWorkbook createWorkbookFromPmvCms(PmvCms pmvCms) {
    if (pmvCms == null) {
      return null;
    }

    var cmsList = pmvCms.getCmsList();
    var headers = new ArrayList<String>();
    headers.add(URI_HEADER);
    headers.addAll(pmvCms.getLocales().stream().map(Locale::getLanguage).filter(StringUtils::isNotBlank).toList());
    var workbook = new XSSFWorkbook();
    var worksheet = workbook.createSheet(SHEET_NAME);

    // save header
    var headerRow = worksheet.createRow(0);
    for (var column = 0; column < headers.size(); column++) {
      var cell = headerRow.createCell(column);
      cell.setCellValue(headers.get(column));
    }

    // start save data
    for (var rowCount = 0; rowCount < cmsList.size(); rowCount++) {
      var row = worksheet.createRow(rowCount + 1);
      var cms = cmsList.get(rowCount);
      for (var columnCount = 0; columnCount < headers.size(); columnCount++) {
        var cell = row.createCell(columnCount);
        if (columnCount == 0) {
          cell.setCellValue(cms.getUri());
        } else {
          cell.setCellValue(CmsContentUtils.getContentValueByLanguage(cms, headers.get(columnCount)));
        }
      }
    }

    return workbook;
  }

  private Map<String, Workbook> collectWorkbooksAndCmsFiles(String projectName, Map<String, PmvCms> pmvCmsMap,
      Map<String, byte[]> cmsFiles) {
    Map<String, Workbook> workbooks = new HashMap<>();
    boolean isAllProjects = StringUtils.isBlank(projectName);

    if (isAllProjects) {
      projectName = Ivy.cms().co("/Labels/AllProjects");
      pmvCmsMap.forEach((name, pmvCms) -> {
        addPmvCmsToWorkbooks(name, pmvCms, workbooks);
        CmsFileUtils.addPmvCmsFiles(name, pmvCms, cmsFiles);
      });
    } else {
      addPmvCmsToWorkbooks(projectName, pmvCmsMap.get(projectName), workbooks);
      CmsFileUtils.addPmvCmsFiles(projectName, pmvCmsMap.get(projectName), cmsFiles);
    }

    return workbooks;
  }

  private void addPmvCmsToWorkbooks(String projectName, PmvCms pmvCms, Map<String, Workbook> workbooks) {
    XSSFWorkbook workbook = createWorkbookFromPmvCms(pmvCms);
    if (workbook != null) {
      workbooks.put(projectName, workbook);
    }
  }

  private StreamedContent convertToZip(String projectName, String applicationName, Map<String, Workbook> workbooks,
      Map<String, byte[]> files) throws Exception {
    try (var baos = new ByteArrayOutputStream(); var zipOut = new ZipOutputStream(baos)) {
      for (Entry<String, Workbook> entry : workbooks.entrySet()) {
        String fileName = String.format(EXCEL_FILE_NAME, entry.getKey());
        zipOut.putNextEntry(new ZipEntry(fileName));
        zipOut.write(convertWorkbookToByteArray(entry.getValue()));
        zipOut.closeEntry();
      }
      CmsFileUtils.writeCmsFileToZip(files, zipOut);
      zipOut.finish();

      return CmsFileUtils.buildStreamedContent(baos.toByteArray(), projectName, applicationName);
    } finally {
      closeWorkbooks(workbooks);
    }
  }

  private byte[] convertWorkbookToByteArray(Workbook workbook) throws IOException {
    try (var outputStream = new ByteArrayOutputStream()) {
      workbook.write(outputStream);
      return outputStream.toByteArray();
    }
  }

  private void closeWorkbooks(Map<String, Workbook> workbooks) {
    workbooks.values().forEach(this::closeWorkbook);
  }

  private void closeWorkbook(Workbook workbook) {
    try {
      if (workbook != null) {
        workbook.close();
      }
    } catch (IOException e) {
      Ivy.log().error("Error closing workbook", e);
    }
  }
}
