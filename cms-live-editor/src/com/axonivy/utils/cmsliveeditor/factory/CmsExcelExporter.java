package com.axonivy.utils.cmsliveeditor.factory;

import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.SHEET_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.URI_HEADER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;
import com.axonivy.utils.cmsliveeditor.utils.CmsFileUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class CmsExcelExporter implements CmsExporter {

  @Override
  public StreamedContent export(String projectName, String applicationName, Map<String, PmvCms> pmvCmsMap) {
    Map<String, byte[]> cmsFiles = new HashMap<>();
    Map<String, Workbook> workbooks = collectWorkbooksAndCmsFiles(projectName, pmvCmsMap, cmsFiles);
    try {
      return CmsFileUtils.convertToZip(projectName, applicationName, workbooks, cmsFiles);
    } catch (Exception e) {
      Ivy.log().error("Error exporting Excel zip", e);
      return null;
    }
  }

  public static XSSFWorkbook createWorkbookFromPmvCms(PmvCms pmvCms) {
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
          cell.setCellValue(getContentValue(cms, headers.get(columnCount)));
        }
      }
    }

    return workbook;
  }

  public static String getContentValue(Cms cms, String language) {
    return cms.getContents().stream().filter(content -> Strings.CS.equals(content.getLocale().getLanguage(), language))
        .findFirst().map(CmsContent::getContent).orElse(StringUtils.EMPTY);
  }

  public static Map<String, Workbook> collectWorkbooksAndCmsFiles(String projectName, Map<String, PmvCms> pmvCmsMap,
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

  public static void addPmvCmsToWorkbooks(String projectName, PmvCms pmvCms, Map<String, Workbook> workbooks) {
    XSSFWorkbook workbook = createWorkbookFromPmvCms(pmvCms);
    if (workbook != null) {
      workbooks.put(projectName, workbook);
    }
  }
}
