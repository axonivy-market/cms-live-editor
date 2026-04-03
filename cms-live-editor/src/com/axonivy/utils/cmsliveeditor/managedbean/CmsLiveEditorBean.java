package com.axonivy.utils.cmsliveeditor.managedbean;

import static ch.ivyteam.ivy.environment.Ivy.cms;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CMS_LIVE_EDITOR_DEMO_PMV_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CMS_LIVE_EDITOR_PMV_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CMS_SETTING_DIALOG;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CONTENT_FORM;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CONTENT_FORM_CMS_COLUMN;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CONTENT_FORM_EDITABLE_COLUMN;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CONTENT_FORM_PATH_COLUMN;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CONTENT_FORM_TABLE_CMS_KEYS;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.ERROR_MESSAGE_FOR_CMS_FILE_UPLOAD;
import static java.util.stream.Collectors.toList;
import static javax.faces.application.FacesMessage.SEVERITY_INFO;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.primefaces.PF;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import com.axonivy.utils.cmsliveeditor.constants.UserConstants;
import com.axonivy.utils.cmsliveeditor.dto.CmsValueDto;
import com.axonivy.utils.cmsliveeditor.enums.ExportType;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.ExportOption;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;
import com.axonivy.utils.cmsliveeditor.model.SavedCms;
import com.axonivy.utils.cmsliveeditor.service.CmsService;
import com.axonivy.utils.cmsliveeditor.service.IvyUserService;
import com.axonivy.utils.cmsliveeditor.service.TranslationService;
import com.axonivy.utils.cmsliveeditor.utils.CmsContentUtils;
import com.axonivy.utils.cmsliveeditor.utils.CmsFileUtils;
import com.axonivy.utils.cmsliveeditor.utils.FacesContexts;
import com.axonivy.utils.cmsliveeditor.utils.FileUtils;
import com.axonivy.utils.cmsliveeditor.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.application.ActivityState;
import ch.ivyteam.ivy.application.IActivity;
import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.IProcessModel;
import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.application.app.IApplicationRepository;
import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.ContentObjectReader;
import ch.ivyteam.ivy.cm.ContentObjectValue;
import ch.ivyteam.ivy.cm.exec.ContentManagement;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.ISecurityContext;

@ViewScoped
@ManagedBean
public class CmsLiveEditorBean implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  private static final ObjectMapper mapper = new ObjectMapper();
  private final CmsService cmsService = CmsService.getInstance();

  private Map<String, Map<String, SavedCms>> savedCmsMap;
  private List<Cms> cmsList;
  private List<Cms> filteredCMSList;
  private Cms lastSelectedCms;
  private Cms selectedCms;
  private String selectedProjectName;
  private String searchKey;
  private StreamedContent fileDownload;
  private StreamedContent yamlFileDownload;
  private boolean isShowEditorCms;
  private Map<String, PmvCms> pmvCmsMap;
  private boolean isEditableCms;
  private String resetConfirmText;
  private boolean isInEditMode;
  private String selectedSourceLocale;
  private String selectedTargetLocale;
  private List<Locale> languageList;
  private List<Cms> selectedCmsEntries;
  private List<ExportOption> exportOptions;
  
  @PostConstruct
  private void init() {
    isShowEditorCms = FacesContexts.evaluateValueExpression("#{data.showEditorCms}", Boolean.class);
    savedCmsMap = new HashMap<>();
    pmvCmsMap = new HashMap<>();
    exportOptions = List.of(new ExportOption("XLSX", "pi pi-save", ExportType.EXCEL),
        new ExportOption("YAML", "pi pi-file", ExportType.YAML));
    for (var app : IApplicationRepository.of(ISecurityContext.current()).all()) {
      app.getProcessModels().stream().filter(CmsLiveEditorBean::isActive).map(IProcessModel::getReleasedProcessModelVersion)
          .filter(CmsLiveEditorBean::isActive)
          .forEach(pmv -> getAllChildren(pmv.getName(), ContentManagement.cms(pmv).root(), new ArrayList<>()));
    }
  
    onAppChange();
    initLocales();
  }

  public void exportCms(ExportType type) {
      try {
        String applicationName = currentApplicationName();
          switch (type) {
          case EXCEL -> fileDownload = CmsFileUtils.exportCmsToZip(selectedProjectName, applicationName, pmvCmsMap, type);
          case YAML -> yamlFileDownload = CmsFileUtils.exportCmsToZip(selectedProjectName, applicationName, pmvCmsMap, type);
          }
      } catch (Exception ex) {
          Ivy.log().error("CMS export failed", ex);
      }
  }
  
  public StreamedContent getFile(ExportType type) {
    return switch (type) {
        case EXCEL -> fileDownload;
        case YAML -> yamlFileDownload;
    };
}

  public void writeCmsToApplication() {
    isEditableCms = false;
    if (selectedCms.isFile()) {
      cmsService.writeCmsFileToApplication(selectedCms);
      clearNewUploadFile();
    } else {
      cmsService.writeCmsToApplication(savedCmsMap);
    }
    selectedCms.getContents().forEach(s -> s.setEditing(false));
    onAppChange();
    PF.current().ajax().update(CONTENT_FORM);
    lastSelectedCms = null;
  }

  public boolean isRenderResetAllChange() {
    return filteredCMSList.stream().anyMatch(Cms::isDifferentWithApplication);
  }

  public boolean isRenderUndoChange() {
    return Optional.ofNullable(selectedCms).map(Cms::isDifferentWithApplication).orElse(false);
  }

  public void removeCmsFileInApplicationCms(int index) {
    try {
      var cmsContent = this.selectedCms.getContents().get(index);
      cmsContent.setEditing(cmsContent.getApplicationFileSize() > 0);
      if (cmsContent.getNewFileSize() > 0) {
        cmsContent.setNewUploadedFile(null);
        cmsContent.setNewFileSize(0);
        cmsContent.setNewFileContent(null);
      } else {
        cmsContent.setApplicationFileSize(0);
        cmsContent.setApplicationFileContent(null);
      }
    } catch (Exception e) {
      Ivy.log().error(e);
    }
  }

  /*
   * 
   * This method is used to reset all values in filteredCMSList where each CMS has the flag isDifferentWithApplication
   * set to true. Then, we get the project CMS value to remap the 'new content' to the Project CMS because we have
   * already deleted the value in the application CMS.
   * 
   */
  public void resetAllChanges() {
    selectedCms = null;
    filteredCMSList.stream().filter(Cms::isDifferentWithApplication).forEach(cms -> {
      if (cms.isFile()) {
        cmsService.removeAllCmsFiles(cms);
      } else {
        savedCmsMap.remove(cms.getUri());
        cmsService.removeApplicationCmsByUri(cms.getUri());
        cms.getContents().forEach(content -> content.saveContent(content.getOriginalContent()));
      }
    });
    onAppChange();
    isEditableCms = false;
    this.resetConfirmText = null;
    PF.current().ajax().update(CONTENT_FORM);
  }

  /*
   * 
   * This method is used to remove all values in the application CMS that we are clicking to update.
   * 
   */
  public void undoChange() {
    savedCmsMap.remove(selectedCms.getUri());
    filteredCMSList.stream().filter(cms -> cms.getUri().equals(selectedCms.getUri())).forEach(cms -> {
      if (selectedCms.isFile()) {
        cmsService.removeAllCmsFiles(selectedCms);
        selectedCms.getContents().forEach(cmsContent -> {
          cmsContent.setApplicationFileSize(0);
          cmsContent.setApplicationFileContent(null);
        });
      } else {
        cmsService.removeApplicationCmsByUri(cms.getUri());
        cms.getContents().forEach(content -> content.saveContent(content.getOriginalContent()));
      }
    });
    onAppChange();
    isEditableCms = false;
    PF.current().ajax().update(CONTENT_FORM);
  }

  public void onEditableButton() {
    lastSelectedCms = selectedCms;
    isEditableCms = true;
    isInEditMode = true;
    PF.current().ajax().update(CONTENT_FORM);
  }

  public void onCancelEditableButton() {
    isEditableCms = false;
    lastSelectedCms = null;
    isInEditMode = false;
    clearNewUploadFile();
    PF.current().ajax().update(CONTENT_FORM_PATH_COLUMN, CONTENT_FORM_EDITABLE_COLUMN);
  }

  public void onHideSettingDialog() {
    initLocales();
    PF.current().ajax().update(CMS_SETTING_DIALOG);
  }

  private void clearNewUploadFile() {
    if (selectedCms.isFile()) {
      selectedCms.getContents().stream().forEach(cmsContent -> {
        cmsContent.setNewUploadedFile(null);
        cmsContent.setNewFileSize(0);
        cmsContent.setNewFileContent(null);
        cmsContent.setEditing(false);
        loadCmsFileFromApplicationCms(cmsContent, IApplication.current());
      });
    }
  }

  public boolean isDisableEditableButton() {
    return ObjectUtils.isEmpty(selectedCms);
  }

  public void search() {
    if (isEditing()) {
      return;
    }
    filteredCMSList = cmsList.stream().filter(entry -> isCmsMatchSearchKey(entry, searchKey))
        .map(cmsService::compareWithCmsInApplication).collect(Collectors.toList());

    if (selectedCms != null) {
      selectedCms =
          filteredCMSList.stream().filter(entry -> entry.getUri().equals(selectedCms.getUri())).findAny().orElse(null);
    }
    initLocales();
    PF.current().ajax().update(CONTENT_FORM, CMS_SETTING_DIALOG);
  }

  public void translate(CmsContent content) {
    String src = Locale.forLanguageTag(selectedSourceLocale).getLanguage().toUpperCase(Locale.ENGLISH);
    String target = content.getLocale().getLanguage().toUpperCase(Locale.ENGLISH);
    String newValue = TranslationService.translate(content.getContent(), src, target);
    content.setContent(newValue);
    PrimeFaces.current().ajax().addCallbackParam("langIndex", content.getIndex());
    PrimeFaces.current().ajax().addCallbackParam("newContent", newValue);
  }

  public void translateAll() {
    String src = Locale.forLanguageTag(selectedSourceLocale).getLanguage();
    String target = Locale.forLanguageTag(selectedTargetLocale).getLanguage();
    TranslationService.batchTranslate(selectedCmsEntries, src, target);
  }

  public void applyTranslations() {
    for (Cms cms : CmsContentUtils.getTranslatedCms(selectedCmsEntries)) {
      CmsContent target = getTargetCmsContent(cms);
      if (target == null || !target.isTranslated()) {
        continue;
      }
      handleCmsContentSave(cms.getUri(), target.getTranslatedContent(), target);
      target.setTranslated(false);
      target.setTranslatedContent(null);
    }
    cmsService.writeCmsToApplication(savedCmsMap);
    selectedCmsEntries = new ArrayList<>();
    onAppChange();
    PF.current().ajax().update(CONTENT_FORM);
  }

  public CmsContent getTargetCmsContent(Cms cms) {
    return CmsContentUtils.getCmsContentByLocale(cms, selectedTargetLocale);
  }

  public CmsContent getSourceCmsContent(Cms cms) {
    return CmsContentUtils.getCmsContentByLocale(cms, selectedSourceLocale);
  }

  public void cancelTranslations() {
    for (Cms cms : CmsContentUtils.getTranslatedCms(selectedCmsEntries)) {
      cms.getContents().stream().filter(Objects::nonNull).forEach(c -> {
        c.setTranslated(false);
        c.setTranslatedContent(null);
      });
    }
  }

  public boolean isRenderTranslateButton(CmsContent cms) {
    return !cms.isFile() && !cms.getLocale().getLanguage().equals(selectedSourceLocale);
  }

  public String getDisplayLocaleByCode(String locale) {
    return Locale.forLanguageTag(locale).getDisplayLanguage();
  }

  public void onAppChange() {
    if (isEditing()) {
      isEditableCms = true;
      selectedCms = lastSelectedCms; // Revert to last valid selection
      return;
    }

    if (StringUtils.isBlank(selectedProjectName)) {
      cmsList = pmvCmsMap.values().stream().map(PmvCms::getCmsList).flatMap(List::stream).toList();
    } else {
      cmsList = pmvCmsMap.values().stream().filter(pmvCms -> pmvCms.getPmvName().equals(selectedProjectName))
          .map(PmvCms::getCmsList).flatMap(List::stream).toList();
    }
    search();
  }

  public List<Locale> getTargetLocales() {
    return CmsContentUtils.getExcludedLocales(languageList, selectedSourceLocale);
  }

  public void rowSelect() {
    isEditableCms = false;
    if (isEditing()) {
      isEditableCms = true;
      selectedCms = lastSelectedCms; // Revert to last valid selection
    } else {
      if (selectedCms.isFile()) {
        loadFileContentOfSelectedCms();
      }
      if (isInEditMode) {
        isInEditMode = false;
        PF.current().ajax().update(CONTENT_FORM);
      } else {
        PF.current().ajax().update(CONTENT_FORM_CMS_COLUMN);
      }
    }
  }

  public void onRowSelect(SelectEvent<Cms> event) {
    if (selectedCmsEntries != null && selectedCmsEntries.size() == 1) {
      this.selectedCms = event.getObject();
      rowSelect();
    }
  }

  private void loadFileContentOfSelectedCms() {
    IProcessModelVersion selectedPmv = IApplication.current().getProcessModelVersions()
        .filter(pmv -> pmv.getName().equals(selectedCms.getPmvName())).findFirst().orElse(null);
    if (selectedPmv == null) {
      return;
    }

    Optional.ofNullable(ContentManagement.cms(selectedPmv)).flatMap(cms -> cms.get(selectedCms.getUri()))
        .ifPresent(this::loadFileContentOfCmsContent);
  }

  private void loadFileContentOfCmsContent(ContentObject contentObject) {
    try {
      for (CmsContent cmsContent : selectedCms.getContents()) {
        if (cmsContent == null) {
          break;
        }
        loadCmsFileFromProjectCms(contentObject, cmsContent);
        loadCmsFileFromApplicationCms(cmsContent, IApplication.current());
      }
    } catch (Exception e) {
      Ivy.log().error(e);
    }
  }

  public void loadCmsFileFromProjectCms(ContentObject contentObject, CmsContent cmsContent) {
    ContentObjectValue value = contentObject.value().get(cmsContent.getLocale());
    byte[] bytes = Optional.ofNullable(value).map(ContentObjectValue::read).map(ContentObjectReader::bytes).orElse(null);
    if (bytes != null) {
      cmsContent.setFileContent(bytes);
      cmsContent.setFileSize(FileUtils.calculateToKB(bytes.length));
    }
  }

  public void loadCmsFileFromApplicationCms(CmsContent cmsContent, IApplication currentApplication) {
    var cmsEntity = ContentManagement.cms(currentApplication).get(cmsContent.getUri());
    ContentObject currentContentObject = cmsEntity
        .orElseGet(() -> ContentManagement.cms(currentApplication).root().child().file(selectedCms.getUri(), selectedCms.getFileExtension()));
    byte[] bytesOfApplicationCmsFile = currentContentObject.value().get(cmsContent.getLocale()).read().bytes();
    if (bytesOfApplicationCmsFile != null) {
      cmsContent.setApplicationFileContent(bytesOfApplicationCmsFile);
      cmsContent.setApplicationFileSize(FileUtils.calculateToKB(bytesOfApplicationCmsFile.length));
    }
  }

  public void saveAll() throws JsonProcessingException {
    var languageIndexAndContentJsonString =
        FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("values");
    List<CmsValueDto> cmsValues = mapper.readValue(languageIndexAndContentJsonString, new TypeReference<>() {});
    for (CmsValueDto currentCmsValue : cmsValues) {
      save(currentCmsValue.getLanguageIndex(), currentCmsValue.getContents());
    }
  }

  public void checkIsEditingAndShowMessage() {
    isEditing();
  }

  private boolean isEditing() {
    if (lastSelectedCms == null) {
      return false;
    }
    var isEditing = lastSelectedCms.isEditing();
    if (isEditing) {
      showHaveNotBeenSavedDialog();
      PF.current().ajax().update(CONTENT_FORM_TABLE_CMS_KEYS);
    }
    return isEditing;
  }

  private void showHaveNotBeenSavedDialog() {
    var editingCmsList = lastSelectedCms.getContents().stream().filter(CmsContent::isEditing).map(CmsContent::getLocale)
        .map(Locale::getDisplayLanguage).collect(Collectors.toList());
    var detail = Utils.convertListToHTMLList(editingCmsList);
    showDialog(cms().co("/Labels/SomeFieldsHaveNotBeenSaved"), detail);
  }

  private void showDialog(String summary, String detail) {
    var message = new FacesMessage(SEVERITY_INFO, summary, detail);
    PrimeFaces.current().dialog().showMessageDynamic(message, false);
  }

  public void getAllChildren(String pmvName, ContentObject contentObject, List<Locale> locales) {
    // Exclude the CMS of itself
    if (!isShowEditorCms && Strings.CS.contains(pmvName, CMS_LIVE_EDITOR_PMV_NAME)
        && !Strings.CS.contains(pmvName, CMS_LIVE_EDITOR_DEMO_PMV_NAME)) {
      return;
    }

    if (contentObject.isRoot()) {
      locales =
          contentObject.cms().locales().stream().filter(locale -> isNotBlank(locale.getLanguage())).collect(toList());
    }

    for (ContentObject child : contentObject.children()) {
      if (child.children().isEmpty()) {
        var cms = convertToCms(child, locales, pmvName, child.meta().fileExtension());
        if (cms.getContents() != null) {
          var contents = pmvCmsMap.getOrDefault(pmvName, new PmvCms(pmvName, locales));
          contents.addCms(cms);
          pmvCmsMap.putIfAbsent(pmvName, contents);
        }
      }
      getAllChildren(pmvName, child, locales);
    }
  }

  private Cms convertToCms(ContentObject contentObject, List<Locale> locales, String pmvName, String fileExtension) {
    var cms = new Cms();
    cms.setUri(contentObject.uri());
    cms.setPmvName(pmvName);
    boolean isFile = StringUtils.isNotBlank(fileExtension);
    if (isFile) {
      cmsService.convertToCmsFile(contentObject, locales, cms, fileExtension);
    } else {
      cmsService.convertToCmsText(contentObject, locales, cms);
    }

    return cms;
  }

  private static boolean isActive(IActivity processModelVersion) {
    return processModelVersion != null && ActivityState.ACTIVE == processModelVersion.getActivityState();
  }

  private boolean isCmsMatchSearchKey(Cms entry, String searchKey) {
    if (StringUtils.isNotBlank(searchKey)) {
      return Strings.CI.contains(entry.getUri(), searchKey)
          || entry.getContents().stream().anyMatch(value -> Strings.CI.contains(value.getContent(), searchKey));
    }
    return true;
  }

  private void saveCms(SavedCms savedCms) {
    Map<String, SavedCms> cmsLocaleMap = savedCmsMap.computeIfAbsent(savedCms.getUri(), key -> new HashMap<>());
    cmsLocaleMap.put(savedCms.getLocale(), savedCms);
  }

  public void save(int languageIndex, String content) {
    selectedCms.getContents().stream().filter(value -> value.getIndex() == languageIndex).findAny()
        .ifPresent(cmsContent -> handleCmsContentSave(selectedCms.getUri(), content, cmsContent));
  }

  private void handleCmsContentSave(String uri, String newContent, CmsContent cmsContent) {
    cmsContent.saveContent(newContent);
    var locale = cmsContent.getLocale();
    SavedCms savedCms =
        new SavedCms(uri, locale.toString(), cmsContent.getOriginalContent(), cmsContent.getContent());
    saveCms(savedCms);
  }

  public void setValueChanged() {
    FacesContext context = FacesContext.getCurrentInstance();
    Map<String, String> params = context.getExternalContext().getRequestParameterMap();
    int languageIndex = Integer.parseInt(params.get("languageIndex"));
    String newContent = params.get("content");
    CmsContent currentCmsContent = selectedCms.getContents().get(languageIndex);
    String sanitizedContent = Utils.sanitizeContent(currentCmsContent.getOriginalContent(), newContent);
    if (sanitizedContent.equals(currentCmsContent.getContent())) {
      return;
    }
    currentCmsContent.setEditing(true);
    if (lastSelectedCms != null) {
      lastSelectedCms.getContents().get(languageIndex).setEditing(true);
    }
  }

  private static String currentApplicationName() {
    return IApplication.current() != null ? IApplication.current().getName() : StringUtils.EMPTY;
  }

  public void downloadFinished() {
    showDialog(cms().co("/Labels/Message"), cms().co("/Labels/CmsDownloaded"));
  }

  public String getActiveIndex() {
    return Optional.ofNullable(selectedCms).map(Cms::getContents).map(
        values -> IntStream.rangeClosed(0, values.size()).mapToObj(Integer::toString).collect(Collectors.joining(",")))
        .orElse(StringUtils.EMPTY);
  }

  public boolean isTheSameContent(String originalContent, String content) {
    Document originValue = Jsoup.parse(originalContent);
    Document newValue = Jsoup.parse(content);

    return originValue.body().html().equals(newValue.body().html());
  }

  public void handleFileUpload(FileUploadEvent event) {
    int cmsIndex = (Integer) event.getComponent().getAttributes().get("index");
    CmsContent cmsContent = selectedCms.getContents().get(cmsIndex);
    UploadedFile file = event.getFile();
    long maxUploadedFileSize = FileUtils.getMaxUploadedFileSize();
    boolean isValidFileSize = FileUtils.isValidFileSize(file.getSize(), maxUploadedFileSize);
    String fileExtension = FileUtils.getFileExtension(file);
    boolean isValidFileType = selectedCms.getFileType().getFileExtension().contains(fileExtension);

    if (!isValidFileSize || !isValidFileType) {
      addErrorMessage(Ivy.cms().co("/Labels/Error"), StringUtils.EMPTY, cmsIndex);
      if (!isValidFileSize) {
        addErrorMessage(StringUtils.EMPTY, Ivy.cms().co("/Labels/InvalidFileSizeMessage", List.of(maxUploadedFileSize)), cmsIndex);
      }

      if (!isValidFileType) {
        addErrorMessage(StringUtils.EMPTY, Ivy.cms().co("/Labels/InvalidFileTypeMessage"), cmsIndex);
      }
    }

    if (isValidFileSize && isValidFileType) {
      handleUploadNewFile(file, cmsContent);
    } else {
      handleUploadNewFile(null, cmsContent);
    }
  }

  private void addErrorMessage(String summary, String messageDetails, int cmsIndex) {
    FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, messageDetails);
    FacesContext.getCurrentInstance().addMessage(String.format(ERROR_MESSAGE_FOR_CMS_FILE_UPLOAD, cmsIndex), errorMessage);
  }

  private void handleUploadNewFile(UploadedFile newUploadedFile, CmsContent cmsContent) {
    if (newUploadedFile != null) {
      cmsContent.setNewFileContent(newUploadedFile.getContent());
      cmsContent.setNewFileSize(FileUtils.calculateToKB(newUploadedFile.getSize()));
      cmsContent.setEditing(true);
    } else {
      cmsContent.setNewFileContent(null);
      cmsContent.setNewFileSize(0);
      cmsContent.setEditing(false);
    }
  }

  private void initLocales() {
    languageList = CmsContentUtils.getLocalListFromCMS(filteredCMSList);

    selectedSourceLocale = IvyUserService.getUserProperty(UserConstants.SOURCE_LANG);
    selectedTargetLocale = IvyUserService.getUserProperty(UserConstants.TARGET_LANG);

    if (selectedSourceLocale == null && !languageList.isEmpty() && selectedSourceLocale == null) {
      selectedSourceLocale = languageList.get(0).toLanguageTag();
    }
    selectedCmsEntries = new ArrayList<>();
    List<Locale> targets = getTargetLocales();
    if (!targets.isEmpty() && selectedTargetLocale == null) {
      selectedTargetLocale = targets.get(0).toLanguageTag();
    }
  }

  public void saveSettings() {
    IvyUserService.updateUserProperty(selectedSourceLocale, selectedTargetLocale);
  }

  public List<Cms> getFilteredCMSKeys() {
    return filteredCMSList;
  }

  public void setFilteredCMSKeys(List<Cms> filteredCMSKeys) {
    this.filteredCMSList = filteredCMSKeys;
  }

  public Cms getSelectedCms() {
    return selectedCms;
  }

  public void setSelectedCms(Cms selectedCms) {
    this.selectedCms = selectedCms;
  }

  public String getSearchKey() {
    return searchKey;
  }

  public void setSearchKey(String searchKey) {
    this.searchKey = searchKey;
  }

  public StreamedContent getFileDownload() {
    return fileDownload;
  }

  public String getSelectedProjectName() {
    return selectedProjectName;
  }

  public void setSelectedProjectName(String selectedProjectName) {
    this.selectedProjectName = selectedProjectName;
  }
  public boolean isEditableCms() {
    return isEditableCms;
  }

  public void setEditableCms(boolean isEditableCms) {
    this.isEditableCms = isEditableCms;
  }

  public String getResetConfirmText() {
    return resetConfirmText;
  }

  public void setResetConfirmText(String resetConfirmText) {
    this.resetConfirmText = resetConfirmText;
  }

  public Set<String> getProjectCms() {
    return this.pmvCmsMap.keySet();
  }

  public String getSelectedSourceLocale() {
    return selectedSourceLocale;
  }

  public void setSelectedSourceLocale(String selectedSourceLocale) {
    this.selectedSourceLocale = selectedSourceLocale;
  }

  public List<Locale> getLanguageList() {
    return languageList;
  }

  public void setLanguageList(List<Locale> languageList) {
    this.languageList = languageList;
  }

  public List<Cms> getSelectedCmsEntries() {
    return selectedCmsEntries;
  }

  public void setSelectedCmsEntries(List<Cms> selectedCmsEntries) {
    this.selectedCmsEntries = selectedCmsEntries;
  }

  public String getSelectedTargetLocale() {
    return selectedTargetLocale;
  }

  public void setSelectedTargetLocale(String selectedTargetLocale) {
    this.selectedTargetLocale = selectedTargetLocale;
  }

  public List<ExportOption> getExportOptions() {
    return exportOptions;
  }

  public void setExportOptions(List<ExportOption> exportOptions) {
    this.exportOptions = exportOptions;
  }

  public StreamedContent getYamlFileDownload() {
    return yamlFileDownload;
  }

  public void setYamlFileDownload(StreamedContent yamlFileDownload) {
    this.yamlFileDownload = yamlFileDownload;
  }

  public void setFileDownload(StreamedContent fileDownload) {
    this.fileDownload = fileDownload;
  }
  
  
}
