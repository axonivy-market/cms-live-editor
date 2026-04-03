package com.axonivy.utils.cmsliveeditor.utils;

import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.EXCEL_FILE_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.FILE_EXTENSION_FORMAT;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.SHEET_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.URI_HEADER;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.ZIP_CONTENT_TYPE;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.ZIP_FILE_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.CommonConstants.HYPHEN;
import static org.apache.commons.lang3.StringUtils.*;

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
  private static final String SLASH = "/";
  private static final String DOUBLE_QUOTE = "\"";
  private static final List<String> YAML_PREFIXES = List.of(SPACE, HYPHEN, "?", ":");
  private static final List<String> YAML_SPECIALS = List.of(":", "#", "\t", "\\", DOUBLE_QUOTE);

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
      pmvCmsMap.forEach((pmvName, pmvCms) -> addCmsYamlFilesToArchive(files, pmvName, pmvCms, true));
    } else {
      addCmsYamlFilesToArchive(files, projectName, pmvCmsMap.get(projectName), false);
    }
    return files;
  }

  private static void addCmsYamlFilesToArchive(Map<String, String> archiveFiles, String projectName, PmvCms cmsData,
      boolean includeProjectFolderInPath) {
    if (cmsData == null)
      return;

    List<Locale> validLocales =
        cmsData.getLocales().stream().filter(locale -> StringUtils.isNotBlank(locale.getLanguage())).toList();

    for (Locale locale : validLocales) {
      Map<String, String> uriToContentMap = new HashMap<>();

      for (Cms cmsEntry : cmsData.getCmsList()) {
        if (cmsEntry == null || cmsEntry.isFile())
          continue;

        uriToContentMap.put(cmsEntry.getUri(), getContentValue(cmsEntry, locale.getLanguage()));
      }

      String yamlContent = convertFlatMapToYaml(uriToContentMap);
      String fileName = "cms_" + locale.getLanguage() + ".yaml";

      String archiveEntryPath = includeProjectFolderInPath ? projectName + SLASH + fileName : fileName;

      archiveFiles.put(archiveEntryPath, yamlContent);
    }
  }

  private static String convertFlatMapToYaml(Map<String, String> flatKeyValueMap) {
    Map<String, Object> hierarchicalMap = new TreeMap<>();
    flatKeyValueMap.forEach((key, value) -> insertPathIntoTree(hierarchicalMap, key, value));

    StringBuilder yamlBuilder = new StringBuilder();
    buildYamlString(hierarchicalMap, yamlBuilder, 0);
    return yamlBuilder.toString();
  }

  @SuppressWarnings("unchecked")
  private static void insertPathIntoTree(Map<String, Object> rootMap, String path, String value) {
    String normalizedPath = path;

    if (StringUtils.isNotEmpty(normalizedPath) && normalizedPath.startsWith(SLASH)) {
      normalizedPath = normalizedPath.substring(1);
    }

    if (StringUtils.isBlank(normalizedPath))
      return;

    String[] pathSegments = normalizedPath.split(SLASH);
    Map<String, Object> currentNode = rootMap;

    for (int i = 0; i < pathSegments.length; i++) {
      String segment = pathSegments[i];

      if (i == pathSegments.length - 1) {
        currentNode.put(segment, value);
      } else {
        currentNode = (Map<String, Object>) currentNode.computeIfAbsent(segment, key -> new TreeMap<>());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static void buildYamlString(Map<String, Object> currentMap, StringBuilder yamlBuilder, int indentLevel) {
    String indentSpaces = generateIndent(indentLevel);

    for (var entry : currentMap.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (value instanceof Map<?, ?> nestedMap) {
        yamlBuilder.append(indentSpaces).append(key).append(":\n");
        buildYamlString((Map<String, Object>) nestedMap, yamlBuilder, indentLevel + 1);
      } else {
        String stringValue = value == null ? EMPTY : value.toString();

        if (containsLineBreak(stringValue)) {
          yamlBuilder.append(indentSpaces).append(key).append(": |-").append(LF);

          String blockIndent = generateIndent(indentLevel + 1);
          for (String line : splitIntoLines(stringValue)) {
            yamlBuilder.append(blockIndent).append(line).append(LF);
          }
        } else {
          yamlBuilder.append(indentSpaces).append(key).append(": ").append(escapeYamlValue(stringValue)).append(LF);
        }
      }
    }
  }

  private static boolean containsLineBreak(String value) {
    return value.contains(LF) || value.contains(CR);
  }

  private static String[] splitIntoLines(String value) {
    String normalized = value.replace("\r\n", LF).replace(CR, LF);

    return normalized.split(LF, INDEX_NOT_FOUND);
  }

  private static String generateIndent(int indentLevel) {
    return StringUtils.repeat(SPACE, indentLevel);
  }

  private static String escapeYamlValue(String value) {
    if (value == null)
      return "\"\"";

    if (!requiresQuoting(value))
      return value;

    return DOUBLE_QUOTE + escapeYamlSpecialCharacters(value) + DOUBLE_QUOTE;
  }

  private static boolean requiresQuoting(String value) {
    if (value.isEmpty() || value.endsWith(SPACE) || YAML_PREFIXES.stream().anyMatch(value::startsWith)) {
      return true;
    }

    return YAML_SPECIALS.stream().anyMatch(value::contains);
  }

  private static String escapeYamlSpecialCharacters(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\t", "\\t");
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
