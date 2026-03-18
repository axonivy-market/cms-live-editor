package com.axonivy.utils.cmsliveeditor.service;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.call.SubProcessCall;
import ch.ivyteam.ivy.process.call.SubProcessCallResult;
import ch.ivyteam.ivy.process.call.SubProcessCallStartParamCaller;
import deepl.translate.Options;

public class DeepLTranslationService {

  private static final String PROCESS_PATH = "deepl/translate";

  public static String translate(String text, Options translateOptions) {
    try {
      SubProcessCallStartParamCaller call =
          SubProcessCall.withPath(PROCESS_PATH).withStartName("text").withParam("text", text).withParam("options", translateOptions);
      SubProcessCallResult result = call.call();
      return (String) result.get("translation");
    } catch (Exception e) {
      Ivy.log().error("#translate DeepL translation failed", e);
      return text;
    }
  }
}