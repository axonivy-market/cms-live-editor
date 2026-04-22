// package com.axonivy.utils.cmsliveeditor.test.utils;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.junit.jupiter.api.Assertions.assertTrue;
//
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Locale;
// import java.util.Map;
//
// import org.apache.poi.ss.usermodel.Workbook;
// import org.junit.jupiter.api.Test;
//
// import com.axonivy.utils.cmsliveeditor.model.Cms;
// import com.axonivy.utils.cmsliveeditor.model.CmsContent;
// import com.axonivy.utils.cmsliveeditor.model.PmvCms;
// import com.axonivy.utils.cmsliveeditor.utils.CmsFileUtils;
//
// public class CmsFileUtilsTest {
//
// // ==================== collectCmsFiles ====================
//
// @Test
// public void testCollectCmsFilesShouldReturnEmptyMapWhenPmvCmsIsNull() {
// Map<String, byte[]> result = CmsFileUtils.collectCmsFiles("project", null);
// assertNotNull(result);
// assertTrue(result.isEmpty());
// }
//
// @Test
// public void testCollectCmsFilesShouldReturnEmptyMapWhenCmsListIsEmpty() {
// PmvCms pmvCms = buildPmvCms("project", List.of(Locale.ENGLISH), Collections.emptyList());
//
// Map<String, byte[]> result = CmsFileUtils.collectCmsFiles("project", pmvCms);
//
// assertNotNull(result);
// assertTrue(result.isEmpty());
// }
//
// @Test
// public void testCollectCmsFilesShouldReturnEmptyMapWhenNoCmsEntryIsFile() {
// PmvCms pmvCms =
// buildPmvCms("project", List.of(Locale.ENGLISH), List.of(buildTextCms("/Labels/Hello", "en", "Hello")));
//
// Map<String, byte[]> result = CmsFileUtils.collectCmsFiles("project", pmvCms);
//
// assertNotNull(result);
// assertTrue(result.isEmpty());
// }
//
// // ==================== addPmvCmsToWorkbooks ====================
//
// @Test
// public void testAddPmvCmsToWorkbooksShouldNotAddWhenPmvCmsIsNull() {
// Map<String, Workbook> workbooks = new HashMap<>();
// CmsFileUtils.addPmvCmsToWorkbooks("project", null, workbooks);
// assertTrue(workbooks.isEmpty());
// }
//
// @Test
// public void testAddPmvCmsToWorkbooksShouldAddWorkbookForValidPmvCms() {
// PmvCms pmvCms = buildPmvCms("project", List.of(Locale.ENGLISH), Collections.emptyList());
// Map<String, Workbook> workbooks = new HashMap<>();
//
// CmsFileUtils.addPmvCmsToWorkbooks("project", pmvCms, workbooks);
//
// assertEquals(1, workbooks.size());
// assertTrue(workbooks.containsKey("project"));
// assertNotNull(workbooks.get("project"));
// }
//
// @Test
// public void testAddPmvCmsToWorkbooksShouldUseProvidedProjectNameAsKey() {
// PmvCms pmvCms = buildPmvCms("internal-name", List.of(Locale.ENGLISH), Collections.emptyList());
// Map<String, Workbook> workbooks = new HashMap<>();
//
// CmsFileUtils.addPmvCmsToWorkbooks("display-name", pmvCms, workbooks);
//
// assertTrue(workbooks.containsKey("display-name"));
// }
//
// // ==================== addPmvCmsFiles ====================
//
// @Test
// public void testAddPmvCmsFilesShouldNotAddWhenPmvCmsIsNull() {
// Map<String, byte[]> cmsFiles = new HashMap<>();
// CmsFileUtils.addPmvCmsFiles("project", null, cmsFiles);
// assertTrue(cmsFiles.isEmpty());
// }
//
// @Test
// public void testAddPmvCmsFilesShouldNotAddWhenNoCmsEntryIsFile() {
// PmvCms pmvCms =
// buildPmvCms("project", List.of(Locale.ENGLISH), List.of(buildTextCms("/Labels/Hello", "en", "Hello")));
// Map<String, byte[]> cmsFiles = new HashMap<>();
//
// CmsFileUtils.addPmvCmsFiles("project", pmvCms, cmsFiles);
//
// assertTrue(cmsFiles.isEmpty());
// }
//
// // ==================== Helpers ====================
//
// private PmvCms buildPmvCms(String name, List<Locale> locales, List<Cms> cmsList) {
// PmvCms pmvCms = new PmvCms();
// pmvCms.setPmvName(name);
// pmvCms.setLocales(locales);
// pmvCms.setCmsList(cmsList);
// return pmvCms;
// }
//
// private Cms buildTextCms(String uri, String language, String contentValue) {
// CmsContent content = new CmsContent();
// content.setLocale(Locale.forLanguageTag(language));
// content.setContent(contentValue);
//
// Cms cms = new Cms();
// cms.setUri(uri);
// cms.setContents(List.of(content));
// cms.setFile(false);
// return cms;
// }
// }
