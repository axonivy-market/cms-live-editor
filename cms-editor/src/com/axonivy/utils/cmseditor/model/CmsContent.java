package com.axonivy.utils.cmseditor.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;

import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import com.axonivy.utils.cmseditor.service.DocumentPreviewService;
import com.axonivy.utils.cmseditor.utils.Utils;

import ch.ivyteam.ivy.environment.Ivy;

public class CmsContent implements Serializable {

  @Serial
  private static final long serialVersionUID = 1830742314488808118L;

  private int index;

  private Locale locale;

  private String originalContent;

  private String content;

  private boolean isEditing;

  private boolean isFile;

  private String uri;

  private String fileName;

  private long fileSize;

  private StreamedContent data;
  
  private UploadedFile newUploadedFile;
  
  private long newFileSize;
  
  private StreamedContent newData;
  
  private byte[] newFileContent;

  private final boolean isHtml;

  public CmsContent(int index, Locale locale, String originalContent, String content) {
    super();
    this.index = index;
    this.locale = locale;
    this.originalContent = originalContent;
    this.content = content;
    this.isEditing = false;
    this.isHtml = Utils.containsHtmlTag(originalContent);
  }

  public CmsContent(int index, Locale locale, boolean isFile, String fileName, String uri) {
    super();
    this.index = index;
    this.locale = locale;
    this.isFile = isFile;
    this.fileName = fileName;
    this.uri = uri;
    this.isHtml = false;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    // do nothing. ignore value when submit form, just accept only click save
  }

  public void saveContent(String contents) {
    this.content = Utils.sanitizeContent(originalContent, contents);
    this.isEditing = false;
  }

  public boolean isEditing() {
    return isEditing;
  }

  public void setEditing(boolean isEditting) {
    this.isEditing = isEditting;
  }

  public String getOriginalContent() {
    return originalContent;
  }

  public void setOriginalContent(String originalContent) {
    this.originalContent = originalContent;
  }

  public boolean isHtml() {
    return isHtml;
  }

  public boolean isFile() {
    return isFile;
  }

  public void setFile(boolean isFile) {
    this.isFile = isFile;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public StreamedContent getData() {
    return data;
  }

  public void setData(StreamedContent data) {
    this.data = data;
  }

  public long getFileSize() {
    return fileSize;
  }

  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
  }

  public UploadedFile getNewUploadedFile() {
    return newUploadedFile;
  }

  public void setNewUploadedFile(UploadedFile newUploadedFile) {
    this.newUploadedFile = newUploadedFile;
    try {
      setNewFileContent(newUploadedFile.getContent());
      setNewData(DocumentPreviewService.getInstance().convertToStreamContent(fileName, newUploadedFile.getContent()));
      setNewFileSize((long) Math.ceil(newUploadedFile.getSize() / 1024.0));
    } catch (Exception e) {
      Ivy.log().error(e);
    }
  }

  public long getNewFileSize() {
    return newFileSize;
  }

  public void setNewFileSize(long newFileSize) {
    this.newFileSize = newFileSize;
  }

  public StreamedContent getNewData() {
    return newData;
  }

  public void setNewData(StreamedContent newData) {
    this.newData = newData;
  }

  public byte[] getNewFileContent() {
    return newFileContent;
  }

  public void setNewFileContent(byte[] newFileContent) {
    this.newFileContent = newFileContent;
  }
}
