window.cmsLiveEditors = window.cmsLiveEditors || {};
window.cmsDirtyEditors = new Set();
window.cmsOriginalPlaceholders = window.cmsOriginalPlaceholders || {};
window.cmsLiveEditorIds = window.cmsLiveEditorIds || {};
window.cmsInitialContents = window.cmsInitialContents || {};
window.cmsSunEditorCores = {}; // Store SunEditor core instances
window.cmsToggleButtonMap = {}; // Map toggle buttons to editor IDs

const CMS_PLACEHOLDER_ERROR_CLASS = "cms-placeholder-error";
const CMS_SAVE_ERROR_CONTAINER_ID = "content-form:cms-error-container";
const FULL_TOOLBAR = [
  ["font", "fontSize", "formatBlock"],
  ["paragraphStyle", "blockquote"],
  ["bold", "underline", "italic", "strike", "subscript", "superscript"],
  ["fontColor", "hiliteColor", "textStyle"],
  ["removeFormat"],
  ["outdent", "indent"],
  ["align", "list", "lineHeight", "horizontalRule"],
  ["table", "link"],
  ["fullScreen"],
  ["undo", "redo"],
  ["toggleView"],
];

const toggleViewModePlugin = {
  name: "toggleView",
  display: "command",
  title: "Switch to text mode",
  innerHTML: '<i class="fa-solid fa-spell-check"></i>',

  add: function (core) {
    const context = core.context;
    context.toggleView = { isTextMode: false };

    let button = document.createElement("button");
    button.className = "se-btn";
    button.title = this.title;
    button.innerHTML = this.innerHTML;
    // debugger;
    // Get the editor element
    const editorId = core.context.element.topArea.id;

    // Create a unique ID for this button
    const buttonId = "toggle-btn-" + editorId;
    button.id = buttonId;
    button.type = "button";

    // Store the mapping: button ID -> editor ID
    window.cmsToggleButtonMap[buttonId] = editorId;

    // Also set the data attribute for inspection
    button.dataset.editorId = editorId;
    button.setAttribute("data-editor-id", editorId);
    button.setAttribute("aria-label", "Toggle editor for " + editorId);

    console.log("Toggle button initialized:", {
      buttonId: buttonId,
      editorId: editorId,
      buttonElement: button.id,
    });

    // Use capture phase to intercept click before SunEditor processes it
    button.addEventListener(
      "click",
      function (e) {
        e.preventDefault();
        e.stopPropagation();

        console.log("Toggle button clicked - buttonId:", buttonId, "editorId:", editorId);

        // Find the SunEditor wrapper - it's the parent SPAN with class 'ui-inputwrapper-filled'
        let container = editorElement.parentElement;
        while (container && !container.classList.contains("ui-inputwrapper-filled")) {
          container = container.parentElement;
        }

        if (container) {
          console.log("Successfully hiding SunEditor container");
          container.style.display = "none";
        } else {
          console.log("Could not find SunEditor wrapper, hiding textarea");
          editorElement.style.display = "none";
        }

        return false;
      },
      true,
    ); // Use capture phase

    return button;
  },

  action: function () {
    // This method is called by SunEditor but we handle clicks in the button's event listener
    console.log(this.context.element.topArea.id);
  },
};

function initSunEditor(languageIndex, editorId) {
  const textarea = document.getElementById(editorId);
  if (!textarea) {
    return;
  }
  const editor = SUNEDITOR.create(textarea, {
    buttonList: FULL_TOOLBAR,
    plugins: [toggleViewModePlugin],
    attributesWhitelist: {
      all: "style|class|width|height|role|border|cellspacing|cellpadding|src|alt|href|target",
    },
  });
  window.cmsLiveEditors[languageIndex] = editor;
  window.cmsLiveEditorIds[languageIndex] = editorId;

  // Register the editor core with its ID for the toggle button
  window.cmsSunEditorCores[editorId] = editor.core;
  console.log("Editor registered - ID:", editorId, "- ready for toggle button");

  // Store original content and placeholder pattern for later comparison
  try {
    const initialContents = editor.getContents();
    window.cmsInitialContents[languageIndex] = initialContents;
    window.cmsOriginalPlaceholders[languageIndex] = extractPlaceholders(initialContents).sort();
  } catch (e) {
    window.cmsInitialContents[languageIndex] = "";
    window.cmsOriginalPlaceholders[languageIndex] = [];
  }

  function markDirtyIfChanged() {
    const currentContent = editor.getContents();
    const originalContents = window.cmsInitialContents[languageIndex] || "";

    if (currentContent === originalContents) {
      // Back to original -> not dirty anymore
      window.cmsDirtyEditors.delete(languageIndex);
      setEditorError(languageIndex, false);
    } else {
      window.cmsDirtyEditors.add(languageIndex);
      setValueChanged([
        { name: "languageIndex", value: languageIndex },
        { name: "content", value: currentContent },
      ]);
    }
  }

  function debounce(fn, delay) {
    let timer;
    return function (...args) {
      clearTimeout(timer);
      timer = setTimeout(() => fn.apply(this, args), delay);
    };
  }

  // Handle fast typing
  editor.onChange = debounce(() => {
    markDirtyIfChanged();
  }, 200);

  // Handle quick CMS switching (click outside editor)
  editor.onBlur = () => {
    markDirtyIfChanged();
  };
}

function saveAllEditors() {
  const dirtyEditors = new Set(window.cmsDirtyEditors);
  if (dirtyEditors.size === 0) {
    return true;
  }

  const editorKeys = Object.keys(window.cmsLiveEditors || {});
  const allLocalesEdited =
    editorKeys.length > 0 && dirtyEditors.size === editorKeys.length;
  const values = [];
  let placeholderError = false;
  let hasAnyError = false;
  let expectedPlaceholders = null;

  for (const languageIndex of dirtyEditors) {
    const editor = window.cmsLiveEditors[languageIndex];
    const contents = editor.getContents();

    if (!validateNotEmpty(editor, languageIndex)) {
      hasAnyError = true;
      continue;
    }

    const validationResult = validatePlaceholders({
      languageIndex,
      contents,
      allLocalesEdited,
      expectedPlaceholders
    });

    if (!validationResult.valid) {
      hasAnyError = true;
      placeholderError = true;
      setEditorError(languageIndex, true);
      continue;
    }

    expectedPlaceholders = validationResult.expectedPlaceholders;
    setEditorError(languageIndex, false);

    values.push({
      languageIndex: Number(languageIndex),
      contents: contents
    });
  }

  if (hasAnyError) {
    setErrorMessageVisible(placeholderError);
    return false;
  }

  setErrorMessageVisible(false);
  destroyEditors();
  saveAllValue([{
    name: 'values',
    value: JSON.stringify(values)
  }]);

  return true;
}

function validateNotEmpty(editor, languageIndex) {
  const text = removeNonPrintableChars(editor.getText()).trim();

  if (text.length === 0) {
    editor.noticeOpen("The content must not be empty.");
    setEditorError(languageIndex, true);
    return false;
  }

  return true;
}

/** Placeholder validation:
* - If all locales are edited → ensure placeholders are consistent across locales.
* - If only some locales edited → ensure placeholder numbers match the original of this locale.
*/
function validatePlaceholders({languageIndex, contents, allLocalesEdited, expectedPlaceholders}) {
  const newPlaceholders = extractPlaceholders(contents).sort();

  if (allLocalesEdited) {
    if (expectedPlaceholders === null) {
      return {
        valid: true,
        expectedPlaceholders: newPlaceholders
      };
    }

    return {
      valid: arePlaceholderListsEqual(expectedPlaceholders, newPlaceholders),
      expectedPlaceholders
    };
  }

  const originalPlaceholders =
    window.cmsOriginalPlaceholders[languageIndex] || [];

  return {
    valid: arePlaceholderListsEqual(originalPlaceholders, newPlaceholders),
    expectedPlaceholders
  };
}

function setEditorError(languageIndex, hasError) {
  const container = getEditorContainer(languageIndex);
  if (!container) {
    return;
  }

  container.classList.toggle('cms-editor-error', hasError);
}

function getEditorContainer(languageIndex) {
  const editorId = window.cmsLiveEditorIds[languageIndex];
  if (!editorId) {
    return null;
  }

  const textarea = document.getElementById(editorId);
  if (!textarea) {
    return null;
  }

  // SunEditor creates .sun-editor next to textarea
  return (
    textarea.nextElementSibling?.classList.contains('sun-editor')
      ? textarea.nextElementSibling
      : textarea.parentElement?.querySelector('.sun-editor')
  ) || null;
}

/** Extracts numbered placeholders from the editing content.
* A placeholder is defined as format {number}, e.g. {0}, {1}
*/
function extractPlaceholders(content) {
  if (!content) {
    return [];
  }
  const matches = content.match(/\{\d+\}/g);
  return matches ? matches.slice() : [];
}

/** Compares two placeholder lists for exact equality.
* The lists must:
* - Have the same length
* - Contain the same elements
*/
function arePlaceholderListsEqual(a, b) {
  if (a.length !== b.length) {
    return false;
  }
  for (let i = 0; i < a.length; i++) {
    if (a[i] !== b[i]) {
      return false;
    }
  }
  return true;
}

function removeNonPrintableChars(str) {
  return str.replace(/[\u00A0\u0000\u200B]/g, '');
}

function bindCmsWarning(hoverId, warningId) {
  const hoverElement = document.getElementById(hoverId);
  const targetElement = document.getElementById(warningId);
  if (!hoverElement || !targetElement) return;

  let hideTimeout;

  function showWarning() {
    clearTimeout(hideTimeout);
    targetElement.style.display = "block";
  }

  function hideWarning() {
    if (targetElement.dataset && targetElement.dataset.forceVisible === 'true') {
      return;
    }
    hideTimeout = setTimeout(function() {
      targetElement.style.display = "none";
    }, 500);
  }

  hoverElement.addEventListener("mouseenter", showWarning);
  hoverElement.addEventListener("mouseleave", hideWarning);
  targetElement.addEventListener("mouseenter", function() {
    clearTimeout(hideTimeout);
  });
  targetElement.addEventListener("mouseleave", hideWarning);
}

function setErrorMessageVisible(isVisible) {
  const element = document.getElementById(CMS_SAVE_ERROR_CONTAINER_ID);
  if (!element) {
    return;
  }
  element.dataset.forceVisible = isVisible ? 'true' : 'false';
  element.style.display = isVisible ? 'block' : 'none';
}

function initCmsWarnings() {
  bindCmsWarning("content-form:download-button", "content-form:cms-warning-container");
  bindCmsWarning("content-form:save-button", "content-form:cms-warning-save-container");
}

function showDialog(dialogId) {
  PF(dialogId).show();
  setTimeout(function() {
    PF(dialogId).hide();
  }, 1500);
}

function destroyEditors() {
  for (const key in window.cmsLiveEditors) {
    try {
      window.cmsLiveEditors[key].destroy();
    } catch (e) {}
  }
  window.cmsLiveEditors = {};
  window.cmsDirtyEditors.clear();
}

function showSaveSuccess() {
  const bar = document.getElementById('content-form:save-success-bar');
  if (!bar) {
    return;
  };
  bar.classList.add('show');
  if (bar.hideTimeout) {
    clearTimeout(bar.hideTimeout);
  }
  bar.hideTimeout = setTimeout(() => {
    bar.classList.remove('show');
  }, 3500);
}

let pathPanelScrollTop = 0;

function getPathPanel() {
  return document.querySelector(
    '#content-form\\:path-column .panel'
  );
}

function savePathPanelScroll() {
  const panel = getPathPanel();
  if (panel) {
    pathPanelScrollTop = panel.scrollTop;
  }
}

function restorePathPanelScroll() {
  const panel = getPathPanel();
  if (panel) {
    setTimeout(() => {
      panel.scrollTop = pathPanelScrollTop;
    }, 0);
  }
}

function handleTabClose(panel) {
  let messageElements = panel[0].getElementsByClassName("ui-messages-error ui-corner-all");
  if (messageElements && messageElements.length > 0) {
    messageElements[0].style.display = "none";
  }
}

document.addEventListener("DOMContentLoaded", initCmsWarnings);