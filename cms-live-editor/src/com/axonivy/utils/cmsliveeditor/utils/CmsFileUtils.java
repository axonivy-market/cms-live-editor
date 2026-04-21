package com.axonivy.utils.cmsliveeditor.utils;

import static com.axonivy.utils.cmsliveeditor.constants.CommonConstants.HYPHEN_CHARACTER;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.EXCEL_FILE_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.FILE_EXTENSION_FORMAT;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.SHEET_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.URI_HEADER;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.ZIP_CONTENT_TYPE;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.ZIP_FILE_NAME;
import static org.apache.commons.lang3.StringUtils.CR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.INDEX_NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.SPACE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.constants.CommonConstants;
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
  private static final String DOUBLE_QUOTE = "\"";
  private static final Map<String, String> YAML_ESCAPE_MAP = Map.of("\\", "\\\\", "\"", "\\\"", "\t", "\\t");
  private static final List<String> YAML_PREFIXES = List.of(SPACE, HYPHEN_CHARACTER, "?", ":");
  private static final List<String> YAML_SPECIALS = List.of(":", "#", "\t", "\\", DOUBLE_QUOTE);
  private static final Set<String> YAML_KEYWORDS = Set.of("true", "false", "null", "~", "yes", "no", "on", "off");
  private static final String YAML_FILE_FORMAT = "cms_%s.yaml";

  private static final Path BASE_PATH = Path.of("virtual-root").toAbsolutePath().normalize();

  public static Map<String, byte[]> collectCmsFiles(String projectName, PmvCms pmvCms) {
    if (pmvCms == null) {
      return new HashMap<>();
    }
    return pmvCms.getCmsList().stream().filter(Cms::isFile).peek(CmsFileUtils::loadFileContentOfCms)
        .flatMap(cms -> cms.getContents().stream()).filter(Objects::nonNull).filter(CmsContent::isFile)
        .map(content -> toZipEntry(projectName, content, BASE_PATH)).filter(Objects::nonNull)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
          return a;
        }));
  }

  private static Map.Entry<String, byte[]> toZipEntry(String projectName, CmsContent content, Path basePath) {
    try {
      byte[] data = resolveFileContent(content);
      if (data == null || data.length == 0) {
        Ivy.log().error("File Content not found for: " + content.getFileName());
        return null;
      }

      String uri = FileUtils.normalizeUri(content.getUri());
      String fileName = content.getFileName();

      if (!FileUtils.isValidFileName(fileName)) {
        Ivy.log().warn("Invalid filename: " + fileName);
        return null;
      }

      if (!FileUtils.isSafePath(basePath, uri)) {
        Ivy.log().warn("Blocked path traversal: " + uri);
        return null;
      }

      String zipEntryPath = FileUtils.buildNormalizedPath(projectName, uri);

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

  public static byte[] convertWorkbookToByteArray(Workbook workbook) throws IOException {
    try (var outputStream = new ByteArrayOutputStream()) {
      workbook.write(outputStream);
      return outputStream.toByteArray();
    }
  }

  public static void closeWorkbooks(Map<String, Workbook> workbooks) {
    workbooks.values().forEach(CmsFileUtils::closeWorkbook);
  }

  private static void closeWorkbook(Workbook workbook) {
    try {
      if (workbook != null) {
        workbook.close();
      }
    } catch (IOException e) {
      Ivy.log().error("Error closing workbook", e);
    }
  }

  public static Map<String, Workbook> collectWorkbooksAndCmsFiles(String projectName, Map<String, PmvCms> pmvCmsMap,
      Map<String, byte[]> cmsFiles) {
    Map<String, Workbook> workbooks = new HashMap<>();
    if (StringUtils.isBlank(projectName)) {
      projectName = Ivy.cms().co("/Labels/AllProjects");
      for (var entry : pmvCmsMap.entrySet()) {
        addPmvCmsToWorkbooks(entry.getKey(), entry.getValue(), workbooks);
        addPmvCmsFiles(entry.getKey(), entry.getValue(), cmsFiles);
      }
    } else {
      addPmvCmsToWorkbooks(projectName, pmvCmsMap.get(projectName), workbooks);
      addPmvCmsFiles(projectName, pmvCmsMap.get(projectName), cmsFiles);
    }
    return workbooks;
  }

  public static Map<String, String> collectYamlFilesAndCmsFiles(String projectName, Map<String, PmvCms> pmvCmsMap,
      Map<String, byte[]> cmsFiles) {
    Map<String, String> files = new TreeMap<>();
    if (projectName == null || projectName.isBlank()) {
      for (var entry : pmvCmsMap.entrySet()) {
        addCmsYamlFilesToArchive(files, entry.getValue(), true);
        addPmvCmsFiles(entry.getKey(), entry.getValue(), cmsFiles);
      }
    } else {
      addCmsYamlFilesToArchive(files, pmvCmsMap.get(projectName), false);
      addPmvCmsFiles(projectName, pmvCmsMap.get(projectName), cmsFiles);
    }
    return files;
  }

  /**
   * Converts CMS data into YAML files (one per locale) and adds them to the archive map.
   *
   * Flow:
   * 1. Filter valid locales (non-empty language)
   * 2. Build URI → localized content map
   * 3. Convert map to YAML
   * 4. Generate archive path (optionally grouped by project)
   */
  private static void addCmsYamlFilesToArchive(Map<String, String> archiveFiles, PmvCms cmsData,
      boolean includeProjectFolderInPath) {
    if (cmsData == null) {
      return;
    }

    List<Locale> validLocales =
        cmsData.getLocales().stream().filter(locale -> StringUtils.isNotBlank(locale.getLanguage())).toList();

    for (Locale locale : validLocales) {
      Map<String, String> uriToContentMap = buildUriToContentMap(cmsData, locale);

      String yamlContent = convertFlatMapToYaml(uriToContentMap);
      String archiveEntryPath = buildArchivePath(cmsData, locale, includeProjectFolderInPath);

      archiveFiles.put(archiveEntryPath, yamlContent);
    }
  }

  /**
   * Builds a map of URI → localized content for a given locale. If CMS entries are files, skip them.
   */
  private static Map<String, String> buildUriToContentMap(PmvCms cmsData, Locale locale) {
    Map<String, String> localizedContentByUri = new HashMap<>();

    for (Cms cmsEntry : cmsData.getCmsList()) {
      if (cmsEntry == null || cmsEntry.isFile()) {
        continue;
      }

      localizedContentByUri.put(cmsEntry.getUri(), getContentValue(cmsEntry, locale.getLanguage()));
    }

    return localizedContentByUri;
  }

  private static String buildArchivePath(PmvCms cmsData, Locale locale, boolean includeProjectFolderInPath) {
    String fileName = String.format(YAML_FILE_FORMAT, locale.getLanguage());

    if (includeProjectFolderInPath) {
      return cmsData.getPmvName() + CommonConstants.SLASH_CHARACTER + fileName;
    }

    return fileName;
  }

  private static String convertFlatMapToYaml(Map<String, String> flatKeyValueMap) {
    Map<String, Object> hierarchicalMap = new TreeMap<>();
    for (var entry : flatKeyValueMap.entrySet()) {
      insertPathIntoTree(hierarchicalMap, entry.getKey(), entry.getValue());
    }

    StringBuilder yamlBuilder = new StringBuilder();
    buildYamlString(hierarchicalMap, yamlBuilder, 0);
    return yamlBuilder.toString();
  }

  @SuppressWarnings("unchecked")
  private static void insertPathIntoTree(Map<String, Object> rootMap, String path, String value) {
    String normalizedPath = path;

    if (StringUtils.isNotEmpty(normalizedPath) && normalizedPath.startsWith(CommonConstants.SLASH_CHARACTER)) {
      normalizedPath = normalizedPath.substring(1);
    }

    if (StringUtils.isBlank(normalizedPath)) {
      return;
    }

    String[] pathSegments = normalizedPath.split(CommonConstants.SLASH_CHARACTER);
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

  /**
   * Escapes a YAML value if necessary.
   *
   * Rules:
   * - If value does NOT require quoting → return as-is
   * - Otherwise:
   *     - Escape special characters (\, ", tab, etc.)
   *     - Wrap in double quotes
   *
   * Example:
   *   hello        → hello
   *   true         → "true"   (keyword)
   *   value:123    → "value:123" (contains special char)
   */
  private static String escapeYamlValue(String value) {
    if (value == null) {
      return "\"\"";
    }

    if (!requiresQuoting(value)) {
      return value;
    }

    return DOUBLE_QUOTE + escapeYamlSpecialCharacters(value) + DOUBLE_QUOTE;
  }

  /**
   * Determines whether a YAML value must be quoted.
   *
   * Quoting is required if:
   * - Value can be misinterpreted (e.g. "true", "null", "yes")
   * - Starts with special YAML prefixes (?, :, -, space)
   * - Contains special characters (:, #, \, ", tab)
   * - Ends with whitespace
   */
  private static boolean requiresQuoting(String value) {
    if (isPotentiallyMisinterpretedByYaml(value)) {
      return true;
    }

    return YAML_SPECIALS.stream().anyMatch(value::contains);
  }

  /**
   * Detects values that YAML might interpret incorrectly.
   *
   * Examples:
   *   "true"  → boolean
   *   "null"  → null
   *   "yes"   → boolean
   *   ""      → empty
   *
   * Also checks:
   * - Leading special characters
   * - Trailing spaces
   */
  private static boolean isPotentiallyMisinterpretedByYaml(String value) {
    return value == null || value.isEmpty() || value.endsWith(SPACE) || YAML_PREFIXES.stream().anyMatch(value::startsWith)
        || YAML_KEYWORDS.contains(value.toLowerCase(Locale.ROOT));
  }

  /**
   * Escapes special YAML characters using predefined mappings.
   *
   * Current mappings:
   *   \  → \\
   *   "  → \"
   *   tab → \t
   *
   * Applied only when quoting is required.
   */
  private static String escapeYamlSpecialCharacters(String value) {
    String result = value;
    for (var entry : YAML_ESCAPE_MAP.entrySet()) {
      result = result.replace(entry.getKey(), entry.getValue());
    }
    return result;
  }

  /**
   * Packages YAML files into a ZIP archive.
   *
   * Steps:
   * 1. Create ZIP output stream
   * 2. Add each YAML file as a ZipEntry
   * 3. Convert to byte array
   * 4. Wrap into StreamedContent for download
   */
  public static StreamedContent convertToZipYaml(String projectName, String applicationName, Map<String, String> files,
      Map<String, byte[]> cmsFiles) {
    try (var baos = new ByteArrayOutputStream(); var zipOut = new ZipOutputStream(baos)) {
      for (Entry<String, String> entry : files.entrySet()) {
        zipOut.putNextEntry(new ZipEntry(entry.getKey()));
        zipOut.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
        zipOut.closeEntry();
      }

      writeCmsFileToZip(cmsFiles, zipOut);
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

  public static StreamedContent convertToZip(String projectName, String applicationName,
      Map<String, Workbook> workbooks, Map<String, byte[]> files) throws Exception {

    try (var baos = new ByteArrayOutputStream(); var zipOut = new ZipOutputStream(baos)) {

      // Excel files
      for (Entry<String, Workbook> entry : workbooks.entrySet()) {
        var fileName = String.format(EXCEL_FILE_NAME, entry.getKey());
        zipOut.putNextEntry(new ZipEntry(fileName));
        zipOut.write(CmsFileUtils.convertWorkbookToByteArray(entry.getValue()));
        zipOut.closeEntry();
      }

      writeCmsFileToZip(files, zipOut);
      zipOut.close();

      byte[] zipBytes = baos.toByteArray();

      return DefaultStreamedContent.builder()
          .name(String.format(ZIP_FILE_NAME, Ivy.cms().co("/Labels/CMSDownload"), projectName, applicationName))
          .contentType(ZIP_CONTENT_TYPE).stream(() -> new ByteArrayInputStream(zipBytes)).build();

    } finally {
      CmsFileUtils.closeWorkbooks(workbooks);
    }
  }

  private static void writeCmsFileToZip(Map<String, byte[]> files, ZipOutputStream zipOut) throws IOException {
    // CMS files
    for (Entry<String, byte[]> entry : files.entrySet()) {
      zipOut.putNextEntry(new ZipEntry(entry.getKey()));
      zipOut.write(entry.getValue());
      zipOut.closeEntry();
    }
  }

  public static FileType getFileTypeByExtension(String extension) {
    String fileExtension = String.format(FILE_EXTENSION_FORMAT, StringUtils.lowerCase(extension, Locale.ENGLISH));
    return FileType.fromExtension(fileExtension);
  }

  public static void addPmvCmsToWorkbooks(String projectName, PmvCms pmvCms, Map<String, Workbook> workbooks) {
    var workbook = createWorkbookFromPmvCms(pmvCms);
    if (workbook != null) {
      workbooks.put(projectName, workbook);
    }
  }

  public static void addPmvCmsFiles(String projectName, PmvCms pmvCms, Map<String, byte[]> cmsFiles) {
    var files = collectCmsFiles(projectName, pmvCms);
    if (files != null) {
      cmsFiles.putAll(files);
    }
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
    byte[] bytes =
        Optional.ofNullable(value).map(ContentObjectValue::read).map(ContentObjectReader::bytes).orElse(null);
    if (bytes != null) {
      cmsContent.setFileContent(bytes);
      cmsContent.setFileSize(FileUtils.calculateToKB(bytes.length));
    }
  }

  public static void loadCmsFileFromApplicationCms(Cms cms, CmsContent cmsContent, IApplication currentApplication) {
    var cmsEntity = ContentManagement.cms(currentApplication).get(cmsContent.getUri());
    ContentObject currentContentObject = cmsEntity.orElseGet(
        () -> ContentManagement.cms(currentApplication).root().child().file(cms.getUri(), cms.getFileExtension()));
    byte[] bytesOfApplicationCmsFile = currentContentObject.value().get(cmsContent.getLocale()).read().bytes();
    if (bytesOfApplicationCmsFile != null) {
      cmsContent.setApplicationFileContent(bytesOfApplicationCmsFile);
      cmsContent.setApplicationFileSize(FileUtils.calculateToKB(bytesOfApplicationCmsFile.length));
    }
  }

}
