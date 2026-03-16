package com.axonivy.utils.cmsliveeditor.test;

import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.*;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.interactable;
import static com.codeborne.selenide.Condition.matchText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Condition.empty;
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
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

@IvyWebTest
public class CmsLiveEditorWebTest {

  private String testCmsUri = "/TestContent";
  private String testCmsValue = "Test Content";
  private static final String TEST_CMS_FILE_DOCX_URI= "/Test/TestFileDocX";
  private static final String TEST_CMS_TEXT_URI = "/Test/TestContent";
  private static final String PRIMEFACES_MESSAGE_DIALOG = "primefacesmessagedlg";

  private static final String CMS_PATH_URI = "[id^='content-form:table-cms-keys:'][id$=':cms-uri']";
  private static final String CMS_VALUE_TAB_SELECTOR = "[id^='content-form:cms-values:'][id$=':cms-values-tab']";

  /**
   * Dear Bug Hunter,
   * This credential is intentionally included for educational purposes only and does not provide access to any production systems.
   * Please do not submit it as part of our bug bounty program.
   */
  @BeforeEach
  void startProcess() {
    loginAndStartProcess("cmsAdmin", "123456");
  }

  @Test
  public void testDownloadButtonShouldBeVisible() {
    $(By.id(DOWNLOAD_BUTTON_ID)).shouldBe(visible);
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
  public void testEditedButNotSaveShouldShowError() {
    var cmsList = $$(CMS_PATH_URI);
    var selectedCms = cmsList.get(0);
    var otherCms = cmsList.get(1);
    selectedCms.click();
    $$(CMS_VALUE_TAB_SELECTOR).shouldHave(sizeGreaterThanOrEqual(1));
    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();

    $(SUN_EDITOR_EDITABLE_SELECTOR).setValue("Content is updated at 2 " + System.currentTimeMillis());
    $(".se-btn.se-resizing-enabled.se-tooltip").should(enabled);
    Selenide.sleep(1000);
    otherCms.click();

    var errorDialog = $(By.id(PRIMEFACES_MESSAGE_DIALOG));
    closeDialog(errorDialog);

    // assert check save before search items
    sendKeysToSearchInput("Lorem ifsum");
    errorDialog.should(visible);
    errorDialog.$(".ui-dialog-titlebar-close").click();
  }

  @Test
  public void testHoverDownloadButtonToShowWarningMessage() {
    $(By.id(DOWNLOAD_BUTTON_ID)).shouldBe(enabled).scrollIntoView(true).hover();
    $(By.id(CMS_WARNING_CONTAINER_ID)).shouldBe(visible);
    $("body").hover();
    Selenide.sleep(1000);
    $(By.id(CMS_WARNING_CONTAINER_ID)).shouldNotBe(visible);
  }

  @Test
  public void testHoverEditButtonToShowWarningMessage() {
    var cmsList = $$(CMS_PATH_URI);
    var selectedCms = cmsList.get(0);
    selectedCms.click();
    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();

    $(By.id(SAVE_BUTTON_ID)).shouldBe(enabled).hover();
    $(By.id(CMS_WARNING_SAVE_CONTAINER_ID)).shouldBe(visible);
  }

  @Test
  public void testEditedAndSavedShouldNotShowError() {
    var cmsList = $$(CMS_PATH_URI);
    var selectedCms = cmsList.get(0);
    var otherCms = cmsList.get(1);
    selectedCms.click();
    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();
    $(SUN_EDITOR_EDITABLE_SELECTOR).setValue("Content is updated at " + System.currentTimeMillis());
    Selenide.sleep(1000);
    $(By.id(SAVE_BUTTON_ID)).shouldBe(enabled).click();
    $(By.id(SAVE_SUCCESS_BAR_ID)).shouldBe(visible);
    $(By.id(UNDO_CHANGES_PATH_ID)).shouldBe(visible);
    otherCms.click();
    $(By.id(PRIMEFACES_MESSAGE_DIALOG)).should(hidden);
  }

  @Test
  public void testSaveWithInvalidPlaceholderAcrossLocalesShouldShowError() {
    var cmsList = $$(CMS_PATH_URI);
    cmsList.first().click();
    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();

    var editors = $$(SUN_EDITOR_EDITABLE_SELECTOR);
    editors.shouldHave(sizeGreaterThanOrEqual(2));

    editors.get(0).setValue("File count {0}");
    editors.get(1).setValue("Date value {1}");
    $(By.id(SAVE_BUTTON_ID)).shouldBe(enabled).click();
    $(By.id(CMS_ERROR_CONTAINER_ID)).shouldBe(visible);
  }

  @Test
  public void testResetAllChanges() {
    openFirstCmsAndEdit();
    updateAndSaveContent();
    openResetDialog();
    confirmReset();
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
    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();
    updateAndSaveContent();
    openResetDialog();
    $(By.id(RESET_CONFIRM_INPUT_ID)).shouldBe(empty);
  }

  @Test
  public void testSaveWithValidPlaceholdersAcrossLocalesShouldSucceed() {
    var cmsList = $$(CMS_PATH_URI);
    cmsList.first().click();

    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();

    var editors = $$(SUN_EDITOR_EDITABLE_SELECTOR);
    editors.shouldHave(sizeGreaterThanOrEqual(2));

    for (SelenideElement editor : editors) {
      editor.click();
      editor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
      editor.sendKeys("Files count {0}");
    }
    $(By.id(SAVE_BUTTON_ID)).click();

    $(By.id(SAVE_SUCCESS_BAR_ID)).shouldBe(visible);
    undoCmsChanges();
  }

  @Test
  public void testSaveWithChoicePlaceholderShouldSucceed() {
    var cmsList = $$(CMS_PATH_URI);
    cmsList.first().click();
    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();

    var editors = $$(SUN_EDITOR_EDITABLE_SELECTOR);
    editors.shouldHave(sizeGreaterThanOrEqual(1));

    String content = "There {0,choice,0#are no files|1#is one file|1<{0} files}";

    for (var editor : editors) {
      editor.click();
      editor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
      editor.sendKeys(content);
    }

    $(By.id(SAVE_BUTTON_ID)).shouldBe(enabled).click();
    $(By.id(SAVE_SUCCESS_BAR_ID)).shouldBe(visible);
    undoCmsChanges();
  }

  @Test
  public void testSaveWithReorderedPlaceholdersShouldSucceed() {
    var cmsList = $$(CMS_PATH_URI);
    cmsList.first().click();
    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();

    var editors = $$(SUN_EDITOR_EDITABLE_SELECTOR);
    editors.shouldHave(sizeGreaterThanOrEqual(2));

    String[] contents =
        {"{0} has {1} files", "{1} Dateien gehören zu {0}", "{0} tiene {1} archivos", "{1} dosyalar {0} için"};

    for (int i = 0; i < editors.size(); i++) {
      var editor = editors.get(i);
      editor.click();
      editor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
      editor.sendKeys(contents[i % contents.length]);
    }

    $(By.id(SAVE_BUTTON_ID)).shouldBe(enabled).click();
    $(By.id(SAVE_SUCCESS_BAR_ID)).shouldBe(visible);
    undoCmsChanges();
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
    var warningDialog = $(By.id(PRIMEFACES_MESSAGE_DIALOG));
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

  public void undoCmsChanges() {
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
    exception.shouldBe(visible).shouldHave(matchText("Access denied. Need role CMS_ADMIN"));
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
    var selectedCms = $$(CMS_PATH_URI).first();
    selectedCms.click();
    $(By.id(EDIT_BUTTON_ID)).shouldBe(enabled).click();
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
    SelenideElement resetBtn =
        $(By.id(RESET_ALL_CHANGES_BUTTON_ID)).scrollIntoView(true).click(ClickOptions.usingJavaScript());
    Selenide.executeJavaScript("arguments[0].click()", resetBtn);
    return resetBtn;
  }

  private void confirmReset() {
    $(By.id(RESET_CONFIRM_INPUT_ID)).setValue("reset");
    $(By.id(RESET_BTN_ID)).shouldBe(interactable).click();
  }
}
