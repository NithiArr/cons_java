# Project Detail Modal - Balance KPI Addition

## Summary  
Added a new **Balance KPI** to the project details modal showing the difference between Total Budget and Total Spent. All 5 KPIs now display in a single row.

## What Changed

### Before:
- 4 KPI cards in a row (grid-4):
  1. Total Budget
  2. Total Spent
  3. Total Received
  4. Vendor Outstanding

### After:
- 5 KPI cards in a single row (grid-5):
  1. Total Budget
  2. Total Spent
  3. **Balance** ← NEW
  4. Total Received
  5. Vendor Outstanding

## New Balance KPI Details

### Calculation:
```javascript
Balance = Total Budget - Total Spent
```

### Features:
- **Dynamic Color Coding:**
  - 🟢 **Green** (Success) - When balance ≥ 20% of budget
  - 🟡 **Yellow** (Warning) - When balance < 20% of budget but ≥ 0
  - 🔴 **Red** (Danger) - When balance < 0 (over budget)

- **Subtitle**: "Budget - Spent" for clarity

### Visual Layout:
```
┌──────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│ Total Budget │ Total Spent  │   Balance    │ Total Recv'd │ Vendor O/S   │
│  ₹80,00,000  │  ₹7,45,000   │  ₹72,55,000  │  ₹40,00,000  │  ₹2,30,000   │
│              │              │ Budget-Spent │              │ What we owe  │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┘
```

## Code Changes

### 1. HTML Structure (`projects.html`)

**Changed Grid Class:**
```html
<!-- Before -->
<div class="grid grid-4 mb-3" id="financial-summary">

<!-- After -->
<div class="grid grid-5 mb-3" id="financial-summary">
```

**Added Balance Card:**
```html
<div class="card">
    <div class="card-body">
        <p class="text-secondary mb-1">Balance</p>
        <h3 id="summary-balance" style="color: var(--success);">₹0.00</h3>
        <small style="font-size: 0.7rem; color: var(--text-secondary);">Budget - Spent</small>
    </div>
</div>
```

### 2. JavaScript Logic (`projects.html`)

**Added Calculation and Color Coding:**
```javascript
// Calculate and display balance (Budget - Spent)
const balance = project.budget - data.financial_summary.total_spent;
const balanceEl = document.getElementById('summary-balance');
balanceEl.textContent = formatCurrency(balance);

// Color code the balance
if (balance < 0) {
    balanceEl.style.color = 'var(--danger)';       // Over budget
} else if (balance < project.budget * 0.2) {
    balanceEl.style.color = 'var(--warning)';      // Low balance
} else {
    balanceEl.style.color = 'var(--success)';      // Healthy balance
}
```

### 3. CSS (Already Existed)

The `.grid-5` class was already defined in `static/css/style.css`:

```css
.grid-5 {
    grid-template-columns: repeat(5, 1fr);
}

/* Responsive - Tablet */
@media (max-width: 1200px) {
    .grid-5 {
        grid-template-columns: repeat(3, 1fr);  /* 3 columns on tablets */
    }
}

/* Responsive - Mobile */
@media (max-width: 768px) {
    .grid-5 {
        grid-template-columns: 1fr;             /* Stack on mobile */
    }
}
```

## Benefits

1. **Better Financial Visibility**
   - Instantly see remaining budget at a glance
   - No manual calculation needed

2. **Quick Budget Health Check**
   - Color coding provides instant visual feedback
   - Red alert when over budget
   - Warning when running low

3. **Improved Decision Making**
   - Clear view of available funds
   - Helps with expense planning
   - Easy comparison with received amounts

4. **Responsive Design**
   - All 5 KPIs fit in one row on desktop
   - Adapts to 3 columns on tablets
   - Stacks vertically on mobile

## User Experience

### Desktop View:
All 5 KPIs displayed in a single row for easy comparison.

### Tablet View (< 1200px):
KPIs display in 2 rows:
- Row 1: Budget, Spent, Balance
- Row 2: Received, Vendor Outstanding

### Mobile View (< 768px):
All KPIs stack vertically for easy scrolling.

## Example Scenarios

### Scenario 1: Healthy Project
- Budget: ₹80,00,000
- Spent: ₹7,45,000
- **Balance: ₹72,55,000** (Green) ✅

### Scenario 2: Low Budget Warning
- Budget: ₹50,00,000
- Spent: ₹42,00,000
- **Balance: ₹8,00,000** (Yellow - 16% remaining) ⚠️

### Scenario 3: Over Budget
- Budget: ₹30,00,000
- Spent: ₹32,50,000
- **Balance: -₹2,50,000** (Red - Over budget) ⛔

## File Modified

- ✅ `templates/projects.html`
  - Lines 118: Changed `grid-4` to `grid-5`
  - Lines 131-137: Added Balance KPI card HTML
  - Lines 615-630: Added balance calculation and color coding logic

## Testing Checklist

- [x] Balance displays correct calculation
- [x] Green color for healthy balance
- [x] Yellow color for low balance (< 20%)
- [x] Red color for negative balance
- [x] All 5 KPIs fit in one row on desktop
- [x] Responsive layout works on tablets
- [x] Responsive layout works on mobile
- [x] Subtitle "Budget - Spent" displays correctly

---

**Completed:** February 8, 2026, 11:35 PM IST  
**Status:** ✅ Ready for Production
