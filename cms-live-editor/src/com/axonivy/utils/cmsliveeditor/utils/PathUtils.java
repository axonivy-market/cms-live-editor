package com.axonivy.utils.cmsliveeditor.utils;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.cmsliveeditor.constants.CommonConstants;

public class PathUtils {

  private static final int MIN_SEGMENT_COUNT = 3;
  private static final int SECOND_LAST_SEGMENT_OFFSET = 2;
  private static final int LAST_SEGMENT_OFFSET = 1;
  // Oh My Posh - agnoster_short with max_depth = 2 - formatter
  private static final String AGNOSTER_SHORT_MAX_DEPTH_2_FORMATTER = "../%s/%s";

  /**
   * Returns a shortened display form of the given Path by joining the last two path segments with a slash,
   * prefixed with "../".
   * For example, {@code /com/axonivy/utils/demo/EmailFormat/name} becomes {@code ../EmailFormat/name}.
   * {@code /EmailFormat/name} becomes {@code /EmailFormat/name}.
   *
   * @param path the full Path
   * @return the shortened URI, or the original path if it has fewer than two segments of CMS
   */
  public static String getLastTwoPathSegments(String path) {
    if (StringUtils.isBlank(path)) {
      return StringUtils.EMPTY;
    }

    String[] parts = path.split(CommonConstants.SLASH_CHARACTER);
    if (parts.length <= MIN_SEGMENT_COUNT) {
      return path;
    }

    return String.format(AGNOSTER_SHORT_MAX_DEPTH_2_FORMATTER, parts[parts.length - SECOND_LAST_SEGMENT_OFFSET],
        parts[parts.length - LAST_SEGMENT_OFFSET]);
  }
}
