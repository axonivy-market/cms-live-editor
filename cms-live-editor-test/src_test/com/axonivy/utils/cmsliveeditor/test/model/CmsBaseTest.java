package com.axonivy.utils.cmsliveeditor.test.model;

import com.axonivy.utils.cmsliveeditor.model.Cms;

public class CmsBaseTest {
  static final String URI_A = "/cms/path/A";
  static final String URI_B = "/cms/path/B";
  static Cms cmsA;
  static Cms cmsB;
  static Cms cmsNullPath;

  static {
    cmsA = cmsWithUri(URI_A);
    cmsB = cmsWithUri(URI_B);
    cmsNullPath = cmsWithUri(null);
  }

  static Cms cmsWithUri(String uri) {
    Cms cms = new Cms();
    cms.setUri(uri);
    return cms;
  }

}
