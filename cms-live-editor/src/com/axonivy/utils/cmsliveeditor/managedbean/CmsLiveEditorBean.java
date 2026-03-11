package com.axonivy.utils.cmsliveeditor.managedbean;

import static ch.ivyteam.ivy.environment.Ivy.cms;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CMS_LIVE_EDITOR_DEMO_PMV_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CMS_LIVE_EDITOR_PMV_NAME;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CONTENT_FORM;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CONTENT_FORM_CMS_COLUMN;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CONTENT_FORM_EDITABLE_COLUMN;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CONTENT_FORM_PATH_COLUMN;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.CONTENT_FORM_TABLE_CMS_KEYS;
import static com.axonivy.utils.cmsliveeditor.constants.CmsConstants.ERROR_MESSAGE_FOR_CMS_FILE_UPLOAD;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.faces.application.FacesMessage.SEVERITY_INFO;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import com.axonivy.utils.cmsliveeditor.constants.CommonConstants;
import com.axonivy.utils.cmsliveeditor.constants.FileConstants;
import com.axonivy.utils.cmsliveeditor.dto.CmsValueDto;
import com.axonivy.utils.cmsliveeditor.enums.FileType;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.PmvCms;
import com.axonivy.utils.cmsliveeditor.model.SavedCms;
import com.axonivy.utils.cmsliveeditor.service.CmsService;
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
import ch.ivyteam.ivy.cm.ContentManagementSystem;
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
  private static final String CMS_FILE_FORMAT = "%s_%s.%s";
  private static final String FILE_EXTENSION_FORMAT = ".%s";
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
  private boolean isShowEditorCms;
  private Map<String, PmvCms> pmvCmsMap;
  private boolean isEditableCms;
  private String resetConfirmText;
  private boolean isInEditMode;

  @PostConstruct
  private void init() {
    isShowEditorCms = FacesContexts.evaluateValueExpression("#{data.showEditorCms}", Boolean.class);
    savedCmsMap = new HashMap<>();
    pmvCmsMap = new HashMap<>();
    for (var app : IApplicationRepository.of(ISecurityContext.current()).all()) {
      app.getProcessModels().stream().filter(CmsLiveEditorBean::isActive).map(IProcessModel::getReleasedProcessModelVersion)
          .filter(CmsLiveEditorBean::isActive)
          .forEach(pmv -> getAllChildren(pmv.getName(), ContentManagement.cms(pmv).root(), new ArrayList<>()));
    }
    onAppChange();
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
      cmsContent.setNewUploadedFile(null);
      cmsContent.setNewFileSize(0);
      cmsContent.setNewFileContent(null);
      cmsContent.setEditing(cmsContent.getApplicationFileSize() > 0);
      cmsContent.setApplicationFileSize(0);
      cmsContent.setApplicationFileContent(null);
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

  private void clearNewUploadFile() {
    if (selectedCms.isFile()) {
      selectedCms.getContents().stream().forEach(cmsContent -> {
        cmsContent.setNewUploadedFile(null);
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
    PF.current().ajax().update(CONTENT_FORM);
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

  private void loadFileContentOfSelectedCms() {
    IProcessModelVersion selectedPmv = IApplication.current().getProcessModelVersions()
        .filter(pmv -> pmv.getProjectName().equals(selectedCms.getPmvName())).findFirst().orElseGet(null);
    if (selectedPmv == null) {
      return;
    }

    ContentManagementSystem contentManagementSystem = ContentManagement.cms(selectedPmv);
    if (contentManagementSystem == null) {
      return;
    }

    ContentObject contentObject = contentManagementSystem.get(selectedCms.getUri()).orElseGet(null);
    if (contentObject == null) {
      return;
    }

    loadFileContentOfCmsContent(contentObject);
  }

  private void loadFileContentOfCmsContent(ContentObject contentObject) {
    try {
      for (CmsContent cmsContent : selectedCms.getContents()) {
        if (cmsContent == null) {
          break;
        }
        LoadCmsFileFromProjectCms(contentObject, cmsContent);
        LoadCmsFileFromApplicationCms(cmsContent, IApplication.current());
      }
    } catch (Exception e) {
      Ivy.log().error(e);
    }
  }

  public void LoadCmsFileFromProjectCms(ContentObject contentObject, CmsContent cmsContent) {
    ContentObjectValue value = contentObject.value().get(cmsContent.getLocale());
    byte[] bytes = ofNullable(value).map(ContentObjectValue::read).map(ContentObjectReader::bytes).orElseGet(null);
    if (bytes != null) {
      cmsContent.setFileContent(bytes);
      cmsContent.setFileSize(FileUtils.calculateToKB(bytes.length));
    }
  }

  public void LoadCmsFileFromApplicationCms(CmsContent cmsContent, IApplication currentApplication) {
    var cmsEntity = ContentManagement.cms(currentApplication).get(cmsContent.getUri());
    ContentObject currentContentObject = cmsEntity.orElseGet(() -> ContentManagement.cms(currentApplication).root()
        .child().file(selectedCms.getUri(), selectedCms.getFileExtension()));
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
      convertToCmsFile(contentObject, locales, cms, fileExtension);
    } else {
      convertToCmsText(contentObject, locales, cms);
    }

    return cms;
  }

  private void convertToCmsFile(ContentObject contentObject, List<Locale> locales, Cms cms, String fileExtension) {
    cms.setFile(true);
    cms.setFileExtension(StringUtils.upperCase(fileExtension, Locale.ENGLISH));
    FileType fileType = getFileTypeByExtension(fileExtension);
    cms.setFileType(fileType);
    for (var i = 0; i < locales.size(); i++) {
      Locale locale = locales.get(i);
      String language = locale.getLanguage();
      String projectCmsFileUri = String.format(CMS_FILE_FORMAT, contentObject.uri(), language, fileExtension);
      String fileName = projectCmsFileUri.substring(projectCmsFileUri.lastIndexOf(CommonConstants.SLASH_CHARACTER) + 1);
      cms.addContent(new CmsContent(i, locale, true, fileName, projectCmsFileUri));
    }
  }

  private void convertToCmsText(ContentObject contentObject, List<Locale> locales, Cms cms) {
    for (var i = 0; i < locales.size(); i++) {
      Locale locale = locales.get(i);
      ContentObjectValue value = contentObject.value().get(locale);
      String projectCmsValueString =
          ofNullable(value).map(ContentObjectValue::read).map(ContentObjectReader::string).orElse(EMPTY);
      String cmsApplicationValue = cmsService.getCmsFromApplication(cms.getUri(), locale);
      if (StringUtils.isBlank(cmsApplicationValue)) {
        cmsApplicationValue = projectCmsValueString;
      }
      cms.addContent(new CmsContent(i, locale, projectCmsValueString, cmsApplicationValue));
    }
  }

  private FileType getFileTypeByExtension(String extension) {
    String fileExtension = String.format(FILE_EXTENSION_FORMAT, StringUtils.lowerCase(extension, Locale.ENGLISH));
    return FileType.fromExtension(fileExtension);
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
        .ifPresent(cmsContent -> handleCmsContentSave(content, cmsContent));
  }

  private void handleCmsContentSave(String newContent, CmsContent cmsContent) {
    cmsContent.saveContent(newContent);
    var locale = cmsContent.getLocale();
    SavedCms savedCms =
        new SavedCms(selectedCms.getUri(), locale.toString(), cmsContent.getOriginalContent(), cmsContent.getContent());
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

  public void handleBeforeDownloadFile() throws Exception {
    String applicationName = IApplication.current() != null ? IApplication.current().getName() : StringUtils.EMPTY;
    this.fileDownload = CmsFileUtils.writeCmsToZipStreamedContent(selectedProjectName, applicationName, this.pmvCmsMap);
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
    int index = (Integer) event.getComponent().getAttributes().get("index");
    CmsContent cmsContent = selectedCms.getContents().get(index);
    UploadedFile file = event.getFile();
    long maxUploadedFileSize = getMaxUploadedFileSize();
    boolean isValidFileSize = FileUtils.isValidFileSize(file.getSize(), maxUploadedFileSize);
    String fileExtension = getFileExtension(file);
    boolean isValidFileType = selectedCms.getFileType().getFileExtension().contains(fileExtension);

    if (!isValidFileSize || !isValidFileType) {
      FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, Ivy.cms().co("/Labels/Error"), "");
      FacesContext.getCurrentInstance().addMessage(String.format(ERROR_MESSAGE_FOR_CMS_FILE_UPLOAD, index), message);
      if (!isValidFileSize) {
        FacesMessage invalidFileSizeMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "",
            Ivy.cms().co("/Labels/InvalidFileSizeMessage", List.of(maxUploadedFileSize)));
        FacesContext.getCurrentInstance().addMessage(String.format(ERROR_MESSAGE_FOR_CMS_FILE_UPLOAD, index),
            invalidFileSizeMessage);
      }

      if (!isValidFileType) {
        FacesMessage invalidFileTypeMessage =
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "", Ivy.cms().co("/Labels/InvalidFileTypeMessage"));
        FacesContext.getCurrentInstance().addMessage(String.format(ERROR_MESSAGE_FOR_CMS_FILE_UPLOAD, index),
            invalidFileTypeMessage);
      }
    }

    if (isValidFileSize && isValidFileType) {
      handleUploadNewFile(file, cmsContent);
    } else {
      handleUploadNewFile(null, cmsContent);
    }
  }

  private long getMaxUploadedFileSize() {
    try {
      return Long.parseLong(Ivy.var().get("com.axonivy.market.CMSLiveEditor.MaxUploadedFileSize"));
    } catch (Exception e) {
      Ivy.log().error(e);
      return FileConstants.DEFAULT_VALID_SIZE_MB;
    }
  }

  private String getFileExtension(UploadedFile file) {
    if (file == null) {
      return StringUtils.EMPTY;
    }
    String extension = StringUtils.EMPTY;
    String fileName = file.getFileName();
    if (StringUtils.isNotBlank(fileName)) {
      int lastDot = fileName.lastIndexOf(CommonConstants.DOT_CHARACTER);
      if (lastDot > 0 && lastDot < fileName.length() - 1) {
        extension = fileName.substring(lastDot + 1).toLowerCase();
      }
    }
    return extension;
  }

  private void handleUploadNewFile(UploadedFile newUploadedFile, CmsContent cmsContent ) {
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
}
