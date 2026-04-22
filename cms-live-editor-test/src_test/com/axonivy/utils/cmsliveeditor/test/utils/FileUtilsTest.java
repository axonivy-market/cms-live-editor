package com.axonivy.utils.cmsliveeditor.test.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;
import org.primefaces.model.file.CommonsUploadedFile;
import org.primefaces.model.file.UploadedFile;

import com.axonivy.utils.cmsliveeditor.constants.DocumentConstants;
import com.axonivy.utils.cmsliveeditor.constants.FileConstants;
import com.axonivy.utils.cmsliveeditor.enums.FileType;
import com.axonivy.utils.cmsliveeditor.utils.FileUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class FileUtilsTest {

  private static final long ONE_HUNDRED = 100;

  // ===== Common constants =====
  private static final String BASE_FOLDER = "base";

  // ===== File names =====
  private static final String FILE_TXT = "file.txt";
  private static final String INVALID_FILE_1 = "../file.txt";
  private static final String INVALID_FILE_2 = "a/b.txt";
  private static final String INVALID_FILE_3 = "a\\b.txt";

  // ===== Paths =====
  private static final String VALID_PATH = "folder/file.txt";
  private static final String VALID_PATH_WITH_SLASH = "/folder/file.txt";
  private static final String PATH_TRAVERSAL_1 = "../file.txt";
  private static final String PATH_TRAVERSAL_2 = "a/../../file.txt";
  private static final String WINDOWS_PATH = "C:\\evil.txt";

  private static final String PATH_A_B = "a/b";
  private static final String PATH_A_B_C = "a/b/c";

  // ===== Normalize inputs =====
  private static final String RAW_PATH_1 = "/a/b/c";
  private static final String RAW_PATH_2 = "///a/b/c";
  private static final String RAW_PATH_3 = "\\a\\b\\c";

  @Test
  public void testCalculateToKB() {
    assertEquals(0, FileUtils.calculateToKB(0));
    assertEquals(1, FileUtils.calculateToKB(FileConstants.BYTE_IN_KB));
    assertEquals(ONE_HUNDRED, FileUtils.calculateToKB(ONE_HUNDRED * FileConstants.BYTE_IN_KB));
  }

  @Test
  public void testIsValidFileSize() {
    long twoMBFileSize = 2 * FileConstants.KB_IN_MB * FileConstants.BYTE_IN_KB;
    assertEquals(false, FileUtils.isValidFileSize(twoMBFileSize, 1));
    long oneMBFileSize = 1 * FileConstants.KB_IN_MB * FileConstants.BYTE_IN_KB;
    assertEquals(true, FileUtils.isValidFileSize(oneMBFileSize, 1));
  }

  @Test
  public void testGetMaxUploadedFileSize() {
    assertEquals(50, FileUtils.getMaxUploadedFileSize());
  }

  @Test
  public void testGetFileExtension() {
    FileItem fileItem = new DiskFileItem("", DocumentConstants.PDF_CONTENT_TYPE, false, "test.pdf", 0, null);
    UploadedFile file = new CommonsUploadedFile(fileItem, 1L);
    assertEquals("pdf", FileUtils.getFileExtension(file));
  }

  @Test
  public void testNormalizeUri() {
    assertEquals(PATH_A_B_C, FileUtils.normalizeUri(RAW_PATH_1));
    assertEquals(PATH_A_B_C, FileUtils.normalizeUri(RAW_PATH_2));
    assertEquals(PATH_A_B_C, FileUtils.normalizeUri(RAW_PATH_3));
    assertEquals(Strings.EMPTY, FileUtils.normalizeUri(null));
  }

  @Test
  public void testIsValidFileName() {
    assertEquals(true, FileUtils.isValidFileName(FILE_TXT));
    assertEquals(false, FileUtils.isValidFileName(INVALID_FILE_1));
    assertEquals(false, FileUtils.isValidFileName(INVALID_FILE_2));
    assertEquals(false, FileUtils.isValidFileName(INVALID_FILE_3));
    assertEquals(false, FileUtils.isValidFileName(null));
    assertEquals(false, FileUtils.isValidFileName(Strings.EMPTY));
  }

  @Test
  public void testIsSafePath() {
    var base = java.nio.file.Path.of(BASE_FOLDER).toAbsolutePath().normalize();

    assertEquals(true, FileUtils.isSafePath(base, VALID_PATH));
    assertEquals(true, FileUtils.isSafePath(base, VALID_PATH_WITH_SLASH));

    // ❌ path traversal
    assertEquals(false, FileUtils.isSafePath(base, PATH_TRAVERSAL_1));
    assertEquals(false, FileUtils.isSafePath(base, PATH_TRAVERSAL_2));

    // ❌ Windows absolute path
    assertEquals(false, FileUtils.isSafePath(base, WINDOWS_PATH));

    // ❌ null / blank
    assertEquals(false, FileUtils.isSafePath(base, null));
    assertEquals(false, FileUtils.isSafePath(base, Strings.EMPTY));
  }

  @Test
  public void testBuildNormalizedPath() {
    assertEquals(PATH_A_B_C, FileUtils.buildNormalizedPath("a", "b", "c"));
    assertEquals(PATH_A_B_C, FileUtils.buildNormalizedPath("a/", "/b/", "c"));
    assertEquals(PATH_A_B, FileUtils.buildNormalizedPath("a", Strings.EMPTY, "b"));
    assertEquals(PATH_A_B, FileUtils.buildNormalizedPath("a", null, "b"));

    // mixed slashes
    assertEquals(PATH_A_B_C, FileUtils.buildNormalizedPath("a\\b", "c"));
  }

  @Test
  void testGetFileTypeByExtension() {
    // valid cases
    assertEquals(FileType.EXCEL, FileUtils.getFileTypeByExtension("xlsx"));
    assertEquals(FileType.EXCEL, FileUtils.getFileTypeByExtension("XLSX"));

    // unknown extension
    assertEquals(FileType.OTHERS, FileUtils.getFileTypeByExtension("invalid"));
    assertEquals(FileType.OTHERS, FileUtils.getFileTypeByExtension(null));
  }
}

