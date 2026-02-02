package com.axonivy.utils.cmseditor.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.cmseditor.utils.Utils;

public class UtilsTest {

  @Test
  public void testReformatHTML_containsHtmlTag() {
    String originalContent = "<p>Original Content</p>";
    String content = "<p>Some content with <table><tr><td>data</td></tr></table> table.</p>";

    String expected = """
        <p>Some content with</p>
        <table>
         <tbody>
          <tr>
           <td>data</td>
          </tr>
         </tbody>
        </table> table.
        <p></p>
        """.trim();
    String result = Utils.sanitizeContent(originalContent, content);
    assertEquals(expected, result);
  }

  @Test
  public void testReformatHTML_noHtmlTag() {
    String originalContent = "Original Content";
    String content = "Some plain content without HTML.";

    String expected = "Some plain content without HTML.";
    String result = Utils.sanitizeContent(originalContent, content);

    assertEquals(expected, result);
  }

  @Test
  public void testContainsHtmlTag_true() {
    String htmlString = "<p>This is a paragraph.</p>";

    assertTrue(Utils.containsHtmlTag(htmlString));
  }

  @Test
  public void testContainsHtmlTag_false() {
    String plainString = "This is a plain text.";

    assertFalse(Utils.containsHtmlTag(plainString));
  }

  @Test
  public void testConvertListToHTMLList() {
    List<String> items = Arrays.asList("Item 1", "Item 2", "Item 3");

    String expected = "<ul> <li style='padding:0 2rem 0.25rem 0;'> Item 1 </li><li style='padding:0 2rem 0.25rem 0;'> Item 2 </li><li style='padding:0 2rem 0.25rem 0;'> Item 3 </li> </ul>";
    String result = Utils.convertListToHTMLList(items);

    assertEquals(expected, result);
  }

  @Test
  public void testIsHtmlSyntaxValid_nonHtmlOriginal() {
    String originalContent = "Plain text";
    String newContent = "<p>Some html</p>";

    assertTrue(Utils.isHtmlSyntaxValid(originalContent, newContent));
  }

  @Test
  public void testIsHtmlSyntaxValid_htmlRequiresHtmlNew() {
    String originalContent = "<p>Original</p>";
    String newContent = "No html anymore";

    assertFalse(Utils.isHtmlSyntaxValid(originalContent, newContent));
  }

  @Test
  public void testHasSamePlaceholders_same() {
    String originalContent = "Hello {1}, you have {2} messages.";
    String newContent = "Hallo {1}, du hast {2} Nachrichten.";

    assertTrue(Utils.hasSamePlaceholders(originalContent, newContent));
  }

  @Test
  public void testHasSamePlaceholders_differentCount() {
    String originalContent = "Hello {1}, you have {2} messages.";
    String newContent = "Hallo, du hast Nachrichten.";

    assertFalse(Utils.hasSamePlaceholders(originalContent, newContent));
  }

  @Test
  public void testHasSamePlaceholders_differentIndexes() {
    String originalContent = "Hello {1}, you have {2} messages.";
    String newContent = "Hallo {1}, du hast {3} Nachrichten.";

    assertFalse(Utils.hasSamePlaceholders(originalContent, newContent));
  }
}