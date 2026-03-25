window.cmsLiveEditors = window.cmsLiveEditors || {};
window.cmsDirtyEditors = new Set();
window.cmsOriginalPlaceholders = window.cmsOriginalPlaceholders || {};
window.cmsLiveEditorIds = window.cmsLiveEditorIds || {};
window.cmsInitialContents = window.cmsInitialContents || {};
window.cmsValidationFailed = window.cmsValidationFailed || false;
window.cmsInvalidLocaleIndices = window.cmsInvalidLocaleIndices || [];

const CMS_INVALID_LOCALE_INDICES_ID = 'content-form:invalid-locale-indices';

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text ?? '';
  return div.innerHTML;
}

function initSunEditor(isFormatButtonListVisible, languageIndex, editorId) {
  const textarea = document.getElementById(editorId);
  if (!textarea) {
    return;
  }

  let buttonList;
  if (isFormatButtonListVisible) {
    buttonList = [
      ['font', 'fontSize', 'formatBlock'],
      ['paragraphStyle', 'blockquote'],
      ['bold', 'underline', 'italic', 'strike', 'subscript', 'superscript'],
      ['fontColor', 'hiliteColor', 'textStyle'],
      ['removeFormat'],
      ['outdent', 'indent'],
      ['align', 'list', 'lineHeight', 'horizontalRule'],
      ['table', 'link'],
      ['fullScreen'],
      ['undo', 'redo']
    ];
  } else {
    buttonList = [
      ['font'],
      ['bold', 'underline', 'italic'],
      ['fontColor', 'align', 'list'],
      ['fullScreen']
    ];
  }

  const editor = SUNEDITOR.create(textarea, {
    buttonList,
    attributesWhitelist: {
      all: 'style|class|width|height|role|border|cellspacing|cellpadding|src|alt|href|target'
    }
  });

  // In plain-text mode, make sure the initial value is treated as text.
  // Otherwise patterns like ChoiceFormat '1<...' can be parsed as an HTML tag and the rest disappears.
  if (!isFormatButtonListVisible) {
    const rawText = textarea.value || '';
    const escapedText = escapeHtml(rawText).replace(/\r\n|\r|\n/g, '<br>');
    editor.setContents(`<p>${escapedText}</p>`);
  }

  const initialContent = editor.getContents();
  window.cmsLiveEditors[languageIndex] = editor;
  window.cmsLiveEditorIds[languageIndex] = editorId;

  const failedIndices = window.cmsInvalidLocaleIndices || [];
  setEditorError(languageIndex, failedIndices.includes(Number(languageIndex)));

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
  const invalidLocaleIndicesElement = document.getElementById(CMS_INVALID_LOCALE_INDICES_ID);
  if (!invalidLocaleIndicesElement) {
    return [];
  }

  try {
    const parsed = JSON.parse(invalidLocaleIndicesElement.textContent || '[]');
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed.map(Number).filter(Number.isFinite);
  } catch (e) {
    return [];
  }
}

function applyValidationFailedState() {
  const failedIndices = getInvalidLocaleIndices();
  window.cmsInvalidLocaleIndices = failedIndices;

  const maxAttempts = 10;
  let attempts = 0;

  function tryApply() {
    attempts += 1;

    // Clear all first, then set errors only for failed locales.
    for (const languageIndex in window.cmsLiveEditorIds) {
      setEditorError(Number(languageIndex), failedIndices.includes(Number(languageIndex)));
    }

    // Stop retrying once at least one failed editor is styled, or we reached the limit.
    if ((failedIndices.length === 0 || document.querySelector('.sun-editor.cms-editor-error')) || attempts >= maxAttempts) {
      return;
    }

    setTimeout(tryApply, 100);
  }

  setTimeout(tryApply, 0);
}

function clearValidationFailedState() {
  window.cmsValidationFailed = false;
  window.cmsInvalidLocaleIndices = [];
  for (const languageIndex in window.cmsLiveEditorIds) {
    setEditorError(Number(languageIndex), false);
  }

  document.querySelectorAll('.sun-editor.cms-editor-error').forEach((element) => {
    element.classList.remove('cms-editor-error');
  });
}

function saveAllEditors() {
  // Reset validation flag for a new save attempt.
  window.cmsValidationFailed = false;
  window.cmsInvalidLocaleIndices = [];

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

  // Send all data to backend
  destroyEditors();

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