window.cmsEditors = window.cmsEditors || {};
window.cmsDirtyEditors = new Set();
window.cmsOriginalPlaceholders = window.cmsOriginalPlaceholders || {};
window.cmsEditorIds = window.cmsEditorIds || {};
window.cmsInitialContents = window.cmsInitialContents || {};

function initSunEditor(isFormatButtonListVisible, languageIndex, editorId) {
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

  const editor = SUNEDITOR.create(document.getElementById(editorId), {
    buttonList: buttonList,
    attributesWhitelist: {
      all: 'style|width|height|role|border|cellspacing|cellpadding|src|alt|href|target'
    }
  });

  window.cmsEditors[languageIndex] = editor;
  window.cmsEditorIds[languageIndex] = editorId;

  // Store original content and placeholder pattern for later comparison
  try {
    const initialContents = editor.getContents();
    window.cmsInitialContents[languageIndex] = initialContents;
    window.cmsOriginalPlaceholders[languageIndex] = extractPlaceholders(initialContents).sort();
  } catch (e) {
    window.cmsInitialContents[languageIndex] = '';
    window.cmsOriginalPlaceholders[languageIndex] = [];
  }

  function markDirty() {
    const currentContents = editor.getContents();
    const originalContents = window.cmsInitialContents[languageIndex] || '';

    if (currentContents === originalContents) {
      // Back to original -> not dirty anymore
      window.cmsDirtyEditors.delete(languageIndex);
      setEditorError(languageIndex, false);
    } else {
      // Content changed compared to original
      window.cmsDirtyEditors.add(languageIndex);
      setValueChanged([
        { name: 'languageIndex', value: languageIndex }
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
    markDirty();
  }, 200);

  // Handle quick CMS switching (click outside editor)
  editor.onBlur = () => {
    markDirty();
  };
}

function saveAllEditors() {
  const values = [];
  let hasValidationError = false;
  // Validate and collect values only for locales that the user modified
  for (const languageIndex of window.cmsDirtyEditors) {
    const editor = window.cmsEditors[languageIndex];

    const contents = editor.getContents();
    const text = removeNonPrintableChars(editor.getText()).trim();
    if (text.length === 0) {
      editor.noticeOpen("The content must not be empty.");
      setEditorError(languageIndex, true);
      hasValidationError = true;
      continue;
    }

    // HTML syntax validation based only on new content
    if (!isHtmlSyntaxValid(contents)) {
      // editor.noticeOpen("Invalid HTML syntax: please check opening and closing tags.");
      setEditorError(languageIndex, true);
      hasValidationError = true;
      continue;
    }

    // Placeholder validation: ensure same set as original in this editor
    const originalPlaceholders = window.cmsOriginalPlaceholders[languageIndex] || [];
    const newPlaceholders = extractPlaceholders(contents).sort();
    if (!arePlaceholderListsEqual(originalPlaceholders, newPlaceholders)) {
      // editor.noticeOpen("Placeholder mismatch: please keep the same {n} placeholders.");
      setEditorError(languageIndex, true);
      hasValidationError = true;
      continue;
    }

    // Clear error highlight when all validations for this editor pass
    setEditorError(languageIndex, false);

    values.push({
      languageIndex: Number(languageIndex),
      contents: contents
    });
  }

  if (hasValidationError) {
    return false;
  }

  saveAllValue([{
    name: 'values',
    value: JSON.stringify(values)
  }]);

  return true;
}

function setEditorError(languageIndex, hasError) {
  const editorId = window.cmsEditorIds[languageIndex];
  if (!editorId) {
    return;
  }

  const textarea = document.getElementById(editorId);
  if (!textarea) {
    return;
  }
  // suneditor creates a sibling .sun-editor element next to the textarea
  let container = textarea.nextElementSibling;
  if (!container || !container.classList.contains('sun-editor')) {
    container = textarea.parentElement && textarea.parentElement.querySelector('.sun-editor');
  }
  if (!container) {
    return;
  }

  if (hasError) {
    container.classList.add('cms-editor-error');
  } else {
    container.classList.remove('cms-editor-error');
  }
}

function extractPlaceholders(content) {
  if (!content) {
    return [];
  }
  const matches = content.match(/\{\d+\}/g);
  return matches ? matches.slice() : [];
}

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

function isHtmlSyntaxValid(content) {
  if (!content || !/[<>]/.test(content)) {
    return true;
  }

  const tagPattern = /<\/?([a-zA-Z0-9]+)([^>]*)>/g;
  const voidTags = [
    "br", "hr", "img", "input", "meta", "link", "area", "base", "col", "embed", "param", "source",
    "track", "wbr"
  ];

  const stack = [];
  let match;

  while ((match = tagPattern.exec(content)) !== null) {
    const tagName = match[1] ? match[1].toLowerCase() : "";
    const rest = match[2] || "";
    const fullMatch = match[0];

    // Skip comments/doctype
    if (tagName.startsWith("!") || tagName.startsWith("?")) {
      continue;
    }

    const isClosing = /^<\//.test(fullMatch);
    const selfClosing = /\/$/.test(rest.trim());

    if (selfClosing || voidTags.indexOf(tagName) !== -1) {
      continue;
    }

    if (isClosing) {
      if (stack.length === 0) {
        return false;
      }
      const open = stack.pop();
      if (open !== tagName) {
        return false;
      }
    } else {
      stack.push(tagName);
    }
  }

  return stack.length === 0;
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
  for (const key in window.cmsEditors) {
    try {
      window.cmsEditors[key].destroy();
    } catch (e) {}
  }
  window.cmsEditors = {};
  window.cmsDirtyEditors.clear();
}

document.addEventListener("DOMContentLoaded", initCmsWarnings);