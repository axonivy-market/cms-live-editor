window.cmsEditors = window.cmsEditors || {};
window.cmsDirtyEditors = new Set();
window.cmsOriginalPlaceholders = window.cmsOriginalPlaceholders || {};
window.cmsEditorIds = window.cmsEditorIds || {};
window.cmsInitialContents = window.cmsInitialContents || {};

const CMS_PLACEHOLDER_ERROR_CLASS = 'cms-placeholder-error';
const CMS_SAVE_WARNING_CONTAINER_ID = 'content-form:cms-error-container';

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
      all: 'style|class|width|height|role|border|cellspacing|cellpadding|src|alt|href|target'
    }
  });
  const initialContent = editor.getContents();
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

function markDirtyIfChanged() {
  const currentContent = editor.getContents();
  const originalContents = window.cmsInitialContents[languageIndex] || '';

  if (currentContent === originalContents) {
    // Back to original -> not dirty anymore
    window.cmsDirtyEditors.delete(languageIndex);
    setEditorError(languageIndex, false);
    }

  else if (currentContent !== initialContent) {
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

function saveAllEditors() {
  const values = [];
  let hasValidationError = false;
  let hasPlaceholderError = false;
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

    // Placeholder validation: ensure same set as original in this editor
    const originalPlaceholders = window.cmsOriginalPlaceholders[languageIndex] || [];
    const newPlaceholders = extractPlaceholders(contents).sort();
    if (!arePlaceholderListsEqual(originalPlaceholders, newPlaceholders)) {
      // editor.noticeOpen("Placeholder mismatch: please keep the same {n} placeholders.");
      setEditorError(languageIndex, true);
      hasValidationError = true;
      hasPlaceholderError = true;
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
    // Show the same warning message area as hover warning, but immediately.
    setSaveWarningVisible(hasPlaceholderError);
    return false;
  }

  // Hide placeholder warning when validations pass.
  setSaveWarningVisible(false);

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

function stripPlaceholderHighlightSpans(html) {
  if (!html || typeof html !== 'string') {
    return html;
  }

  // Remove our visual highlight wrappers while keeping inner text intact.
  const wrapperPattern = new RegExp(
    `<span\\b[^>]*class\\s*=\\s*"[^\"]*\\b${CMS_PLACEHOLDER_ERROR_CLASS}\\b[^\"]*"[^>]*>([\\s\\S]*?)<\\/span>`,
    'gi'
  );

  let cleaned = html;
  while (wrapperPattern.test(cleaned)) {
    cleaned = cleaned.replace(wrapperPattern, '$1');
  }
  return cleaned;
}

function buildPlaceholderCounts(placeholders) {
  const counts = new Map();
  for (const p of placeholders || []) {
    counts.set(p, (counts.get(p) || 0) + 1);
  }
  return counts;
}

function wrapExtraPlaceholdersInTextNodes(html, allowedCounts, wrapperClass) {
  if (!html) {
    return html;
  }

  const container = document.createElement('div');
  container.innerHTML = html;

  const countsSeen = new Map();
  const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT);
  const nodesToProcess = [];
  let node;
  while ((node = walker.nextNode())) {
    if (node.nodeValue && /\{\d+\}/.test(node.nodeValue)) {
      nodesToProcess.push(node);
    }
  }

  for (const textNode of nodesToProcess) {
    const text = textNode.nodeValue;
    const regex = /\{\d+\}/g;
    let lastIndex = 0;
    let match;
    let changed = false;
    const fragment = document.createDocumentFragment();

    while ((match = regex.exec(text)) !== null) {
      const token = match[0];
      const start = match.index;

      if (start > lastIndex) {
        fragment.appendChild(document.createTextNode(text.slice(lastIndex, start)));
      }

      const seen = (countsSeen.get(token) || 0) + 1;
      countsSeen.set(token, seen);
      const allowed = allowedCounts.get(token) || 0;

      if (seen > allowed) {
        const span = document.createElement('span');
        span.className = wrapperClass;
        span.textContent = token;
        fragment.appendChild(span);
        changed = true;
      } else {
        fragment.appendChild(document.createTextNode(token));
      }

      lastIndex = start + token.length;
    }

    if (lastIndex < text.length) {
      fragment.appendChild(document.createTextNode(text.slice(lastIndex)));
    }

    if (changed) {
      textNode.parentNode.replaceChild(fragment, textNode);
    }
  }

  return container.innerHTML;
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

function setSaveWarningVisible(visible) {
  const el = document.getElementById(CMS_SAVE_WARNING_CONTAINER_ID);
  if (!el) {
    return;
  }
  el.dataset.forceVisible = visible ? 'true' : 'false';
  el.style.display = visible ? 'block' : 'none';
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

let linkPanelScrollTop = 0;

function getLinkPanel() {
  return document.querySelector(
    '#content-form\\:link-column .panel'
  );
}

function saveLinkPanelScroll() {
  const panel = getLinkPanel();
  if (panel) {
    linkPanelScrollTop = panel.scrollTop;
  }
}

function restoreLinkPanelScroll() {
  const panel = getLinkPanel();
  if (panel) {
    setTimeout(() => {
      panel.scrollTop = linkPanelScrollTop;
    }, 0);
  }
}

document.addEventListener("DOMContentLoaded", initCmsWarnings);