package com.axonivy.utils.cmseditor.utils;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.parser.Parser;

public class Utils {

  private static final String TABLE_ELEMENT = "table";
  private static final String UNORDERED_PATTERN = "<ul> %s </ul>";
  private static final String LIST_ITEM_PATTERN = "<li style='padding:0 2rem 0.25rem 0;'> %s </li>";

  public static String sanitizeContent(String originalContent, String content) {
    var doc = Jsoup.parseBodyFragment(content);
    if (!isOnlyWrappedPlainText(content)) {
      var originalDoc = Jsoup.parseBodyFragment(originalContent);
      migrateTableAttr(originalDoc, doc);
      doc.outputSettings().escapeMode(EscapeMode.base).prettyPrint(true);
      return Parser.unescapeEntities(doc.body().html(), false);
    }
    return doc.body().text();
  }

  public static boolean isOnlyWrappedPlainText(String html) {
    if (html == null || html.isBlank()) {
      return true;
    }
    Document doc = Jsoup.parseBodyFragment(html);
    if (doc.body().childrenSize() != 1) {
      return false;
    }
    var element = doc.body().child(0);
    if (!"p".equals(element.tagName())) {
      return false;
    }
    // Allow only text nodes and <br>
    return element.childNodes().stream()
        .allMatch(node -> node.nodeName().equals("#text") || node.nodeName().equals("br"));
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
