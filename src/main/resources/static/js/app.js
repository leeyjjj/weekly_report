/* ======================== */
/* Toast Notification       */
/* ======================== */
var Toast = {
    container: null,
    init: function() {
        this.container = document.getElementById('toastContainer');
    },
    show: function(message, type, duration) {
        if (!this.container) this.init();
        if (!this.container) return;
        type = type || 'success';
        duration = duration || 3000;
        var toast = document.createElement('div');
        toast.className = 'toast toast-' + type;
        var icon = type === 'success' ? '\u2713' : type === 'error' ? '\u2717' : '\u2139';
        toast.innerHTML = '<span class="toast-icon">' + icon + '</span>' +
                          '<span class="toast-msg">' + message + '</span>' +
                          '<button class="toast-close" onclick="this.parentElement.remove()">&times;</button>';
        this.container.appendChild(toast);
        setTimeout(function() { toast.classList.add('toast-show'); }, 10);
        setTimeout(function() {
            toast.classList.remove('toast-show');
            setTimeout(function() { toast.remove(); }, 300);
        }, duration);
    }
};

/* ======================== */
/* Confirm Modal            */
/* ======================== */
var Modal = {
    overlay: null,
    pendingFormId: null,
    init: function() {
        this.overlay = document.getElementById('confirmModal');
    },
    open: function(formId, title, message) {
        if (!this.overlay) this.init();
        if (!this.overlay) return;
        this.pendingFormId = formId;
        document.getElementById('modalTitle').textContent = title || '삭제 확인';
        document.getElementById('modalMessage').textContent = message || '정말 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.';
        this.overlay.classList.remove('hidden');
    },
    confirm: function() {
        if (this.pendingFormId) {
            document.getElementById(this.pendingFormId).submit();
        }
        this.close();
    },
    close: function() {
        if (this.overlay) this.overlay.classList.add('hidden');
        this.pendingFormId = null;
    }
};

/* ======================== */
/* Keyboard Shortcuts       */
/* ======================== */
document.addEventListener('keydown', function(e) {
    // Ctrl+Enter or Cmd+Enter to submit form
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        var active = document.activeElement;
        if (active && active.form) {
            e.preventDefault();
            active.form.requestSubmit();
        }
    }
    // Escape to close modal
    if (e.key === 'Escape') {
        Modal.close();
    }
});

/* ======================== */
/* Character Counter        */
/* ======================== */
function initCharCounter(textareaId, counterId) {
    var ta = document.getElementById(textareaId);
    var counter = document.getElementById(counterId);
    if (!ta || !counter) return;
    function update() {
        var len = ta.value.length;
        counter.textContent = len.toLocaleString() + '자';
    }
    ta.addEventListener('input', update);
    update();
}

/* ======================== */
/* Clipboard Copy           */
/* ======================== */
function copyToClipboard(text, successMsg) {
    navigator.clipboard.writeText(text).then(function() {
        Toast.show(successMsg || '클립보드에 복사되었습니다', 'success');
    }).catch(function() {
        Toast.show('복사에 실패했습니다', 'error');
    });
}

function copyHtmlToClipboard(html, successMsg) {
    var blob = new Blob([html], { type: 'text/html' });
    var item = new ClipboardItem({ 'text/html': blob });
    navigator.clipboard.write([item]).then(function() {
        Toast.show(successMsg || 'HTML로 클립보드에 복사되었습니다', 'success');
    }).catch(function() {
        Toast.show('복사에 실패했습니다', 'error');
    });
}

/* ======================== */
/* Dark Mode                */
/* ======================== */
function toggleTheme() {
    var html = document.documentElement;
    var current = html.getAttribute('data-theme');
    var next = current === 'dark' ? 'light' : 'dark';
    html.setAttribute('data-theme', next);
    localStorage.setItem('theme', next);
    updateThemeIcon(next);
}

function updateThemeIcon(theme) {
    var sunIcon = document.getElementById('sunIcon');
    var moonIcon = document.getElementById('moonIcon');
    if (!sunIcon || !moonIcon) return;
    if (theme === 'dark') {
        sunIcon.style.display = 'none';
        moonIcon.style.display = 'block';
    } else {
        sunIcon.style.display = 'block';
        moonIcon.style.display = 'none';
    }
}

/* ======================== */
/* Mobile Nav Toggle        */
/* ======================== */
function toggleNav() {
    var links = document.querySelector('.nav-links');
    if (links) links.classList.toggle('nav-open');
}

/* ======================== */
/* Markdown Preview         */
/* ======================== */
function updatePreview(textareaId, previewId) {
    var ta = document.getElementById(textareaId);
    var preview = document.getElementById(previewId);
    if (!ta || !preview || typeof marked === 'undefined') return;
    preview.innerHTML = marked.parse(ta.value || '');
}

/* ======================== */
/* Nav Active State         */
/* ======================== */
function setActiveNav() {
    var path = window.location.pathname;
    var links = document.querySelectorAll('.nav-links a');
    links.forEach(function(link) {
        var href = link.getAttribute('href');
        if (!href) return;
        var active = false;
        if (href === '/') {
            active = (path === '/');
        } else if (href.startsWith('/reservations')) {
            active = path.startsWith('/reservations') || path.startsWith('/rooms');
        } else {
            active = path.startsWith(href);
        }
        if (active) link.classList.add('active');
    });
}

/* ======================== */
/* Init on DOM Ready        */
/* ======================== */
document.addEventListener('DOMContentLoaded', function() {
    Toast.init();
    Modal.init();
    setActiveNav();
});

/* Theme: apply immediately (before DOMContentLoaded) */
(function() {
    var saved = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', saved);
})();
