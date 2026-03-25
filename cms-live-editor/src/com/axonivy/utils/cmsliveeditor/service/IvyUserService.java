package com.axonivy.utils.cmsliveeditor.service;

import com.axonivy.utils.cmsliveeditor.constants.UserConstants;

import ch.ivyteam.ivy.environment.Ivy;

public class IvyUserService {

  private IvyUserService() {
  }

  public static void updateUserProperty(String sourceLang, String targetLang) {
    Ivy.session().getSessionUser().setProperty(UserConstants.SOURCE_LANG, sourceLang);
    Ivy.session().getSessionUser().setProperty(UserConstants.TARGET_LANG, targetLang);
  }

  public static String getUserProperty(String propertyName) {
    return Ivy.session().getSessionUser().getProperty(propertyName);
  }
}
