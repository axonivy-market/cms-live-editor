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

  public static final String COLON_SPACE = ": ";
  public static final String BLOCK_SCALAR = ": |-";
  public static final String EMPTY_QUOTES = "\"\"";

  // Line ending
  public static final String CRLF = "\r\n";
  // Escaping
  public static final String ESCAPED_BACKSLASH = "\\\\";
  public static final String ESCAPED_DOUBLE_QUOTE = "\\\"";
  public static final String ESCAPED_TAB = "\\t";

  public static final Map<String, String> YAML_ESCAPE_MAP = Map.of(CommonConstants.BACKSLASH, ESCAPED_BACKSLASH,
      CommonConstants.DOUBLE_QUOTE, ESCAPED_DOUBLE_QUOTE, CommonConstants.TAB, ESCAPED_TAB);

  // Special prefixes (need quoting)
  public static final List<String> YAML_PREFIXES =
      List.of(CommonConstants.SPACE, CommonConstants.HYPHEN_CHARACTER, CommonConstants.QUESTION_MARK,
          CommonConstants.COLON);

  // Special characters (require escaping/quoting)
  public static final List<String> YAML_SPECIALS = List.of(CommonConstants.COLON, CommonConstants.HASH,
      CommonConstants.TAB, CommonConstants.BACKSLASH, CommonConstants.DOUBLE_QUOTE);

  // YAML keywords (must be quoted)
  public static final Set<String> YAML_KEYWORDS =
      Set.of("true", "false", "null", CommonConstants.TILDE, "yes", "no", "on", "off");

}
