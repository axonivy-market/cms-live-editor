package com.axonivy.utils.cmsliveeditor.test.factory;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.cmsliveeditor.enums.ExportType;
import com.axonivy.utils.cmsliveeditor.factory.CmsExcelExporter;
import com.axonivy.utils.cmsliveeditor.factory.CmsExporter;
import com.axonivy.utils.cmsliveeditor.factory.CmsExporterFactory;
import com.axonivy.utils.cmsliveeditor.factory.CmsYamlExporter;

class CmsExporterFactoryTest {

  @Test
  public void testCreateShouldReturnExcelExporterForExcelType() {
    CmsExporter exporter = CmsExporterFactory.create(ExportType.EXCEL);
    assertNotNull(exporter);
    assertInstanceOf(CmsExcelExporter.class, exporter);
  }

  @Test
  public void testCreateShouldReturnYamlExporterForYamlType() {
    CmsExporter exporter = CmsExporterFactory.create(ExportType.YAML);
    assertNotNull(exporter);
    assertInstanceOf(CmsYamlExporter.class, exporter);
  }
}