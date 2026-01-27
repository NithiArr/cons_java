from flask import Blueprint, render_template, request, jsonify, redirect, url_for
from flask_login import login_required, current_user
from models import db, Project, Vendor, Purchase, PurchaseItem, Expense, Payment, ClientPayment
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
    
    # Calculate total spent (expenses + purchases)
    total_expenses = sum([
        float(e.amount) for p in projects for e in p.expenses
    ])
    total_purchases = sum([
        float(pur.total_amount) for p in projects for pur in p.purchases
    ])
    total_spent = total_expenses + total_purchases
    
    # Calculate total received (client payments)
    total_received = sum([
        float(cp.amount) for p in projects for cp in p.client_payments
    ])
    
    # Outstanding = Spent - Received
    total_outstanding = total_spent - total_received
    
    # Balance budget = Budget - Spent
    balance_budget = total_budget - total_spent
    
    return jsonify({
        'total_projects': total_projects,
        'status_breakdown': status_breakdown,
        'total_budget': total_budget,
        'total_spent': total_spent,
        'total_received': total_received,
        'total_outstanding': total_outstanding,
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
                'total_outstanding': 0
            }
        
        # Calculate project financials
        expenses = sum([float(e.amount) for e in project.expenses])
        purchases = sum([float(p.total_amount) for p in project.purchases])
        spent = expenses + purchases
        
        received = sum([float(cp.amount) for cp in project.client_payments])
        outstanding = spent - received
        
        status_data[status]['count'] += 1
        status_data[status]['total_budget'] += float(project.budget or 0)
        status_data[status]['total_spent'] += spent
        status_data[status]['total_outstanding'] += outstanding
    
    return jsonify(status_data)

@dashboard_bp.route('/api/project-financial-table', methods=['GET'])
@login_required
def get_project_financial_table():
    """Get detailed financial table for all projects"""
    
    projects = company_filter(Project.query).all()
    
    data = []
    
    for project in projects:
        # Calculate financials
        expenses = sum([float(e.amount) for e in project.expenses])
        purchases = sum([float(p.total_amount) for p in project.purchases])
        amount_spent = expenses + purchases
        
        amount_received = sum([float(cp.amount) for cp in project.client_payments])
        outstanding = amount_spent - amount_received
        
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
            'outstanding': outstanding,
            'balance_budget': balance_budget,
            'indicator': indicator
        })
    
    return jsonify(data)

# ==================== VENDOR ANALYTICS ====================
@dashboard_bp.route('/api/vendor-summary', methods=['GET'])
@login_required
def get_vendor_summary():
    """Get vendor summary with purchases and payments"""
    
    vendors = company_filter(Vendor.query).all()
    
    data = []
    
    for vendor in vendors:
        # Total purchases
        total_purchases = sum([float(p.total_amount) for p in vendor.purchases])
        
        # Total paid
        total_paid = sum([float(pay.amount) for pay in vendor.payments])
        
        # Outstanding
        outstanding = total_purchases - total_paid
        
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
    
    for purchase in vendor.purchases:
        # Calculate paid amount for this purchase
        paid = sum([float(p.amount) for p in purchase.payments])
        balance = float(purchase.total_amount) - paid
        
        data.append({
            'purchase_id': purchase.purchase_id,
            'project_name': purchase.project.name,
            'invoice_number': purchase.invoice_number,
            'invoice_date': purchase.invoice_date.isoformat(),
            'amount': float(purchase.total_amount),
            'paid': paid,
            'balance': balance
        })
    
    return jsonify(data)

@dashboard_bp.route('/api/vendor-material-summary/<int:vendor_id>', methods=['GET'])
@login_required
def get_vendor_material_summary(vendor_id):
    """Get material-wise summary for a vendor"""
    
    vendor = company_filter(Vendor.query).filter_by(vendor_id=vendor_id).first_or_404()
    
    # Aggregate by material
    material_data = {}
    
    for purchase in vendor.purchases:
        for item in purchase.items:
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

# ==================== DAILY CASH BALANCE ====================
@dashboard_bp.route('/api/daily-cash-balance', methods=['GET'])
@login_required
def get_daily_cash_balance():
    """Get daily cash balance for a project and date"""
    
    project_id = request.args.get('project_id', type=int)
    date_str = request.args.get('date')
    
    if not project_id or not date_str:
        return jsonify({'error': 'project_id and date required'}), 400
    
    project = company_filter(Project.query).filter_by(project_id=project_id).first_or_404()
    target_date = datetime.fromisoformat(date_str).date()
    
    # Calculate opening balance (all transactions before target date)
    prev_client_payments = sum([
        float(cp.amount) for cp in project.client_payments
        if cp.payment_date < target_date
    ])
    
    prev_expenses = sum([
        float(e.amount) for e in project.expenses
        if e.expense_date < target_date
    ])
    
    prev_payments = sum([
        float(p.amount) for p in project.payments
        if p.payment_date < target_date
    ])
    
    opening_balance = prev_client_payments - prev_expenses - prev_payments
    
    # Today's transactions
    today_client_payments = [
        {
            'type': 'client_payment',
            'amount': float(cp.amount),
            'payment_mode': cp.payment_mode,
            'reference': cp.reference_number,
            'remarks': cp.remarks
        }
        for cp in project.client_payments if cp.payment_date == target_date
    ]
    
    today_expenses = [
        {
            'type': 'expense',
            'amount': float(e.amount),
            'category': e.category,
            'payment_mode': e.payment_mode,
            'description': e.description
        }
        for e in project.expenses if e.expense_date == target_date
    ]
    
    today_payments = [
        {
            'type': 'vendor_payment',
            'amount': float(p.amount),
            'vendor_name': p.vendor.name,
            'payment_mode': p.payment_mode
        }
        for p in project.payments if p.payment_date == target_date
    ]
    
    # Totals for today
    total_client_receipts = sum([t['amount'] for t in today_client_payments])
    total_expenses = sum([t['amount'] for t in today_expenses])
    total_payments = sum([t['amount'] for t in today_payments])
    
    # Closing balance
    closing_balance = opening_balance + total_client_receipts - total_expenses - total_payments
    
    # All transactions combined
    all_transactions = today_client_payments + today_expenses + today_payments
    
    return jsonify({
        'project_name': project.name,
        'date': date_str,
        'opening_balance': opening_balance,
        'client_receipts': total_client_receipts,
        'expenses': total_expenses,
        'vendor_payments': total_payments,
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
