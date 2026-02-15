from flask import Blueprint, request, jsonify, abort
from flask_login import login_required, current_user
from models import (
    Company, Project, Vendor, Expense, ExpenseItem, Payment, 
    ClientPayment, MasterCategory, SubCategory, db
)
from functools import wraps
from datetime import datetime, date
from sqlalchemy import or_


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

def get_or_404(model, **kwargs):
    """Get object or return 404"""
    # Helper to support filter_by style kwargs
    obj = db.session.query(model).filter_by(**kwargs).first()
    if not obj:
        abort(404)
    return obj

@api_bp.route('/projects', methods=['GET'])
@login_required
def get_projects():
    """Get all projects for user's company"""
    projects = Project.query.filter_by(company_id=current_user.company_id).all()
    return jsonify([{
        'project_id': str(p.project_id),
        'name': p.name,
        'location': p.location,
        'start_date': p.start_date.isoformat() if p.start_date else None,
        'end_date': p.end_date.isoformat() if p.end_date else None,
        'budget': float(p.budget) if p.budget else 0,
        'status': p.status,
        'created_at': p.created_at.isoformat()
    } for p in projects])

@api_bp.route('/projects/<project_id>', methods=['GET'])
@login_required
def get_project(project_id):
    """Get specific project"""
    project = get_or_404(Project, project_id=project_id, company_id=current_user.company_id)
    return jsonify({
        'project_id': str(project.project_id),
        'name': project.name,
        'location': project.location,
        'start_date': project.start_date.isoformat() if project.start_date else None,
        'end_date': project.end_date.isoformat() if project.end_date else None,
        'budget': float(project.budget) if project.budget else 0,
        'status': project.status
    })

@api_bp.route('/projects', methods=['POST'])
@login_required
@role_required('OWNER', 'ADMIN', 'MANAGER')
def create_project():
    """Create new project"""
    data = request.get_json()
    
    project = Project(
        company=current_user.company,
        name=data['name'],
        location=data.get('location'),
        start_date=datetime.fromisoformat(data['start_date']) if data.get('start_date') else None,
        end_date=datetime.fromisoformat(data['end_date']) if data.get('end_date') else None,
        budget=data.get('budget', 0),
        status=data.get('status', 'PLANNING')
    )
    db.session.add(project)
    db.session.commit()
    
    return jsonify({'message': 'Project created', 'project_id': str(project.project_id)}), 201

@api_bp.route('/projects/<project_id>', methods=['PUT'])
@login_required
@role_required('OWNER', 'ADMIN', 'MANAGER')
def update_project(project_id):
    """Update project"""
    project = get_or_404(Project, project_id=project_id, company_id=current_user.company_id)
    data = request.get_json()
    
    project.name = data.get('name', project.name)
    project.location = data.get('location', project.location)
    
    if data.get('start_date'):
        project.start_date = datetime.fromisoformat(data['start_date'])
    
    if data.get('end_date'):
        project.end_date = datetime.fromisoformat(data['end_date'])
        
    project.budget = data.get('budget', project.budget)
    project.status = data.get('status', project.status)
    db.session.commit()
    
    return jsonify({'message': 'Project updated'})

@api_bp.route('/projects/<project_id>', methods=['DELETE'])
@login_required
@role_required('OWNER', 'ADMIN')
def delete_project(project_id):
    """Delete project"""
    project = get_or_404(Project, project_id=project_id, company_id=current_user.company_id)
    db.session.delete(project)
    db.session.commit()
    return jsonify({'message': 'Project deleted'})

# ==================== VENDORS API ====================
@api_bp.route('/vendors', methods=['GET'])
@login_required
def get_vendors():
    """Get all vendors"""
    vendors = Vendor.query.filter_by(company_id=current_user.company_id).all()
    return jsonify([{
        'vendor_id': str(v.vendor_id),
        'name': v.name,
        'phone': v.phone,
        'email': v.email,
        'gst_number': v.gst_number,
        'created_at': v.created_at.isoformat()
    } for v in vendors])

@api_bp.route('/vendors', methods=['POST'])
@login_required
@role_required('OWNER', 'ADMIN', 'MANAGER')
def create_vendor():
    """Create new vendor"""
    data = request.get_json()
    
    vendor = Vendor(
        company=current_user.company,
        name=data['name'],
        phone=data.get('phone'),
        email=data.get('email'),
        gst_number=data.get('gst_number')
    )
    db.session.add(vendor)
    db.session.commit()
    
    return jsonify({'message': 'Vendor created', 'vendor_id': str(vendor.vendor_id)}), 201

@api_bp.route('/vendors/<vendor_id>', methods=['PUT'])
@login_required
@role_required('OWNER', 'ADMIN', 'MANAGER')
def update_vendor(vendor_id):
    """Update vendor"""
    vendor = get_or_404(Vendor, vendor_id=vendor_id, company_id=current_user.company_id)
    data = request.get_json()
    
    vendor.name = data.get('name', vendor.name)
    vendor.phone = data.get('phone', vendor.phone)
    vendor.email = data.get('email', vendor.email)
    vendor.gst_number = data.get('gst_number', vendor.gst_number)
    db.session.commit()
    
    return jsonify({'message': 'Vendor updated'})

@api_bp.route('/vendors/<vendor_id>', methods=['DELETE'])
@login_required
@role_required('OWNER', 'ADMIN')
def delete_vendor(vendor_id):
    """Delete vendor"""
    vendor = get_or_404(Vendor, vendor_id=vendor_id, company_id=current_user.company_id)
    db.session.delete(vendor)
    db.session.commit()
    return jsonify({'message': 'Vendor deleted'})

# ==================== PURCHASES API (Mapped to Expenses) ====================
@api_bp.route('/purchases', methods=['GET'])
@login_required
def get_purchases():
    """Get all purchases (Expenses with expense_type 'Material Purchase')"""
    purchases = Expense.query.filter_by(company_id=current_user.company_id, expense_type='Material Purchase').order_by(Expense.expense_date.desc()).all()
    return jsonify([{
        'purchase_id': str(p.expense_id),
        'expense_id': str(p.expense_id),
        'project_id': str(p.project_id),
        'project_name': p.project.name,
        'vendor_id': str(p.vendor_id) if p.vendor_id else None,
        'vendor_name': p.vendor.name if p.vendor else None,
        'invoice_number': p.invoice_number,
        'invoice_date': p.expense_date.isoformat(),
        'total_amount': float(p.amount),
        'payment_type': p.payment_mode,
        'category': p.category, # This is the Material Category (e.g. Cement)
        'items': [{
            'item_name': item.item_name,
            'quantity': float(item.quantity),
            'measuring_unit': item.measuring_unit,
            'unit_price': float(item.unit_price),
            'total_price': float(item.total_price)
        } for item in p.items]
    } for p in purchases])

@api_bp.route('/purchases', methods=['POST'])
@login_required
@role_required('OWNER', 'ADMIN', 'MANAGER')
def create_purchase():
    """Create new purchase (Expense + Items)"""
    data = request.get_json()
    
    # Get project and vendor references - Verify existence
    project = get_or_404(Project, project_id=data['project_id'])
    vendor = None
    if data.get('vendor_id'):
        vendor = get_or_404(Vendor, vendor_id=data['vendor_id'])
    
    # Create embedded items
    items = [
        ExpenseItem(
            item_name=item_data['item_name'],
            quantity=item_data['quantity'],
            measuring_unit=item_data.get('measuring_unit', 'Unit'),
            unit_price=item_data['unit_price'],
            total_price=item_data['total_price']
        )
        for item_data in data.get('items', [])
    ]
    
    purchase = Expense(
        company=current_user.company,
        project=project,
        vendor=vendor,
        expense_type='Material Purchase',
        category=data.get('category'), # Material Category (e.g. Cement)
        invoice_number=data.get('invoice_number'),
        expense_date=datetime.fromisoformat(data['invoice_date']),
        amount=data['total_amount'],
        payment_mode=data['payment_type'],
        description=f"Invoice #{data.get('invoice_number')}",
        items=items
    )
    db.session.add(purchase)
    db.session.commit()
    
    return jsonify({'message': 'Purchase created', 'purchase_id': str(purchase.expense_id)}), 201

@api_bp.route('/purchases/<purchase_id>', methods=['DELETE'])
@login_required
@role_required('OWNER', 'ADMIN')
def delete_purchase(purchase_id):
    """Delete purchase (Expense)"""
    purchase = get_or_404(Expense, expense_id=purchase_id, company_id=current_user.company_id, expense_type='Material Purchase')
    db.session.delete(purchase)
    db.session.commit()
    return jsonify({'message': 'Purchase deleted'})

# ==================== EXPENSES API ====================
@api_bp.route('/expenses', methods=['GET'])
@login_required
def get_expenses():
    """Get all regular expenses"""
    expenses = Expense.query.filter_by(company_id=current_user.company_id, expense_type='Regular Expense').order_by(Expense.expense_date.desc()).all()
    return jsonify([{
        'expense_id': str(e.expense_id),
        'project_id': str(e.project.project_id),
        'project_name': e.project.name,
        'category': e.category, # e.g. Travel
        'amount': float(e.amount),
        'payment_mode': e.payment_mode,
        'expense_date': e.expense_date.isoformat(),
        'description': e.description,
        'bill_url': e.bill_url
    } for e in expenses])

@api_bp.route('/expenses', methods=['POST'])
@login_required
@role_required('OWNER', 'ADMIN', 'ACCOUNTANT')
def create_expense():
    """Create new regular expense"""
    data = request.get_json()
    
    project = get_or_404(Project, project_id=data['project_id'])
    
    expense = Expense(
        company=current_user.company,
        project=project,
        expense_type='Regular Expense',
        category=data.get('category'), # e.g. Travel
        amount=data['amount'],
        payment_mode=data['payment_mode'],
        expense_date=datetime.fromisoformat(data['expense_date']),
        description=data.get('description'),
        bill_url=data.get('bill_url')
    )
    db.session.add(expense)
    db.session.commit()
    
    return jsonify({'message': 'Expense created', 'expense_id': str(expense.expense_id)}), 201

@api_bp.route('/expenses/<expense_id>', methods=['GET'])
@login_required
def get_expense(expense_id):
    """Get specific expense by ID"""
    expense = get_or_404(Expense, expense_id=expense_id, company_id=current_user.company_id)
    
    result = {
        'expense_id': str(expense.expense_id),
        'project_id': str(expense.project_id),
        'vendor_id': str(expense.vendor_id) if expense.vendor_id else None,
        'expense_type': expense.expense_type,
        'category': expense.category,
        'amount': float(expense.amount),
        'payment_mode': expense.payment_mode,
        'expense_date': expense.expense_date.isoformat(),
        'description': expense.description,
        'invoice_number': expense.invoice_number,
        'bill_url': expense.bill_url
    }
    
    if expense.expense_type == 'Material Purchase':
        result['items'] = [{
            'item_name': item.item_name,
            'quantity': float(item.quantity),
            'measuring_unit': item.measuring_unit,
            'unit_price': float(item.unit_price),
            'total_price': float(item.total_price)
        } for item in expense.items]
    
    return jsonify(result)

@api_bp.route('/expenses/<expense_id>', methods=['PUT'])
@login_required
@role_required('OWNER', 'ADMIN', 'ACCOUNTANT', 'MANAGER')
def update_expense(expense_id):
    """Update expense (works for both purchases and regular expenses)"""
    expense = get_or_404(Expense, expense_id=expense_id, company_id=current_user.company_id)
    data = request.get_json()
    
    if data.get('project_id'):
        expense.project = get_or_404(Project, project_id=data['project_id'])
    if data.get('vendor_id'):
        expense.vendor = get_or_404(Vendor, vendor_id=data['vendor_id'])
    
    
    expense.category = data.get('category', expense.category)
    expense.amount = data.get('total_amount', data.get('amount', expense.amount))
    expense.payment_mode = data.get('payment_type', data.get('payment_mode', expense.payment_mode))
    
    if data.get('invoice_date') or data.get('expense_date'):
        date_str = data.get('invoice_date', data.get('expense_date'))
        expense.expense_date = datetime.fromisoformat(date_str)
    
    expense.invoice_number = data.get('invoice_number', expense.invoice_number)
    expense.description = data.get('description', expense.description)
    
    # Update items if provided (for purchases)
    if 'items' in data and expense.expense_type == 'Material Purchase':
        expense.items = [
            ExpenseItem(
                item_name=item_data['item_name'],
                quantity=item_data['quantity'],
                measuring_unit=item_data.get('measuring_unit', 'Unit'),
                unit_price=item_data['unit_price'],
                total_price=item_data['total_price']
            )
            for item_data in data['items']
        ]
    
    db.session.commit()
    return jsonify({'message': 'Expense updated'})

@api_bp.route('/expenses/<expense_id>', methods=['DELETE'])
@login_required
@role_required('OWNER', 'ADMIN', 'ACCOUNTANT')
def delete_expense(expense_id):
    """Delete expense"""
    expense = get_or_404(Expense, expense_id=expense_id, company_id=current_user.company_id)
    db.session.delete(expense)
    db.session.commit()
    return jsonify({'message': 'Expense deleted'})

# ==================== PAYMENTS API ====================
@api_bp.route('/payments', methods=['GET'])
@login_required
def get_payments():
    """Get all vendor payments"""
    payments = Payment.query.filter_by(company_id=current_user.company_id).order_by(Payment.payment_date.desc()).all()
    return jsonify([{
        'payment_id': str(p.payment_id),
        'vendor_id': str(p.vendor_id),
        'vendor_name': p.vendor.name,
        'project_id': str(p.project_id),
        'project_name': p.project.name,
        'purchase_id': str(p.expense_id) if p.expense_id else None,
        'expense_id': str(p.expense_id) if p.expense_id else None,
        'amount': float(p.amount),
        'payment_date': p.payment_date.isoformat(),
        'payment_mode': p.payment_mode
    } for p in payments])

@api_bp.route('/payments', methods=['POST'])
@login_required
@role_required('OWNER', 'ADMIN', 'ACCOUNTANT')
def create_payment():
    """Create new vendor payment"""
    data = request.get_json()
    
    vendor = get_or_404(Vendor, vendor_id=data['vendor_id'])
    project = get_or_404(Project, project_id=data['project_id'])
    expense = get_or_404(Expense, expense_id=data['purchase_id']) if data.get('purchase_id') else None
    
    payment = Payment(
        company=current_user.company,
        vendor=vendor,
        project=project,
        expense=expense,
        amount=data['amount'],
        payment_date=datetime.fromisoformat(data['payment_date']),
        payment_mode=data['payment_mode']
    )
    db.session.add(payment)
    db.session.commit()
    
    return jsonify({'message': 'Payment created', 'payment_id': str(payment.payment_id)}), 201

@api_bp.route('/payments/<payment_id>', methods=['DELETE'])
@login_required
@role_required('OWNER', 'ADMIN', 'ACCOUNTANT')
def delete_payment(payment_id):
    """Delete payment"""
    payment = get_or_404(Payment, payment_id=payment_id, company_id=current_user.company_id)
    db.session.delete(payment)
    db.session.commit()
    return jsonify({'message': 'Payment deleted'})

@api_bp.route('/projects/<project_id>/payments', methods=['GET'])
@login_required
def get_project_payments(project_id):
    """Get payments for a specific project"""
    project = get_or_404(Project, project_id=project_id, company_id=current_user.company_id)
        
    payments = Payment.query.filter_by(project_id=project.project_id).order_by(Payment.payment_date.desc()).all()
    
    return jsonify([{
        'payment_id': str(p.payment_id),
        'vendor_name': p.vendor.name if p.vendor else 'Unknown',
        'amount': float(p.amount),
        'payment_date': p.payment_date.isoformat(),
        'payment_mode': p.payment_mode
    } for p in payments])

@api_bp.route('/master/categories', methods=['GET'])
@login_required
def get_master_categories():
    """Get all master categories"""
    type_filter = request.args.get('type')
    
    query = MasterCategory.query.filter_by(is_active=True)
    if type_filter:
        query = query.filter_by(type=type_filter)
        
    categories = query.order_by(MasterCategory.name).all()
    
    return jsonify([{
        'id': str(c.category_id),
        'name': c.name,
        'type': c.type,
        'subcategories': [{
            'name': s.name,
            'default_unit': s.default_unit
        } for s in c.subcategories]
    } for c in categories])

@api_bp.route('/master/categories', methods=['POST'])
@login_required
@role_required('OWNER', 'ADMIN')
def create_master_category():
    """Create new master category"""
    data = request.get_json()
    
    if db.session.query(MasterCategory).filter_by(name=data['name']).first():
        return jsonify({'error': 'Category already exists'}), 400
        
    category = MasterCategory(
        name=data['name'],
        type=data['type'],
        is_active=True
    )
    
    for item in data.get('subcategories', []):
        sub = SubCategory(name=item['name'], default_unit=item.get('default_unit'))
        category.subcategories.append(sub)
        
    db.session.add(category)
    db.session.commit()
    
    return jsonify({'message': 'Category created', 'id': str(category.category_id)}), 201

@api_bp.route('/master/categories/<category_id>', methods=['PUT'])
@login_required
@role_required('OWNER', 'ADMIN')
def update_master_category(category_id):
    """Update master category"""
    category = get_or_404(MasterCategory, category_id=category_id)
    data = request.get_json()
    
    category.name = data.get('name', category.name)
    category.type = data.get('type', category.type)
    
    if 'subcategories' in data:
        # Easy way: clear and re-add. 
        # Better way: update in place, but for now this is safe as IDs of subcategories aren't critical references elsewhere yet.
        # Actually, they might be if we reference subcategories? No, Expense just stores string subcategory.
        category.subcategories = []
        for item in data['subcategories']:
            sub = SubCategory(name=item['name'], default_unit=item.get('default_unit'))
            category.subcategories.append(sub)
        
    db.session.commit()
    return jsonify({'message': 'Category updated'})

@api_bp.route('/master/categories/<category_id>', methods=['DELETE'])
@login_required
@role_required('OWNER', 'ADMIN')
def delete_master_category(category_id):
    """Delete master category"""
    category = get_or_404(MasterCategory, category_id=category_id)
    db.session.delete(category)
    db.session.commit()
    return jsonify({'message': 'Category deleted'})

# ==================== CLIENT PAYMENTS API ====================
@api_bp.route('/client-payments', methods=['GET'])
@login_required
def get_client_payments():
    """Get all client payments"""
    payments = ClientPayment.query.filter_by(company_id=current_user.company_id).order_by(ClientPayment.payment_date.desc()).all()
    return jsonify([{
        'client_payment_id': str(p.client_payment_id),
        'project_id': str(p.project_id),
        'project_name': p.project.name,
        'amount': float(p.amount),
        'payment_date': p.payment_date.isoformat(),
        'payment_mode': p.payment_mode,
        'reference_number': p.reference_number,
        'remarks': p.remarks
    } for p in payments])

@api_bp.route('/client-payments', methods=['POST'])
@login_required
@role_required('OWNER', 'ADMIN', 'ACCOUNTANT')
def create_client_payment():
    """Create new client payment"""
    data = request.get_json()
    
    project = get_or_404(Project, project_id=data['project_id'])
    
    payment = ClientPayment(
        company=current_user.company,
        project=project,
        amount=data['amount'],
        payment_date=datetime.fromisoformat(data['payment_date']),
        payment_mode=data['payment_mode'],
        reference_number=data.get('reference_number'),
        remarks=data.get('remarks')
    )
    db.session.add(payment)
    db.session.commit()
    
    return jsonify({'message': 'Client payment created', 'client_payment_id': str(payment.client_payment_id)}), 201

@api_bp.route('/client-payments/<payment_id>', methods=['DELETE'])
@login_required
@role_required('OWNER', 'ADMIN', 'ACCOUNTANT')
def delete_client_payment(payment_id):
    """Delete client payment"""
    payment = get_or_404(ClientPayment, client_payment_id=payment_id, company_id=current_user.company_id)
    db.session.delete(payment)
    db.session.commit()
    return jsonify({'message': 'Client payment deleted'})
