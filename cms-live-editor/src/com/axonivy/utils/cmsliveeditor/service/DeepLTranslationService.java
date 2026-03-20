package com.axonivy.utils.cmsliveeditor.service;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.call.SubProcessCall;
import ch.ivyteam.ivy.process.call.SubProcessCallResult;
import ch.ivyteam.ivy.process.call.SubProcessCallStartParamCaller;
import deepl.translate.Options;

public class DeepLTranslationService {

  private static final String PROCESS_PATH = "deepl/translate";
  private static final String TEXT = "text";
  private static final String OPTIONS = "options";
  private static final String TRANSLATION = "translation";

  public static String translate(String text, Options translateOptions) {
    try {
      SubProcessCallStartParamCaller call =
          SubProcessCall.withPath(PROCESS_PATH).withStartName(TEXT).withParam(TEXT, text).withParam(OPTIONS, translateOptions);
      SubProcessCallResult result = call.call();
      return (String) result.get(TRANSLATION);
    } catch (Exception e) {
      Ivy.log().error("#translate DeepL translation failed", e);
      return text;
    }
  }
}