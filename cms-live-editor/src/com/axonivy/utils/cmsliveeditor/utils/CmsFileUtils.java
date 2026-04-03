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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.enums.ExportType;
import com.axonivy.utils.cmsliveeditor.enums.FileType;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;

import ch.ivyteam.ivy.environment.Ivy;

public class CmsFileUtils {
  /**
   * Unified export method for Excel and YAML
   */
  public static StreamedContent exportCmsToZip(String projectName, String applicationName,
      Map<String, PmvCms> pmvCmsMap, ExportType type) throws Exception {
    if (type == ExportType.EXCEL) {
      Map<String, Workbook> workbooks = collectWorkbooks(projectName, pmvCmsMap);
      return convertToZip(projectName, applicationName, workbooks);
    } else {
      Map<String, String> files = collectYamlFiles(projectName, pmvCmsMap);
      return convertToZipYaml(projectName, applicationName, files);
    }
  }

  private static Map<String, Workbook> collectWorkbooks(String projectName, Map<String, PmvCms> pmvCmsMap) {
    Map<String, Workbook> workbooks = new HashMap<>();
    if (StringUtils.isBlank(projectName)) {
      projectName = Ivy.cms().co("/Labels/AllProjects");
      pmvCmsMap.forEach((k, v) -> addPmvCmsToWorkbooks(k, v, workbooks));
    } else {
      addPmvCmsToWorkbooks(projectName, pmvCmsMap.get(projectName), workbooks);
    }
    return workbooks;
  }

  private static void addPmvCmsToWorkbooks(String projectName, PmvCms pmvCms, Map<String, Workbook> workbooks) {
    Workbook workbook = createWorkbookFromPmvCms(pmvCms);
    if (workbook != null) {
      workbooks.put(projectName, workbook);
    }
  }

  private static Workbook createWorkbookFromPmvCms(PmvCms pmvCms) {
    if (pmvCms == null)
      return null;

    var cmsList = pmvCms.getCmsList();
    List<String> headers = new ArrayList<>();
    headers.add(URI_HEADER);
    headers.addAll(pmvCms.getLocales().stream().map(Locale::getLanguage).filter(StringUtils::isNotBlank).toList());

    var workbook = new XSSFWorkbook();
    var sheet = workbook.createSheet(SHEET_NAME);

    // Header
    var headerRow = sheet.createRow(0);
    for (int col = 0; col < headers.size(); col++) {
      headerRow.createCell(col).setCellValue(headers.get(col));
    }

    // Data
    for (int rowIdx = 0; rowIdx < cmsList.size(); rowIdx++) {
      var row = sheet.createRow(rowIdx + 1);
      var cms = cmsList.get(rowIdx);
      for (int colIdx = 0; colIdx < headers.size(); colIdx++) {
        var cell = row.createCell(colIdx);
        if (colIdx == 0) {
          cell.setCellValue(cms.getUri());
        } else {
          cell.setCellValue(getContentValue(cms, headers.get(colIdx)));
        }
      }
    }

    return workbook;
  }

  private static String getContentValue(Cms cms, String language) {
    return cms.getContents().stream().filter(c -> language.equals(c.getLocale().getLanguage())).findFirst()
        .map(CmsContent::getContent).orElse(StringUtils.EMPTY);
  }

  private static StreamedContent convertToZip(String projectName, String applicationName,
      Map<String, Workbook> workbooks) throws Exception {
    try (var baos = new ByteArrayOutputStream(); var zipOut = new ZipOutputStream(baos)) {
      for (Entry<String, Workbook> entry : workbooks.entrySet()) {
        String fileName = String.format(EXCEL_FILE_NAME, entry.getKey());
        zipOut.putNextEntry(new ZipEntry(fileName));
        zipOut.write(convertWorkbookToByteArray(entry.getValue()));
        zipOut.closeEntry();
      }
      zipOut.finish();
      byte[] zipBytes = baos.toByteArray();
      return DefaultStreamedContent.builder()
          .name(String.format(ZIP_FILE_NAME, Ivy.cms().co("/Labels/CMSDownload"), projectName, applicationName))
          .contentType(ZIP_CONTENT_TYPE).stream(() -> new ByteArrayInputStream(zipBytes)).build();
    } finally {
      closeWorkbooks(workbooks);
    }
  }

  private static byte[] convertWorkbookToByteArray(Workbook workbook) throws IOException {
    try (var outputStream = new ByteArrayOutputStream()) {
      workbook.write(outputStream);
      return outputStream.toByteArray();
    }
  }

  private static void closeWorkbooks(Map<String, Workbook> workbooks) {
    workbooks.values().forEach(CmsFileUtils::closeWorkbook);
  }

  private static void closeWorkbook(Workbook workbook) {
    try {
      if (workbook != null)
        workbook.close();
    } catch (IOException e) {
      Ivy.log().error("Error closing workbook", e);
    }
  }

  private static Map<String, String> collectYamlFiles(String projectName, Map<String, PmvCms> pmvCmsMap) {
    Map<String, String> files = new TreeMap<>();
    if (StringUtils.isBlank(projectName)) {
      pmvCmsMap.forEach((pmvName, pmvCms) -> addYamlFilesForProject(files, pmvName, pmvCms, true));
    } else {
      addYamlFilesForProject(files, projectName, pmvCmsMap.get(projectName), false);
    }
    return files;
  }

  private static void addYamlFilesForProject(Map<String, String> files, String projectName, PmvCms pmvCms,
      boolean includeProjectFolder) {
    if (pmvCms == null)
      return;

    List<Locale> locales = pmvCms.getLocales().stream().filter(l -> StringUtils.isNotBlank(l.getLanguage())).toList();

    for (Locale locale : locales) {
      Map<String, String> flat = new HashMap<>();
      for (Cms cmsEntry : pmvCms.getCmsList()) {
        if (cmsEntry == null || cmsEntry.isFile())
          continue;
        flat.put(cmsEntry.getUri(), getContentValue(cmsEntry, locale.getLanguage()));
      }
      String yaml = buildYaml(flat);
      String fileName = "cms_" + locale.getLanguage() + ".yaml";
      String zipEntryName = includeProjectFolder ? projectName + "/" + fileName : fileName;
      files.put(zipEntryName, yaml);
    }
  }

  private static String buildYaml(Map<String, String> flatMap) {
    Map<String, Object> tree = new TreeMap<>();
    flatMap.forEach((key, value) -> insert(tree, key, value));

    StringBuilder yaml = new StringBuilder();
    renderYaml(tree, yaml, 0);
    return yaml.toString();
  }

  @SuppressWarnings("unchecked")
  private static void insert(Map<String, Object> root, String key, String value) {
    String normalized = key;
    if (StringUtils.isNotEmpty(normalized) && normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    if (StringUtils.isBlank(normalized))
      return;

    String[] parts = normalized.split("/");
    Map<String, Object> current = root;
    for (int i = 0; i < parts.length; i++) {
      if (i == parts.length - 1) {
        current.put(parts[i], value);
      } else {
        current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new TreeMap<>());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static void renderYaml(Map<String, Object> map, StringBuilder yaml, int indent) {
    String space = "  ".repeat(indent);
    for (var entry : map.entrySet()) {
      if (entry.getValue() instanceof Map) {
        yaml.append(space).append(entry.getKey()).append(":\n");
        renderYaml((Map<String, Object>) entry.getValue(), yaml, indent + 1);
      } else {
        String value = entry.getValue() == null ? "" : entry.getValue().toString();
        if (value.contains("\n") || value.contains("\r")) {
          yaml.append(space).append(entry.getKey()).append(": |-").append("\n");
          String blockIndent = "  ".repeat(indent + 1);
          for (String line : value.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1)) {
            yaml.append(blockIndent).append(line).append("\n");
          }
        } else {
          yaml.append(space).append(entry.getKey()).append(": ").append(escape(value)).append("\n");
        }
      }
    }
  }

  private static String escape(String value) {
    if (value == null)
      return "\"\"";
    boolean needsQuoting = value.isEmpty() || value.startsWith(" ") || value.endsWith(" ") || value.startsWith("-")
        || value.startsWith("?") || value.startsWith(":") || value.contains(":") || value.contains("#")
        || value.contains("\t") || value.contains("\\") || value.contains("\"");
    if (needsQuoting) {
      String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "\\t");
      return "\"" + escaped + "\"";
    }
    return value;
  }

  private static StreamedContent convertToZipYaml(String projectName, String applicationName,
      Map<String, String> files) {
    try (var baos = new ByteArrayOutputStream(); var zipOut = new ZipOutputStream(baos)) {
      for (Entry<String, String> entry : files.entrySet()) {
        zipOut.putNextEntry(new ZipEntry(entry.getKey()));
        zipOut.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
        zipOut.closeEntry();
      }
      zipOut.finish();
      byte[] zipBytes = baos.toByteArray();
      return DefaultStreamedContent.builder()
          .name(String.format(ZIP_FILE_NAME, Ivy.cms().co("/Labels/CMSDownload"), projectName, applicationName))
          .contentType(ZIP_CONTENT_TYPE).stream(() -> new ByteArrayInputStream(zipBytes)).build();
    } catch (IOException e) {
      Ivy.log().error("Error creating YAML zip", e);
      return null;
    }
  }

  public static FileType getFileTypeByExtension(String extension) {
    String fileExtension = String.format(FILE_EXTENSION_FORMAT, StringUtils.lowerCase(extension, Locale.ENGLISH));
    return FileType.fromExtension(fileExtension);
  }
}
