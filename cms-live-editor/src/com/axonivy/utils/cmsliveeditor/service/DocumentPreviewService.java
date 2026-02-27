package com.axonivy.utils.cmsliveeditor.service;

import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.DOCX_EXTENSION;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.DOC_EXTENSION;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.EML_EXTENSION;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.JPEG_CONTENT_TYPE;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.JPEG_EXTENSION;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.JPG_EXTENSION;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.MSG_EXTENSION;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.PDF_CONTENT_TYPE;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.PDF_EXTENSION;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.PNG_CONTENT_TYPE;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.PNG_EXTENSION;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.XLSX_EXTENSION;
import static com.axonivy.utils.cmsliveeditor.constants.DocumentConstants.XLS_EXTENSION;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.aspose.cells.PdfSaveOptions;
import com.aspose.cells.Workbook;
import com.aspose.email.MailMessage;
import com.aspose.words.Document;
import com.aspose.words.LoadFormat;
import com.aspose.words.LoadOptions;
import com.aspose.words.Orientation;
import com.aspose.words.PageSetup;
import com.aspose.words.SaveFormat;
import com.aspose.words.Section;
import com.axonivy.utils.cmsliveeditor.enums.AsposeProduct;


public class DocumentPreviewService {

  private static final DocumentPreviewService INSTANCE = new DocumentPreviewService();

  private DocumentPreviewService() {}

  public static DocumentPreviewService getInstance() {
    return INSTANCE;
  }

  public StreamedContent convertToStreamContent(String fileName, byte[] fileContent) throws Exception {
    StreamedContent content = null;
    if (fileName.endsWith(XLSX_EXTENSION) || fileName.endsWith(XLS_EXTENSION)) {
      LicenseLoader.loadLicenseforProduct(AsposeProduct.CELLS);
      content = convertExcelToPdfStreamedContent(fileContent, fileName);
    } else if (fileName.endsWith(DOC_EXTENSION) || fileName.endsWith(DOCX_EXTENSION)) {
      LicenseLoader.loadLicenseforProduct(AsposeProduct.WORDS);
      content = convertWordToPdfStreamedContent(fileContent, fileName);
    } else if (fileName.endsWith(EML_EXTENSION) || fileName.endsWith(MSG_EXTENSION)) {
      LicenseLoader.loadLicenseforProduct(AsposeProduct.WORDS);
      LicenseLoader.loadLicenseforProduct(AsposeProduct.EMAIL);
      content = convertEmlToPdfStreamedContent(fileContent, fileName);
    } else {
      String contentType = getContentTypeByFileName(fileName);
      content = convertOutputStreamToStreamedContent(fileName, contentType, fileContent);
    }
    return content;
  }

  private StreamedContent convertExcelToPdfStreamedContent(byte[] data, String fileName) throws Exception {
    try (InputStream inputStream = new ByteArrayInputStream(data);
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream()) {
      Workbook workbook = new Workbook(inputStream);
      PdfSaveOptions options = new PdfSaveOptions();
      options.setOnePagePerSheet(false);
      options.setAllColumnsInOnePagePerSheet(true);
      workbook.save(pdfOut, options);
      return convertOutputStreamToStreamedContent(pdfOut, fileName);
    }
  }

  private StreamedContent convertWordToPdfStreamedContent(byte[] data, String fileName) throws Exception {
    try (InputStream inputStream = new ByteArrayInputStream(data);
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream()) {
      LoadOptions loadOptions = new LoadOptions();
      loadOptions.setLoadFormat(LoadFormat.AUTO);
      Document document = new Document(inputStream, loadOptions);
      document.save(pdfOut, SaveFormat.PDF);
      return convertOutputStreamToStreamedContent(pdfOut, fileName);
    }
  }

  private StreamedContent convertEmlToPdfStreamedContent(byte[] data, String fileName) throws Exception {
    try (InputStream inputStream = new ByteArrayInputStream(data);
        ByteArrayOutputStream mhtmlStream = new ByteArrayOutputStream();
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream()) {
      MailMessage mailMsg = MailMessage.load(inputStream);
      mailMsg.save(mhtmlStream, com.aspose.email.SaveOptions.getDefaultMhtml());
      var loadOptions = new LoadOptions();
      loadOptions.setLoadFormat(LoadFormat.MHTML);

      try (InputStream mhtmlInput = new ByteArrayInputStream(mhtmlStream.toByteArray())) {
        Document doc = new Document(mhtmlInput);
        for (Section section : doc.getSections()) {
          PageSetup pageSetup = section.getPageSetup();
          pageSetup.setOrientation(Orientation.LANDSCAPE);
        }
        doc.save(pdfOut, SaveFormat.PDF);
      }
      return convertOutputStreamToStreamedContent(pdfOut, fileName);
    }
  }

  private StreamedContent convertOutputStreamToStreamedContent(ByteArrayOutputStream pdfOut, String fileName) {
    byte[] pdfBytes = pdfOut.toByteArray();
    return convertOutputStreamToStreamedContent(fileName, PDF_CONTENT_TYPE, pdfBytes);
  }

  private StreamedContent convertOutputStreamToStreamedContent(String fileName, String contentType,
      byte[] fileContent) {
    return DefaultStreamedContent.builder().contentType(contentType).name(fileName)
        .stream(() -> new ByteArrayInputStream(fileContent)).build();
  }

  private String getContentTypeByFileName(String fileName) {
    if (StringUtils.isBlank(fileName)) {
      return StringUtils.EMPTY;
    }

    String extension = StringUtils.lowerCase(fileName.substring(fileName.lastIndexOf(".")));
    String contentType = switch (extension) {
      case PDF_EXTENSION -> PDF_CONTENT_TYPE;
      case PNG_EXTENSION -> PNG_CONTENT_TYPE;
      case JPEG_EXTENSION -> JPEG_CONTENT_TYPE;
      case JPG_EXTENSION -> PDF_CONTENT_TYPE;
      default -> StringUtils.EMPTY;
    };
    return contentType;
  }

}
