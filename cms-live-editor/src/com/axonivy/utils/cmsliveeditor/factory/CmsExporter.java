package com.axonivy.utils.cmsliveeditor.factory;

import java.util.Map;

import org.primefaces.model.StreamedContent;

import com.axonivy.utils.cmsliveeditor.model.PmvCms;

public interface CmsExporter {
  /**
   * Collect export files (text-based: yaml, or binary: excel) and also collect CMS binary files into cmsFiles map.
   */
  StreamedContent export(String projectName, String applicationName, Map<String, PmvCms> pmvCmsMap);
}
