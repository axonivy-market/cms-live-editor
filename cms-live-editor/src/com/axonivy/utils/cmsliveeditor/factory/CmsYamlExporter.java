package com.axonivy.utils.cmsliveeditor.factory;

import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.DOUBLE_QUOTE;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.YAML_ESCAPE_MAP;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.YAML_KEYWORDS;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.YAML_PREFIXES;
import static com.axonivy.utils.cmsliveeditor.constants.FileConstants.YAML_SPECIALS;
import static org.apache.commons.lang3.StringUtils.CR;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.INDEX_NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.SPACE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.constants.CommonConstants;
import com.axonivy.utils.cmsliveeditor.constants.FileConstants;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;
import com.axonivy.utils.cmsliveeditor.utils.CmsContentUtils;
import com.axonivy.utils.cmsliveeditor.utils.CmsFileUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class CmsYamlExporter implements CmsExporter {

  private static final String YAML_FILE_FORMAT = "cms_%s.yaml";

  @Override
  public StreamedContent export(String projectName, String applicationName, Map<String, PmvCms> pmvCmsMap) {
    Map<String, byte[]> cmsFiles = new HashMap<>();
    Map<String, String> yamlFiles = collectYamlFilesAndCmsFiles(projectName, pmvCmsMap, cmsFiles);
    return convertToZipYaml(projectName, applicationName, yamlFiles, cmsFiles);
  }

  private StreamedContent convertToZipYaml(String projectName, String applicationName, Map<String, String> files,
      Map<String, byte[]> cmsFiles) {
    try (var baos = new ByteArrayOutputStream(); var zipOut = new ZipOutputStream(baos)) {
      for (Entry<String, String> entry : files.entrySet()) {
        writeTextEntry(zipOut, entry.getKey(), entry.getValue());
      }
      CmsFileUtils.writeCmsFileToZip(cmsFiles, zipOut);
      zipOut.finish();

      return CmsFileUtils.buildStreamedContent(baos.toByteArray(), projectName, applicationName);
    } catch (IOException e) {
      Ivy.log().error("Error creating YAML zip", e);
      return null;
    }
  }

  private Map<String, String> collectYamlFilesAndCmsFiles(String projectName, Map<String, PmvCms> pmvCmsMap,
      Map<String, byte[]> cmsFiles) {
    Map<String, String> files = new TreeMap<>();
    if (StringUtils.isBlank(projectName)) {
      for (var entry : pmvCmsMap.entrySet()) {
        addCmsYamlFilesToArchive(files, entry.getValue(), true);
        CmsFileUtils.addPmvCmsFiles(entry.getKey(), entry.getValue(), cmsFiles);
      }
    } else {
      addCmsYamlFilesToArchive(files, pmvCmsMap.get(projectName), false);
      CmsFileUtils.addPmvCmsFiles(projectName, pmvCmsMap.get(projectName), cmsFiles);
    }
    return files;
  }

  /**
   * Converts CMS data into YAML files (one per locale) and adds them to the archive map.
   *
   * Flow: 1. Filter valid locales (non-empty language) 2. Build URI → localized content map 3. Convert map to YAML 4.
   * Generate archive path (optionally grouped by project)
   */
  private void addCmsYamlFilesToArchive(Map<String, String> archiveFiles, PmvCms cmsData,
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
  private Map<String, String> buildUriToContentMap(PmvCms cmsData, Locale locale) {
    Map<String, String> localizedContentByUri = new HashMap<>();
    for (Cms cmsEntry : cmsData.getCmsList()) {
      if (cmsEntry == null || cmsEntry.isFile()) {
        continue;
      }
      localizedContentByUri.put(cmsEntry.getUri(),
          CmsContentUtils.getContentValueByLanguage(cmsEntry, locale.getLanguage()));
    }
    return localizedContentByUri;
  }

  private String buildArchivePath(PmvCms cmsData, Locale locale, boolean includeProjectFolderInPath) {
    String fileName = String.format(YAML_FILE_FORMAT, locale.getLanguage());
    if (includeProjectFolderInPath) {
      return cmsData.getPmvName() + CommonConstants.SLASH_CHARACTER + fileName;
    }
    return fileName;
  }

  private String convertFlatMapToYaml(Map<String, String> flatKeyValueMap) {
    Map<String, Object> hierarchicalMap = new TreeMap<>();
    for (var entry : flatKeyValueMap.entrySet()) {
      insertPathIntoTree(hierarchicalMap, entry.getKey(), entry.getValue());
    }
    StringBuilder yamlBuilder = new StringBuilder();
    buildYamlString(hierarchicalMap, yamlBuilder, 0);
    return yamlBuilder.toString();
  }

  @SuppressWarnings("unchecked")
  private void insertPathIntoTree(Map<String, Object> rootMap, String path, String value) {
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
  private void buildYamlString(Map<String, Object> currentMap, StringBuilder yamlBuilder, int indentLevel) {
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

  private boolean containsLineBreak(String value) {
    return value.contains(LF) || value.contains(CR);
  }

  private String[] splitIntoLines(String value) {
    String normalized = value.replace("\r\n", LF).replace(CR, LF);
    return normalized.split(LF, INDEX_NOT_FOUND);
  }

  private String generateIndent(int indentLevel) {
    return StringUtils.repeat(SPACE, indentLevel);
  }

  /**
   * Escapes a YAML value if necessary.
   *
   * Rules: - If value does NOT require quoting → return as-is - Otherwise: - Escape special characters (\, ", tab,
   * etc.) - Wrap in double quotes
   *
   * Example: hello → hello true → "true" (keyword) value:123 → "value:123" (contains special char)
   */
  private String escapeYamlValue(String value) {
    if (value == null) {
      return "\"\"";
    }
    if (!requiresQuoting(value)) {
      return value;
    }
    return FileConstants.DOUBLE_QUOTE + escapeYamlSpecialCharacters(value) + DOUBLE_QUOTE;
  }

  /**
   * Determines whether a YAML value must be quoted.
   *
   * Quoting is required if: - Value can be misinterpreted (e.g. "true", "null", "yes") - Starts with special YAML
   * prefixes (?, :, -, space) - Contains special characters (:, #, \, ", tab) - Ends with whitespace
   */
  private boolean requiresQuoting(String value) {
    if (isPotentiallyMisinterpretedByYaml(value)) {
      return true;
    }
    return YAML_SPECIALS.stream().anyMatch(value::contains);
  }

  /**
   * Detects values that YAML might interpret incorrectly.
   *
   * Examples: "true" → boolean "null" → null "yes" → boolean "" → empty
   *
   * Also checks: - Leading special characters - Trailing spaces
   */
  private boolean isPotentiallyMisinterpretedByYaml(String value) {
    return value == null || value.isEmpty() || value.endsWith(SPACE)
        || YAML_PREFIXES.stream().anyMatch(value::startsWith) || YAML_KEYWORDS.contains(value.toLowerCase(Locale.ROOT));
  }

  /**
   * Escapes special YAML characters using predefined mappings.
   *
   * Current mappings: \ → \\ " → \" tab → \t
   *
   * Applied only when quoting is required.
   */
  private String escapeYamlSpecialCharacters(String value) {
    String result = value;
    for (var entry : YAML_ESCAPE_MAP.entrySet()) {
      result = result.replace(entry.getKey(), entry.getValue());
    }
    return result;
  }

  private void writeTextEntry(ZipOutputStream zipOut, String name, String content) throws IOException {
    zipOut.putNextEntry(new ZipEntry(name));
    zipOut.write(content.getBytes(StandardCharsets.UTF_8));
    zipOut.closeEntry();
  }
}