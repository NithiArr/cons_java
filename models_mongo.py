from flask_login import UserMixin
from datetime import datetime
from werkzeug.security import generate_password_hash, check_password_hash
from mongoengine import (
    Document, EmbeddedDocument, 
    StringField, IntField, FloatField, DateTimeField, DateField,
    ReferenceField, EmbeddedDocumentListField, ListField, CASCADE, BooleanField
)

class SubCategoryItem(EmbeddedDocument):
    name = StringField(required=True)
    default_unit = StringField(max_length=20) # e.g. "kg", "bag", "nos"

class MasterCategory(Document):
    name = StringField(required=True, unique=True)
    type = StringField(required=True, choices=['MATERIAL', 'EXPENSE']) # 'MATERIAL' or 'EXPENSE'
    subcategories = EmbeddedDocumentListField(SubCategoryItem)
    is_active = BooleanField(default=True)

    def __repr__(self):
        return f'<MasterCategory {self.name} ({self.type})>'

class Company(Document):
    """Company model for multi-tenant isolation"""
    meta = {'collection': 'companies'}
    
    name = StringField(required=True, max_length=200)
    address = StringField()
    phone = StringField(max_length=20)
    email = StringField(max_length=120)
    created_at = DateTimeField(default=datetime.utcnow)
    
    def __repr__(self):
        return f'<Company {self.name}>'


class User(UserMixin, Document):
    """User model with 5-tier role system"""
    meta = {'collection': 'users'}
    
    company = ReferenceField(Company, required=True, reverse_delete_rule=CASCADE)
    name = StringField(required=True, max_length=200)
    email = StringField(required=True, unique=True, max_length=120)
    password_hash = StringField(required=True, max_length=255)
    role = StringField(required=True, max_length=20, choices=['OWNER', 'ADMIN', 'MANAGER', 'ACCOUNTANT', 'ENGINEER'])
    created_at = DateTimeField(default=datetime.utcnow)
    
    def get_id(self):
        """Override get_id for Flask-Login"""
        return str(self.id)
    
    def set_password(self, password):
        """Hash and set password"""
        self.password_hash = generate_password_hash(password)
    
    def check_password(self, password):
        """Verify password"""
        return check_password_hash(self.password_hash, password)
    
    def has_permission(self, permission):
        """Check if user has specific permission"""
        permissions = {
            'OWNER': ['read'],
            'ADMIN': ['read', 'create', 'update', 'delete', 'manage_users'],
            'MANAGER': ['read', 'create', 'update', 'manage_projects', 'manage_purchases'],
            'ACCOUNTANT': ['read', 'create', 'update', 'manage_payments', 'manage_expenses'],
            'ENGINEER': ['read']
        }
        return permission in permissions.get(self.role, [])
    
    def __repr__(self):
        return f'<User {self.email} - {self.role}>'


class Project(Document):
    """Project model with status tracking"""
    meta = {'collection': 'projects'}
    
    company = ReferenceField(Company, required=True, reverse_delete_rule=CASCADE)
    name = StringField(required=True, max_length=200)
    location = StringField(max_length=300)
    start_date = DateField()
    end_date = DateField()
    budget = FloatField(default=0)
    status = StringField(default='PLANNING', choices=['PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED'])
    created_at = DateTimeField(default=datetime.utcnow)
    
    def __repr__(self):
        return f'<Project {self.name}>'


class Vendor(Document):
    """Vendor model for company-specific vendors"""
    meta = {'collection': 'vendors'}
    
    company = ReferenceField(Company, required=True, reverse_delete_rule=CASCADE)
    name = StringField(required=True, max_length=200)
    phone = StringField(max_length=20)
    email = StringField(max_length=120)
    gst_number = StringField(max_length=50)
    created_at = DateTimeField(default=datetime.utcnow)
    
    def __repr__(self):
        return f'<Vendor {self.name}>'


class ExpenseItem(EmbeddedDocument):
    """Embedded expense item for line items"""
    item_name = StringField(required=True, max_length=200)
    quantity = FloatField(required=True)
    measuring_unit = StringField(default='Unit', max_length=50)
    unit_price = FloatField(required=True)
    total_price = FloatField(required=True)
    
    def __repr__(self):
        return f'<ExpenseItem {self.item_name}>'


class Expense(Document):
    """Unified Expense model (merging Expenses and Purchases)"""
    meta = {'collection': 'expenses'}
    
    company = ReferenceField(Company, required=True, reverse_delete_rule=CASCADE)
    project = ReferenceField(Project, required=True, reverse_delete_rule=CASCADE)
    vendor = ReferenceField(Vendor, reverse_delete_rule=CASCADE)  # Nullable for regular expenses
    
    expense_type = StringField(required=True, max_length=100)  # 'Regular Expense' or 'Material Purchase'
    category = StringField(max_length=100)  # Main category (e.g. Cement, Travel)
    
    amount = FloatField(required=True)
    payment_mode = StringField(required=True, max_length=20, choices=['CASH', 'BANK', 'UPI', 'CREDIT'])
    expense_date = DateField(required=True)  # Date of expense or invoice
    
    description = StringField()
    invoice_number = StringField(max_length=100)  # Specific to purchases
    bill_url = StringField(max_length=500)
    
    items = EmbeddedDocumentListField(ExpenseItem)
    
    created_at = DateTimeField(default=datetime.utcnow)
    
    def __repr__(self):
        return f'<Expense {self.expense_type} - {self.amount}>'


class Payment(Document):
    """Payment model for vendor payments"""
    meta = {'collection': 'payments'}
    
    company = ReferenceField(Company, required=True, reverse_delete_rule=CASCADE)
    vendor = ReferenceField(Vendor, required=True, reverse_delete_rule=CASCADE)
    project = ReferenceField(Project, required=True, reverse_delete_rule=CASCADE)
    expense = ReferenceField(Expense, reverse_delete_rule=CASCADE)  # Linked to Material Purchase expense
    
    amount = FloatField(required=True)
    payment_date = DateField(required=True)
    payment_mode = StringField(required=True, max_length=20, choices=['CASH', 'BANK', 'UPI'])
    created_at = DateTimeField(default=datetime.utcnow)
    
    def __repr__(self):
        return f'<Payment {self.amount}>'


class ClientPayment(Document):
    """Client payment model for incoming payments"""
    meta = {'collection': 'client_payments'}
    
    company = ReferenceField(Company, required=True, reverse_delete_rule=CASCADE)
    project = ReferenceField(Project, required=True, reverse_delete_rule=CASCADE)
    amount = FloatField(required=True)
    payment_date = DateField(required=True)
    payment_mode = StringField(required=True, max_length=20, choices=['CASH', 'BANK', 'UPI'])
    reference_number = StringField(max_length=100)
    remarks = StringField()
    created_at = DateTimeField(default=datetime.utcnow)
    
    def __repr__(self):
        return f'<ClientPayment {self.amount}>'
