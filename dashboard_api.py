from flask import Blueprint, render_template, request, jsonify, redirect, url_for
from flask_login import login_required, current_user
from models import db, Project, Vendor, Expense, ExpenseItem, Payment, ClientPayment
from sqlalchemy import func
from datetime import datetime, timedelta
from decimal import Decimal

dashboard_bp = Blueprint('dashboard', __name__)

def company_filter(query):
    """Filter by company"""
    return query.filter_by(company_id=current_user.company_id)

# ==================== DASHBOARD PAGES ====================
@dashboard_bp.route('/main')
@login_required
def main_dashboard():
    """Main dashboard for all roles except OWNER"""
    return render_template('dashboard.html', user=current_user)

@dashboard_bp.route('/owner')
@login_required
def owner_dashboard():
    """Owner dashboard with high-level KPIs"""
    if current_user.role != 'OWNER':
        return redirect(url_for('dashboard.main_dashboard'))
    return render_template('owner_dashboard.html', user=current_user)

@dashboard_bp.route('/vendor-analytics')
@login_required
def vendor_analytics():
    """Vendor analytics page"""
    return render_template('vendor_analytics.html', user=current_user)

@dashboard_bp.route('/daily-cash')
@login_required
def daily_cash():
    """Daily cash balance page"""
    return render_template('daily_cash.html', user=current_user)

# ==================== OWNER DASHBOARD KPIs ====================
@dashboard_bp.route('/api/owner-kpis', methods=['GET'])
@login_required
def get_owner_kpis():
    """Get high-level KPIs for owner dashboard"""
    
    # Total projects
    total_projects = company_filter(Project.query).count()
    
    # Projects by status
    status_counts = db.session.query(
        Project.status,
        func.count(Project.project_id)
    ).filter_by(company_id=current_user.company_id).group_by(Project.status).all()
    
    status_breakdown = {status: count for status, count in status_counts}
    
    # Financial aggregates
    projects = company_filter(Project.query).all()
    
    total_budget = sum([float(p.budget or 0) for p in projects])
    
    # Calculate total spent (Regular Expenses + Material Purchases)
    total_spent = sum([
        float(e.amount) for p in projects for e in p.expenses
    ])
    
    # Calculate CREDIT Material Purchases (for outstanding)
    total_credit_purchases = sum([
        float(e.amount) for p in projects for e in p.expenses
        if e.category == 'Material Purchase' and e.payment_mode == 'CREDIT'
    ])
    
    # Calculate total vendor payments made
    total_vendor_payments = sum([
        float(payment.amount) for p in projects for payment in p.payments
    ])
    
    # Calculate total received from clients
    total_received = sum([
        float(cp.amount) for p in projects for cp in p.client_payments
    ])
    
    # Vendor Outstanding = Only CREDIT purchases - vendor payments
    # CASH/UPI/BANK already paid, not included in outstanding
    vendor_outstanding = total_credit_purchases - total_vendor_payments
    
    # Client Outstanding (Receivables) = What clients owe us
    client_outstanding = total_spent - total_received
    
    # Balance budget = Budget - Spent
    balance_budget = total_budget - total_spent
    
    return jsonify({
        'total_projects': total_projects,
        'status_breakdown': status_breakdown,
        'total_budget': total_budget,
        'total_spent': total_spent,
        'total_material_purchases': total_material_purchases,
        'total_vendor_payments': total_vendor_payments,
        'total_received': total_received,
        'vendor_outstanding': vendor_outstanding,  # What we owe vendors
        'client_outstanding': client_outstanding,  # What clients owe us
        'balance_budget': balance_budget
    })

@dashboard_bp.route('/api/status-wise-overview', methods=['GET'])
@login_required
def get_status_wise_overview():
    """Get status-wise project overview"""
    
    projects = company_filter(Project.query).all()
    
    status_data = {}
    
    for project in projects:
        status = project.status
        if status not in status_data:
            status_data[status] = {
                'count': 0,
                'total_budget': 0,
                'total_spent': 0,
                'vendor_outstanding': 0,  # What we owe vendors
                'client_outstanding': 0   # What clients owe us
            }
        
        # Calculate project financials (all expenses)
        spent = sum([float(e.amount) for e in project.expenses])
        
        # CREDIT material purchases only (for outstanding)
        credit_purchases = sum([
            float(e.amount) for e in project.expenses
            if e.category == 'Material Purchase' and e.payment_mode == 'CREDIT'
        ])
        
        # Vendor payments made
        vendor_payments = sum([float(p.amount) for p in project.payments])
        
        # Client payments received
        received = sum([float(cp.amount) for cp in project.client_payments])
        
        # Only CREDIT purchases count as outstanding
        vendor_outstanding = credit_purchases - vendor_payments
        client_outstanding = spent - received
        
        status_data[status]['count'] += 1
        status_data[status]['total_budget'] += float(project.budget or 0)
        status_data[status]['total_spent'] += spent
        status_data[status]['vendor_outstanding'] += vendor_outstanding
        status_data[status]['client_outstanding'] += client_outstanding
    
    return jsonify(status_data)

@dashboard_bp.route('/api/project-financial-table', methods=['GET'])
@login_required
def get_project_financial_table():
    """Get detailed financial table for all projects"""
    
    projects = company_filter(Project.query).all()
    
    data = []
    
    for project in projects:
        # Calculate financials
        amount_spent = sum([float(e.amount) for e in project.expenses])
        
        # CREDIT material purchases only (for outstanding)
        credit_purchases = sum([
            float(e.amount) for e in project.expenses
            if e.category == 'Material Purchase' and e.payment_mode == 'CREDIT'
        ])
        
        # Vendor payments made for this project
        vendor_payments_made = sum([float(p.amount) for p in project.payments])
        
        # Client payments received
        amount_received = sum([float(cp.amount) for cp in project.client_payments])
        
        # Only CREDIT purchases count as outstanding
        vendor_outstanding = credit_purchases - vendor_payments_made  # What we owe vendors
        client_outstanding = amount_spent - amount_received  # What client owes us
        
        budget = float(project.budget or 0)
        balance_budget = budget - amount_spent
        
        # Color indicator
        if amount_spent > budget:
            indicator = 'over'  # Red
        elif amount_spent > budget * 0.9:
            indicator = 'near'  # Yellow
        else:
            indicator = 'healthy'  # Green
        
        data.append({
            'project_id': project.project_id,
            'project_name': project.name,
            'status': project.status,
            'budget': budget,
            'amount_spent': amount_spent,
            'amount_received': amount_received,
            'vendor_outstanding': vendor_outstanding,  # What we owe vendors
            'client_outstanding': client_outstanding,  # What clients owe us
            'balance_budget': balance_budget,
            'indicator': indicator
        })
    
    return jsonify(data)

# ==================== VENDOR ANALYTICS ====================
@dashboard_bp.route('/api/vendor-summary', methods=['GET'])
@login_required
def get_vendor_summary():
    """Get vendor summary with purchases (Expenses) and payments"""
    
    vendors = company_filter(Vendor.query).all()
    
    data = []
    
    for vendor in vendors:
        # All purchases for display
        total_purchases = sum([float(e.amount) for e in vendor.expenses])
        
        # CREDIT purchases from this vendor (for outstanding)
        total_credit_purchases = sum([
            float(e.amount) for e in vendor.expenses
            if e.payment_mode == 'CREDIT'
        ])
        
        # Total paid to this vendor
        total_paid = sum([float(pay.amount) for pay in vendor.payments])
        
        # Outstanding = Only CREDIT purchases - payments
        outstanding = total_credit_purchases - total_paid
        
        data.append({
            'vendor_id': vendor.vendor_id,
            'vendor_name': vendor.name,
            'total_purchases': total_purchases,
            'total_paid': total_paid,
            'outstanding': outstanding
        })
    
    return jsonify(data)

@dashboard_bp.route('/api/vendor-purchase-history/<int:vendor_id>', methods=['GET'])
@login_required
def get_vendor_purchase_history(vendor_id):
    """Get purchase history for a specific vendor"""
    
    vendor = company_filter(Vendor.query).filter_by(vendor_id=vendor_id).first_or_404()
    
    data = []
    
    # Iterate over expenses linked to vendor (material purchases)
    for expense in vendor.expenses:
        # Calculate paid and balance based on payment mode
        if expense.payment_mode in ['CASH', 'UPI', 'BANK']:
            # Already paid at time of purchase
            paid = float(expense.amount)
            balance = 0
        else:
            # CREDIT - get vendor payments made for this vendor on this project
            project_vendor_payments = sum([
                float(p.amount) for p in vendor.payments
                if p.project_id == expense.project_id
            ])
            
            # Get total CREDIT purchases for this vendor on this project
            project_credit_purchases = sum([
                float(e.amount) for e in vendor.expenses
                if e.project_id == expense.project_id and e.payment_mode == 'CREDIT'
            ])
            
            # Distribute payments proportionally across CREDIT purchases in this project
            if project_credit_purchases > 0:
                proportion = float(expense.amount) / project_credit_purchases
                paid = project_vendor_payments * proportion
            else:
                paid = 0
            
            balance = float(expense.amount) - paid
        
        data.append({
            'purchase_id': expense.expense_id,
            'project_name': expense.project.name,
            'invoice_number': expense.invoice_number or '-',
            'invoice_date': expense.expense_date.isoformat(),
            'amount': float(expense.amount),
            'payment_mode': expense.payment_mode,
            'paid': paid,
            'balance': balance
        })
    
    return jsonify(data)

@dashboard_bp.route('/api/vendor-material-summary/<int:vendor_id>', methods=['GET'])
@login_required
def get_vendor_material_summary(vendor_id):
    """Get material-wise summary for a vendor (optionally filtered by project)"""
    from flask import request
    
    vendor = company_filter(Vendor.query).filter_by(vendor_id=vendor_id).first_or_404()
    project_name = request.args.get('project', None)
    
    # Aggregate by material
    material_data = {}
    
    for expense in vendor.expenses:
        # Filter by project if specified
        if project_name and expense.project.name != project_name:
            continue
        # Check items (ExpenseItems)
        for item in expense.items:
            material = item.item_name
            if material not in material_data:
                material_data[material] = {
                    'total_quantity': 0,
                    'total_amount': 0
                }
            
            material_data[material]['total_quantity'] += float(item.quantity)
            material_data[material]['total_amount'] += float(item.total_price)
    
    # Convert to list
    data = [
        {
            'material': material,
            'total_quantity': values['total_quantity'],
            'total_amount': values['total_amount']
        }
        for material, values in material_data.items()
    ]
    
    return jsonify(data)

# ==================== PROJECT PAYMENT ANALYTICS ====================
@dashboard_bp.route('/api/project-payment-details/<int:project_id>', methods=['GET'])
@login_required
def get_project_payment_details(project_id):
    """Get comprehensive payment details for a specific project"""
    
    project = company_filter(Project.query).filter_by(project_id=project_id).first_or_404()
    
    # Split expenses into 'Material Purchase' and 'Regular Expense'
    # Material Purchases (replacing Purchase History)
    purchase_history = []
    regular_expenses = []
    
    total_purchases_amount = 0
    total_expenses_amount = 0
    
    for expense in project.expenses:
        if expense.category == 'Material Purchase':
            # It's a purchase
            vendor_payments = sum([float(p.amount) for p in expense.payments])
            
            # For DISPLAY in table: CASH/UPI/BANK show as fully paid
            if expense.payment_mode in ['CASH', 'UPI', 'BANK']:
                # Already paid at source - show as fully paid
                displayed_paid = float(expense.amount)
                displayed_balance = 0
            else:
                # CREDIT - show actual payments and balance
                displayed_paid = vendor_payments
                displayed_balance = float(expense.amount) - vendor_payments
            
            purchase_history.append({
                'purchase_id': expense.expense_id,
                'vendor_name': expense.vendor.name if expense.vendor else 'Unknown',
                'invoice_number': expense.invoice_number or '-',
                'invoice_date': expense.expense_date.isoformat(),
                'subcategory': expense.subcategory,
                'amount': float(expense.amount),
                'payment_mode': expense.payment_mode,
                'paid': displayed_paid,
                'balance': displayed_balance
            })
            total_purchases_amount += float(expense.amount)
            
        else:
            # Regular Expense
            regular_expenses.append({
                'expense_id': expense.expense_id,
                'expense_date': expense.expense_date.isoformat(),
                'category': expense.category,
                'subcategory': expense.subcategory,
                'amount': float(expense.amount),
                'payment_mode': expense.payment_mode,
                'description': expense.description
            })
            total_expenses_amount += float(expense.amount)
    
    # Vendor Payments
    vendor_payments = []
    for payment in project.payments:
        vendor_payments.append({
            'payment_id': payment.payment_id,
            'payment_date': payment.payment_date.isoformat() if payment.payment_date else None,
            'vendor_name': payment.vendor.name,
            'amount': float(payment.amount),
            'payment_mode': payment.payment_mode,
            'purchase_invoice': payment.expense.invoice_number if payment.expense else '-'
        })
    
    # Client Payments
    client_payments = []
    for cp in project.client_payments:
        client_payments.append({
            'client_payment_id': cp.client_payment_id,
            'payment_date': cp.payment_date.isoformat() if cp.payment_date else None,
            'amount': float(cp.amount),
            'payment_mode': cp.payment_mode,
            'reference_number': cp.reference_number,
            'remarks': cp.remarks
        })
    
    # Financial Summary
    total_spent = total_purchases_amount + total_expenses_amount
    total_received = sum([float(cp.amount) for cp in project.client_payments])
    
    total_vendor_payments = sum([float(p.amount) for p in project.payments])
    
    # Calculate CREDIT purchases only (for outstanding)
    total_credit_purchases = sum([
        float(e.amount) for e in project.expenses
        if e.category == 'Material Purchase' and e.payment_mode == 'CREDIT'
    ])
    
    # Vendor Outstanding = Only CREDIT purchases - vendor payments
    # CASH/UPI/BANK are already paid, so NOT included in outstanding
    vendor_outstanding = total_credit_purchases - total_vendor_payments
    client_outstanding = total_spent - total_received
    
    return jsonify({
        'project_name': project.name,
        'project_status': project.status,
        'purchase_history': purchase_history,
        'vendor_payments': vendor_payments,
        'expenses': regular_expenses,
        'client_payments': client_payments,
        'financial_summary': {
            'total_purchases': total_purchases_amount,
            'total_expenses': total_expenses_amount,
            'total_spent': total_spent,
            'total_received': total_received,
            'vendor_outstanding': vendor_outstanding,
            'client_outstanding': client_outstanding,
            'total_vendor_payments': total_vendor_payments
        }
    })

# ==================== DAILY CASH BALANCE ====================
@dashboard_bp.route('/api/daily-cash-balance', methods=['GET'])
@login_required
def get_daily_cash_balance():
    """Get cash balance for a project and date range"""
    
    project_id_param = request.args.get('project_id')
    from_date_str = request.args.get('from_date')
    to_date_str = request.args.get('to_date')
    
    # Backward compatibility: support single 'date' parameter
    if not from_date_str and request.args.get('date'):
        from_date_str = request.args.get('date')
        to_date_str = from_date_str
    
    if not project_id_param or not from_date_str or not to_date_str:
        return jsonify({'error': 'project_id, from_date and to_date required'}), 400
    
    from_date = datetime.fromisoformat(from_date_str).date()
    to_date = datetime.fromisoformat(to_date_str).date()
    
    # Check if viewing all projects or a specific project
    if project_id_param == 'all':
        # Get all projects for the user's company
        projects = company_filter(Project.query).all()
        project_name = 'All Projects'
    else:
        # Get specific project
        project_id = int(project_id_param)
        project = company_filter(Project.query).filter_by(project_id=project_id).first_or_404()
        projects = [project]
        project_name = project.name
    
    # Initialize counters
    opening_balance = 0
    total_client_receipts = 0
    total_expenses_outflow = 0
    total_payments_outflow = 0
    expenses_credit = 0
    expenses_cash = 0
    expenses_bank_upi = 0
    all_transactions = []
    
    # Process each project
    for project in projects:
        # Calculate opening balance (all transactions before from_date)
        prev_client_payments = sum([
            float(cp.amount) for cp in project.client_payments
            if cp.payment_date < from_date
        ])
        
        prev_direct_expenses = sum([
            float(e.amount) for e in project.expenses
            if e.expense_date < from_date and e.payment_mode in ['CASH', 'BANK', 'UPI']
        ])
        
        prev_vendor_payments = sum([
            float(p.amount) for p in project.payments
            if p.payment_date < from_date
        ])
        
        opening_balance += prev_client_payments - prev_direct_expenses - prev_vendor_payments
        
        # Period transactions (from_date to to_date inclusive)
        period_client_payments = [
            {
                'type': 'client_payment',
                'date': cp.payment_date.isoformat(),
                'amount': float(cp.amount),
                'payment_mode': cp.payment_mode,
                'reference': cp.reference_number,
                'remarks': cp.remarks,
                'project_name': project.name  # Add project name
            }
            for cp in project.client_payments 
            if from_date <= cp.payment_date <= to_date
        ]
        
        # Period direct expenses (Cash/Bank/UPI)
        period_expenses = [
            {
                'type': 'expense',
                'date': e.expense_date.isoformat(),
                'amount': float(e.amount),
                'category': e.category,
                'subcategory': e.subcategory,
                'payment_mode': e.payment_mode,
                'description': e.description,
                'project_name': project.name  # Add project name
            }
            for e in project.expenses 
            if from_date <= e.expense_date <= to_date and e.payment_mode in ['CASH', 'BANK', 'UPI']
        ]
        
        # Period vendor payments
        period_payments = [
            {
                'type': 'vendor_payment',
                'date': p.payment_date.isoformat(),
                'amount': float(p.amount),
                'vendor_name': p.vendor.name,
                'payment_mode': p.payment_mode,
                'project_name': project.name  # Add project name
            }
            for p in project.payments 
            if from_date <= p.payment_date <= to_date
        ]
        
        # Aggregate totals
        total_client_receipts += sum([t['amount'] for t in period_client_payments])
        total_expenses_outflow += sum([t['amount'] for t in period_expenses])
        total_payments_outflow += sum([t['amount'] for t in period_payments])
        
        # Split expenses by payment mode
        expenses_credit += sum([
            float(e.amount) for e in project.expenses
            if from_date <= e.expense_date <= to_date and e.payment_mode == 'CREDIT'
        ])
        
        # Split paid expenses into CASH and BANK/UPI
        expenses_cash += sum([
            float(e.amount) for e in project.expenses
            if from_date <= e.expense_date <= to_date and e.payment_mode == 'CASH'
        ])
        
        expenses_bank_upi += sum([
            float(e.amount) for e in project.expenses
            if from_date <= e.expense_date <= to_date and e.payment_mode in ['UPI', 'BANK']
        ])
        
        # Add to all transactions
        all_transactions.extend(period_client_payments + period_expenses + period_payments)
    
    # Closing balance
    closing_balance = opening_balance + total_client_receipts - total_expenses_outflow - total_payments_outflow
    
    # Sort all transactions by date
    all_transactions.sort(key=lambda x: x['date'])
    
    return jsonify({
        'project_name': project_name,
        'from_date': from_date_str,
        'to_date': to_date_str,
        'opening_balance': opening_balance,
        'client_receipts': total_client_receipts,
        'expenses': total_expenses_outflow,
        'expenses_credit': expenses_credit,  # CREDIT expenses
        'expenses_cash': expenses_cash,      # CASH expenses
        'expenses_bank_upi': expenses_bank_upi,  # UPI/BANK expenses
        'vendor_payments': total_payments_outflow,
        'closing_balance': closing_balance,
        'transactions': all_transactions
    })

# ==================== PAYMENT MODE SPLIT ====================
@dashboard_bp.route('/api/payment-mode-split', methods=['GET'])
@login_required
def get_payment_mode_split():
    """Get payment mode analysis (CASH/BANK/UPI split)"""
    
    # Get all transactions for the company
    client_payments = company_filter(ClientPayment.query).all()
    expenses = company_filter(Expense.query).all()
    vendor_payments = company_filter(Payment.query).all()
    
    # Initialize data structure
    modes = ['CASH', 'BANK', 'UPI']
    mode_data = {}
    
    for mode in modes:
        # Calculate received (client payments)
        received = sum([
            float(cp.amount) for cp in client_payments 
            if cp.payment_mode == mode
        ])
        
        # Calculate expenses in this mode
        # Only include actual outflows, not CREDIT purchases (if any end up here, though typical expenses are cash/bank)
        expenses_spent = sum([
            float(e.amount) for e in expenses
            if e.payment_mode == mode
        ])
        
        # Calculate vendor payments in this mode
        payments_spent = sum([
            float(p.amount) for p in vendor_payments
            if p.payment_mode == mode
        ])
        
        total_spent = expenses_spent + payments_spent
        balance = received - total_spent
        
        mode_data[mode] = {
            'received': received,
            'spent': total_spent,
            'balance': balance,
            'breakdown': {
                'expenses': expenses_spent,
                'vendor_payments': payments_spent
            }
        }
    
    # Calculate totals
    total_received = sum([md['received'] for md in mode_data.values()])
    total_spent = sum([md['spent'] for md in mode_data.values()])
    total_balance = sum([md['balance'] for md in mode_data.values()])
    
    return jsonify({
        'cash': mode_data['CASH'],
        'bank': mode_data['BANK'],
        'upi': mode_data['UPI'],
        'totals': {
            'received': total_received,
            'spent': total_spent,
            'balance': total_balance
        }
    })
