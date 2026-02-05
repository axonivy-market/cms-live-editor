package com.axonivy.utils.cmseditor.service;

import java.util.Locale;
import java.util.Map;

import com.axonivy.utils.cmseditor.model.Cms;
import com.axonivy.utils.cmseditor.model.CmsContent;
import com.axonivy.utils.cmseditor.model.SavedCms;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.exec.ContentManagement;
import ch.ivyteam.ivy.security.exec.Sudo;

public class CmsService {
  private static CmsService instance;

  public static CmsService getInstance() {
    if (instance == null) {
      instance = new CmsService();
    }
    return instance;
  }

  private ContentObject createOrGetCmsByUri(String uri) {
    IApplication currentApplication = IApplication.current();
    var cmsEntity = ContentManagement.cms(currentApplication).get(uri);
    return cmsEntity.orElseGet(() -> ContentManagement.cms(currentApplication).root().child().string(uri));
  }

  public void writeCmsToApplication(Map<String, Map<String, SavedCms>> savedCmsMap) {
    Sudo.run(() -> savedCmsMap.forEach((uri, localeAndContent) -> {
      ContentObject currentContentObject = createOrGetCmsByUri(uri);
      localeAndContent.forEach((locale, savedCms) -> {
        currentContentObject.value().get(Locale.forLanguageTag(locale)).write().string(savedCms.getNewContent());
      });
    }));
  }

  public String getCmsFromApplication(String uri, Locale locale) {
    IApplication currentApplication = IApplication.current();
    var cmsEntity = ContentManagement.cms(currentApplication).get(uri);
    return cmsEntity.map(contentObject -> contentObject.value().get(locale).read().string()).orElse(null);
  }

  public Cms compareWithCmsInApplication(Cms cms) {
    boolean isDifferent = isCmsDifferentWithApplication(cms);
    cms.getContents().forEach(content -> content.setEditing(false));
    cms.setDifferentWithApplication(isDifferent);
    return cms;
  }

  private boolean isCmsDifferentWithApplication(Cms cms) {
    for (CmsContent cmsContent : cms.getContents()) {
      String valueFromApp = getCmsFromApplication(cms.getUri(), cmsContent.getLocale());
      if (valueFromApp != null && !valueFromApp.equals(cmsContent.getOriginalContent())) {
        return true;
      }
    }
    return false;
  }
}
