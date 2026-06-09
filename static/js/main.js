// Shared utility functions for all pages

// ==================== API HELPER ====================
async function apiCall(url, method = 'GET', data = null) {
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
        }
    };

    if (data && method !== 'GET') {
        options.body = JSON.stringify(data);
    }

    try {
        const response = await fetch(url, options);
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return await response.json();
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

// ==================== CURRENCY / NUMBER FORMATTING ====================

// Format currency in Indian format
function formatCurrency(amount) {
    return '₹' + parseFloat(amount || 0).toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

// Utility to parse amount strings with commas and shorthands (2l, 10k)
function parseAmount(value) {
    if (typeof value !== 'string') return parseFloat(value) || 0;

    let cleanValue = value.replace(/[₹,\s]/g, '').toLowerCase();
    if (!cleanValue) return 0;

    let multiplier = 1;
    if (cleanValue.endsWith('k')) {
        multiplier = 1000;
        cleanValue = cleanValue.slice(0, -1);
    } else if (cleanValue.endsWith('l')) {
        multiplier = 100000;
        cleanValue = cleanValue.slice(0, -1);
    } else if (cleanValue.endsWith('cr')) {
        multiplier = 10000000;
        cleanValue = cleanValue.slice(0, -2);
    }

    return (parseFloat(cleanValue) || 0) * multiplier;
}

// Format a number with Indian commas
function formatNumber(num) {
    if (isNaN(num)) return '';
    const parts = num.toString().split('.');
    let lastThree = parts[0].substring(parts[0].length - 3);
    const otherNumbers = parts[0].substring(0, parts[0].length - 3);
    if (otherNumbers !== '') {
        lastThree = ',' + lastThree;
    }
    const res = otherNumbers.replace(/\B(?=(\d{2})+(?!\d))/g, ",") + lastThree;
    return parts.length > 1 ? res + "." + parts[1] : res;
}

// ==================== AMOUNT INPUT FORMATTING ====================

// Initialize amount formatting for inputs with .amount-input class
function initAmountFormatting() {
    document.body.addEventListener('input', (e) => {
        if (e.target.classList.contains('amount-input')) {
            const input = e.target;
            let val = input.value;

            // Save state before modification
            const oldVal = val;
            const oldCursor = input.selectionStart;

            // 1. Remove all characters except digits, shorthands, and one dot
            let cleanVal = val.replace(/[^0-9kKlLcCrR.]/g, '');

            // 2. Handle shorthand characters - if they exist, don't comma-format yet
            if (/[klcr]/i.test(cleanVal)) {
                input.value = cleanVal;
                return;
            }

            // 3. Comma formatting logic
            const parts = cleanVal.split('.');
            let integerPart = parts[0];
            const decimalPart = parts.length > 1 ? parts[1] : null;

            if (integerPart === "" && decimalPart !== null) integerPart = "0";

            if (integerPart !== "") {
                const num = parseInt(integerPart, 10);
                if (!isNaN(num)) {
                    integerPart = formatNumber(num);
                }
            }

            let newVal = integerPart;
            if (decimalPart !== null) newVal += "." + decimalPart.substring(0, 2);

            // 4. Update input value and restore cursor position
            input.value = newVal;

            // Cursor logic: find how many digits were before the cursor and find them again
            let digitsBeforeCursor = oldVal.substring(0, oldCursor).replace(/\D/g, '').length;
            let newCursor = 0;
            let digitsCount = 0;
            while (newCursor < newVal.length && digitsCount < digitsBeforeCursor) {
                if (/\d/.test(newVal[newCursor])) {
                    digitsCount++;
                }
                newCursor++;
            }
            input.setSelectionRange(newCursor, newCursor);
        }
    });

    document.body.addEventListener('keydown', (e) => {
        if (e.target.classList.contains('amount-input') && e.key === 'Enter') {
            e.target.blur(); // Trigger shorthand expansion
        }
    });

    document.body.addEventListener('blur', (e) => {
        if (e.target.classList.contains('amount-input')) {
            const input = e.target;
            const value = parseAmount(input.value);
            input.value = formatNumber(value);
            // Trigger a change event so listeners (like totals) can update
            input.dispatchEvent(new Event('change', { bubbles: true }));
        }
    }, true);
}

// ==================== DATE PICKER ====================

// Initialize date range picker for elements with .date-range-picker class
function initDateRangePicker(selector, onChangeCallback) {
    if (!window.flatpickr) return;

    const instance = flatpickr(selector, {
        mode: "range",
        dateFormat: "Y-m-d",
        altInput: true,
        altFormat: "d M Y",
        allowInput: true,
        onClose: function (selectedDates, dateStr, instance) {
            if (onChangeCallback) {
                if (selectedDates.length === 2) {
                    onChangeCallback(selectedDates, dateStr);
                } else if (dateStr === "") {
                    onChangeCallback([], "");
                }
            }
        },
        onReady: function (selectedDates, dateStr, instance) {
            // Add a "Clear" button to the calendar footer
            const clearBtn = document.createElement("div");
            clearBtn.innerHTML = "Clear Data";
            clearBtn.style.textAlign = "center";
            clearBtn.style.padding = "5px";
            clearBtn.style.cursor = "pointer";
            clearBtn.style.color = "var(--danger)";
            clearBtn.style.fontWeight = "bold";
            clearBtn.style.borderTop = "1px solid var(--border-color)";
            clearBtn.onclick = () => {
                instance.clear();
                instance.close();
                if (onChangeCallback) onChangeCallback([], "");
            };
            instance.calendarContainer.appendChild(clearBtn);

            // Handle backspace/delete in the input field
            const altInput = instance.altInput;
            altInput.addEventListener('keydown', (e) => {
                if ((e.key === 'Backspace' || e.key === 'Delete') && altInput.value === "") {
                    instance.clear();
                    if (onChangeCallback) onChangeCallback([], "");
                }
            });

            // Re-filter immediately when manually cleared
            altInput.addEventListener('input', (e) => {
                if (altInput.value === "") {
                    instance.clear();
                    if (onChangeCallback) onChangeCallback([], "");
                }
            });
        }
    });

    return instance;
}

// ==================== DATE FORMAT ====================
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '-';

    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();

    return `${day}/${month}/${year}`;
}

// ==================== LOADING / ERROR STATES ====================

function showLoading(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.innerHTML = '<tr><td colspan="100" class="text-center loading">Loading...</td></tr>';
    }
}

function showError(elementId, message) {
    const element = document.getElementById(elementId);
    if (element) {
        element.innerHTML = `<tr><td colspan="100" class="text-center" style="color: var(--danger);">${message}</td></tr>`;
    }
}

// ==================== MODAL HELPERS ====================

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('active');
        // Prevent body scroll when modal is open on mobile
        document.body.style.overflow = 'hidden';
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }
}

// Close modal on outside click (background)
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal')) {
        e.target.classList.remove('active');
        document.body.style.overflow = '';
    }
});

// ==================== FORM HELPERS ====================

function validateForm(formId) {
    const form = document.getElementById(formId);
    if (!form) return false;

    const inputs = form.querySelectorAll('[required]');
    let isValid = true;

    inputs.forEach(input => {
        if (!input.value.trim()) {
            input.style.borderColor = 'var(--danger)';
            isValid = false;
        } else {
            input.style.borderColor = '';
        }
    });

    return isValid;
}

function resetForm(formId) {
    const form = document.getElementById(formId);
    if (form) {
        form.reset();
        form.querySelectorAll('input, select, textarea').forEach(el => {
            el.style.borderColor = '';
        });
    }
}

// ==================== NOTIFICATIONS ====================

function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `alert alert-${type}`;
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        z-index: 9999;
        min-width: 280px;
        max-width: calc(100vw - 40px);
        animation: slideUp 0.3s ease-out;
        box-shadow: var(--shadow-lg);
    `;

    // On mobile, show at top below the header
    if (window.innerWidth <= 768) {
        notification.style.top = 'calc(var(--mobile-header-height, 56px) + 0.5rem)';
        notification.style.left = '1rem';
        notification.style.right = '1rem';
        notification.style.minWidth = 'unset';
    }

    document.body.appendChild(notification);

    setTimeout(() => {
        notification.style.opacity = '0';
        notification.style.transition = 'opacity 0.3s ease';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// ==================== UTILITIES ====================

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Export to CSV (client-side)
function exportTableToCSV(tableId, filename) {
    const table = document.getElementById(tableId);
    if (!table) return;

    let csv = [];
    const rows = table.querySelectorAll('tr');

    rows.forEach(row => {
        const cols = row.querySelectorAll('td, th');
        const rowData = Array.from(cols).map(col => {
            return '"' + col.textContent.trim().replace(/"/g, '""') + '"';
        });
        csv.push(rowData.join(','));
    });

    const csvContent = csv.join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename || 'export.csv';
    a.click();
    window.URL.revokeObjectURL(url);
}

// ==================== MOBILE MENU SETUP ====================

function setupMobileMenu() {
    const sidebar = document.getElementById('sidebar');
    const menuToggle = document.getElementById('mobileMenuToggle');
    const overlay = document.getElementById('mobileOverlay');
    const sidebarClose = document.getElementById('sidebarClose');

    if (!sidebar) return;

    function openSidebar() {
        sidebar.classList.add('active');
        overlay.classList.add('active');
        document.body.style.overflow = 'hidden';
    }

    function closeSidebar() {
        sidebar.classList.remove('active');
        overlay.classList.remove('active');
        document.body.style.overflow = '';
    }

    // Hamburger button in mobile header
    if (menuToggle) {
        menuToggle.addEventListener('click', () => {
            if (sidebar.classList.contains('active')) {
                closeSidebar();
            } else {
                openSidebar();
            }
        });
    }

    // Close button inside sidebar
    if (sidebarClose) {
        sidebarClose.addEventListener('click', closeSidebar);
    }

    // Overlay click
    if (overlay) {
        overlay.addEventListener('click', closeSidebar);
    }

    // Close when a nav link is clicked on mobile
    const navLinks = sidebar.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', () => {
            if (window.innerWidth <= 768) {
                closeSidebar();
            }
        });
    });

    // Bottom nav "More" button opens sidebar
    const bottomNavMore = document.getElementById('bottomNavMore');
    if (bottomNavMore) {
        bottomNavMore.addEventListener('click', () => {
            if (sidebar.classList.contains('active')) {
                closeSidebar();
            } else {
                openSidebar();
            }
        });
    }

    // Touch swipe-right to open sidebar (from left edge)
    let touchStartX = 0;
    let touchStartY = 0;

    document.addEventListener('touchstart', (e) => {
        touchStartX = e.touches[0].clientX;
        touchStartY = e.touches[0].clientY;
    }, { passive: true });

    document.addEventListener('touchend', (e) => {
        const touchEndX = e.changedTouches[0].clientX;
        const touchEndY = e.changedTouches[0].clientY;
        const deltaX = touchEndX - touchStartX;
        const deltaY = Math.abs(touchEndY - touchStartY);

        // Swipe right from left edge (within 40px of left) to open
        if (touchStartX < 40 && deltaX > 80 && deltaY < 80) {
            openSidebar();
        }

        // Swipe left on sidebar to close
        if (sidebar.classList.contains('active') && deltaX < -80 && deltaY < 80) {
            closeSidebar();
        }
    }, { passive: true });
}

// ==================== SET ACTIVE NAV LINK ====================

function setActiveNavLink() {
    const currentPath = window.location.pathname;

    // Highlight sidebar links
    document.querySelectorAll('.nav-link').forEach(link => {
        const href = link.getAttribute('href');
        if (href && (href === currentPath || (currentPath.startsWith(href) && href !== '/'))) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });

    // Highlight bottom nav items
    document.querySelectorAll('.bottom-nav-item[data-url]').forEach(item => {
        const url = item.getAttribute('data-url');
        if (url && (url === currentPath || (currentPath.startsWith(url) && url !== '/'))) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });
}

// ==================== FILTER BAR TOGGLE (MOBILE) ====================

function setupFilterToggle() {
    // Inject toggle button before each filter-bar that doesn't already have one
    document.querySelectorAll('.filter-bar').forEach(bar => {
        // Only on mobile
        if (window.innerWidth > 768) return;

        // Check if a toggle already exists immediately before this bar
        const prev = bar.previousElementSibling;
        if (prev && prev.classList.contains('filter-panel-toggle')) return;

        const toggleBtn = document.createElement('button');
        toggleBtn.className = 'btn btn-secondary filter-panel-toggle';
        toggleBtn.innerHTML = `
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="16" height="16">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 3c2.755 0 5.455.232 8.083.678.533.09.917.556.917 1.096v1.044a2.25 2.25 0 0 1-.659 1.591l-5.432 5.432a2.25 2.25 0 0 0-.659 1.591v2.927a2.25 2.25 0 0 1-1.244 2.013L9.75 21v-6.568a2.25 2.25 0 0 0-.659-1.591L3.659 7.409A2.25 2.25 0 0 1 3 5.818V4.774c0-.54.384-1.006.917-1.096A48.32 48.32 0 0 1 12 3Z" />
            </svg>
            Filters
        `;

        bar.parentNode.insertBefore(toggleBtn, bar);

        toggleBtn.addEventListener('click', () => {
            const isOpen = bar.classList.contains('filter-open');
            bar.classList.toggle('filter-open', !isOpen);
            toggleBtn.innerHTML = isOpen
                ? `<svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="16" height="16"><path stroke-linecap="round" stroke-linejoin="round" d="M12 3c2.755 0 5.455.232 8.083.678.533.09.917.556.917 1.096v1.044a2.25 2.25 0 0 1-.659 1.591l-5.432 5.432a2.25 2.25 0 0 0-.659 1.591v2.927a2.25 2.25 0 0 1-1.244 2.013L9.75 21v-6.568a2.25 2.25 0 0 0-.659-1.591L3.659 7.409A2.25 2.25 0 0 1 3 5.818V4.774c0-.54.384-1.006.917-1.096A48.32 48.32 0 0 1 12 3Z" /></svg> Filters`
                : `<svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="16" height="16"><path stroke-linecap="round" stroke-linejoin="round" d="M12 3c2.755 0 5.455.232 8.083.678.533.09.917.556.917 1.096v1.044a2.25 2.25 0 0 1-.659 1.591l-5.432 5.432a2.25 2.25 0 0 0-.659 1.591v2.927a2.25 2.25 0 0 1-1.244 2.013L9.75 21v-6.568a2.25 2.25 0 0 0-.659-1.591L3.659 7.409A2.25 2.25 0 0 1 3 5.818V4.774c0-.54.384-1.006.917-1.096A48.32 48.32 0 0 1 12 3Z" /></svg> Hide Filters`;
        });
    });
}

// ==================== TABLE SWIPE HINTS ====================

function addTableSwipeHints() {
    if (window.innerWidth > 768) return;

    document.querySelectorAll('.table-container').forEach(container => {
        if (container.querySelector('.swipe-hint')) return;

        const hint = document.createElement('div');
        hint.className = 'swipe-hint';
        hint.textContent = '← Scroll horizontally to see more →';
        container.insertBefore(hint, container.firstChild);

        // Hide hint after first scroll
        container.addEventListener('scroll', () => {
            hint.style.display = 'none';
        }, { once: true });
    });
}

// ==================== PREVENT DOUBLE SUBMIT ====================

function preventDoubleSubmit() {
    document.querySelectorAll('form').forEach(form => {
        let submitted = false;
        form.addEventListener('submit', (e) => {
            if (submitted) {
                e.preventDefault();
                return;
            }
            submitted = true;
            // Auto-reset after 5s in case of error
            setTimeout(() => { submitted = false; }, 5000);

            // Visually disable the submit button
            const submitBtn = form.querySelector('[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.style.opacity = '0.6';
                setTimeout(() => {
                    submitBtn.disabled = false;
                    submitBtn.style.opacity = '';
                }, 5000);
            }
        });
    });
}

// ==================== INITIALISE ON DOM READY ====================

document.addEventListener('DOMContentLoaded', () => {
    setupMobileMenu();
    setActiveNavLink();
    initAmountFormatting();
    setupFilterToggle();
    addTableSwipeHints();
    preventDoubleSubmit();
});

// ==================== GLOBAL ERROR HANDLER ====================

window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled promise rejection:', event.reason);
    showNotification('An error occurred. Please try again.', 'error');
});
