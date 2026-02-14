from flask import Blueprint, render_template, request, jsonify, redirect, url_for
from flask_login import login_required, current_user
from models_mongo import Project, Vendor, Expense, Payment, ClientPayment, ExpenseItem
from datetime import datetime
import collections

dashboard_bp = Blueprint('dashboard', __name__)

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

# ==================== HELPER FUNCTIONS ====================
def get_project_financials(project):
    """Calculate financials for a project"""
    expenses = Expense.objects(project=project)
    payments = Payment.objects(project=project)
    client_payments = ClientPayment.objects(project=project)
    
    total_spent = sum(e.amount for e in expenses)
    total_received = sum(cp.amount for cp in client_payments)
    
    # CREDIT Material Purchases
    credit_purchases = sum(
        e.amount for e in expenses 
        if e.expense_type == 'Material Purchase' and e.payment_mode == 'CREDIT'
    )
    
    # Vendor payments made
    vendor_payments_total = sum(p.amount for p in payments)
    
    # Vendor Outstanding (Only for CREDIT purchases)
    vendor_outstanding = credit_purchases - vendor_payments_total
    if vendor_outstanding < 0: vendor_outstanding = 0
    
    # Client Outstanding (Receivables)
    client_outstanding = total_spent - total_received
    
    return {
        'total_spent': total_spent,
        'total_received': total_received,
        'credit_purchases': credit_purchases,
        'vendor_payments': vendor_payments_total,
        'vendor_outstanding': vendor_outstanding,
        'client_outstanding': client_outstanding
    }

# ==================== API ENDPOINTS ====================

@dashboard_bp.route('/api/owner-kpis', methods=['GET'])
@login_required
def get_owner_kpis():
    """Get high-level KPIs for owner dashboard"""
    projects = Project.objects(company=current_user.company)
    
    total_projects = projects.count()
    total_budget = sum(p.budget for p in projects)
    
    status_counts = collections.Counter(p.status for p in projects)
    
    total_spent = 0
    total_received = 0
    total_vendor_payments = 0
    total_credit_purchases = 0
    
    for project in projects:
        fins = get_project_financials(project)
        total_spent += fins['total_spent']
        total_received += fins['total_received']
        total_vendor_payments += fins['vendor_payments']
        total_credit_purchases += fins['credit_purchases']
        
    vendor_outstanding = total_credit_purchases - total_vendor_payments
    client_outstanding = total_spent - total_received
    balance_budget = total_budget - total_spent
    
    return jsonify({
        'total_projects': total_projects,
        'status_breakdown': dict(status_counts),
        'total_budget': total_budget,
        'total_spent': total_spent,
        'total_vendor_payments': total_vendor_payments,
        'total_received': total_received,
        'vendor_outstanding': vendor_outstanding,
        'client_outstanding': client_outstanding,
        'balance_budget': balance_budget
    })

@dashboard_bp.route('/api/project-financial-table', methods=['GET'])
@login_required
def get_project_financial_table():
    """Get detailed financial table for all projects"""
    projects = Project.objects(company=current_user.company)
    data = []
    
    for project in projects:
        fins = get_project_financials(project)
        
        amount_spent = fins['total_spent']
        budget = project.budget
        
        # Color indicator
        if amount_spent > budget:
            indicator = 'over'  # Red
        elif amount_spent > budget * 0.9:
            indicator = 'near'  # Yellow
        else:
            indicator = 'healthy'  # Green
            
        data.append({
            'project_id': str(project.id),
            'project_name': project.name,
            'status': project.status,
            'budget': budget,
            'amount_spent': amount_spent,
            'amount_received': fins['total_received'],
            'vendor_outstanding': fins['vendor_outstanding'],
            'client_outstanding': fins['client_outstanding'],
            'balance_budget': budget - amount_spent,
            'indicator': indicator
        })
        
    return jsonify(data)

@dashboard_bp.route('/api/vendor-summary', methods=['GET'])
@login_required
def get_vendor_summary():
    """Get vendor summary"""
    vendors = Vendor.objects(company=current_user.company)
    data = []
    
    for vendor in vendors:
        expenses = Expense.objects(vendor=vendor)
        payments = Payment.objects(vendor=vendor)
        
        total_purchases = sum(e.amount for e in expenses)
        total_paid = sum(p.amount for p in payments)
        
        # CREDIT purchases
        credit_purchases = sum(e.amount for e in expenses if e.payment_mode == 'CREDIT')
        
        # Outstanding
        outstanding = credit_purchases - total_paid
        
        data.append({
            'vendor_id': str(vendor.id),
            'vendor_name': vendor.name,
            'total_purchases': total_purchases,
            'total_paid': total_paid,
            'outstanding': outstanding
        })
        
    return jsonify(data)

@dashboard_bp.route('/api/vendor-purchase-history/<vendor_id>', methods=['GET'])
@login_required
def get_vendor_purchase_history(vendor_id):
    """Get purchase history for a specific vendor"""
    vendor = Vendor.objects(id=vendor_id, company=current_user.company).first()
    if not vendor:
        return jsonify({'error': 'Vendor not found'}), 404
        
    expenses = Expense.objects(vendor=vendor)
    data = []
    
    for expense in expenses:
        # Calculate paid amount for this specific expense
        if expense.payment_mode in ['CASH', 'UPI', 'BANK']:
            paid = expense.amount
            balance = 0
        else:
            # For CREDIT, we need to approximate based on total payments to vendor for this project
            # This logic mimics the old SQLAlchemy version
            project_payments = Payment.objects(vendor=vendor, project=expense.project)
            total_project_payments = sum(p.amount for p in project_payments)
            
            project_expenses = Expense.objects(vendor=vendor, project=expense.project, payment_mode='CREDIT')
            total_project_credit = sum(e.amount for e in project_expenses)
            
            if total_project_credit > 0:
                proportion = expense.amount / total_project_credit
                paid = total_project_payments * proportion
            else:
                paid = 0
                
            balance = expense.amount - paid
            
        data.append({
            'purchase_id': str(expense.id),
            'project_name': expense.project.name,
            'invoice_number': expense.invoice_number or '-',
            'invoice_date': expense.expense_date.isoformat(),
            'amount': expense.amount,
            'payment_mode': expense.payment_mode,
            'paid': paid,
            'balance': balance
        })
        
    return jsonify(data)

@dashboard_bp.route('/api/project-payment-details/<project_id>', methods=['GET'])
@login_required
def get_project_payment_details(project_id):
    """Get comprehensive payment details for a specific project"""
    project = Project.objects(id=project_id, company=current_user.company).first()
    if not project:
        return jsonify({'error': 'Project not found'}), 404
        
    expenses = Expense.objects(project=project)
    payments = Payment.objects(project=project)
    client_payments = ClientPayment.objects(project=project)
    
    purchase_history = []
    regular_expenses = []
    
    total_purchases_amount = 0
    total_expenses_amount = 0
    
    for expense in expenses:
        if expense.expense_type == 'Material Purchase':
            # Calculate payments for this expense - tricky in Mongo without direct link in Payment model (unless added)
            # In new Payment model, we have `expense` ReferenceField!
            expense_payments = Payment.objects(expense=expense)
            payment_sum = sum(p.amount for p in expense_payments)
            
            if expense.payment_mode in ['CASH', 'UPI', 'BANK']:
                displayed_paid = expense.amount
                displayed_balance = 0
            else:
                displayed_paid = payment_sum
                displayed_balance = expense.amount - payment_sum
                
            purchase_history.append({
                'purchase_id': str(expense.id),
                'vendor_name': expense.vendor.name if expense.vendor else 'Unknown',
                'invoice_number': expense.invoice_number or '-',
                'invoice_date': expense.expense_date.isoformat(),
                'category': expense.category, # Material Category
                'amount': expense.amount,
                'payment_mode': expense.payment_mode,
                'paid': displayed_paid,
                'balance': displayed_balance
            })
            total_purchases_amount += expense.amount
        else:
            regular_expenses.append({
                'expense_id': str(expense.id),
                'expense_date': expense.expense_date.isoformat(),
                'category': expense.category, # e.g. Travel
                'amount': expense.amount,
                'payment_mode': expense.payment_mode,
                'description': expense.description
            })
            total_expenses_amount += expense.amount

    vendor_payments_list = []
    for p in payments:
        vendor_payments_list.append({
            'payment_id': str(p.id),
            'payment_date': p.payment_date.isoformat(),
            'vendor_name': p.vendor.name,
            'amount': p.amount,
            'payment_mode': p.payment_mode,
            'purchase_invoice': p.expense.invoice_number if p.expense else '-'
        })

    client_payments_list = []
    for cp in client_payments:
        client_payments_list.append({
            'client_payment_id': str(cp.id),
            'payment_date': cp.payment_date.isoformat(),
            'amount': cp.amount,
            'payment_mode': cp.payment_mode,
            'reference_number': cp.reference_number,
            'remarks': cp.remarks
        })
        
    # Financial Summary
    total_spent = total_purchases_amount + total_expenses_amount
    total_received = sum(cp.amount for cp in client_payments)
    total_vendor_payments = sum(p.amount for p in payments)
    
    # Credit purchases
    credit_purchases = sum(e.amount for e in expenses if e.expense_type == 'Material Purchase' and e.payment_mode == 'CREDIT')
    
    vendor_outstanding = credit_purchases - total_vendor_payments
    if vendor_outstanding < 0: vendor_outstanding = 0
    
    client_outstanding = total_spent - total_received
    
    return jsonify({
        'project_name': project.name,
        'project_status': project.status,
        'purchase_history': purchase_history,
        'vendor_payments': vendor_payments_list,
        'expenses': regular_expenses,
        'client_payments': client_payments_list,
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

@dashboard_bp.route('/api/daily-cash-balance', methods=['GET'])
@login_required
def get_daily_cash_balance():
    """Get cash balance for a project and date range"""
    project_id_param = request.args.get('project_id')
    from_date_str = request.args.get('from_date')
    to_date_str = request.args.get('to_date')
    
    if not from_date_str and request.args.get('date'):
        from_date_str = request.args.get('date')
        to_date_str = from_date_str
        
    if not project_id_param or not from_date_str:
        return jsonify({'error': 'Missing parameters'}), 400
        
    from_date = datetime.fromisoformat(from_date_str).date()
    to_date = datetime.fromisoformat(to_date_str).date()
    
    if project_id_param == 'all':
        projects = Project.objects(company=current_user.company)
        project_name = 'All Projects'
    else:
        project = Project.objects(id=project_id_param, company=current_user.company).first()
        if not project:
            return jsonify({'error': 'Project not found'}), 404
        projects = [project]
        project_name = project.name
        
    opening_balance = 0
    transactions = []
    
    # Range totals
    client_receipts = 0
    expenses_outflow = 0
    payments_outflow = 0
    
    for p in projects:
        # Fetch all related documents
        cps = ClientPayment.objects(project=p)
        exps = Expense.objects(project=p)
        pays = Payment.objects(project=p)
        
        # Calculate opening balance (prior to from_date)
        for cp in cps:
            if cp.payment_date < from_date:
                opening_balance += cp.amount
            elif from_date <= cp.payment_date <= to_date:
                client_receipts += cp.amount
                transactions.append({
                    'type': 'client_payment',
                    'date': cp.payment_date.isoformat(),
                    'amount': cp.amount,
                    'payment_mode': cp.payment_mode,
                    'reference': cp.reference_number,
                    'remarks': cp.remarks,
                    'project_name': p.name
                })
                
        for e in exps:
            # Only count CASH/BANK/UPI expenses as outflow
            if e.payment_mode in ['CASH', 'BANK', 'UPI']:
                if e.expense_date < from_date:
                    opening_balance -= e.amount
                elif from_date <= e.expense_date <= to_date:
                    expenses_outflow += e.amount
                    transactions.append({
                        'type': 'expense',
                        'date': e.expense_date.isoformat(),
                        'amount': e.amount,
                        'category': e.category,
                        'payment_mode': e.payment_mode,
                        'description': e.description,
                        'project_name': p.name
                    })
                    
        for py in pays:
            if py.payment_date < from_date:
                opening_balance -= py.amount
            elif from_date <= py.payment_date <= to_date:
                payments_outflow += py.amount
                transactions.append({
                    'type': 'vendor_payment',
                    'date': py.payment_date.isoformat(),
                    'amount': py.amount,
                    'vendor_name': py.vendor.name,
                    'payment_mode': py.payment_mode,
                    'project_name': p.name
                })
                
    transactions.sort(key=lambda x: x['date'])
    closing_balance = opening_balance + client_receipts - expenses_outflow - payments_outflow
    
    return jsonify({
        'project_name': project_name,
        'from_date': from_date_str,
        'to_date': to_date_str,
        'opening_balance': opening_balance,
        'client_receipts': client_receipts,
        'expenses': expenses_outflow,
        'vendor_payments': payments_outflow,
        'closing_balance': closing_balance,
        'transactions': transactions
    })

@dashboard_bp.route('/api/vendor-material-summary/<vendor_id>', methods=['GET'])
@login_required
def get_vendor_material_summary(vendor_id):
    """Get material-wise summary for a vendor"""
    vendor = Vendor.objects(id=vendor_id, company=current_user.company).first()
    if not vendor:
        return jsonify({'error': 'Vendor not found'}), 404

    project_filter = request.args.get('project')
    
    expenses = Expense.objects(vendor=vendor, expense_type='Material Purchase')
    
    if project_filter:
        project = Project.objects(name=project_filter, company=current_user.company).first()
        if project:
            expenses = expenses.filter(project=project)
            
    material_map = {}
    
    for expense in expenses:
        # Group by Category (Material Category)
        mat = expense.category or 'Uncategorized'
        if mat not in material_map:
            material_map[mat] = {'quantity': 0, 'amount': 0}
        
        material_map[mat]['amount'] += expense.amount
        
        # Sum quantities from items
        for item in expense.items:
            material_map[mat]['quantity'] += item.quantity
            
    result = []
    for mat, data in material_map.items():
        result.append({
            'material': mat,
            'total_quantity': data['quantity'],
            'total_amount': data['amount']
        })
        
    return jsonify(result)
