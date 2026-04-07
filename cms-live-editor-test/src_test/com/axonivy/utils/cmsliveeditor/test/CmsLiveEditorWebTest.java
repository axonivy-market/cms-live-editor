package com.axonivy.utils.cmsliveeditor.test;

import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CMS_ERROR_CONTAINER_ID;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CMS_WARNING_CONTAINER_ID;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CMS_WARNING_SAVE_CONTAINER_ID;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.DOWNLOAD_BUTTON_ID;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.EDIT_BUTTON_ID;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.ORANGE_DOT_CLASS;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.RESET_ALL_CHANGES_BUTTON_ID;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.RESET_BTN_ID;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.RESET_CONFIRM_INPUT_ID;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.SAVE_BUTTON_ID;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.SAVE_SUCCESS_BAR_ID;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.SEARCH_INPUT_ID;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.SETTING_BUTTON_ID;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.SUN_EDITOR_EDITABLE_SELECTOR;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.TRANSLATE_ALL_BUTTON_ID;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.UNDO_CHANGES_PATH_ID;
import static com.codeborne.selenide.ScrollIntoViewOptions.*;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.interactable;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.oneOfTexts;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

import java.io.File;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.codeborne.selenide.ClickOptions;
import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

@IvyWebTest
public class CmsLiveEditorWebTest {

  private String testCmsUri = "/TestContent";
  private String testCmsValue = "Test Content";
  private static final String TEST_CMS_FILE_DOCX_URI= "/Test/TestFileDocX";
  private static final String TEST_CMS_TEXT_URI = "/Test/TestContent";
  private static final String CONTENT_NOT_BEEN_SAVED_DIALOG_SELECTOR = "[id^='content-not-been-saved-dlg']";
  private static final String MORE_TEXT_DECORATION_BUTTON_CSS_QUERY_CLASS = ".se-btn.se-btn-more.se-tooltip";
  private static final String CMS_PATH_URI = "[id^='content-form:table-cms-keys:'][id$=':cms-uri']";
  private static final String CMS_VALUE_TAB_SELECTOR = "[id^='content-form:cms-values:'][id$=':cms-values-tab']";
  private static final String TEST_CMS_HTML_URI = "/Test/TestContentHtml";
  private static final String TRANSLATED_CMS_REVIEW_DIALOG = "[id$=':table-translation-review']";
  private static final String CMS_SETTINGS_DIALOG = "[id$=':cms-settings-dialog']";
  private static final String CMS_SETTINGS_SAVE_BUTTON = "[id$='content-form:cms-setting-save-btn']";
  private static final String EDITOR_TRANSLATE_BTN = "[id$=':cms-edit-value'] .cms-translate-btn";

  /**
   * Dear Bug Hunter,
   * This credential is intentionally included for educational purposes only and does not provide access to any production systems.
   * Please do not submit it as part of our bug bounty program.
   */
  @BeforeEach
  void startProcess() {
    Configuration.browserSize = "1366x2000";
    loginAndStartProcess("cmsAdmin", "123456");
  }

  @Test
  public void testDownloadButtonShouldBeVisible() {
    $(By.id(DOWNLOAD_BUTTON_ID)).shouldBe(visible);
  }

  @Test
  public void testTranslateMultipleButtonShouldBeVisible() {
    $(By.id(TRANSLATE_ALL_BUTTON_ID)).shouldBe(visible);
  }

  @Test
  public void testSettingsButtonShouldBeVisible() {
    $(By.id(SETTING_BUTTON_ID)).shouldBe(visible);
  }

  @Test
  public void testFilterByCmsUriShouldDisplayTwoRows() {
    sendKeysToSearchInput(testCmsUri);
    assertCmsTableRowCountGte(2);
  }

  @Test
  public void testFilterByCmsValueShouldDisplayTwoRows() {
    sendKeysToSearchInput(testCmsValue);
    assertCmsTableRowCountGte(2);
  }

  @Test
  public void testFilterByCmsFileShouldDisplayOneRow() {
    sendKeysToSearchInput(TEST_CMS_FILE_DOCX_URI);
    assertCmsTableRowCountGte(1);
  }

  private void assertCmsTableRowCountGte(int size) {
    $$(CMS_PATH_URI).shouldHave(sizeGreaterThanOrEqual(size));
  }

  private void sendKeysToSearchInput(String keysToSend) {
    $(By.id(SEARCH_INPUT_ID)).shouldBe(visible).sendKeys(keysToSend);
  }

  @Test
  void testTextDecorationFeatureShouldOnlyVisibleInHtmlFormat() {
    var cmsList = $$(CMS_PATH_URI);
    var cmsWithHtmlFormat = cmsList.findBy(exactText(TEST_CMS_HTML_URI));
    var rawTextCMS = cmsList.findBy(exactText(TEST_CMS_TEXT_URI));
    cmsWithHtmlFormat.click();
    $$(CMS_VALUE_TAB_SELECTOR).shouldHave(sizeGreaterThanOrEqual(1));
    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();
    $(MORE_TEXT_DECORATION_BUTTON_CSS_QUERY_CLASS).should(enabled);
    rawTextCMS.click();
    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();
    $$(MORE_TEXT_DECORATION_BUTTON_CSS_QUERY_CLASS).shouldHave(CollectionCondition.size(0));
  }

  @Test
  public void testEditedButNotSaveShouldShowError() {
    var cmsList = $$(CMS_PATH_URI);
    var cmsElement = cmsList.findBy(exactText(TEST_CMS_TEXT_URI));
    var otherElement = cmsList.findBy(exactText(TEST_CMS_FILE_DOCX_URI));
    cmsElement.click();
    $$(CMS_VALUE_TAB_SELECTOR).shouldHave(sizeGreaterThanOrEqual(1));
    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();

    $(SUN_EDITOR_EDITABLE_SELECTOR).setValue("Content is updated at 2 " + System.currentTimeMillis());
    Selenide.sleep(1000);
    otherElement.click();

    var errorDialog = $(By.cssSelector(CONTENT_NOT_BEEN_SAVED_DIALOG_SELECTOR));
    closeDialog(errorDialog);

    // assert check save before search items
    sendKeysToSearchInput("Lorem ifsum");
    errorDialog.should(visible, Duration.ofSeconds(2));
    errorDialog.$(".ui-dialog-titlebar-close").click();
  }

  @Test
  public void testHoverDownloadButtonToShowWarningMessage() {
    $(By.id(DOWNLOAD_BUTTON_ID)).shouldBe(enabled).scrollIntoView(instant().block(Block.start)).hover();
    $(By.id(CMS_WARNING_CONTAINER_ID)).shouldBe(visible);
    $("body").hover();
    Selenide.sleep(1000);
    $(By.id(CMS_WARNING_CONTAINER_ID)).shouldNotBe(visible);
  }

  @Test
  public void testHoverEditButtonToShowWarningMessage() {
    var cmsList = $$(CMS_PATH_URI);
    var cmsElement = cmsList.findBy(exactText(TEST_CMS_TEXT_URI));
    cmsElement.shouldBe(visible, Duration.ofSeconds(5)).click();
    cmsElement.click();
    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();

    $(By.id(SAVE_BUTTON_ID)).shouldBe(enabled).scrollTo().hover();
    $(By.id(CMS_WARNING_SAVE_CONTAINER_ID)).shouldBe(visible);
  }

  @Test
  public void testEditedAndSavedShouldNotShowError() {
    var cmsList = $$(CMS_PATH_URI);
    var cmsElement = cmsList.findBy(exactText(TEST_CMS_TEXT_URI));
    var otherElement = cmsList.findBy(exactText(TEST_CMS_FILE_DOCX_URI));
    cmsElement.shouldBe(visible, Duration.ofSeconds(5)).click();
    cmsElement.click();
    $(By.id(EDIT_BUTTON_ID)).click();
    $(SUN_EDITOR_EDITABLE_SELECTOR).setValue("Content is updated at " + System.currentTimeMillis());
    $(By.id(SAVE_BUTTON_ID)).shouldBe(enabled, Duration.ofSeconds(2)).scrollIntoView(true).shouldBe(interactable).click();
    $(By.id(SAVE_SUCCESS_BAR_ID)).shouldBe(visible, Duration.ofSeconds(2));
    $(By.id(UNDO_CHANGES_PATH_ID)).shouldBe(visible);
    otherElement.click();
    $(By.cssSelector(CONTENT_NOT_BEEN_SAVED_DIALOG_SELECTOR)).should(hidden);
  }

  @Test
  public void testSaveWithInvalidPlaceholderAcrossLocalesShouldShowError() {
    openFirstCmsAndEdit();

    var editors = $$(SUN_EDITOR_EDITABLE_SELECTOR);
    editors.shouldHave(sizeGreaterThanOrEqual(2));

    editors.get(0).setValue("File count {0}");
    editors.get(1).setValue("Date value {0} {1}");
    $(By.id(SAVE_BUTTON_ID)).shouldBe(enabled).click();
    $(By.id(CMS_ERROR_CONTAINER_ID)).shouldBe(visible);
  }

  @Test
  public void testResetAllChanges() {
    var selectedCms = openFirstCmsAndEdit();
    updateAndSaveContent();
    openResetDialog();
    confirmReset();
    selectedCms.click();
    $(By.id(RESET_ALL_CHANGES_BUTTON_ID)).shouldNotBe(visible);
    $$(ORANGE_DOT_CLASS).shouldHave(CollectionCondition.size(0));
  }

  @Test
  public void testResetAllChangesShouldNotBeTypedResetAutomatically() {
    SelenideElement selectedCms = openFirstCmsAndEdit();
    updateAndSaveContent();
    openResetDialog();
    $(By.id(RESET_CONFIRM_INPUT_ID)).shouldBe(empty);
    confirmReset();
    $(By.id(RESET_ALL_CHANGES_BUTTON_ID)).shouldNotBe(visible);
    $$(ORANGE_DOT_CLASS).shouldHave(CollectionCondition.size(0));

    // reset all change at 2nd time
    selectedCms.click();
    $(By.id(EDIT_BUTTON_ID)).shouldBe(visible).shouldBe(enabled).click();
    updateAndSaveContent();
    openResetDialog();
    $(By.id(RESET_CONFIRM_INPUT_ID)).shouldBe(empty);
  }

  @Test
  public void testSaveWithValidPlaceholdersAcrossLocalesShouldSucceed() {
    openFirstCmsAndEdit();

    var editors = $$(SUN_EDITOR_EDITABLE_SELECTOR);
    editors.shouldHave(sizeGreaterThanOrEqual(2));

    fillContentsToEditor(editors, "Files count {0}");
    editors.first().shouldNotBe(empty);
    $(By.id(SAVE_BUTTON_ID)).shouldBe(enabled);
    saveAndUndoCmsChanges();
  }

  @Test
  public void testSaveWithChoicePlaceholderShouldSucceed() {
    openFirstCmsAndEdit();

    var editors = $$(SUN_EDITOR_EDITABLE_SELECTOR);
    editors.filter(visible).shouldHave(sizeGreaterThanOrEqual(2));

    fillContentsToEditor(editors, "There {0,choice,0#are no files|1#is one file|1<are {0,number,integer} files}");
    saveAndUndoCmsChanges();
  }

  @Test
  public void testSaveWithReorderedPlaceholdersShouldSucceed() {
    openFirstCmsAndEdit();

    var editors = $$(SUN_EDITOR_EDITABLE_SELECTOR);
    editors.shouldHave(sizeGreaterThanOrEqual(2));
    editors.first().shouldBe(visible);

    String[] contents =
        {"{0} has {1} files", "{1} Dateien gehören zu {0}", "{0} tiene {1} archivos", "{1} dosyalar {0} için"};

    for (int i = 0; i < editors.size(); i++) {
      var editor = editors.get(i);
      editor.shouldBe(visible).click();
      editor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
      editor.sendKeys(Keys.DELETE);
      editor.sendKeys(contents[i % contents.length]);
      editor.pressTab();
    }
    saveAndUndoCmsChanges();
  }

  @Test
  public void testEditCmsFileDocX() {
    var cmsList = $$(CMS_PATH_URI);
    var cmsElement = cmsList.findBy(exactText(TEST_CMS_FILE_DOCX_URI));
    cmsElement.shouldBe(visible, Duration.ofSeconds(2)).should(enabled).click();
    $$(CMS_VALUE_TAB_SELECTOR).shouldHave(sizeGreaterThanOrEqual(1));
    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();
    File pdfFile = new File("resource_test/blank_pdf.pdf");
    var firstInputElement = $(By.id("content-form:cms-edit-value:0:upload_input"));
    firstInputElement.uploadFile(pdfFile);
    var errorMessageElement = $(".ui-messages-error-summary");
    errorMessageElement.shouldBe(visible).shouldHave(matchText("Error"));
    File docFile = new File("resource_test/blank_doc.docx");
    firstInputElement.uploadFile(docFile);
    errorMessageElement.shouldBe(hidden);
    cmsList.findBy(exactText(TEST_CMS_TEXT_URI)).click();
    var warningDialog = $(By.cssSelector(CONTENT_NOT_BEEN_SAVED_DIALOG_SELECTOR)).shouldBe(visible, Duration.ofSeconds(2));
    closeDialog(warningDialog);
    var removeFileElement = $(".pi-trash");
    removeFileElement.shouldBe(visible).click();
    removeFileElement.shouldBe(hidden);
  }

  private void closeDialog(SelenideElement dialog) {
    dialog.should(visible);
    dialog.$(".ui-dialog-titlebar-close").click();
    Selenide.sleep(1000);
  }

  private void fillContentsToEditor(ElementsCollection editors, String content) {
    for (var editor : editors) {
      editor.shouldBe(visible).click();
      editor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
      editor.sendKeys(Keys.DELETE);
      editor.sendKeys(content);
      editor.pressTab();
    }
  }

  private void saveAndUndoCmsChanges() {
    $(By.id(SAVE_BUTTON_ID)).shouldBe(visible).shouldBe(enabled).click();
    $(By.id(SAVE_SUCCESS_BAR_ID)).shouldBe(visible);

    var undoButton = $(By.id("content-form:undo-change-path"));
    if (undoButton.exists()) {
      undoButton.shouldBe(enabled).click();
    }
  }

  @Test
  public void testUserCorrectRole() {
    var exception = $(By.cssSelector(".exception-content"));
    exception.shouldNotBe(visible);
  }

  /**
   * Dear Bug Hunter,
   * This credential is intentionally included for educational purposes only and does not provide access to any production systems.
   * Please do not submit it as part of our bug bounty program.
   */
  @Test
  public void testUserIncorrectRole() {
    loginAndStartProcess("normalUser", "123456");
    var exception = $(By.cssSelector(".exception-content"));
    exception.shouldBe(visible).shouldHave(oneOfTexts("Access denied. Need one of these roles [CMS_ADMIN]"));
  }

  @Test
  public void testOpenTranslateDialogShouldBeOpened() {
    $(".cms-translate-btn").shouldBe(visible, Duration.ofSeconds(5)).click();

    SelenideElement table = $$(By.cssSelector(TRANSLATED_CMS_REVIEW_DIALOG)).first();
    table.shouldBe(visible, Duration.ofSeconds(5));
    $$(By.cssSelector(".ui-dialog .p-button-primary")).filter(visible).first().click();
    table.shouldNotBe(visible, Duration.ofSeconds(5));
  }

  @Test
  public void testOpenSettingsDialogAndSave() {
    $(".cms-settings-btn").shouldBe(visible, Duration.ofSeconds(5)).click();
    SelenideElement settingsDialog = $$(By.cssSelector(CMS_SETTINGS_DIALOG)).first();
    settingsDialog.shouldBe(visible, Duration.ofSeconds(5));
    SelenideElement saveButton = $(By.cssSelector(CMS_SETTINGS_SAVE_BUTTON));
    saveButton.shouldBe(enabled, Duration.ofSeconds(5)).click();
    settingsDialog.shouldNotBe(visible, Duration.ofSeconds(5));
  }

  @Test
  public void testTranslateButtonInEditSection() {
    var cmsList = $$(CMS_PATH_URI);
    var cmsElement = cmsList.findBy(exactText(TEST_CMS_TEXT_URI));
    cmsElement.shouldBe(visible, Duration.ofSeconds(5)).click();
    $$(CMS_VALUE_TAB_SELECTOR).shouldHave(sizeGreaterThanOrEqual(1));
    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();
    SelenideElement translateBtn = $$(By.cssSelector(EDITOR_TRANSLATE_BTN)).filter(visible).first();
    translateBtn.shouldBe(enabled, Duration.ofSeconds(5)).click();
  }

  /**
   * Dear Bug Hunter,
   * This credential is intentionally included for educational purposes only and does not provide access to any production systems.
   * Please do not submit it as part of our bug bounty program.
   */
  private void loginAndStartProcess(String username, String password) {
    String loginProcessUrl =
        String.format("/cms-live-editor-test/193BDA54C9726ADF/logInUser.ivp?username=%s&password=%s", username, password);
    open(EngineUrl.createProcessUrl(loginProcessUrl));
    open(EngineUrl.createProcessUrl("/cms-live-editor/18DE86A37D77D574/start.ivp?showEditorCms=true"));
  }

  private SelenideElement openFirstCmsAndEdit() {
    SelenideElement selectedCms = $$(CMS_PATH_URI).shouldHave(CollectionCondition.sizeGreaterThan(0)).first();
    selectedCms.click();
    $(By.id(EDIT_BUTTON_ID)).shouldBe(visible).shouldBe(enabled).click();
    return selectedCms;
  }

  private void updateAndSaveContent() {
    $(SUN_EDITOR_EDITABLE_SELECTOR).setValue("Content is updated at " + System.currentTimeMillis());
    $(By.id(SAVE_BUTTON_ID)).shouldBe(enabled).click();
    $(By.id(SAVE_SUCCESS_BAR_ID)).shouldBe(visible);
  }

  private SelenideElement openResetDialog() {
    $(By.id(RESET_ALL_CHANGES_BUTTON_ID)).shouldBe(visible);
    $$(ORANGE_DOT_CLASS).shouldHave(CollectionCondition.sizeGreaterThanOrEqual(1));
    SelenideElement resetBtn = $(By.id(RESET_ALL_CHANGES_BUTTON_ID)).scrollIntoView(instant().block(Block.start))
        .click(ClickOptions.usingJavaScript());
    Selenide.executeJavaScript("arguments[0].click()", resetBtn);
    return resetBtn;
  }

  private void confirmReset() {
    $(By.id(RESET_CONFIRM_INPUT_ID)).setValue("reset");
    $(By.id(RESET_BTN_ID)).shouldBe(interactable).click();
  }
}