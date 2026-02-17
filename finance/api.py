from django.http import JsonResponse
from django.views.decorators.http import require_http_methods
from django.contrib.auth.decorators import login_required
from django.db import transaction
from .models import Expense, ExpenseItem, Payment, ClientPayment
from core.models import Project, Vendor
import json
from django.utils import timezone
from datetime import datetime

# ================= PURCHASE API (Material Expenses) =================

@login_required
def purchases_list(request):
    if request.method == 'POST':
        return create_purchase(request)
    return get_purchases(request)

@login_required
def purchase_detail(request, purchase_id):
    if request.method == 'GET':
        return get_purchase_detail(request, purchase_id)
    elif request.method == 'PUT':
        return update_purchase(request, purchase_id)
    elif request.method == 'DELETE':
        return delete_purchase(request, purchase_id)
    return JsonResponse({'error': 'Method not allowed'}, status=405)

def get_purchase_detail(request, purchase_id):
    try:
        p = Expense.objects.get(expense_id=purchase_id, company=request.user.company, expense_type='Material Purchase')
        
        items = [{
            'item_name': item.item_name,
            'quantity': float(item.quantity),
            'measuring_unit': item.measuring_unit,
            'unit_price': float(item.unit_price),
            'total_price': float(item.total_price)
        } for item in p.items.all()]
        
        data = {
            'purchase_id': str(p.expense_id),
            'expense_id': str(p.expense_id),
            'project_id': str(p.project.project_id),
            'project_name': p.project.name,
            'vendor_id': str(p.vendor.vendor_id) if p.vendor else None,
            'vendor_name': p.vendor.name if p.vendor else None,
            'invoice_number': p.invoice_number,
            'expense_date': p.expense_date.isoformat(),
            'total_amount': float(p.amount),
            'payment_mode': p.payment_mode,
            'category': p.category,
            'items': items
        }
        return JsonResponse(data)
    except Expense.DoesNotExist:
        return JsonResponse({'error': 'Purchase not found'}, status=404)

@transaction.atomic
def update_purchase(request, purchase_id):
    try:
        p = Expense.objects.get(expense_id=purchase_id, company=request.user.company, expense_type='Material Purchase')
        data = json.loads(request.body)
        
        if 'project_id' in data:
            p.project = Project.objects.get(project_id=data['project_id'], company=request.user.company)
        if 'vendor_id' in data:
            p.vendor = Vendor.objects.get(vendor_id=data['vendor_id'], company=request.user.company)
        if 'category' in data:
            p.category = data['category']
        if 'invoice_number' in data:
            p.invoice_number = data['invoice_number']
        if 'invoice_date' in data:
             p.expense_date = datetime.fromisoformat(data['invoice_date']).date()
        if 'total_amount' in data:
            p.amount = data['total_amount']
        if 'payment_type' in data:
            p.payment_mode = data['payment_type']
            
        p.save()
        
        # Update items: Delete all and recreate
        if 'items' in data:
            p.items.all().delete()
            for item_data in data['items']:
                ExpenseItem.objects.create(
                    expense=p,
                    item_name=item_data['item_name'],
                    quantity=item_data['quantity'],
                    measuring_unit=item_data.get('measuring_unit', 'Unit'),
                    unit_price=item_data['unit_price'],
                    total_price=item_data['total_price']
                )
                
        return JsonResponse({'message': 'Purchase updated'})
    except Expense.DoesNotExist:
        return JsonResponse({'error': 'Purchase not found'}, status=404)
    except Exception as e:
        return JsonResponse({'error': str(e)}, status=400)

def delete_purchase(request, purchase_id):
    try:
        purchase = Expense.objects.get(
            expense_id=purchase_id, 
            company=request.user.company, 
            expense_type='Material Purchase'
        )
        purchase.delete()
        return JsonResponse({'message': 'Purchase deleted'})
    except Expense.DoesNotExist:
        return JsonResponse({'error': 'Purchase not found'}, status=404)

# ================= EXPENSE API (Regular Expenses) =================

@login_required
def expenses_list(request):
    if request.method == 'POST':
        return create_expense(request)
    return get_expenses(request)

@login_required
def expense_detail(request, expense_id):
    if request.method == 'GET':
        return get_expense_detail(request, expense_id)
    elif request.method == 'PUT':
        return update_expense(request, expense_id)
    elif request.method == 'DELETE':
        return delete_expense(request, expense_id)
    return JsonResponse({'error': 'Method not allowed'}, status=405)

def get_purchases(request):
    # Filter by user's company and expense_type='Material Purchase'
    purchases = Expense.objects.filter(
        company=request.user.company, 
        expense_type='Material Purchase'
    ).order_by('-expense_date')
    
    data = []
    for p in purchases:
        items = [{
            'item_name': item.item_name,
            'quantity': float(item.quantity),
            'measuring_unit': item.measuring_unit,
            'unit_price': float(item.unit_price),
            'total_price': float(item.total_price)
        } for item in p.items.all()]
        
        data.append({
            'purchase_id': str(p.expense_id),
            'expense_id': str(p.expense_id),
            'project_id': str(p.project.project_id),
            'project_name': p.project.name,
            'vendor_id': str(p.vendor.vendor_id) if p.vendor else None,
            'vendor_name': p.vendor.name if p.vendor else None,
            'invoice_number': p.invoice_number,
            'invoice_date': p.expense_date.isoformat(),
            'total_amount': float(p.amount),
            'payment_type': p.payment_mode,
            'category': p.category, 
            'items': items
        })
        
    return JsonResponse(data, safe=False)

def get_expense_detail(request, expense_id):
    try:
        e = Expense.objects.get(expense_id=expense_id, company=request.user.company, expense_type='Regular Expense')
        data = {
            'expense_id': str(e.expense_id),
            'project_id': str(e.project.project_id),
            'project_name': e.project.name,
            'category': e.category,
            'amount': float(e.amount),
            'payment_mode': e.payment_mode,
            'expense_date': e.expense_date.isoformat(),
            'description': e.description,
            'bill_url': e.bill_url
        }
        return JsonResponse(data)
    except Expense.DoesNotExist:
        return JsonResponse({'error': 'Expense not found'}, status=404)

def update_expense(request, expense_id):
    try:
        e = Expense.objects.get(expense_id=expense_id, company=request.user.company, expense_type='Regular Expense')
        data = json.loads(request.body)
        
        if 'project_id' in data:
            e.project = Project.objects.get(project_id=data['project_id'], company=request.user.company)
        if 'category' in data:
            e.category = data['category']
        if 'amount' in data:
            e.amount = data['amount']
        if 'payment_mode' in data:
            e.payment_mode = data['payment_mode']
        if 'expense_date' in data:
            e.expense_date = datetime.fromisoformat(data['expense_date']).date()
        if 'description' in data:
            e.description = data['description']
        if 'bill_url' in data:
            e.bill_url = data['bill_url']
            
        e.save()
        return JsonResponse({'message': 'Expense updated'})
    except Expense.DoesNotExist:
         return JsonResponse({'error': 'Expense not found'}, status=404)
    except Exception as e:
        return JsonResponse({'error': str(e)}, status=400)

def get_expenses(request):
    expenses = Expense.objects.filter(
        company=request.user.company,
        expense_type='Regular Expense'
    ).order_by('-expense_date')
    
    data = [{
        'expense_id': str(e.expense_id),
        'project_id': str(e.project.project_id),
        'project_name': e.project.name,
        'category': e.category,
        'amount': float(e.amount),
        'payment_mode': e.payment_mode,
        'expense_date': e.expense_date.isoformat(),
        'description': e.description,
        'bill_url': e.bill_url
    } for e in expenses]
    
    return JsonResponse(data, safe=False)

def create_expense(request):
    try:
        data = json.loads(request.body)
        print(f"Received expense data: {data}")  # Debug log
        project = Project.objects.get(project_id=data['project_id'], company=request.user.company)
        
        expense = Expense.objects.create(
            company=request.user.company,
            project=project,
            expense_type='Regular Expense',
            category=data.get('category'),
            amount=data['amount'],
            payment_mode=data['payment_mode'],
            expense_date=datetime.fromisoformat(data['expense_date']).date(),
            description=data.get('description'),
            bill_url=data.get('bill_url')
        )
        return JsonResponse({'message': 'Expense created', 'expense_id': str(expense.expense_id)}, status=201)
    except Project.DoesNotExist:
        return JsonResponse({'error': 'Project not found'}, status=404)
    except Exception as e:
        print(f"Error creating expense: {str(e)}")  # Debug log
        import traceback
        traceback.print_exc()  # Print full stack trace
        return JsonResponse({'error': str(e)}, status=400)

def delete_expense(request, expense_id):
    try:
        expense = Expense.objects.get(
            expense_id=expense_id, 
            company=request.user.company, 
            expense_type='Regular Expense'
        )
        expense.delete()
        return JsonResponse({'message': 'Expense deleted'})
    except Expense.DoesNotExist:
        return JsonResponse({'error': 'Expense not found'}, status=404)

# ================= PAYMENT API (Vendor Payments) =================

@login_required
def payments_list(request):
    if request.method == 'POST':
        return create_payment(request)
    return get_payments(request)

@login_required
def payment_detail(request, payment_id):
    if request.method == 'DELETE':
        return delete_payment(request, payment_id)
    return JsonResponse({'error': 'Method not allowed'}, status=405)

def get_payments(request):
    payments = Payment.objects.filter(company=request.user.company).order_by('-payment_date')
    data = [{
        'payment_id': str(p.payment_id),
        'payment_date': p.payment_date.isoformat(),
        'project_id': str(p.project.project_id),
        'project_name': p.project.name,
        'vendor_id': str(p.vendor.vendor_id),
        'vendor_name': p.vendor.name,
        'amount': float(p.amount),
        'payment_mode': p.payment_mode,
        'purchase_invoice': p.expense.invoice_number if p.expense else '-'
    } for p in payments]
    return JsonResponse(data, safe=False)

def create_payment(request):
    try:
        data = json.loads(request.body)
        project = Project.objects.get(project_id=data['project_id'], company=request.user.company)
        vendor = Vendor.objects.get(vendor_id=data['vendor_id'], company=request.user.company)
        
        expense = None
        if data.get('expense_id'):
            expense = Expense.objects.get(expense_id=data['expense_id']) # Should verify company?
            
        payment = Payment.objects.create(
            company=request.user.company,
            project=project,
            vendor=vendor,
            expense=expense,
            amount=data['amount'],
            payment_date=datetime.fromisoformat(data['payment_date']).date(),
            payment_mode=data['payment_mode']
        )
        return JsonResponse({'message': 'Payment recorded', 'payment_id': str(payment.payment_id)}, status=201)
    except (Project.DoesNotExist, Vendor.DoesNotExist):
        return JsonResponse({'error': 'Invalid project or vendor'}, status=404)
    except Exception as e:
        return JsonResponse({'error': str(e)}, status=400)

def delete_payment(request, payment_id):
    try:
        payment = Payment.objects.get(payment_id=payment_id, company=request.user.company)
        payment.delete()
        return JsonResponse({'message': 'Payment deleted'})
    except Payment.DoesNotExist:
        return JsonResponse({'error': 'Payment not found'}, status=404)

# ================= CLIENT PAYMENT API =================

@login_required
def client_payments_list(request):
    if request.method == 'POST':
        return create_client_payment(request)
    return get_client_payments(request)

@login_required
def client_payment_detail(request, client_payment_id):
    if request.method == 'DELETE':
         return delete_client_payment(request, client_payment_id)
    return JsonResponse({'error': 'Method not allowed'}, status=405)

def get_client_payments(request):
    payments = ClientPayment.objects.filter(company=request.user.company).order_by('-payment_date')
    data = [{
        'client_payment_id': str(cp.client_payment_id),
        'payment_date': cp.payment_date.isoformat(),
        'project_id': str(cp.project.project_id),
        'project_name': cp.project.name,
        'amount': float(cp.amount),
        'payment_mode': cp.payment_mode,
        'reference_number': cp.reference_number,
        'remarks': cp.remarks
    } for cp in payments]
    return JsonResponse(data, safe=False)

def create_client_payment(request):
    try:
        data = json.loads(request.body)
        project = Project.objects.get(project_id=data['project_id'], company=request.user.company)
        
        payment = ClientPayment.objects.create(
            company=request.user.company,
            project=project,
            amount=data['amount'],
            payment_date=datetime.fromisoformat(data['payment_date']).date(),
            payment_mode=data['payment_mode'],
            reference_number=data.get('reference_number'),
            remarks=data.get('remarks')
        )
        return JsonResponse({'message': 'Client payment recorded', 'client_payment_id': str(payment.client_payment_id)}, status=201)
    except Project.DoesNotExist:
        return JsonResponse({'error': 'Project not found'}, status=404)
    except Exception as e:
        return JsonResponse({'error': str(e)}, status=400)

def delete_client_payment(request, client_payment_id):
    try:
        payment = ClientPayment.objects.get(client_payment_id=client_payment_id, company=request.user.company)
        payment.delete()
        return JsonResponse({'message': 'Payment deleted'})
    except ClientPayment.DoesNotExist:
        return JsonResponse({'error': 'Payment not found'}, status=404)
