package com.axonivy.utils.cmsliveeditor.service;

import java.util.Optional;

import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.CmsContent;
import com.axonivy.utils.cmsliveeditor.utils.FileUtils;

import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.ContentObjectReader;
import ch.ivyteam.ivy.cm.ContentObjectValue;
import ch.ivyteam.ivy.cm.exec.ContentManagement;
import ch.ivyteam.ivy.environment.Ivy;

public class CmsContentLoader {

  private CmsContentLoader() {}

  public static void loadFileContentOfCms(Cms selectedCms) {
    IApplication.current().getProcessModelVersions().filter(pmv -> pmv.getName().equals(selectedCms.getPmvName()))
        .findFirst().ifPresent(pmv -> loadFileContentFromPmv(selectedCms, pmv));
  }

  private static void loadFileContentFromPmv(Cms cms, IProcessModelVersion pmv) {
    Optional.ofNullable(ContentManagement.cms(pmv)).flatMap(contentMgmt -> contentMgmt.get(cms.getUri()))
        .ifPresent(contentObject -> loadFileContentOfCmsContent(cms, contentObject));
  }

  private static void loadFileContentOfCmsContent(Cms cms, ContentObject contentObject) {
    try {
      for (CmsContent cmsContent : cms.getContents()) {
        if (cmsContent == null) {
          break;
        }
        loadCmsFileFromProjectCms(contentObject, cmsContent);
        loadCmsFileFromApplicationCms(cms, cmsContent, IApplication.current());
      }
    } catch (Exception e) {
      Ivy.log().error(e);
    }
  }

  public static void loadCmsFileFromProjectCms(ContentObject contentObject, CmsContent cmsContent) {
    byte[] bytes = Optional.ofNullable(contentObject.value().get(cmsContent.getLocale())).map(ContentObjectValue::read)
        .map(ContentObjectReader::bytes).orElse(null);
    if (bytes != null) {
      cmsContent.setFileContent(bytes);
      cmsContent.setFileSize(FileUtils.calculateToKB(bytes.length));
    }
  }

  public static void loadCmsFileFromApplicationCms(Cms cms, CmsContent cmsContent, IApplication currentApplication) {
    var cmsEntity = ContentManagement.cms(currentApplication).get(cmsContent.getUri());
    ContentObject contentObject = cmsEntity.orElseGet(
        () -> ContentManagement.cms(currentApplication).root().child().file(cms.getUri(), cms.getFileExtension()));
    byte[] bytes = contentObject.value().get(cmsContent.getLocale()).read().bytes();
    if (bytes != null) {
      cmsContent.setApplicationFileContent(bytes);
      cmsContent.setApplicationFileSize(FileUtils.calculateToKB(bytes.length));
    }
  }
}