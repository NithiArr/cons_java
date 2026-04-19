// Shared utility functions for all pages

// API helper
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

// Excel download helper — uses fetch + Blob so Content-Disposition filename is preserved
async function downloadExcel(url, fallbackName) {
    try {
        showNotification('Preparing export...', 'success');
        const response = await fetch(url);
        if (!response.ok) throw new Error('Export failed: ' + response.status);

        // Read filename from Content-Disposition header
        const disposition = response.headers.get('Content-Disposition') || '';
        let filename = fallbackName || 'export.xlsx';
        const match = disposition.match(/filename="?([^";\n]+)"?/i);
        if (match && match[1]) filename = match[1].trim();

        const blob = await response.blob();
        const blobUrl = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = blobUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(blobUrl);
        showNotification('Download started: ' + filename, 'success');
    } catch (err) {
        console.error('Download error:', err);
        showNotification('Export failed. Please try again.', 'error');
    }
}


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
            // We keep characters we want to allow the user to type
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
            // This handles commas being added/removed
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

// Format date
// Format date
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '-';

    // Enforce dd/mm/yyyy format
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();

    return `${day}/${month}/${year}`;
}

// Show loading state
function showLoading(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.innerHTML = '<tr><td colspan="100" class="text-center loading">Loading...</td></tr>';
    }
}

// Show error message
function showError(elementId, message) {
    const element = document.getElementById(elementId);
    if (element) {
        element.innerHTML = `<tr><td colspan="100" class="text-center" style="color: var(--danger);">${message}</td></tr>`;
    }
}

// Modal helpers
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('active');
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('active');
    }
}

// Close modal on outside click
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal')) {
        e.target.classList.remove('active');
    }
});

// Form validation
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

// Reset form
function resetForm(formId) {
    const form = document.getElementById(formId);
    if (form) {
        form.reset();
        // Reset any custom styling
        form.querySelectorAll('input, select, textarea').forEach(el => {
            el.style.borderColor = '';
        });
    }
}

// Show notification
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `alert alert-${type}`;
    notification.textContent = message;
    notification.style.position = 'fixed';
    notification.style.top = '20px';
    notification.style.right = '20px';
    notification.style.zIndex = '9999';
    notification.style.minWidth = '300px';

    document.body.appendChild(notification);

    setTimeout(() => {
        notification.remove();
    }, 3000);
}

// Debounce function for search inputs
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

// Mobile menu toggle
function setupMobileMenu() {
    const sidebar = document.querySelector('.sidebar');
    if (!sidebar) return;

    // Create mobile menu button
    const menuButton = document.createElement('button');
    menuButton.className = 'mobile-menu-toggle';
    menuButton.innerHTML = '☰';
    menuButton.setAttribute('aria-label', 'Toggle menu');

    // Create overlay
    const overlay = document.createElement('div');
    overlay.className = 'mobile-overlay';

    // Add to DOM
    document.body.appendChild(menuButton);
    document.body.appendChild(overlay);

    // Toggle menu
    menuButton.addEventListener('click', () => {
        sidebar.classList.toggle('active');
        overlay.classList.toggle('active');
    });

    // Close on overlay click
    overlay.addEventListener('click', () => {
        sidebar.classList.remove('active');
        overlay.classList.remove('active');
    });

    // Close on nav link click (mobile)
    const navLinks = sidebar.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', () => {
            if (window.innerWidth <= 768) {
                sidebar.classList.remove('active');
                overlay.classList.remove('active');
            }
        });
    });
}

// Set active nav link
function setActiveNavLink() {
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.nav-link');

    navLinks.forEach(link => {
        if (link.getAttribute('href') === currentPath) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });
}

// Initialize Tom Select (searchable dropdowns) on given selector
// Falls back gracefully if TomSelect is not loaded
function initSearchableDropdowns(selector) {
    if (typeof TomSelect === 'undefined') return;

    const sel = selector || 'select[data-placeholder]';
    const elements = typeof sel === 'string'
        ? document.querySelectorAll(sel)
        : (sel instanceof NodeList ? sel : [sel]);

    elements.forEach(el => {
        // Skip if already initialized
        if (el.tomselect) return;
        // Skip hidden inputs or disabled elements
        if (el.disabled) return;

        try {
            new TomSelect(el, {
                plugins: ['dropdown_input'],
                sortField: { field: 'text', direction: 'asc' },
                dropdownParent: 'body',
                create: false,
                maxOptions: null,
                placeholder: el.dataset.placeholder || '-- Select --'
            });
        } catch (err) {
            console.warn('TomSelect init failed for', el, err);
        }
    });
}

// Move all modals to <body> level so they're never trapped inside a stacking context
// (e.g., caused by the sidebar's backdrop-filter or a parent div's transform/opacity)
function teleportModalsToBody() {
    document.querySelectorAll('.modal').forEach(modal => {
        if (modal.parentElement !== document.body) {
            document.body.appendChild(modal);
        }
    });
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    teleportModalsToBody();
    setupMobileMenu();
    setActiveNavLink();
    initAmountFormatting();
});

// Global error handler
window.addEventListener('unhandledrejection', (event) => {
    console.error('Unhandled promise rejection:', event.reason);
    showNotification('An error occurred. Please try again.', 'error');
});
