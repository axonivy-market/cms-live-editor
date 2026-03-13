# CMS Live Editor
In Axon Ivy, languages for UIs, notifications, and emails are managed in the CMS (Content Management System). We are excited to introduce the new CMS Live Editor, which significantly simplifies language editing.

**Key features:**

- Process based, i.e. the CMS Live Editor can be started from the dashboard
- Live edits are enabled (i.e. no deployment necessary)
- User-friendly editor for translating into new languages
- Support for an unlimited number of languages
- Simple styling options
- Allow users to modify CMS files
- No HTML tags required in translation texts

## Demo
### 1. Install the CMS Live Editor
The CMS Live Editor must be installed in the same security context as the project content you want to edit.

### 2. CMS Live Editor process start:
The CMS Live Editor is now available as a process start in the dashboard. Users must have the role `CMS_ADMIN` to see and start the process.

![](./images/1-cms-live-editor-process.png)

### 3. CMS Live Editor main page:
![](./images/2-cms-live-editor-main-page.png)

1. Project Selector: Each security context can contain multiple projects. The option "All" will be set as default. Select a project if you want to view the content of a specific project only.
2. Search Input: You can enter text to search by URI or by content. The search is **case-insensitive**.
3. Selected CMS: Display the URI path of the selected content.
4. Edit button: Click to edit this CMS, and another column will be rendered for the user to edit the value for a specific language.
5. Save makes these changes immediately visible in the application

  ![](./images/13-cms-live-editor-howto.png)

 ### 4. Validation
- When clicking **Save**, the editor validates **numbered placeholders** in the format `{0}`, `{1}`.
  - If **all locales** of the current CMS entry were edited, they must contain the same set of placeholders (order does not matter).
  - If **only specific locales** were edited, each edited locale must contain exactly the same placeholders as its original value.
- When placeholder validation fails:
   - Save is blocked, the affected editor(s) are highlighted, and an error message is shown: **Invalid placeholder syntax.**
   - You cannot switch to another CMS entry, use the search filter, or change the project. A popup appears **Some fields have not been saved yet**. You must **Cancel** or correct your current edits to continue.
- Example: For the CMS entry *UploadFileExists*, the current edits must still contain `{0}`. Do not remove it, rename it (e.g., to `{1}`), remove the brackets, or add extra placeholders in some locales but not others.
   ![](./images/11-cms-live-editor-placeholder-validation.png)

### 5. File Support

The CMS Live Editor also supports handling files stored in the CMS. Files can be uploaded and previewed:

1. It is possible to preview the content of CMS files. Currently supported file types are Word, Excel, PDF, and images.
![](./images/15-cms-live-editor-preview-content.png)
![](./images/16-cms-live-editor-preview-dialog.png)

2. You can upload a file of the same type as the existing CMS file by clicking the “Attach/Change Document” link.
![](./images/17-cms-live-editor-upload-document.png)
![](./images/18-cms-live-editor-upload-cms-file-success.png)

3. An error message will be displayed if the uploaded file type is different from the project's CMS file type, or if the uploaded file size exceeds the allowed limit. The allowed file size limit is configured in the variable `com.axonivy.utils.cmsliveeditor.MaxUploadedFileSize`.
![](./images/19-cms-live-editor-upload-wrong-cms-file.png)

### 6. Undo changes
- “Undo Change” reverses the changes "made to the currently selected CMS entry.
- "Reset all" will revert **all** changes made in the currently selected project, i.e., all entries marked with a red dot in the path column. As this can be a disruptive action, a confirmation dialog is displayed and the user must type the word "reset" to enable the "Reset all"-action.

### 7. Download for deployment
- The **Download for deployment** button allows users to download a ZIP file containing all translated content.
- This can be used for a permanent engine deployment of the CMS values in the application.
  ![](./images/14-cms-live-editor-undo.png)

## Setup

By default, the maximum file size for CMS uploads is 50 MB. If needed, you can modify this limit by changing the configuration variable `com.axonivy.utils.cmsliveeditor.MaxUploadedFileSize`.

```
@variables.yaml@
```
