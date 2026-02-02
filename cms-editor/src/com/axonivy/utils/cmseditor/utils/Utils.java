package com.axonivy.utils.cmseditor.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.parser.Parser;

public class Utils {

  private static final String HTML_TAG_PATTERN = "<.*?>";
  private static final String PLACEHOLDER_PATTERN = "\\{\\d+}";
  private static final String TABLE_ELEMENT = "table";
  private static final String UNORDERED_PATTERN = "<ul> %s </ul>";
  private static final String LIST_ITEM_PATTERN = "<li style='padding:0 2rem 0.25rem 0;'> %s </li>";

  public static String sanitizeContent(String originalContent, String content) {
    var doc = Jsoup.parseBodyFragment(content);
    if (containsHtmlTag(originalContent)) {
      var originalDoc = Jsoup.parseBodyFragment(originalContent);
      migrateTableAttr(originalDoc, doc);
      doc.outputSettings().escapeMode(EscapeMode.base).prettyPrint(true);
      return Parser.unescapeEntities(doc.body().html(), false);
    } else {
      return doc.body().text();
    }
  }

  public static boolean containsHtmlTag(String str) {
    if (Objects.isNull(str)) {
      return false;
    }
    var pattern = Pattern.compile(HTML_TAG_PATTERN);
    return pattern.matcher(str).find();
  }

  /**
   * Validate that HTML syntax is acceptable for the given contents.
   * <p>
   * If the original content is not HTML, this method always returns {@code true}.
   * If the original content is HTML, the new content must also contain HTML tags
   * and be parsable by Jsoup.
   */
  public static boolean isHtmlSyntaxValid(String originalContent, String content) {
    if (!containsHtmlTag(originalContent)) {
      return true;
    }

    if (!containsHtmlTag(content)) {
      return false;
    }

    try {
      // Jsoup is tolerant, but this at least ensures the fragment is parseable.
      Jsoup.parseBodyFragment(content);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Checks whether the new content contains the same set of numbered
   * placeholders as the original content. Placeholders are of the form
   * {@code {1}}, {@code {2}}, ...
   */
  public static boolean hasSamePlaceholders(String originalContent, String content) {
    List<String> originalPlaceholders = extractPlaceholders(originalContent);
    List<String> newPlaceholders = extractPlaceholders(content);

    // If neither contains placeholders, consider them compatible.
    if (originalPlaceholders.isEmpty() && newPlaceholders.isEmpty()) {
      return true;
    }

    Collections.sort(originalPlaceholders);
    Collections.sort(newPlaceholders);
    return originalPlaceholders.equals(newPlaceholders);
  }

  private static List<String> extractPlaceholders(String content) {
    if (content == null || content.isEmpty()) {
      return List.of();
    }

    Pattern pattern = Pattern.compile(PLACEHOLDER_PATTERN);
    Matcher matcher = pattern.matcher(content);
    List<String> placeholders = new ArrayList<>();
    while (matcher.find()) {
      placeholders.add(matcher.group());
    }
    return placeholders;
  }

  private static void migrateTableAttr(Document originalDoc, Document doc) {
    var originalTables = doc.select(TABLE_ELEMENT);
    var tables = doc.select(TABLE_ELEMENT);
    var minSize = Math.min(originalTables.size(), tables.size());

    for (var i = 0; i < minSize; i++) {
      var originalTable = originalTables.get(i);
      var targetTable = tables.get(i);

      // Copy attributes from originalTable to targetTable
      for (var attr : originalTable.attributes()) {
        targetTable.attr(attr.getKey(), attr.getValue());
      }
    }
  }

  public static String convertListToHTMLList(List<String> stringList) {
    var htmlStringBuilder = new StringBuilder();
    for (String item : stringList) {
      htmlStringBuilder.append(String.format(LIST_ITEM_PATTERN, item));
    }

    return String.format(UNORDERED_PATTERN, htmlStringBuilder.toString());
  }

}
