window.cmsLiveEditors = window.cmsLiveEditors || {};
window.cmsDirtyEditors = new Set();
window.cmsOriginalPlaceholders = window.cmsOriginalPlaceholders || {};
window.cmsLiveEditorIds = window.cmsLiveEditorIds || {};
window.cmsInitialContents = window.cmsInitialContents || {};

const CMS_PLACEHOLDER_ERROR_CLASS = 'cms-placeholder-error';
const CMS_SAVE_ERROR_CONTAINER_ID = 'content-form:cms-error-container';
const ENTER_KEY = 'Enter';
const ENTER_KEY_CODE = 13;
const CTRL_KEY_COPY = 'c';
const CTRL_KEY_PASTE = 'v';
const CTRL_KEY_CUT = 'x';
const CTRL_KEY_ALL = 'a';
const CTRL_KEY_UNDO = 'z';
const NON_HTML_ALLOWED_CTRL_KEYS = new Set([CTRL_KEY_COPY, CTRL_KEY_PASTE, CTRL_KEY_CUT, CTRL_KEY_ALL, CTRL_KEY_UNDO]);

const FULL_TOOLBAR = [
  ['font', 'fontSize', 'formatBlock'],
  ['paragraphStyle', 'blockquote'],
  ['bold', 'underline', 'italic', 'strike', 'subscript', 'superscript'],
  ['fontColor', 'hiliteColor', 'textStyle'],
  ['removeFormat'],
  ['outdent', 'indent'],
  ['align', 'list', 'lineHeight', 'horizontalRule'],
  ['table', 'link'],
  ['fullScreen'],
  ['undo', 'redo'],
];

function initSunEditor(languageIndex, editorId, isHtml) {
  const textarea = document.getElementById(editorId);
  if (!textarea) {
    return;
  }
  const editor = SUNEDITOR.create(textarea, {
    buttonList: isHtml ? FULL_TOOLBAR : [],
    attributesWhitelist: {
      all: 'style|class|width|height|role|border|cellspacing|cellpadding|src|alt|href|target',
    },
    defaultStyle: 'font-family: Inter;',
    font: ['Inter', 'Arial', 'Tahoma', 'Courier New', 'Times New Roman', 'Verdana', 'Georgia', 'Trebuchet MS', 'Impact', 'Comic Sans MS'],
  });
  restricActionForNonHtml(isHtml, editor);
  window.cmsLiveEditors[languageIndex] = editor;
  window.cmsLiveEditorIds[languageIndex] = editorId;

  // Store original content and placeholder pattern for later comparison
  try {
    const initialContents = editor.getContents();
    window.cmsInitialContents[languageIndex] = initialContents;
    window.cmsOriginalPlaceholders[languageIndex] = extractPlaceholders(initialContents).sort();
  } catch (e) {
    window.cmsInitialContents[languageIndex] = '';
    window.cmsOriginalPlaceholders[languageIndex] = [];
  }

function markDirtyIfChanged() {
  const currentContent = editor.getContents();
  const originalContents = window.cmsInitialContents[languageIndex] || '';
  if (currentContent === originalContents) {
    // Back to original -> not dirty anymore
    window.cmsDirtyEditors.delete(languageIndex);
    setEditorError(languageIndex, false);
  } else {
    window.cmsDirtyEditors.add(languageIndex);
    setValueChanged([
      { name: 'languageIndex', value: languageIndex },
      { name: 'content', value: currentContent }
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

function restricActionForNonHtml(isHtmlContent, editor) {
  if (isHtmlContent) {
    return;
  }
  editor.onCommand = function () {
    return false;
  };

  editor.onKeyDown = function (e) {
    const key = (e.key || '').toLowerCase();
    const isNotAllowedCtrlKey = (e.ctrlKey || e.metaKey) && !NON_HTML_ALLOWED_CTRL_KEYS.has(key);
    const isEnterKey = key === ENTER_KEY || e.keyCode === ENTER_KEY_CODE;
    if (isNotAllowedCtrlKey || isEnterKey) {
      e.preventDefault();
      return false;
    }
  };

  editor.onPaste = function (e, cleanData) {
    return cleanData;
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