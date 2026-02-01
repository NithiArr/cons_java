# Edit Button Fix - API Endpoints

## Issue
When clicking "Edit" button on Purchases or Expenses pages, users were getting:
- "Error loading purchase data"
- "Error loading expense data"

## Root Cause
The backend API was missing two critical endpoints:
1. **GET /api/expenses/{id}** - To fetch a single expense/purchase for editing
2. **PUT /api/expenses/{id}** - To update an existing expense/purchase

The frontend Edit buttons were trying to call these endpoints, but they didn't exist, causing 404 errors.

## Solution

### 1. Added GET Endpoint
**File**: `api.py`

```python
@api_bp.route('/expenses/<int:expense_id>', methods=['GET'])
@login_required
def get_expense(expense_id):
    """Get specific expense by ID"""
    expense = company_data_filter(Expense.query).filter_by(expense_id=expense_id).first_or_404()
    
    result = {
        'expense_id': expense.expense_id,
        'project_id': expense.project_id,
        'vendor_id': expense.vendor_id,
        'category': expense.category,
        'subcategory': expense.subcategory,
        'amount': float(expense.amount),
        'payment_mode': expense.payment_mode,
        'expense_date': expense.expense_date.isoformat(),
        'description': expense.description,
        'invoice_number': expense.invoice_number,
        'bill_url': expense.bill_url
    }
    
    # Include items if this is a purchase (Material Purchase)
    if expense.category == 'Material Purchase':
        result['items'] = [{
            'item_name': item.item_name,
            'quantity': float(item.quantity),
            'unit_price': float(item.unit_price),
            'total_price': float(item.total_price)
        } for item in expense.items]
    
    return jsonify(result)
```

**Features**:
- Fetches a single expense by ID
- Automatically includes items if it's a purchase (Material Purchase category)
- Returns all fields needed to pre-fill the edit form
- Returns 404 if not found

### 2. Added PUT Endpoint
**File**: `api.py`

```python
@api_bp.route('/expenses/<int:expense_id>', methods=['PUT'])
@login_required
@role_required('ADMIN', 'ACCOUNTANT', 'MANAGER')
def update_expense(expense_id):
    """Update expense (works for both purchases and regular expenses)"""
    expense = company_data_filter(Expense.query).filter_by(expense_id=expense_id).first_or_404()
    data = request.get_json()
    
    # Update main expense fields
    expense.project_id = data.get('project_id', expense.project_id)
    expense.vendor_id = data.get('vendor_id', expense.vendor_id)
    expense.subcategory = data.get('subcategory', expense.subcategory)
    expense.amount = data.get('total_amount', data.get('amount', expense.amount))
    expense.payment_mode = data.get('payment_type', data.get('payment_mode', expense.payment_mode))
    expense.expense_date = datetime.fromisoformat(data.get('invoice_date', data.get('expense_date', expense.expense_date.isoformat())))
    expense.invoice_number = data.get('invoice_number', expense.invoice_number)
    expense.description = data.get('description', expense.description)
    
    # If this is a purchase (has items), update items
    if 'items' in data and expense.category == 'Material Purchase':
        # Delete existing items
        for item in expense.items:
            db.session.delete(item)
        
        # Add new items
        for item_data in data['items']:
            item = ExpenseItem(
                expense_id=expense.expense_id,
                item_name=item_data['item_name'],
                quantity=item_data['quantity'],
                unit_price=item_data['unit_price'],
                total_price=item_data['total_price']
            )
            db.session.add(item)
    
    db.session.commit()
    return jsonify({'message': 'Expense updated'})
```

**Features**:
- Updates an existing expense or purchase
- Handles both purchase form data (total_amount, payment_type, invoice_date) and expense form data (amount, payment_mode, expense_date)
- For purchases: deletes old items and inserts new ones
- Requires ADMIN, ACCOUNTANT, or MANAGER role

### 3. Fixed Purchases API Response
**File**: `api.py`

**Before**:
```python
return jsonify([{
    'purchase_id': p.expense_id,  # Only had purchase_id
    'project_id': p.project_id,
    ...
}])
```

**After**:
```python
return jsonify([{
    'purchase_id': p.expense_id,  # For backward compatibility
    'expense_id': p.expense_id,   # Added for edit/delete buttons
    'project_id': p.project_id,
    ...
}])
```

**Why**: The Edit/Delete buttons in purchases.html were using `p.expense_id`, but the API was only returning `purchase_id`. Now it returns both.

## API Endpoints Summary

### Purchases (Material Purchase category)
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | /api/purchases | List all purchases |
| GET | /api/expenses/{id} | Get single purchase |
| POST | /api/purchases | Create purchase |
| PUT | /api/expenses/{id} | Update purchase |
| DELETE | /api/purchases/{id} | Delete purchase |

### Expenses (Regular Expense category)
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | /api/expenses | List all expenses |
| GET | /api/expenses/{id} | Get single expense |
| POST | /api/expenses | Create expense |
| PUT | /api/expenses/{id} | Update expense |
| DELETE | /api/expenses/{id} | Delete expense |

## Testing

### Before Fix:
```
Click Edit → "Error loading purchase data" ❌
Click Edit on expense → "Error loading expense data" ❌
```

### After Fix:
```
Click Edit → Modal opens with pre-filled data ✅
Modify fields → Click Save → "Purchase updated successfully" ✅
Click Edit on expense → Modal opens with pre-filled data ✅
Modify fields → Click Save → "Expense updated successfully" ✅
```

## Files Modified

1. **api.py** (Lines 293-363):
   - Added `get_expense(expense_id)` function
   - Added `update_expense(expense_id)` function
   - Updated purchases API to include `expense_id` field

## Notes

- Both endpoints work with the unified Expense model
- The GET endpoint automatically detects if it's a purchase (Material Purchase category) and includes items
- The PUT endpoint handles both regular expenses and purchases with items
- Items are replaced atomically (delete all, then insert new) to ensure data consistency
- All endpoints respect company data isolation via `company_data_filter()`
- Proper role-based access control on the PUT endpoint
