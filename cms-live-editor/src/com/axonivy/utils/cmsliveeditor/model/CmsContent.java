package com.axonivy.utils.cmsliveeditor.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;

import org.apache.commons.lang.BooleanUtils;
import org.primefaces.model.file.UploadedFile;

import com.axonivy.utils.cmsliveeditor.utils.Utils;

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

  private byte[] fileContent;

  private UploadedFile newUploadedFile;

  private long applicationFileSize;

  private byte[] applicationFileContent;

  private long newFileSize;

  private byte[] newFileContent;

  private final boolean isHtml;

  public CmsContent(int index, Locale locale, String originalContent, String content) {
    this.index = index;
    this.locale = locale;
    this.originalContent = originalContent;
    this.content = content;
    this.isEditing = false;
    this.isHtml = BooleanUtils.isTrue(getIsHtmlContent());
  }

  public CmsContent(int index, Locale locale, boolean isFile, String fileName, String uri) {
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

  public byte[] getFileContent() {
    return fileContent;
  }

  public void setFileContent(byte[] fileContent) {
    this.fileContent = fileContent;
  }

  public long getFileSize() {
    return fileSize;
  }

  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
  }

  public long getApplicationFileSize() {
    return applicationFileSize;
  }

  public void setApplicationFileSize(long applicationFileSize) {
    this.applicationFileSize = applicationFileSize;
  }

  public byte[] getApplicationFileContent() {
    return applicationFileContent;
  }

  public void setApplicationFileContent(byte[] applicationFileContent) {
    this.applicationFileContent = applicationFileContent;
  }

  public UploadedFile getNewUploadedFile() {
    return newUploadedFile;
  }

  public void setNewUploadedFile(UploadedFile newUploadedFile) {
    this.newUploadedFile = newUploadedFile;
  }

  public long getNewFileSize() {
    return newFileSize;
  }

  public void setNewFileSize(long newFileSize) {
    this.newFileSize = newFileSize;
  }

  public byte[] getNewFileContent() {
    return newFileContent;
  }

  public void setNewFileContent(byte[] newFileContent) {
    this.newFileContent = newFileContent;
  }

  public Boolean getIsHtmlContent() {
    return BooleanUtils.isNotTrue(isFile) && Utils.containsHtmlTag(this.content);
  }
}
