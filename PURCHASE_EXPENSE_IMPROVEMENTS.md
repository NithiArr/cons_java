# Purchase & Expense Form Improvements

## Changes Made

### 1. **Remove Button (-) for Item Rows**

Added a remove button to each item row in the purchase form:

- **Visual**: Red "−" button on the right side of each item row
- **Functionality**: Removes the item row when clicked
- **Protection**: Prevents removal of the last item (at least 1 item required)
- **Layout**: Changed from grid-4 to 5-column grid (Item Name, Qty, Unit Price, Total, Remove Button)

#### Implementation:
```html
<!-- Each item row now has 5 columns -->
<div class="purchase-item" style="grid-template-columns: 1fr 1fr 1fr 1fr auto;">
    <input type="text" class="item-name" />
    <input type="number" class="item-quantity" />
    <input type="number" class="item-price" />
    <input type="text" class="item-total" readonly />
    <button type="button" onclick="removeItem(this)">−</button>
</div>
```

#### JavaScript Function:
```javascript
function removeItem(button) {
    const container = document.getElementById('items-container');
    const items = container.querySelectorAll('.purchase-item');
    
    // Keep at least one item
    if (items.length > 1) {
        button.closest('.purchase-item').remove();
        calculateGrandTotal();
    } else {
        showNotification('At least one item is required', 'error');
    }
}
```

### 2. **Edit Button on Purchases Page**

Added Edit button to every purchase row:

- **Location**: Actions column (before Delete button)
- **Style**: Blue primary button
- **Spacing**: 0.5rem margin-right for separation
- **Status**: Placeholder function (shows "Edit functionality coming soon")

#### Table Actions Column:
```html
<td>
    <button class="btn btn-sm btn-primary" onclick="editPurchase(expense_id)">Edit</button>
    <button class="btn btn-sm btn-danger" onclick="deletePurchase(expense_id)">Delete</button>
</td>
```

### 3. **Edit Button on Expenses Page**

Added Edit button to every expense row:

- **Location**: Actions column (before Delete button)
- **Style**: Blue primary button, matching purchases page
- **Spacing**: 0.5rem margin-right
- **Status**: Placeholder function (shows "Edit functionality coming soon")

#### Table Actions Column:
```html
<td>
    <button class="btn btn-sm btn-primary" onclick="editExpense(expense_id)">Edit</button>
    <button class="btn btn-sm btn-danger" onclick="deleteExpense(expense_id)">Delete</button>
</td>
```

## Files Modified

1. **templates/purchases.html**
   - Added remove button to item rows (lines 97-103)
   - Modified `addItem()` function to include remove button
   - Added `removeItem()` function (lines 192-205)
   - Added Edit button to Actions column (line 173)
   - Added `editPurchase()` placeholder function (lines 298-302)

2. **templates/expenses.html**
   - Added Edit button to Actions column (line 152)
   - Added `editExpense()` placeholder function (lines 194-198)

## User Experience Improvements

### Before:
❌ Could add items but couldn't remove them  
❌ No way to edit existing purchases  
❌ No way to edit existing expenses  

### After:
✅ Can remove item rows with "-" button  
✅ Protection: Last item cannot be removed  
✅ Edit button on purchases table  
✅ Edit button on expenses table  
✅ Clean button layout with proper spacing  

## Next Steps (TODO)

The Edit buttons are currently placeholders. To fully implement edit functionality:

1. **Create Edit Modal**: Duplicate the Add modal and populate it with existing data
2. **Fetch Existing Data**: Load the purchase/expense data by ID
3. **Pre-fill Form**: Populate all fields with existing values
4. **Update API Call**: Change POST to PUT/PATCH for updating
5. **Handle Items**: For purchases, load existing items into the form
6. **Validation**: Ensure data integrity during updates

## Testing Checklist

- [x] Remove button appears on all item rows
- [x] Remove button works correctly
- [x] Cannot remove last item
- [x] Edit button appears on purchases table
- [x] Edit button appears on expenses table
- [x] Edit buttons show placeholder message
- [x] Delete buttons still work correctly
- [x] Layout looks clean and organized
