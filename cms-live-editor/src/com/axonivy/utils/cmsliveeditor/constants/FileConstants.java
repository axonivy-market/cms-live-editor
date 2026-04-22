package com.axonivy.utils.cmsliveeditor.constants;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileConstants {

  public static final int BYTE_IN_KB = 1024;
  public static final int KB_IN_MB = 1024;
  public static final int DEFAULT_VALID_SIZE_MB = 1;
  public static final String SHEET_NAME = "cms";
  public static final String URI_HEADER = "Uri";
  public static final String ZIP_CONTENT_TYPE = "application/zip";
  public static final String EXCEL_FILE_NAME = "%s.xlsx";
  public static final String ZIP_FILE_NAME = "%s_%s_%s.zip";
  public static final String CMS_FILE_FORMAT = "%s_%s.%s";
  public static final String FILE_EXTENSION_FORMAT = ".%s";

  // YAML Constant
  public static final String DOUBLE_QUOTE = "\"";
  public static final String BACKSLASH = "\\";
  public static final String TAB = "\t";
  public static final String COLON = ":";
  public static final String HASH = "#";
  public static final String QUESTION_MARK = "?";
  public static final String TILDE = "~";

  // YAML file
  public static final String YAML_FILE_FORMAT = "cms_%s.yaml";

  // Escaping
  public static final Map<String, String> YAML_ESCAPE_MAP = Map.of(BACKSLASH, "\\\\", DOUBLE_QUOTE, "\\\"", TAB, "\\t");

  // Special prefixes (need quoting)
  public static final List<String> YAML_PREFIXES = List.of(" ", // SPACE (keep literal for readability)
      "-", // HYPHEN
      QUESTION_MARK, COLON);

  // Special characters (require escaping/quoting)
  public static final List<String> YAML_SPECIALS = List.of(COLON, HASH, TAB, BACKSLASH, DOUBLE_QUOTE);

  // YAML keywords (must be quoted)
  public static final Set<String> YAML_KEYWORDS = Set.of("true", "false", "null", TILDE, "yes", "no", "on", "off");

}
