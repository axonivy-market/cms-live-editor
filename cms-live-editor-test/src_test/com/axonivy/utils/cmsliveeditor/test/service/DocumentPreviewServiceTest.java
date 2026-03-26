package com.axonivy.utils.cmsliveeditor.test.service;

import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.DOCX_EXTENSION;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.PDF_CONTENT_TYPE;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.XLSX_EXTENSION;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.PDF_EXTENSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.enums.AsposeProduct;
import com.axonivy.utils.cmsliveeditor.service.DocumentPreviewService;
import com.axonivy.utils.cmsliveeditor.service.LicenseLoader;
import com.google.common.io.Files;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class DocumentPreviewServiceTest {
  private static final String TEST_FILE_NAME = "test";
  private static DocumentPreviewService documentPreviewService = DocumentPreviewService.getInstance();

  @BeforeAll
  @SuppressWarnings("unchecked")
  static void setup() throws Exception {
    Field field = LicenseLoader.class.getDeclaredField("LOADED_ASPOSE_LICENSES");
    field.setAccessible(true);
    Map<AsposeProduct, Object> licenses = (Map<AsposeProduct, Object>) field.get(null);
    licenses.put(AsposeProduct.CELLS, new Object());
    licenses.put(AsposeProduct.WORDS, new Object());
  }

  @Test
  void testConvertToStreamContent_ExcelXlsx() throws Exception {
    File tempFile = File.createTempFile(TEST_FILE_NAME, XLSX_EXTENSION);
    StreamedContent content = documentPreviewService.convertToStreamContent(tempFile.getName(), Files.toByteArray(tempFile));
    assertNotNull(content);
    assertTrue(content.getName().contains(TEST_FILE_NAME));
    assertTrue(content.getName().endsWith(XLSX_EXTENSION));
    assertEquals(PDF_CONTENT_TYPE, content.getContentType());
    tempFile.deleteOnExit();
  }

  @Test
  void testConvertToStreamContent_WordDocx() throws Exception {
    File tempFile = File.createTempFile(TEST_FILE_NAME, DOCX_EXTENSION);
    StreamedContent content = documentPreviewService.convertToStreamContent(tempFile.getName(), Files.toByteArray(tempFile));
    assertNotNull(content);
    assertTrue(content.getName().contains(TEST_FILE_NAME));
    assertTrue(content.getName().endsWith(DOCX_EXTENSION));
    assertEquals(PDF_CONTENT_TYPE, content.getContentType());
    tempFile.deleteOnExit();
  }

  @Test
  void testConvertToStreamContent_Pdf() throws Exception {
    File tempFile = File.createTempFile(TEST_FILE_NAME, PDF_EXTENSION);
    StreamedContent content = documentPreviewService.convertToStreamContent(tempFile.getName(), Files.toByteArray(tempFile));
    assertNotNull(content);
    assertTrue(content.getName().contains(TEST_FILE_NAME));
    assertTrue(content.getName().endsWith(PDF_EXTENSION));
    assertEquals(PDF_CONTENT_TYPE, content.getContentType());
    tempFile.deleteOnExit();
  }

}