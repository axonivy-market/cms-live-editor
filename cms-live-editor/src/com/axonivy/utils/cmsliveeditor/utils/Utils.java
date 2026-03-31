package com.axonivy.utils.cmsliveeditor.utils;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.parser.Parser;

public class Utils {
  private static final Pattern VALID_HTML_TAG_PATTERN = Pattern.compile("</?[A-Za-z][^>]*>");
  private static final String TABLE_ELEMENT = "table";
  private static final String UNORDERED_PATTERN = "<ul> %s </ul>";
  private static final String LIST_ITEM_PATTERN = "<li style='padding:0 2rem 0.25rem 0;'> %s </li>";
  private static final String PARAGRAPH_TAG = "p";
  private static final String TEXT_NODE = "#text";
  private static final String BR_TAG = "br";
  private static final String LESS_THAN = "<";
  private static final String GREATER_THAN = ">";
  private static final String ESCAPED_LESS_THAN = "&lt;";
  private static final String ESCAPED_GREATER_THAN = "&gt;";
  private static final String HTML_TAG_PATTERN = "<.*?>";

  public static String sanitizeContent(String originalContent, String content) {
    if (!containsHtmlTag(originalContent)) {
      var safeContent = escapeNonTagAngleBrackets(content);
      var doc = Jsoup.parseBodyFragment(safeContent);
      return Parser.unescapeEntities(doc.body().text(), false);
    }

    var doc = Jsoup.parseBodyFragment(content);
    if (!isOnlyWrappedPlainText(content)) {
      var originalDoc = Jsoup.parseBodyFragment(originalContent);
      migrateTableAttr(originalDoc, doc);
      doc.outputSettings().escapeMode(EscapeMode.base).prettyPrint(true);
      return Parser.unescapeEntities(doc.body().html(), false);
    }
    return doc.body().text();
  }

  private static String escapeNonTagAngleBrackets(String content) {
    if (content == null)
      return null;

    Matcher matcher = VALID_HTML_TAG_PATTERN.matcher(content);
    StringBuilder result = new StringBuilder();
    int last = 0;

    while (matcher.find()) {
      result.append(escapeAngles(content.substring(last, matcher.start())));
      result.append(matcher.group());
      last = matcher.end();
    }

    result.append(escapeAngles(content.substring(last)));
    return result.toString();
  }

  private static String escapeAngles(String text) {
    return text.replace(LESS_THAN, ESCAPED_LESS_THAN).replace(GREATER_THAN, ESCAPED_GREATER_THAN);
  }

  public static boolean containsHtmlTag(String str) {
    if (Objects.isNull(str)) {
      return false;
    }
    var pattern = Pattern.compile(HTML_TAG_PATTERN);
    return pattern.matcher(str).find();
  }

  public static boolean isOnlyWrappedPlainText(String html) {
    if (StringUtils.isBlank(html)) {
      return true;
    }
    Element docBody = Jsoup.parseBodyFragment(html).body();
    if (docBody.childrenSize() != 1) {
      return false;
    }
    var element = docBody.child(0);
    if (!PARAGRAPH_TAG.equals(element.tagName())) {
      return false;
    }
    // Allow only text nodes and <br>
    return element.childNodes().stream()
        .allMatch(node -> node.nodeName().equals(TEXT_NODE) || node.nodeName().equals(BR_TAG));
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
