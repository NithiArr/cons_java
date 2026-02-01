# Edit Functionality Implementation - Purchases & Expenses

## Overview
Implemented fully functional edit capability for both Purchases and Expenses with the same UI as the Add forms.

## Features Implemented

### 1. **Edit Purchases**

#### How It Works:
1. **Click Edit Button** → Opens the same modal as "Add Purchase"
2. **Modal Title Changes** → "Add Purchase Order" → "Edit Purchase Order"
3. **Form Pre-filled** → All fields populated with existing data
4. **Items Loaded** → All purchase items are loaded into the form
5. **Save Changes** → Updates the existing record (PUT request)

#### Implementation Details:
```javascript
async function editPurchase(id) {
    // Fetch existing purchase data
    const response = await fetch(`/api/expenses/${id}`);
    const expense = await response.json();
    
    // Set hidden ID field for update mode
    document.getElementById('purchase-id').value = expense.expense_id;
    
    // Change modal title
    document.getElementById('purchase-modal-title').textContent = 'Edit Purchase Order';
    
    // Pre-fill all form fields
    document.getElementById('purchase-project').value = expense.project_id;
    document.getElementById('purchase-vendor').value = expense.vendor_id;
    // ... etc
    
    // Load all items
    expense.items.forEach(item => {
        // Populate item rows with existing data
    });
    
    // Open modal
    openModal('add-purchase-modal');
}
```

#### Form Submission Logic:
```javascript
const purchaseId = document.getElementById('purchase-id').value;
const isEdit = purchaseId !== '';

// Use different endpoint and method based on mode
const url = isEdit ? `/api/expenses/${purchaseId}` : '/api/purchases';
const method = isEdit ? 'PUT' : 'POST';

// Success message changes
showNotification(
    isEdit ? 'Purchase updated successfully' : 'Purchase added successfully', 
    'success'
);
```

### 2. **Edit Expenses**

#### How It Works:
1. **Click Edit Button** → Opens the same modal as "Add Expense"
2. **Modal Title Changes** → "Add Expense" → "Edit Expense"
3. **Form Pre-filled** → All fields populated with existing data
4. **Save Changes** → Updates the existing record (PUT request)

#### Implementation Details:
```javascript
async function editExpense(id) {
    // Fetch existing expense data
    const response = await fetch(`/api/expenses/${id}`);
    const expense = await response.json();
    
    // Set hidden ID field
    document.getElementById('expense-id').value = expense.expense_id;
    
    // Change modal title
    document.getElementById('expense-modal-title').textContent = 'Edit Expense';
    
    // Pre-fill all form fields
    document.getElementById('expense-project').value = expense.project_id;
    document.getElementById('expense-category').value = expense.category;
    document.getElementById('expense-amount').value = expense.amount;
    // ... etc
    
    // Open modal
    openModal('add-expense-modal');
}
```

### 3. **Inline Buttons**

Edit and Delete buttons are now on the same line with proper spacing:

```html
<!-- Purchases Table -->
<td>
    <button onclick="editPurchase()" style="margin-right: 0.5rem;">Edit</button>
    <button onclick="deletePurchase()">Delete</button>
</td>

<!-- Expenses Table -->
<td>
    <button onclick="editExpense()" style="margin-right: 0.5rem;">Edit</button>
    <button onclick="deleteExpense()">Delete</button>
</td>
```

## Files Modified

### templates/purchases.html
1. Added `<input type="hidden" id="purchase-id">` to track edit mode
2. Added `id="purchase-modal-title"` to modal header for dynamic title
3. Implemented `editPurchase(id)` function to:
   - Fetch existing data
   - Pre-fill all form fields
   - Load all purchase items
   - Open modal
4. Updated form submission to handle both create (POST) and update (PUT)
5. Made Edit/Delete buttons inline

### templates/expenses.html
1. Added `<input type="hidden" id="expense-id">` to track edit mode
2. Added `id="expense-modal-title"` to modal header for dynamic title
3. Implemented `editExpense(id)` function to:
   - Fetch existing data
   - Pre-fill all form fields
   - Open modal
4. Updated form submission to handle both create (POST) and update (PUT)
5. Made Edit/Delete buttons inline

## User Flow

### Edit Purchase:
```
1. User clicks "Edit" button on a purchase row
2. System fetches purchase data from API
3. Modal opens with title "Edit Purchase Order"
4. All fields pre-filled with existing data
5. All items loaded into item rows
6. User makes changes
7. User clicks "Save Purchase"
8. System sends PUT request to /api/expenses/{id}
9. Success: "Purchase updated successfully"
10. Modal closes, table refreshes
```

### Edit Expense:
```
1. User clicks "Edit" button on an expense row
2. System fetches expense data from API
3. Modal opens with title "Edit Expense"
4. All fields pre-filled with existing data
5. User makes changes
6. User clicks "Save Expense"
7. System sends PUT request to /api/expenses/{id}
8. Success: "Expense updated successfully"
9. Modal closes, table refreshes
```

## Testing Completed

✅ Edit button opens modal  
✅ Modal title changes to "Edit"  
✅ All fields pre-filled correctly  
✅ Purchase items loaded correctly  
✅ Form validates properly  
✅ Update API call works (PUT method)  
✅ Success message shows correct text  
✅ Modal closes after save  
✅ Table refreshes with updated data  
✅ Buttons appear inline (Edit | Delete)  
✅ Proper spacing between buttons  

## API Endpoints Used

### Fetch Single Record:
- `GET /api/expenses/{id}` - Returns single expense/purchase data

### Update Record:
- `PUT /api/expenses/{id}` - Updates existing expense/purchase

### Create Record (unchanged):
- `POST /api/purchases` - Creates new purchase
- `POST /api/expenses` - Creates new expense

## Notes

1. **Same Modal Reused**: Uses the exact same modal as Add, just with different title and pre-filled data
2. **Hidden ID Field**: Determines if in edit mode or create mode
3. **Dynamic Behavior**: Form submission logic adapts based on whether ID is present
4. **Clean Reset**: After save, modal title resets and ID field cleared for next use
5. **Error Handling**: Proper error messages if fetch fails
6. **Inline Buttons**: Edit and Delete now on same line with 0.5rem spacing
