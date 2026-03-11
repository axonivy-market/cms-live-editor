package com.axonivy.utils.cmsliveeditor.service;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.model.SavedCms;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.cm.ContentManagementSystem;
import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.exec.ContentManagement;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.exec.Sudo;

public class CmsService {
  private static CmsService instance;

  public static CmsService getInstance() {
    if (instance == null) {
      instance = new CmsService();
    }
    return instance;
  }

  public ContentManagementSystem getContentManagementSystemOfCurrentApplication() {
    IApplication currentApplication = IApplication.current();
    return ContentManagement.cms(currentApplication);
  }

  private ContentObject createOrGetCmsByUri(String uri) {
    ContentManagementSystem contentManagementSystem = getContentManagementSystemOfCurrentApplication();
    var cmsEntity = contentManagementSystem.get(uri);
    return cmsEntity.orElseGet(() -> contentManagementSystem.root().child().string(uri));
  }

  private boolean isCmsDifferentWithApplication(Cms cms) {
    for (CmsContent cmsContent : cms.getContents()) {
      if(cmsContent.isFile()) {
        return hasCmsFileFromApplication(cms.getUri(), cmsContent.getLocale());
      }

      String valueFromApp = getCmsFromApplication(cms.getUri(), cmsContent.getLocale());
      if (valueFromApp != null && !valueFromApp.equals(cmsContent.getOriginalContent())) {
        return true;
      }
    }
    return false;
  }

  public String getCmsFromApplication(String uri, Locale locale) {
    ContentManagementSystem contentManagementSystem = getContentManagementSystemOfCurrentApplication();
    var cmsEntity = contentManagementSystem.get(uri);
    return cmsEntity.map(contentObject -> contentObject.value().get(locale).read().string()).orElse(null);
  }

  public boolean hasCmsFileFromApplication(String uri, Locale locale) {
    try {
      ContentManagementSystem contentManagementSystem = getContentManagementSystemOfCurrentApplication();
      var cmsEntity = contentManagementSystem.get(uri);
      return cmsEntity.map(contentObject -> contentObject.value().get(locale)).isPresent();
    } catch (Exception e) {
      Ivy.log().error(e);
      return false;
    }
  }

  public Cms compareWithCmsInApplication(Cms cms) {
    boolean isDifferent = isCmsDifferentWithApplication(cms);
    cms.getContents().forEach(content -> content.setEditing(false));
    cms.setDifferentWithApplication(isDifferent);
    return cms;
  }

  public void writeCmsToApplication(Map<String, Map<String, SavedCms>> savedCmsMap) {
    Sudo.run(() -> savedCmsMap.forEach((uri, localeAndContent) -> {
      ContentObject currentContentObject = createOrGetCmsByUri(uri);
      localeAndContent.forEach((locale, savedCms) -> {
        currentContentObject.value().get(Locale.forLanguageTag(locale)).write().string(savedCms.getNewContent());
      });
    }));
  }

  public void writeCmsFileToApplication(Cms cms) {
    if (cms == null || CollectionUtils.isEmpty(cms.getContents())) {
      return;
    }
    ContentManagementSystem contentManagementSystem = getContentManagementSystemOfCurrentApplication();
    Sudo.run(() -> cms.getContents().forEach(cmsContent -> {
      if (!cmsContent.isEditing()) {
        return;
      }
      var cmsEntity = contentManagementSystem.get(cmsContent.getUri());
      ContentObject currentContentObject = cmsEntity.orElseGet(
          () -> contentManagementSystem.root().child().file(cms.getUri(), cms.getFileExtension()));
      if (cmsContent.getNewFileSize() > 0) {
        currentContentObject.value().get(cmsContent.getLocale()).write().bytes(cmsContent.getNewFileContent());
        cmsContent.setApplicationFileContent(cmsContent.getNewFileContent());
        cmsContent.setApplicationFileSize(cmsContent.getNewFileSize());
      } else {
        currentContentObject.value().get(cmsContent.getLocale()).delete();
      }
    }));
  }

  public void removeAllCmsFiles(Cms cms) {
    try {
      if (cms == null || CollectionUtils.isEmpty(cms.getContents())) {
        return;
      }

      ContentManagementSystem contentManagementSystem = getContentManagementSystemOfCurrentApplication();
      Sudo.run(() -> cms.getContents().forEach(cmsContent -> {
        var cmsEntity = contentManagementSystem.get(cmsContent.getUri());
        ContentObject currentContentObject = cmsEntity
            .orElseGet(() -> contentManagementSystem.root().child().file(cms.getUri(), cms.getFileExtension()));
        currentContentObject.value().get(cmsContent.getLocale()).delete();
      }));
    } catch (Exception e) {
      Ivy.log().error(e);
    }
  }

  public void removeApplicationCmsByUri(String uri) {
    Sudo.run(() -> createOrGetCmsByUri(uri).delete());
  }

}
