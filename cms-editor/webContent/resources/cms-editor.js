window.cmsEditors = window.cmsEditors || {};
window.cmsDirtyEditors = new Set();

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

  function markDirty() {
    window.cmsDirtyEditors.add(languageIndex);
    setValueChanged([
      { name: 'languageIndex', value: languageIndex }, {name: 'content', value: editor.getContents()}
    ]);
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
  for (const languageIndex of window.cmsDirtyEditors) {
    const editor = window.cmsEditors[languageIndex];

    const contents = editor.getContents();
    const text = removeNonPrintableChars(editor.getText()).trim();
    if (text.length === 0) {
      editor.noticeOpen("The content must not be empty.");
      return;
    }

    values.push({
      languageIndex: Number(languageIndex),
      contents: contents
    });
  }

  saveAllValue([{
    name: 'values',
    value: JSON.stringify(values)
  }]);
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

document.addEventListener("DOMContentLoaded", initCmsWarnings);