package com.axonivy.utils.cmsliveeditor.utils;

import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.EXCEL_FILE_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.FILE_EXTENSION_FORMAT;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.SHEET_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.URI_HEADER;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.ZIP_CONTENT_TYPE;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.ZIP_FILE_NAME;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.enums.FileType;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.ContentObjectReader;
import ch.ivyteam.ivy.cm.ContentObjectValue;
import ch.ivyteam.ivy.cm.exec.ContentManagement;
import ch.ivyteam.ivy.environment.Ivy;

public class CmsFileUtils {
  
  private static final Path BASE_PATH = Path.of("virtual-root").toAbsolutePath().normalize();

  public static StreamedContent writeCmsToZipStreamedContent(String projectName, String applicationName,
      Map<String, PmvCms> cmsPmvMap) throws Exception {
    
    var workbooks = new HashMap<String, Workbook>();
    var cmsFiles = new HashMap<String, byte[]>();
    if (StringUtils.isEmpty(projectName)) {
      projectName = Ivy.cms().co("/Labels/AllProjects");
      for (var entry : cmsPmvMap.entrySet()) {
        addPmvCmsToWorkbooks(entry.getKey(), entry.getValue(), workbooks);
        addPmvCmsFiles(entry.getKey(), entry.getValue(), cmsFiles);
      }
    } else {
      PmvCms pmvCms = cmsPmvMap.get(projectName);
      addPmvCmsToWorkbooks(projectName, pmvCms, workbooks);
      addPmvCmsFiles(projectName, pmvCms, cmsFiles);
    }

    return convertToZip(projectName, applicationName, workbooks, cmsFiles);
  }

  private static void addPmvCmsToWorkbooks(String projectName, PmvCms pmvCms, HashMap<String, Workbook> workbooks) {
    var workbook = createWorkbookFromPmvCms(pmvCms);
    if (workbook != null) {
      workbooks.put(projectName, workbook);
    }
  }
  
  private static void addPmvCmsFiles(String projectName, PmvCms pmvCms, HashMap<String, byte[]> cmsFiles) {
    var files = collectCmsFiles(projectName, pmvCms);
    if (files != null) {
      cmsFiles.putAll(files);
    }
  }
  
  public static Map<String, byte[]> collectCmsFiles(String projectName, PmvCms pmvCms) {

    return pmvCms.getCmsList().stream()
        .filter(Cms::isFile)
        .peek(CmsFileUtils::loadFileContentOfCms)
        .flatMap(cms -> cms.getContents().stream())
        .filter(Objects::nonNull)
        .filter(CmsContent::isFile)
        .map(content -> toZipEntry(projectName, content, BASE_PATH))
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (a, b) -> a // handle duplicate keys (keep first)
        ));
  }
  
  private static Map.Entry<String, byte[]> toZipEntry(String projectName, CmsContent content, Path basePath) {
    try {
      byte[] data = resolveFileContent(content);
      if (data == null || data.length == 0) {
        Ivy.log().error("File Content not found for: " + content.getFileName());
        return null;
      }

      String uri = content.getUri();
      String fileName = content.getFileName();

      if (uri == null || fileName == null) {
        return null;
      }

      // Normalize
      uri = uri.replace("\\", "/");
      if (uri.startsWith("/")) {
        uri = uri.substring(1);
      }

      // Validate filename
      if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
        Ivy.log().warn("Invalid filename: " + fileName);
        return null;
      }

      // Validate path (security)
      Path filePath = basePath.resolve(uri).resolve(fileName).normalize();
      if (!filePath.startsWith(basePath)) {
        Ivy.log().warn("Blocked path traversal: " + filePath);
        return null;
      }

      // 🔥 FIX: avoid folder issue → DO NOT end with "/"
      String zipEntryPath = projectName + "/" + uri;

      if (zipEntryPath.endsWith("/")) {
        zipEntryPath = zipEntryPath.substring(0, zipEntryPath.length() - 1);
      }

      return new AbstractMap.SimpleEntry<>(zipEntryPath, data);

    } catch (Exception e) {
      Ivy.log().error("Failed to prepare file: " + content.getFileName(), e);
      return null;
    }
  }

  private static byte[] resolveFileContent(CmsContent content) {
    if (content.getApplicationFileContent() != null && content.getApplicationFileSize() > 0) {
      return content.getApplicationFileContent();
    }
    if (content.getFileContent() != null) {
      return content.getFileContent();
    }
    return null;
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
      var row = worksheet.createRow(rowCount + 1); // second row is first cms
      var cms = cmsList.get(rowCount);
      for (var columnCount = 0; columnCount < headers.size(); columnCount++) {
        var cell = row.createCell(columnCount);
        // set uri
        if (columnCount == 0) {
          cell.setCellValue(cms.getUri());
        } else {
          cell.setCellValue(getContentValue(cms, headers.get(columnCount)));
        }
      }
    }

    return workbook;
  }

  private static String getContentValue(Cms cms, String language) {
    return cms.getContents().stream().filter(content -> Strings.CS.equals(content.getLocale().getLanguage(), language))
        .findFirst().map(CmsContent::getContent).orElse(StringUtils.EMPTY);
  }

  public static StreamedContent convertToZip(String projectName, String applicationName, Map<String, Workbook> workbooks,
      Map<String, byte[]> files) throws Exception {

    try (var baos = new ByteArrayOutputStream(); var zipOut = new ZipOutputStream(baos)) {

      // 1. Excel files
      for (Entry<String, Workbook> entry : workbooks.entrySet()) {
        var fileName = String.format(EXCEL_FILE_NAME, entry.getKey());

        zipOut.putNextEntry(new ZipEntry(fileName));
        zipOut.write(convertWorkbookToByteArray(entry.getValue()));
        zipOut.closeEntry();
      }

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
      closeWorkbooks(workbooks);
    }
  }

  private static byte[] convertWorkbookToByteArray(Workbook workbook) throws Exception {
    try (var outputStream = new ByteArrayOutputStream()) {
      workbook.write(outputStream);
      return outputStream.toByteArray();
    }
  }

  private static void closeWorkbooks(Map<String, Workbook> workbooks) {
    workbooks.forEach((pmv, workbook) -> {
      if (workbook != null) {
        closeWorkbook(workbook);
      }
    });
  }

  private static void closeWorkbook(Workbook workbook) {
    try {
      workbook.close();
    } catch (IOException e) {
      Ivy.log().error("Error when close workbook", e);
    }
  }

  public static FileType getFileTypeByExtension(String extension) {
    String fileExtension = String.format(FILE_EXTENSION_FORMAT, StringUtils.lowerCase(extension, Locale.ENGLISH));
    return FileType.fromExtension(fileExtension);
  }
  
  public static void loadFileContentOfCms(Cms selectedCms) {
    IProcessModelVersion selectedPmv = IApplication.current().getProcessModelVersions()
        .filter(pmv -> pmv.getName().equals(selectedCms.getPmvName())).findFirst().orElse(null);
    if (selectedPmv == null) {
      return;
    }

    Optional.ofNullable(ContentManagement.cms(selectedPmv)).flatMap(cms -> cms.get(selectedCms.getUri()))
        .ifPresent(contentObject -> loadFileContentOfCmsContent(selectedCms, contentObject));
  }

  private static void loadFileContentOfCmsContent(Cms cms, ContentObject contentObject) {
    try {
      for (CmsContent cmsContent : cms.getContents()) {
        if (cmsContent == null) {
          break;
        }
        loadCmsFileFromProjectCms(contentObject, cmsContent);
        loadCmsFileFromApplicationCms(cms, cmsContent, IApplication.current());
      }
    } catch (Exception e) {
      Ivy.log().error(e);
    }
  }

  public static void loadCmsFileFromProjectCms(ContentObject contentObject, CmsContent cmsContent) {
    ContentObjectValue value = contentObject.value().get(cmsContent.getLocale());
    byte[] bytes = Optional.ofNullable(value).map(ContentObjectValue::read).map(ContentObjectReader::bytes).orElse(null);
    if (bytes != null) {
      cmsContent.setFileContent(bytes);
      cmsContent.setFileSize(FileUtils.calculateToKB(bytes.length));
    }
  }

  public static void loadCmsFileFromApplicationCms(Cms cms, CmsContent cmsContent, IApplication currentApplication) {
    var cmsEntity = ContentManagement.cms(currentApplication).get(cmsContent.getUri());
    ContentObject currentContentObject =
        cmsEntity.orElseGet(() -> ContentManagement.cms(currentApplication).root().child().file(cms.getUri(), cms.getFileExtension()));
    byte[] bytesOfApplicationCmsFile = currentContentObject.value().get(cmsContent.getLocale()).read().bytes();
    if (bytesOfApplicationCmsFile != null) {
      cmsContent.setApplicationFileContent(bytesOfApplicationCmsFile);
      cmsContent.setApplicationFileSize(FileUtils.calculateToKB(bytesOfApplicationCmsFile.length));
    }
  }

}
