from flask import Blueprint, request, jsonify
from flask_login import login_required, current_user
from models import db, Project, Vendor, Purchase, PurchaseItem, Expense, Payment, ClientPayment
from functools import wraps
from datetime import datetime
from sqlalchemy import func

api_bp = Blueprint('api', __name__)

def role_required(*roles):
    """Decorator to check user role"""
    def decorator(f):
        @wraps(f)
        def decorated_function(*args, **kwargs):
            if not current_user.is_authenticated:
                return jsonify({'error': 'Authentication required'}), 401
            if current_user.role not in roles:
                return jsonify({'error': 'Insufficient permissions'}), 403
            return f(*args, **kwargs)
        return decorated_function
    return decorator

def company_data_filter(query):
    """Filter query by current user's company"""
    return query.filter_by(company_id=current_user.company_id)

# ==================== PROJECTS API ====================
@api_bp.route('/projects', methods=['GET'])
@login_required
def get_projects():
    """Get all projects for user's company"""
    projects = company_data_filter(Project.query).all()
    return jsonify([{
        'project_id': p.project_id,
        'name': p.name,
        'location': p.location,
        'start_date': p.start_date.isoformat() if p.start_date else None,
        'end_date': p.end_date.isoformat() if p.end_date else None,
        'budget': float(p.budget) if p.budget else 0,
        'status': p.status,
        'created_at': p.created_at.isoformat()
    } for p in projects])

@api_bp.route('/projects/<int:project_id>', methods=['GET'])
@login_required
def get_project(project_id):
    """Get specific project"""
    project = company_data_filter(Project.query).filter_by(project_id=project_id).first_or_404()
    return jsonify({
        'project_id': project.project_id,
        'name': project.name,
        'location': project.location,
        'start_date': project.start_date.isoformat() if project.start_date else None,
        'end_date': project.end_date.isoformat() if project.end_date else None,
        'budget': float(project.budget) if project.budget else 0,
        'status': project.status
    })

@api_bp.route('/projects', methods=['POST'])
@login_required
@role_required('ADMIN', 'MANAGER')
def create_project():
    """Create new project"""
    data = request.get_json()
    
    project = Project(
        company_id=current_user.company_id,
        name=data['name'],
        location=data.get('location'),
        start_date=datetime.fromisoformat(data['start_date']) if data.get('start_date') else None,
        end_date=datetime.fromisoformat(data['end_date']) if data.get('end_date') else None,
        budget=data.get('budget', 0),
        status=data.get('status', 'PLANNING')
    )
    
    db.session.add(project)
    db.session.commit()
    
    return jsonify({'message': 'Project created', 'project_id': project.project_id}), 201

@api_bp.route('/projects/<int:project_id>', methods=['PUT'])
@login_required
@role_required('ADMIN', 'MANAGER')
def update_project(project_id):
    """Update project"""
    project = company_data_filter(Project.query).filter_by(project_id=project_id).first_or_404()
    data = request.get_json()
    
    project.name = data.get('name', project.name)
    project.location = data.get('location', project.location)
    project.start_date = datetime.fromisoformat(data['start_date']) if data.get('start_date') else project.start_date
    project.end_date = datetime.fromisoformat(data['end_date']) if data.get('end_date') else project.end_date
    project.budget = data.get('budget', project.budget)
    project.status = data.get('status', project.status)
    
    db.session.commit()
    return jsonify({'message': 'Project updated'})

@api_bp.route('/projects/<int:project_id>', methods=['DELETE'])
@login_required
@role_required('ADMIN')
def delete_project(project_id):
    """Delete project"""
    project = company_data_filter(Project.query).filter_by(project_id=project_id).first_or_404()
    db.session.delete(project)
    db.session.commit()
    return jsonify({'message': 'Project deleted'})

# ==================== VENDORS API ====================
@api_bp.route('/vendors', methods=['GET'])
@login_required
def get_vendors():
    """Get all vendors"""
    vendors = company_data_filter(Vendor.query).all()
    return jsonify([{
        'vendor_id': v.vendor_id,
        'name': v.name,
        'phone': v.phone,
        'email': v.email,
        'gst_number': v.gst_number,
        'created_at': v.created_at.isoformat()
    } for v in vendors])

@api_bp.route('/vendors', methods=['POST'])
@login_required
@role_required('ADMIN', 'MANAGER')
def create_vendor():
    """Create new vendor"""
    data = request.get_json()
    
    vendor = Vendor(
        company_id=current_user.company_id,
        name=data['name'],
        phone=data.get('phone'),
        email=data.get('email'),
        gst_number=data.get('gst_number')
    )
    
    db.session.add(vendor)
    db.session.commit()
    
    return jsonify({'message': 'Vendor created', 'vendor_id': vendor.vendor_id}), 201

@api_bp.route('/vendors/<int:vendor_id>', methods=['PUT'])
@login_required
@role_required('ADMIN', 'MANAGER')
def update_vendor(vendor_id):
    """Update vendor"""
    vendor = company_data_filter(Vendor.query).filter_by(vendor_id=vendor_id).first_or_404()
    data = request.get_json()
    
    vendor.name = data.get('name', vendor.name)
    vendor.phone = data.get('phone', vendor.phone)
    vendor.email = data.get('email', vendor.email)
    vendor.gst_number = data.get('gst_number', vendor.gst_number)
    
    db.session.commit()
    return jsonify({'message': 'Vendor updated'})

@api_bp.route('/vendors/<int:vendor_id>', methods=['DELETE'])
@login_required
@role_required('ADMIN')
def delete_vendor(vendor_id):
    """Delete vendor"""
    vendor = company_data_filter(Vendor.query).filter_by(vendor_id=vendor_id).first_or_404()
    db.session.delete(vendor)
    db.session.commit()
    return jsonify({'message': 'Vendor deleted'})

# ==================== PURCHASES API ====================
@api_bp.route('/purchases', methods=['GET'])
@login_required
def get_purchases():
    """Get all purchases"""
    purchases = company_data_filter(Purchase.query).all()
    return jsonify([{
        'purchase_id': p.purchase_id,
        'project_id': p.project_id,
        'project_name': p.project.name,
        'vendor_id': p.vendor_id,
        'vendor_name': p.vendor.name,
        'invoice_number': p.invoice_number,
        'invoice_date': p.invoice_date.isoformat(),
        'total_amount': float(p.total_amount),
        'payment_type': p.payment_type,
        'items': [{
            'item_name': item.item_name,
            'quantity': float(item.quantity),
            'unit_price': float(item.unit_price),
            'total_price': float(item.total_price)
        } for item in p.items]
    } for p in purchases])

@api_bp.route('/purchases', methods=['POST'])
@login_required
@role_required('ADMIN', 'MANAGER')
def create_purchase():
    """Create new purchase with items"""
    data = request.get_json()
    
    purchase = Purchase(
        company_id=current_user.company_id,
        project_id=data['project_id'],
        vendor_id=data['vendor_id'],
        invoice_number=data.get('invoice_number'),
        invoice_date=datetime.fromisoformat(data['invoice_date']),
        total_amount=data['total_amount'],
        payment_type=data['payment_type']
    )
    
    db.session.add(purchase)
    db.session.flush()
    
    # Add purchase items
    for item_data in data.get('items', []):
        item = PurchaseItem(
            purchase_id=purchase.purchase_id,
            item_name=item_data['item_name'],
            quantity=item_data['quantity'],
            unit_price=item_data['unit_price'],
            total_price=item_data['total_price']
        )
        db.session.add(item)
    
    db.session.commit()
    return jsonify({'message': 'Purchase created', 'purchase_id': purchase.purchase_id}), 201

@api_bp.route('/purchases/<int:purchase_id>', methods=['DELETE'])
@login_required
@role_required('ADMIN')
def delete_purchase(purchase_id):
    """Delete purchase"""
    purchase = company_data_filter(Purchase.query).filter_by(purchase_id=purchase_id).first_or_404()
    db.session.delete(purchase)
    db.session.commit()
    return jsonify({'message': 'Purchase deleted'})

# ==================== EXPENSES API ====================
@api_bp.route('/expenses', methods=['GET'])
@login_required
def get_expenses():
    """Get all expenses"""
    expenses = company_data_filter(Expense.query).all()
    return jsonify([{
        'expense_id': e.expense_id,
        'project_id': e.project_id,
        'project_name': e.project.name,
        'category': e.category,
        'subcategory': e.subcategory,
        'amount': float(e.amount),
        'payment_mode': e.payment_mode,
        'expense_date': e.expense_date.isoformat(),
        'description': e.description,
        'bill_url': e.bill_url
    } for e in expenses])

@api_bp.route('/expenses', methods=['POST'])
@login_required
@role_required('ADMIN', 'ACCOUNTANT')
def create_expense():
    """Create new expense"""
    data = request.get_json()
    
    expense = Expense(
        company_id=current_user.company_id,
        project_id=data['project_id'],
        category=data['category'],
        subcategory=data.get('subcategory'),
        amount=data['amount'],
        payment_mode=data['payment_mode'],
        expense_date=datetime.fromisoformat(data['expense_date']),
        description=data.get('description'),
        bill_url=data.get('bill_url')
    )
    
    db.session.add(expense)
    db.session.commit()
    
    return jsonify({'message': 'Expense created', 'expense_id': expense.expense_id}), 201

@api_bp.route('/expenses/<int:expense_id>', methods=['DELETE'])
@login_required
@role_required('ADMIN', 'ACCOUNTANT')
def delete_expense(expense_id):
    """Delete expense"""
    expense = company_data_filter(Expense.query).filter_by(expense_id=expense_id).first_or_404()
    db.session.delete(expense)
    db.session.commit()
    return jsonify({'message': 'Expense deleted'})

# ==================== PAYMENTS API ====================
@api_bp.route('/payments', methods=['GET'])
@login_required
def get_payments():
    """Get all vendor payments"""
    payments = company_data_filter(Payment.query).all()
    return jsonify([{
        'payment_id': p.payment_id,
        'vendor_id': p.vendor_id,
        'vendor_name': p.vendor.name,
        'project_id': p.project_id,
        'project_name': p.project.name,
        'purchase_id': p.purchase_id,
        'amount': float(p.amount),
        'payment_date': p.payment_date.isoformat(),
        'payment_mode': p.payment_mode
    } for p in payments])

@api_bp.route('/payments', methods=['POST'])
@login_required
@role_required('ADMIN', 'ACCOUNTANT')
def create_payment():
    """Create new vendor payment"""
    data = request.get_json()
    
    payment = Payment(
        company_id=current_user.company_id,
        vendor_id=data['vendor_id'],
        project_id=data['project_id'],
        purchase_id=data.get('purchase_id'),
        amount=data['amount'],
        payment_date=datetime.fromisoformat(data['payment_date']),
        payment_mode=data['payment_mode']
    )
    
    db.session.add(payment)
    db.session.commit()
    
    return jsonify({'message': 'Payment created', 'payment_id': payment.payment_id}), 201

@api_bp.route('/payments/<int:payment_id>', methods=['DELETE'])
@login_required
@role_required('ADMIN', 'ACCOUNTANT')
def delete_payment(payment_id):
    """Delete payment"""
    payment = company_data_filter(Payment.query).filter_by(payment_id=payment_id).first_or_404()
    db.session.delete(payment)
    db.session.commit()
    return jsonify({'message': 'Payment deleted'})

# ==================== CLIENT PAYMENTS API ====================
@api_bp.route('/client-payments', methods=['GET'])
@login_required
def get_client_payments():
    """Get all client payments"""
    payments = company_data_filter(ClientPayment.query).all()
    return jsonify([{
        'client_payment_id': p.client_payment_id,
        'project_id': p.project_id,
        'project_name': p.project.name,
        'amount': float(p.amount),
        'payment_date': p.payment_date.isoformat(),
        'payment_mode': p.payment_mode,
        'reference_number': p.reference_number,
        'remarks': p.remarks
    } for p in payments])

@api_bp.route('/client-payments', methods=['POST'])
@login_required
@role_required('ADMIN', 'ACCOUNTANT')
def create_client_payment():
    """Create new client payment"""
    data = request.get_json()
    
    payment = ClientPayment(
        company_id=current_user.company_id,
        project_id=data['project_id'],
        amount=data['amount'],
        payment_date=datetime.fromisoformat(data['payment_date']),
        payment_mode=data['payment_mode'],
        reference_number=data.get('reference_number'),
        remarks=data.get('remarks')
    )
    
    db.session.add(payment)
    db.session.commit()
    
    return jsonify({'message': 'Client payment created', 'client_payment_id': payment.client_payment_id}), 201

@api_bp.route('/client-payments/<int:payment_id>', methods=['DELETE'])
@login_required
@role_required('ADMIN', 'ACCOUNTANT')
def delete_client_payment(payment_id):
    """Delete client payment"""
    payment = company_data_filter(ClientPayment.query).filter_by(client_payment_id=payment_id).first_or_404()
    db.session.delete(payment)
    db.session.commit()
    return jsonify({'message': 'Client payment deleted'})
