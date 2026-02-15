from flask_sqlalchemy import SQLAlchemy
from flask_login import UserMixin
from datetime import datetime
from werkzeug.security import generate_password_hash, check_password_hash

db = SQLAlchemy()

class Company(db.Model):
    """Company model for multi-tenant isolation"""
    __tablename__ = 'company'
    
    company_id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(200), nullable=False)
    address = db.Column(db.Text)
    phone = db.Column(db.String(20))
    email = db.Column(db.String(120))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    users = db.relationship('User', back_populates='company', cascade='all, delete-orphan')
    projects = db.relationship('Project', back_populates='company', cascade='all, delete-orphan')
    vendors = db.relationship('Vendor', back_populates='company', cascade='all, delete-orphan')
    expenses = db.relationship('Expense', back_populates='company', cascade='all, delete-orphan')
    payments = db.relationship('Payment', back_populates='company', cascade='all, delete-orphan')
    client_payments = db.relationship('ClientPayment', back_populates='company', cascade='all, delete-orphan')
    
    def __repr__(self):
        return f'<Company {self.name}>'


class User(UserMixin, db.Model):
    """User model with 5-tier role system"""
    __tablename__ = 'user'
    
    user_id = db.Column(db.Integer, primary_key=True)
    company_id = db.Column(db.Integer, db.ForeignKey('company.company_id'), nullable=False)
    name = db.Column(db.String(200), nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(255), nullable=False)
    role = db.Column(db.String(20), nullable=False)  # OWNER, ADMIN, MANAGER, ACCOUNTANT, ENGINEER
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    company = db.relationship('Company', back_populates='users')
    
    def get_id(self):
        """Override get_id for Flask-Login"""
        return str(self.user_id)
    
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


class MasterCategory(db.Model):
    """Master Category for grouped expenses/materials"""
    __tablename__ = 'master_category'
    
    category_id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(100), unique=True, nullable=False)
    type = db.Column(db.String(50), nullable=False) # 'MATERIAL' or 'EXPENSE'
    is_active = db.Column(db.Boolean, default=True)
    
    # Relationships
    subcategories = db.relationship('SubCategory', back_populates='parent_category', cascade='all, delete-orphan')
    
    def __repr__(self):
        return f'<MasterCategory {self.name} ({self.type})>'


class SubCategory(db.Model):
    """Subcategories for MasterCategory"""
    __tablename__ = 'sub_category'
    
    subcategory_id = db.Column(db.Integer, primary_key=True)
    parent_category_id = db.Column(db.Integer, db.ForeignKey('master_category.category_id'), nullable=False)
    name = db.Column(db.String(100), nullable=False)
    default_unit = db.Column(db.String(20)) # e.g. "kg", "bag", "nos"
    
    # Relationships
    parent_category = db.relationship('MasterCategory', back_populates='subcategories')
    
    def __repr__(self):
        return f'<SubCategory {self.name}>'


class Project(db.Model):
    """Project model with status tracking"""
    __tablename__ = 'project'
    
    project_id = db.Column(db.Integer, primary_key=True)
    company_id = db.Column(db.Integer, db.ForeignKey('company.company_id'), nullable=False)
    name = db.Column(db.String(200), nullable=False)
    location = db.Column(db.String(300))
    start_date = db.Column(db.Date)
    end_date = db.Column(db.Date)
    budget = db.Column(db.Numeric(15, 2), default=0)
    status = db.Column(db.String(20), default='PLANNING')  # PLANNING, ACTIVE, ON_HOLD, COMPLETED
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    company = db.relationship('Company', back_populates='projects')
    expenses = db.relationship('Expense', back_populates='project', cascade='all, delete-orphan')
    payments = db.relationship('Payment', back_populates='project', cascade='all, delete-orphan')
    client_payments = db.relationship('ClientPayment', back_populates='project', cascade='all, delete-orphan')
    
    def __repr__(self):
        return f'<Project {self.name}>'


class Vendor(db.Model):
    """Vendor model for company-specific vendors"""
    __tablename__ = 'vendor'
    
    vendor_id = db.Column(db.Integer, primary_key=True)
    company_id = db.Column(db.Integer, db.ForeignKey('company.company_id'), nullable=False)
    name = db.Column(db.String(200), nullable=False)
    phone = db.Column(db.String(20))
    email = db.Column(db.String(120))
    gst_number = db.Column(db.String(50))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    company = db.relationship('Company', back_populates='vendors')
    expenses = db.relationship('Expense', back_populates='vendor', cascade='all, delete-orphan')
    payments = db.relationship('Payment', back_populates='vendor', cascade='all, delete-orphan')
    
    def __repr__(self):
        return f'<Vendor {self.name}>'


class Expense(db.Model):
    """Unified Expense model (merging Expenses and Purchases)"""
    __tablename__ = 'expense'
    
    expense_id = db.Column(db.Integer, primary_key=True)
    company_id = db.Column(db.Integer, db.ForeignKey('company.company_id'), nullable=False)
    project_id = db.Column(db.Integer, db.ForeignKey('project.project_id'), nullable=False)
    vendor_id = db.Column(db.Integer, db.ForeignKey('vendor.vendor_id'), nullable=True) # Nullable for regular expenses
    
    expense_type = db.Column(db.String(100), nullable=False) # 'Regular Expense' or 'Material Purchase'
    category = db.Column(db.String(100)) # Main category from MasterCategory
    
    amount = db.Column(db.Numeric(15, 2), nullable=False)
    payment_mode = db.Column(db.String(20), nullable=False) # CASH, BANK, UPI, CREDIT
    expense_date = db.Column(db.Date, nullable=False) # Date of expense or invoice
    
    description = db.Column(db.Text)
    invoice_number = db.Column(db.String(100)) # Specific to purchases
    bill_url = db.Column(db.String(500))
    
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    company = db.relationship('Company', back_populates='expenses')
    project = db.relationship('Project', back_populates='expenses')
    vendor = db.relationship('Vendor', back_populates='expenses')
    items = db.relationship('ExpenseItem', back_populates='expense', cascade='all, delete-orphan')
    payments = db.relationship('Payment', back_populates='expense')
    
    def __repr__(self):
        return f'<Expense {self.expense_type} - {self.amount}>'


class ExpenseItem(db.Model):
    """Expense item model for line items (formerly PurchaseItem)"""
    __tablename__ = 'expense_item'
    
    expense_item_id = db.Column(db.Integer, primary_key=True)
    expense_id = db.Column(db.Integer, db.ForeignKey('expense.expense_id'), nullable=False)
    item_name = db.Column(db.String(200), nullable=False)
    quantity = db.Column(db.Numeric(10, 2), nullable=False)
    measuring_unit = db.Column(db.String(20), default='Unit')
    unit_price = db.Column(db.Numeric(15, 2), nullable=False)
    total_price = db.Column(db.Numeric(15, 2), nullable=False)
    
    # Relationships
    expense = db.relationship('Expense', back_populates='items')
    
    def __repr__(self):
        return f'<ExpenseItem {self.item_name}>'


class Payment(db.Model):
    """Payment model for vendor payments"""
    __tablename__ = 'payment'
    
    payment_id = db.Column(db.Integer, primary_key=True)
    company_id = db.Column(db.Integer, db.ForeignKey('company.company_id'), nullable=False)
    vendor_id = db.Column(db.Integer, db.ForeignKey('vendor.vendor_id'), nullable=False)
    project_id = db.Column(db.Integer, db.ForeignKey('project.project_id'), nullable=False)
    expense_id = db.Column(db.Integer, db.ForeignKey('expense.expense_id'), nullable=True) # Linked to Material Purchase expense
    
    amount = db.Column(db.Numeric(15, 2), nullable=False)
    payment_date = db.Column(db.Date, nullable=False)
    payment_mode = db.Column(db.String(20), nullable=False)  # CASH, BANK, UPI
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    company = db.relationship('Company', back_populates='payments')
    vendor = db.relationship('Vendor', back_populates='payments')
    project = db.relationship('Project', back_populates='payments')
    expense = db.relationship('Expense', back_populates='payments')
    
    def __repr__(self):
        return f'<Payment {self.amount}>'


class ClientPayment(db.Model):
    """Client payment model for incoming payments"""
    __tablename__ = 'client_payment'
    
    client_payment_id = db.Column(db.Integer, primary_key=True)
    company_id = db.Column(db.Integer, db.ForeignKey('company.company_id'), nullable=False)
    project_id = db.Column(db.Integer, db.ForeignKey('project.project_id'), nullable=False)
    amount = db.Column(db.Numeric(15, 2), nullable=False)
    payment_date = db.Column(db.Date, nullable=False)
    payment_mode = db.Column(db.String(20), nullable=False)  # CASH, BANK, UPI
    reference_number = db.Column(db.String(100))
    remarks = db.Column(db.Text)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    company = db.relationship('Company', back_populates='client_payments')
    project = db.relationship('Project', back_populates='client_payments')
    
    def __repr__(self):
        return f'<ClientPayment {self.amount}>'
