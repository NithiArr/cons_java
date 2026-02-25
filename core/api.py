from django.http import JsonResponse
from datetime import datetime
from django.db.models import Sum, Count
from django.contrib.auth.decorators import login_required
from .models import Project, Vendor, MasterCategory
from finance.models import Expense, Payment, ClientPayment, ExpenseItem

from decimal import Decimal

@login_required
def owner_kpis(request):
    status_filter = request.GET.get('status')
    
    projects_query = Project.objects.all()
    if status_filter:
        projects_query = projects_query.filter(status=status_filter)
        
    total_projects = projects_query.count()
    
    # Filter related objects based on filtered projects
    # We need to filter Expenses, Payments, ClientPayments by the filtered projects
    
    total_budget = projects_query.aggregate(Sum('budget'))['budget__sum'] or Decimal('0')
    
    # Calculate stats for filtered projects
    # Note: Global expenses/payments (not linked to project) are ignored if we limit to projects
    # But current models enforce project link.
    
    expenses_query = Expense.objects.filter(project__in=projects_query)
    payments_query = Payment.objects.filter(project__in=projects_query)
    client_payments_query = ClientPayment.objects.filter(project__in=projects_query)
    
    # Calculate total spent (Expenses)
    total_spent = expenses_query.aggregate(Sum('amount'))['amount__sum'] or Decimal('0')
    
    # Calculate breakdown of expenses
    expenses_credit = expenses_query.filter(payment_mode='CREDIT').aggregate(Sum('amount'))['amount__sum'] or Decimal('0')
    expenses_paid = total_spent - expenses_credit
    
    # Calculate vendor payments
    total_payments = payments_query.aggregate(Sum('amount'))['amount__sum'] or Decimal('0')
    
    # FIXED: Outstanding Payables = Credit Purchases - Vendor Payments (not Total Expenses - Payments)
    vendor_outstanding = expenses_credit - total_payments
    if vendor_outstanding < 0: vendor_outstanding = Decimal('0')
    
    # Status breakdown - Always show full breakdown or just filtered? 
    # Usually breakdown shows all, but if we filter by status, it might look weird. 
    # Let's keep status breakdown for ALL projects regardless of filter, so user sees distribution
    status_counts = Project.objects.values('status').annotate(count=Count('status'))
    status_breakdown = {item['status']: item['count'] for item in status_counts}
    
    # Calculate receipts
    total_received = client_payments_query.aggregate(Sum('amount'))['amount__sum'] or Decimal('0')
    
    # Balance in Hand
    # Paid Outflows = Expenses(Cash/Bank/UPI) + VendorPayments
    # expenses_paid is Cash/Bank/UPI
    total_outflow = expenses_paid + total_payments
    balance_in_hand = total_received - total_outflow
    
    data = {
        'total_projects': total_projects,
        'total_budget': float(total_budget),
        'total_spent': float(total_spent),
        'total_received': float(total_received),
        'expenses_paid': float(expenses_paid),
        'expenses_credit': float(expenses_credit),
        'vendor_outstanding': float(vendor_outstanding),
        'balance_in_hand': float(balance_in_hand),
        'status_breakdown': status_breakdown
    }
    return JsonResponse(data)

@login_required
def project_financial_table(request):
    projects = Project.objects.all()
    data = []
    
    for p in projects:
        # Calculate financials per project
        budget = p.budget or Decimal('0')
        spent = p.expenses.aggregate(Sum('amount'))['amount__sum'] or Decimal('0')
        received = p.client_payments.aggregate(Sum('amount'))['amount__sum'] or Decimal('0')
        
        # Outstanding (Project Expenses - Project Payments)
        project_payments = p.payments.aggregate(Sum('amount'))['amount__sum'] or Decimal('0')
        outstanding = spent - project_payments
        
        balance = budget - spent
        
        indicator = 'on_track'
        if balance < 0:
            indicator = 'over'
        elif balance < (budget * Decimal('0.1')): # Less than 10% remaining
            indicator = 'near'
            
        data.append({
            'project_name': p.name,
            'status': p.status,
            'budget': float(budget),
            'amount_spent': float(spent),
            'amount_received': float(received),
            'vendor_outstanding': float(outstanding),
            'balance_budget': float(balance),
            'indicator': indicator
        })
        
    return JsonResponse(data, safe=False)

@login_required
def master_categories_list(request):
    if request.method == 'POST':
        return create_master_category(request)
    return get_master_categories(request)

@login_required
def master_categories_detail(request, category_id):
    if request.method == 'PUT':
        return update_master_category(request, category_id)
    elif request.method == 'DELETE':
        return delete_master_category(request, category_id)
    return JsonResponse({'error': 'Method not allowed'}, status=405)

def get_master_categories(request):
    """Get all master categories"""
    type_filter = request.GET.get('type')
    
    query = MasterCategory.objects.filter(is_active=True)
    if type_filter:
        query = query.filter(type=type_filter)
        
    categories = query.order_by('name')
    
    data = []
    for c in categories:
        data.append({
            'id': str(c.category_id),
            'name': c.name,
            'type': c.type,
            'subcategories': [{
                'name': s.name,
                'default_unit': s.default_unit
            } for s in c.subcategories.all()]
        })
    
    return JsonResponse(data, safe=False)

def create_master_category(request):
    import json
    from django.db import IntegrityError, transaction
    
    try:
        data = json.loads(request.body)
        
        if not data.get('name'):
            return JsonResponse({'error': 'Category name is required'}, status=400)
            
        if MasterCategory.objects.filter(name=data['name']).exists():
            return JsonResponse({'error': 'Category with this name already exists'}, status=400)
            
        with transaction.atomic():
            category = MasterCategory.objects.create(
                name=data['name'],
                type=data.get('type', 'MATERIAL'),
                is_active=True
            )
            
            from .models import SubCategory
            for item in data.get('subcategories', []):
                if item.get('name'):
                    SubCategory.objects.create(
                        parent_category=category,
                        name=item['name'],
                        default_unit=item.get('default_unit')
                    )
                
        return JsonResponse({'message': 'Category created', 'id': str(category.category_id)}, status=201)
    except json.JSONDecodeError:
        return JsonResponse({'error': 'Invalid JSON data'}, status=400)
    except IntegrityError as e:
        return JsonResponse({'error': f'Database error: {str(e)}'}, status=400)
    except Exception as e:
        return JsonResponse({'error': str(e)}, status=500)

def update_master_category(request, category_id):
    import json
    from django.db import IntegrityError, transaction
    
    try:
        category = MasterCategory.objects.get(category_id=category_id)
        data = json.loads(request.body)
        
        new_name = data.get('name', category.name)
        if new_name != category.name and MasterCategory.objects.filter(name=new_name).exists():
            return JsonResponse({'error': 'Another category with this name already exists'}, status=400)

        with transaction.atomic():
            category.name = new_name
            category.type = data.get('type', category.type)
            category.save()
            
            if 'subcategories' in data:
                # Clear existing
                category.subcategories.all().delete()
                from .models import SubCategory
                for item in data['subcategories']:
                    if item.get('name'):
                        SubCategory.objects.create(
                            parent_category=category,
                            name=item['name'],
                            default_unit=item.get('default_unit')
                        )
                    
        return JsonResponse({'message': 'Category updated'})
    except MasterCategory.DoesNotExist:
        return JsonResponse({'error': 'Category not found'}, status=404)
    except json.JSONDecodeError:
        return JsonResponse({'error': 'Invalid JSON data'}, status=400)
    except IntegrityError as e:
        return JsonResponse({'error': f'Database error: {str(e)}'}, status=400)
    except Exception as e:
        return JsonResponse({'error': str(e)}, status=500)

def delete_master_category(request, category_id):
    try:
        category = MasterCategory.objects.get(category_id=category_id)
        category.delete()
        return JsonResponse({'message': 'Category deleted'})
    except MasterCategory.DoesNotExist:
        return JsonResponse({'error': 'Category not found'}, status=404)

# ================= PROEJCTS API =================

@login_required
def projects_list(request):
    if request.method == 'POST':
        return create_project(request)
    return get_projects(request)

@login_required
def project_detail(request, project_id):
    if request.method == 'GET':
        return get_project(request, project_id)
    elif request.method == 'PUT':
        return update_project(request, project_id)
    elif request.method == 'DELETE':
        return delete_project(request, project_id)
    return JsonResponse({'error': 'Method not allowed'}, status=405)

def get_projects(request):
    projects = Project.objects.filter(company=request.user.company).order_by('-created_at')
    data = [{
        'project_id': str(p.project_id),
        'name': p.name,
        'location': p.location,
        'start_date': p.start_date.isoformat() if p.start_date else None,
        'end_date': p.end_date.isoformat() if p.end_date else None,
        'budget': float(p.budget) if p.budget else 0,
        'status': p.status,
        'created_at': p.created_at.isoformat()
    } for p in projects]
    return JsonResponse(data, safe=False)

def get_project(request, project_id):
    try:
        project = Project.objects.get(project_id=project_id, company=request.user.company)
        return JsonResponse({
            'project_id': str(project.project_id),
            'name': project.name,
            'location': project.location,
            'start_date': project.start_date.isoformat() if project.start_date else None,
            'end_date': project.end_date.isoformat() if project.end_date else None,
            'budget': float(project.budget) if project.budget else 0,
            'status': project.status
        })
    except Project.DoesNotExist:
        return JsonResponse({'error': 'Project not found'}, status=404)

def create_project(request):
    import json
    data = json.loads(request.body)
    
    # Parse dates
    start_date = None
    if data.get('start_date'):
        start_date = datetime.fromisoformat(data['start_date']).date()
    
    end_date = None
    if data.get('end_date'):
        end_date = datetime.fromisoformat(data['end_date']).date()

    project = Project.objects.create(
        company=request.user.company,
        name=data['name'],
        location=data.get('location'),
        start_date=start_date,
        end_date=end_date,
        budget=data.get('budget', 0),
        status=data.get('status', 'PLANNING')
    )
    return JsonResponse({'message': 'Project created', 'project_id': str(project.project_id)}, status=201)

def update_project(request, project_id):
    import json
    try:
        project = Project.objects.get(project_id=project_id, company=request.user.company)
    except Project.DoesNotExist:
        return JsonResponse({'error': 'Project not found'}, status=404)

    data = json.loads(request.body)
    
    project.name = data.get('name', project.name)
    project.location = data.get('location', project.location)
    
    if data.get('start_date'):
        project.start_date = datetime.fromisoformat(data['start_date']).date()
    if data.get('end_date'):
        project.end_date = datetime.fromisoformat(data['end_date']).date()
        
    project.budget = data.get('budget', project.budget)
    project.status = data.get('status', project.status)
    project.save()
    
    return JsonResponse({'message': 'Project updated'})

def delete_project(request, project_id):
    try:
        project = Project.objects.get(project_id=project_id, company=request.user.company)
        project.delete()
        pattern = r"Project deleted"
        return JsonResponse({'message': 'Project deleted'})
    except Project.DoesNotExist:
        return JsonResponse({'error': 'Project not found'}, status=404)

@login_required
def get_project_payment_details(request, project_id):
    try:
        project = Project.objects.get(project_id=project_id, company=request.user.company)
    except Project.DoesNotExist:
        return JsonResponse({'error': 'Project not found'}, status=404)
        
    expenses = Expense.objects.filter(project=project).order_by('-expense_date')
    payments = Payment.objects.filter(project=project).order_by('-payment_date')
    client_payments = ClientPayment.objects.filter(project=project).order_by('-payment_date')
    
    purchase_history = []
    regular_expenses = []
    expenses_list = []
    
    total_purchases_amount = 0
    total_expenses_amount = 0
    
    # Process Expenses
    for expense in expenses:
        if expense.expense_type == 'Material Purchase':
            # Calculate payments for this expense
            expense_payments = Payment.objects.filter(expense=expense, project=project)
            payment_sum = expense_payments.aggregate(Sum('amount'))['amount__sum'] or 0
            
            if expense.payment_mode in ['CASH', 'UPI', 'BANK']:
                displayed_paid = expense.amount
                displayed_balance = 0
            else:
                displayed_paid = payment_sum
                displayed_balance = expense.amount - payment_sum
                
            purchase_history.append({
                'purchase_id': str(expense.expense_id),
                'vendor_name': expense.vendor.name if expense.vendor else 'Unknown',
                'invoice_number': expense.invoice_number or '-',
                'invoice_date': expense.expense_date.isoformat(),
                'category': expense.category,
                'amount': float(expense.amount),
                'payment_mode': expense.payment_mode,
                'paid': float(displayed_paid),
                'balance': float(displayed_balance)
            })
            total_purchases_amount += expense.amount
        else:
            # Regular Expense: Subcategory is stored in ExpenseItem line
            first_item = expense.items.first()
            subcategory = first_item.item_name if first_item else ""
            
            expenses_list.append({
                'expense_id': str(expense.expense_id),
                'expense_type': expense.expense_type,
                'expense_date': expense.expense_date.isoformat(),
                'category': expense.category,
                'subcategory': subcategory,
                'amount': float(expense.amount),
                'payment_mode': expense.payment_mode,
                'description': expense.description
            })
            total_expenses_amount += expense.amount

    # Get expense items for material purchases (for subcategory breakdown)
    expense_items_list = []
    for expense in expenses.filter(expense_type='Material Purchase'):
        items = ExpenseItem.objects.filter(expense=expense)
        for item in items:
            expense_items_list.append({
                'item_id': str(item.expense_item_id),
                'expense_id': str(expense.expense_id),
                'category': expense.category,  # Category from parent expense
                'subcategory': item.item_name,  # Using item_name as subcategory
                'quantity': float(item.quantity),
                'unit_price': float(item.unit_price),
                'total_price': float(item.total_price),
                'measuring_unit': item.measuring_unit
            })

    # Vendor Payments
    vendor_payments_list = []
    for p in payments:
        vendor_payments_list.append({
            'payment_id': str(p.payment_id),
            'payment_date': p.payment_date.isoformat(),
            'vendor_name': p.vendor.name if p.vendor else 'Unknown',
            'amount': float(p.amount),
            'payment_mode': p.payment_mode,
            'purchase_invoice': p.expense.invoice_number if p.expense else '-'
        })

    # Client Payments
    client_payments_list = []
    for cp in client_payments:
        client_payments_list.append({
            'client_payment_id': str(cp.client_payment_id),
            'payment_date': cp.payment_date.isoformat(),
            'amount': float(cp.amount),
            'payment_mode': cp.payment_mode,
            'reference_number': cp.reference_number,
            'remarks': cp.remarks
        })
        
    # Financial Summary
    total_spent = total_purchases_amount + total_expenses_amount
    total_received = client_payments.aggregate(Sum('amount'))['amount__sum'] or 0
    total_vendor_payments = payments.aggregate(Sum('amount'))['amount__sum'] or 0
    
    # Credit purchases: Type=Material AND Mode=CREDIT
    credit_purchases = expenses.filter(expense_type='Material Purchase', payment_mode='CREDIT').aggregate(Sum('amount'))['amount__sum'] or 0
    
    vendor_outstanding = credit_purchases - total_vendor_payments
    if vendor_outstanding < 0: vendor_outstanding = 0
    
    client_outstanding = total_spent - total_received
    
    return JsonResponse({
        'project_name': project.name,
        'project_status': project.status,
        'purchase_history': purchase_history,
        'vendor_payments': vendor_payments_list,
        'expenses': expenses_list,
        'expense_items': expense_items_list,  # Added for material breakdown chart
        'client_payments': client_payments_list,
        'financial_summary': {
            'total_purchases': float(total_purchases_amount),
            'total_expenses': float(total_expenses_amount),
            'total_spent': float(total_spent),
            'total_received': float(total_received),
            'vendor_outstanding': float(vendor_outstanding),
            'client_outstanding': float(client_outstanding),
            'total_vendor_payments': float(total_vendor_payments),
            'remaining_budget': float(project.budget - total_spent) if project.budget else 0
        }
    })

# ================= VENDORS API =================

@login_required
def vendors_list(request):
    if request.method == 'POST':
        return create_vendor(request)
    return get_vendors(request)

@login_required
def vendor_detail(request, vendor_id):
    if request.method == 'PUT':
        return update_vendor(request, vendor_id)
    elif request.method == 'DELETE':
        return delete_vendor(request, vendor_id)
    return JsonResponse({'error': 'Method not allowed'}, status=405)

def get_vendors(request):
    vendors = Vendor.objects.filter(company=request.user.company).order_by('name')
    data = [{
        'vendor_id': str(v.vendor_id),
        'name': v.name,
        'phone': v.phone,
        'email': v.email,
        'gst_number': v.gst_number,
        'created_at': v.created_at.isoformat()
    } for v in vendors]
    return JsonResponse(data, safe=False)

def create_vendor(request):
    import json
    data = json.loads(request.body)
    
    vendor = Vendor.objects.create(
        company=request.user.company,
        name=data['name'],
        phone=data.get('phone'),
        email=data.get('email'),
        gst_number=data.get('gst_number')
    )
    return JsonResponse({'message': 'Vendor created', 'vendor_id': str(vendor.vendor_id)}, status=201)

def update_vendor(request, vendor_id):
    import json
    try:
        vendor = Vendor.objects.get(vendor_id=vendor_id, company=request.user.company)
    except Vendor.DoesNotExist:
        return JsonResponse({'error': 'Vendor not found'}, status=404)

    data = json.loads(request.body)
    
    vendor.name = data.get('name', vendor.name)
    vendor.phone = data.get('phone', vendor.phone)
    vendor.email = data.get('email', vendor.email)
    vendor.gst_number = data.get('gst_number', vendor.gst_number)
    vendor.save()
    
    return JsonResponse({'message': 'Vendor updated'})

def delete_vendor(request, vendor_id):
    try:
        vendor = Vendor.objects.get(vendor_id=vendor_id, company=request.user.company)
        vendor.delete()
        return JsonResponse({'message': 'Vendor deleted'})
    except Vendor.DoesNotExist:
        return JsonResponse({'error': 'Vendor not found'}, status=404)

@login_required
def get_vendor_material_summary(request, vendor_id):
    try:
        vendor = Vendor.objects.get(vendor_id=vendor_id, company=request.user.company)
    except Vendor.DoesNotExist:
        return JsonResponse({'error': 'Vendor not found'}, status=404)

    project_filter = request.GET.get('project')
    
    query = Expense.objects.filter(vendor=vendor, expense_type='Material Purchase')
    
    if project_filter:
        query = query.filter(project__name=project_filter)
            
    expenses = query.order_by('-expense_date').all()
            
    material_map = {}
    
    for expense in expenses:
        # Group by Category (Material Category)
        mat = expense.category or 'Uncategorized'
        if mat not in material_map:
            material_map[mat] = {'quantity': 0, 'amount': 0, 'purchases': []}
        
        material_map[mat]['amount'] += float(expense.amount)
        
        # Sum quantities from items and collect purchase details
        for item in expense.items.all():
            material_map[mat]['quantity'] += float(item.quantity)
            material_map[mat]['purchases'].append({
                'project': expense.project.name,
                'date': expense.expense_date.isoformat(),
                'invoice_number': expense.invoice_number or '-',
                'item_name': item.item_name,
                'quantity': float(item.quantity),
                'unit': item.measuring_unit,
                'unit_price': float(item.unit_price),
                'total_price': float(item.total_price),
                'payment_mode': expense.payment_mode
            })
            
    result = []
    for mat, data in material_map.items():
        result.append({
            'material': mat,
            'total_quantity': float(data['quantity']),
            'total_amount': float(data['amount']),
            'purchases': data['purchases']  # Added purchase details
        })
        
    return JsonResponse(result, safe=False)

@login_required
def get_vendor_summary(request):
    vendors = Vendor.objects.filter(company=request.user.company)
    data = []
    
    for vendor in vendors:
        expenses = Expense.objects.filter(vendor=vendor)
        payments = Payment.objects.filter(vendor=vendor)
        
        total_purchases = expenses.aggregate(Sum('amount'))['amount__sum'] or 0
        total_paid = payments.aggregate(Sum('amount'))['amount__sum'] or 0
        
        # CREDIT purchases
        credit_purchases = expenses.filter(payment_mode='CREDIT').aggregate(Sum('amount'))['amount__sum'] or 0
        
        # Outstanding
        outstanding = credit_purchases - total_paid
        
        data.append({
            'vendor_id': str(vendor.vendor_id),
            'vendor_name': vendor.name,
            'total_purchases': float(total_purchases),
            'total_paid': float(total_paid),
            'outstanding': float(outstanding)
        })
        
    return JsonResponse(data, safe=False)

@login_required
def get_vendor_purchase_history(request, vendor_id):
    try:
        vendor = Vendor.objects.get(vendor_id=vendor_id, company=request.user.company)
    except Vendor.DoesNotExist:
        return JsonResponse({'error': 'Vendor not found'}, status=404)
        
    expenses = Expense.objects.filter(vendor=vendor).order_by('-expense_date')
    data = []
    
    for expense in expenses:
        # Calculate paid amount for this specific expense
        if expense.payment_mode in ['CASH', 'UPI', 'BANK']:
            paid = expense.amount
            balance = 0
        else:
            # For CREDIT, we need to approximate based on total payments to vendor for this project
            project_payments = Payment.objects.filter(vendor=vendor, project=expense.project)
            total_project_payments = project_payments.aggregate(Sum('amount'))['amount__sum'] or 0
            
            project_expenses = Expense.objects.filter(vendor=vendor, project=expense.project, payment_mode='CREDIT')
            total_project_credit = project_expenses.aggregate(Sum('amount'))['amount__sum'] or 0
            
            if total_project_credit > 0:
                proportion = float(expense.amount) / float(total_project_credit)
                paid = float(total_project_payments) * proportion
            else:
                paid = 0
                
            balance = float(expense.amount) - paid
        
        # Get items for this expense
        items = []
        for item in expense.items.all():
            items.append({
                'category': expense.category,  # Parent expense category
                'item_name': item.item_name,
                'quantity': float(item.quantity),
                'unit': item.measuring_unit,
                'unit_price': float(item.unit_price),
                'total_price': float(item.total_price)
            })
            
        data.append({
            'purchase_id': str(expense.expense_id),
            'project_name': expense.project.name,
            'invoice_number': expense.invoice_number or '-',
            'invoice_date': expense.expense_date.isoformat(),
            'amount': float(expense.amount),
            'payment_mode': expense.payment_mode,
            'category': expense.category,
            'paid': float(paid),
            'balance': float(balance),
            'items': items  # Added item details
        })
        
    return JsonResponse(data, safe=False)


@login_required
def get_vendor_outstanding(request, vendor_id):
    """
    Returns outstanding balance for a vendor (company-scoped).
    Optional query param: project_id — returns project-specific outstanding too.
    Outstanding = CREDIT purchases - vendor payments made
    """
    try:
        vendor = Vendor.objects.get(vendor_id=vendor_id, company=request.user.company)
    except Vendor.DoesNotExist:
        return JsonResponse({'error': 'Vendor not found'}, status=404)

    project_id = request.GET.get('project_id')

    # --- Total outstanding for this vendor (across all projects) ---
    all_credit = Expense.objects.filter(
        vendor=vendor, company=request.user.company, payment_mode='CREDIT'
    ).aggregate(s=Sum('amount'))['s'] or Decimal('0')

    all_payments = Payment.objects.filter(
        vendor=vendor, company=request.user.company
    ).aggregate(s=Sum('amount'))['s'] or Decimal('0')

    total_outstanding = max(all_credit - all_payments, Decimal('0'))

    response = {
        'vendor_name': vendor.name,
        'total_outstanding': float(total_outstanding),
    }

    # --- Project-specific outstanding ---
    if project_id:
        try:
            project = Project.objects.get(project_id=project_id, company=request.user.company)
            proj_credit = Expense.objects.filter(
                vendor=vendor, project=project, company=request.user.company, payment_mode='CREDIT'
            ).aggregate(s=Sum('amount'))['s'] or Decimal('0')

            proj_payments = Payment.objects.filter(
                vendor=vendor, project=project, company=request.user.company
            ).aggregate(s=Sum('amount'))['s'] or Decimal('0')

            project_outstanding = max(proj_credit - proj_payments, Decimal('0'))
            response['project_outstanding'] = float(project_outstanding)
            response['project_name'] = project.name
        except Project.DoesNotExist:
            pass

    return JsonResponse(response)


@login_required
def daily_cash_balance(request):
    try:
        project_id = request.GET.get('project_id')
        from_date_str = request.GET.get('from_date')
        to_date_str = request.GET.get('to_date')
        
        if not from_date_str or not to_date_str:
            return JsonResponse({'error': 'Date range required'}, status=400)
            
        from_date = datetime.fromisoformat(from_date_str).date()
        to_date = datetime.fromisoformat(to_date_str).date()
        
        # Base queries
        expenses_query = Expense.objects.filter(company=request.user.company)
        payments_query = Payment.objects.filter(company=request.user.company)
        client_payments_query = ClientPayment.objects.filter(company=request.user.company)
        
        if project_id and project_id != 'all':
            expenses_query = expenses_query.filter(project_id=project_id)
            payments_query = payments_query.filter(project_id=project_id)
            client_payments_query = client_payments_query.filter(project_id=project_id)
            
        # 1. Opening Balance (Before from_date)
        # Inflows
        op_inflow = client_payments_query.filter(payment_date__lt=from_date).aggregate(Sum('amount'))['amount__sum'] or 0
        
        # Outflows
        # Expenses (Cash/Bank/UPI) - excluding CREDIT
        op_exp_outflow = expenses_query.filter(expense_date__lt=from_date).exclude(payment_mode='CREDIT').aggregate(Sum('amount'))['amount__sum'] or 0
        
        # Vendor Payments (Settlements)
        op_pay_outflow = payments_query.filter(payment_date__lt=from_date).aggregate(Sum('amount'))['amount__sum'] or 0
        
        opening_balance = op_inflow - (op_exp_outflow + op_pay_outflow)
        
        # 2. Period Data (from_date to to_date inclusive)
        period_client_payments = client_payments_query.filter(payment_date__range=[from_date, to_date])
        period_expenses = expenses_query.filter(expense_date__range=[from_date, to_date])
        period_payments = payments_query.filter(payment_date__range=[from_date, to_date])
        
        client_receipts = period_client_payments.aggregate(Sum('amount'))['amount__sum'] or 0
        vendor_payments = period_payments.aggregate(Sum('amount'))['amount__sum'] or 0
        
        # Expenses breakdown
        expenses_credit = period_expenses.filter(payment_mode='CREDIT').aggregate(Sum('amount'))['amount__sum'] or 0
        expenses_cash = period_expenses.filter(payment_mode='CASH').aggregate(Sum('amount'))['amount__sum'] or 0
        expenses_bank_upi = period_expenses.filter(payment_mode__in=['BANK', 'UPI']).aggregate(Sum('amount'))['amount__sum'] or 0
        
        # Total Period Outflow (Cash only)
        period_outflow = expenses_cash + expenses_bank_upi + vendor_payments
        
        closing_balance = opening_balance + client_receipts - period_outflow
        
        # 3. Transactions List
        transactions = []
        
        for cp in period_client_payments:
            transactions.append({
                'date': cp.payment_date.isoformat(),
                'type': 'client_payment',
                'id': str(cp.client_payment_id), # Added ID
                'amount': float(cp.amount),
                'payment_mode': cp.payment_mode,
                'project_name': cp.project.name,
                'reference': cp.reference_number,
                'remarks': cp.remarks
            })
            
        for exp in period_expenses:
             # Include CREDIT expenses in list but mark them
             # Prepare items list if it's a material purchase
            items_data = []
            if exp.expense_type == 'Material Purchase':
                for item in exp.items.all():
                    items_data.append({
                        'item_name': item.item_name,
                        'quantity': float(item.quantity),
                        'measuring_unit': item.measuring_unit,
                        'unit_price': float(item.unit_price),
                        'total_price': float(item.total_price)
                    })

            # Get subcategory from items for regular expenses
            subcategory = ""
            if exp.expense_type == 'Regular Expense':
                first_item = exp.items.first()
                subcategory = first_item.item_name if first_item else ""

            transactions.append({
                'date': exp.expense_date.isoformat(),
                'type': 'expense',
                'expense_type': exp.expense_type,
                'id': str(exp.expense_id),
                'amount': float(exp.amount),
                'payment_mode': exp.payment_mode,
                'project_name': exp.project.name,
                'category': exp.category,
                'subcategory': subcategory,
                'description': exp.description,
                'items': items_data # Added items
            })
            
        for pay in period_payments:
            transactions.append({
                'date': pay.payment_date.isoformat(),
                'type': 'vendor_payment',
                'id': str(pay.payment_id), # Added ID
                'amount': float(pay.amount),
                'payment_mode': pay.payment_mode,
                'project_name': pay.project.name,
                'vendor_name': pay.vendor.name,
                'invoice_number': pay.expense.invoice_number if pay.expense else None,
                'category': pay.expense.category if pay.expense else None
            })
            
        # Sort by date desc
        transactions.sort(key=lambda x: x['date'], reverse=True)
        
        return JsonResponse({
            'opening_balance': float(opening_balance),
            'closing_balance': float(closing_balance),
            'client_receipts': float(client_receipts),
            'vendor_payments': float(vendor_payments),
            'expenses_credit': float(expenses_credit),
            'expenses_cash': float(expenses_cash),
            'expenses_bank_upi': float(expenses_bank_upi),
            'transactions': transactions
        })
        
    except Exception as e:
        print(f"Error in daily_cash_balance: {e}")
        return JsonResponse({'error': str(e)}, status=500)
