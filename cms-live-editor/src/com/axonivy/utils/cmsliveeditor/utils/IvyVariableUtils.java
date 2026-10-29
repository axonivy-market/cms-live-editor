package com.axonivy.utils.cmsliveeditor.utils;

import ch.ivyteam.ivy.environment.Ivy;

public class IvyVariableUtils {

  public static int getIntegerVariableOrDefault(String variable, int defaultValue) {
    try {
      return Integer.parseInt(Ivy.var().get(variable));
    } catch (Exception e) {
      Ivy.log().error(e);
      return defaultValue;
    }
  }
}
