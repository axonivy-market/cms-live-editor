window.cmsLiveEditors = window.cmsLiveEditors || {};
window.cmsDirtyEditors = new Set();
window.cmsOriginalPlaceholders = window.cmsOriginalPlaceholders || {};
window.cmsLiveEditorIds = window.cmsLiveEditorIds || {};
window.cmsInitialContents = window.cmsInitialContents || {};
window.cmsValidationFailed = window.cmsValidationFailed || false;

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

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text ?? '';
  return div.innerHTML;
}

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

  if (!isHtml) {
    const rawText = textarea.value || '';
    const escapedText = escapeHtml(rawText).replace(/\r\n|\r|\n/g, '<br>');
    editor.setContents(`<p>${escapedText}</p>`);
  }

  const initialContent = editor.getContents();
  restrictActionForNonHtml(isHtml, editor);
  window.cmsLiveEditors[languageIndex] = editor;
  window.cmsLiveEditorIds[languageIndex] = editorId;

  // Check if this editor's textarea was marked invalid by JSF validation
  setEditorError(languageIndex, textarea.classList.contains('ui-state-error'));

  // Store original content and placeholder pattern for later comparison
  try {
    const initialContents = initialContent;
    window.cmsInitialContents[languageIndex] = initialContents;
    window.cmsOriginalPlaceholders[languageIndex] = extractPlaceholders(initialContents);
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

function getInvalidLocaleIndices() {
  const indices = [];
  for (const languageIndex in window.cmsLiveEditorIds) {
    const editorId = window.cmsLiveEditorIds[languageIndex];
    const textarea = document.getElementById(editorId);
    if (textarea && textarea.classList.contains('ui-state-error')) {
      indices.push(Number(languageIndex));
    }
  }
  return indices;
}

function setTabHeaderError(languageIndex, hasError) {
  const editorId = window.cmsLiveEditorIds[languageIndex];
  if (!editorId) return;

  const textarea = document.getElementById(editorId);
  if (!textarea) return;

  const contentPanel = textarea.closest('.ui-accordion-content');
  if (!contentPanel) return;

  const header = contentPanel.previousElementSibling;
  if (header && header.classList.contains('ui-accordion-header')) {
    header.classList.toggle('cms-editor-error', hasError);
  }
}

function applyValidationFailedState(failedIndices) {
  failedIndices = failedIndices || [];

  for (const languageIndex in window.cmsLiveEditorIds) {
    const idx = Number(languageIndex);
    const hasError = failedIndices.includes(idx);
    setEditorError(idx, hasError);
    setTabHeaderError(idx, hasError);
  }
}

function clearValidationFailedState() {
  window.cmsValidationFailed = false;
  for (const languageIndex in window.cmsLiveEditorIds) {
    setEditorError(Number(languageIndex), false);
    setTabHeaderError(Number(languageIndex), false);
  }

  document.querySelectorAll('.sun-editor.cms-editor-error').forEach((element) => {
    element.classList.remove('cms-editor-error');
  });

  const accordion = document.getElementById('content-form:cms-edit-value');
  if (accordion) {
    accordion.querySelectorAll('.ui-accordion-header.cms-editor-error').forEach((header) => {
      header.classList.remove('cms-editor-error');
    });
  }
}

function restrictActionForNonHtml(isHtmlContent, editor) {
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
  // Reset validation flag for a new save attempt.
  window.cmsValidationFailed = false;

  const dirtyEditors = new Set(window.cmsDirtyEditors);

  if (dirtyEditors.size === 0) {
    return true;
  }

  const values = [];

  for (const languageIndex of dirtyEditors) {
    const editor = window.cmsLiveEditors[languageIndex];
    const contents = editor.getContents();

    values.push({
      languageIndex: Number(languageIndex),
      contents: contents
    });

    // reset error state before backend validation
    setEditorError(languageIndex, false);
  }

  saveAllValue([{
    name: 'values',
    value: JSON.stringify(values)
  }]);

  return true;
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

function updateEditorContent(xhr, status, args) {
  if (!args) return;

  const { langIndex, newContent } = args;
  const editor = window.cmsLiveEditors[langIndex];

  if (editor) {
    editor.setContents(newContent);

    // update dirty state tracking
    window.cmsInitialContents[langIndex] = newContent;
    window.cmsDirtyEditors.delete(langIndex);
  }
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

function handleCmsSaveComplete(args) {
  const validationFailed = Boolean(args?.validationFailed);

  if (!validationFailed) {
	destroyEditors();
    window.cmsValidationFailed = false;
    initCmsWarnings();
    restorePathPanelScroll();
    showSaveSuccess();
    return;
  }

  window.cmsValidationFailed = true;
  var invalidIndices = [];
  try {
    invalidIndices = JSON.parse(args.invalidIndices || '[]');
  } catch (e) {}
  applyValidationFailedState(invalidIndices);
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