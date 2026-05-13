package com.axonivy.utils.cmsliveeditor.service;

import com.axonivy.utils.cmsliveeditor.constants.UserConstants;

import ch.ivyteam.ivy.environment.Ivy;

public class IvyUserService {

  private IvyUserService() {
  }

  public static void updateTranslationUserProperties(String sourceLang, String targetLang) {
    Ivy.session().getSessionUser().setProperty(UserConstants.SOURCE_LANG, sourceLang);
    Ivy.session().getSessionUser().setProperty(UserConstants.TARGET_LANG, targetLang);
  }

  public static String getUserProperty(String propertyName) {
    return Ivy.session().getSessionUser().getProperty(propertyName);
  }

  public static boolean getUserPropertyWithBooleanValue(String propertyName) {
    return Boolean.valueOf(Ivy.session().getSessionUser().getProperty(propertyName));
  }

  public static void setUserPropertyWithBooleanValue(String propertyName, boolean value) {
    Ivy.session().getSessionUser().setProperty(propertyName, String.valueOf(value));
  }
}
