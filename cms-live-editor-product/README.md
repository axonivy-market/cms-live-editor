# CMS Live Editor
In AxonIvy, languages for UIs, notifications, or emails are managed within the CMS. We are excited to introduce the new CMS editor that significantly simplifies language editing! The key features are:

- User-friendly editor for translating new languages
- Edit an unlimited number of languages
- Simple styles available
- No HTML tags needed in the translation text

## Demo
### 1. CMS live editor process start:
Users should have the role of "CMS_ADMIN" to start the process.
![](./images/1-cms-live-editor-process.png)

### 2. CMS live editor main page:
![](./images/2-cms-live-editor-main-page.png)

1. Project Selector: Each security context can contain multiple projects. First, choose the project you want to work on. The option "All" will be set as default when the user clicks start process for the first time.
   ![](./images/3-cms-live-editor-project-selection.png)

2. Search Input: You can enter text to search by URI and project CMS. The search is case-insensitive.
   ![](./images/4-cms-live-editor-search-bar.png)

3. Selected CMS: Displays the uri path of the selected content.
4. Edit button: Click to edit this CMS, and another column will be rendered for the user to edit the value for a specific language.
   ![](./images/5-cms-live-editor-edit-button.png)
   ![](./images/6-cms-live-editor-edit-column.png)
5. Save button:
- When we hover to "Save" button , a warning message will be display to impress user
  ![](./images/10-cms-live-editor-save-button-warning.png)
- Change the CMS to application CMS, then the "Orange-dot" will be automatically marked in the row of the application CMS to notify users that this CMS has a different value in the application CMS compared to the project CMS.
- In the header of the link column, red text "Reset all changes" will be displayed, allowing users to restore all updated CMS values that are different from the project's CMS.
- In the header of the CMS column, the blue text "Undo Changes" will be shown to enable users to undo all changes for this project's CMS (remove all values in the application CMS that belong to this project's CMS).
- The value of a specific language that the user edited will have a strikethrough for the project's CMS sitting on the next newly edited value.
  ![](./images/7-cms-live-editor-after-updating.png)
6. "Reset all changes" button: Display the confirm dialog and the user have to type "reset" word correctly , and button "Reset All" can be clickable. when users click it, all application CMS that we updated from project CMS will be deleted
   ![](./images/8-cms-live-editor-reset-all-changes.png)
   ![](./images/9-cms-live-editor-after-restoring.png)
7. "Download for deployment" button: Downloads a zip file containing all translated contents.

- When we hover to "download" button , a warning message will be display to impress user

  ![](./images/11-cms-live-editor-download-button-warning.png)

