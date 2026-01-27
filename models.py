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
    purchases = db.relationship('Purchase', back_populates='company', cascade='all, delete-orphan')
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
    purchases = db.relationship('Purchase', back_populates='project', cascade='all, delete-orphan')
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
    purchases = db.relationship('Purchase', back_populates='vendor', cascade='all, delete-orphan')
    payments = db.relationship('Payment', back_populates='vendor', cascade='all, delete-orphan')
    
    def __repr__(self):
        return f'<Vendor {self.name}>'


class Expense(db.Model):
    """Expense model for project expenses"""
    __tablename__ = 'expense'
    
    expense_id = db.Column(db.Integer, primary_key=True)
    company_id = db.Column(db.Integer, db.ForeignKey('company.company_id'), nullable=False)
    project_id = db.Column(db.Integer, db.ForeignKey('project.project_id'), nullable=False)
    category = db.Column(db.String(100), nullable=False)
    subcategory = db.Column(db.String(100))  # NEW: For detailed expense classification
    amount = db.Column(db.Numeric(15, 2), nullable=False)
    payment_mode = db.Column(db.String(20), nullable=False)  # CASH, BANK, UPI
    expense_date = db.Column(db.Date, nullable=False)
    description = db.Column(db.Text)
    bill_url = db.Column(db.String(500))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    company = db.relationship('Company', back_populates='expenses')
    project = db.relationship('Project', back_populates='expenses')
    
    def __repr__(self):
        return f'<Expense {self.category} - {self.amount}>'


class Purchase(db.Model):
    """Purchase model for purchase orders"""
    __tablename__ = 'purchase'
    
    purchase_id = db.Column(db.Integer, primary_key=True)
    company_id = db.Column(db.Integer, db.ForeignKey('company.company_id'), nullable=False)
    project_id = db.Column(db.Integer, db.ForeignKey('project.project_id'), nullable=False)
    vendor_id = db.Column(db.Integer, db.ForeignKey('vendor.vendor_id'), nullable=False)
    invoice_number = db.Column(db.String(100))
    invoice_date = db.Column(db.Date, nullable=False)
    total_amount = db.Column(db.Numeric(15, 2), nullable=False)
    payment_type = db.Column(db.String(20), nullable=False)  # CASH, CREDIT
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    company = db.relationship('Company', back_populates='purchases')
    project = db.relationship('Project', back_populates='purchases')
    vendor = db.relationship('Vendor', back_populates='purchases')
    items = db.relationship('PurchaseItem', back_populates='purchase', cascade='all, delete-orphan')
    payments = db.relationship('Payment', back_populates='purchase')
    
    def __repr__(self):
        return f'<Purchase {self.invoice_number}>'


class PurchaseItem(db.Model):
    """Purchase item model for line items"""
    __tablename__ = 'purchase_item'
    
    purchase_item_id = db.Column(db.Integer, primary_key=True)
    purchase_id = db.Column(db.Integer, db.ForeignKey('purchase.purchase_id'), nullable=False)
    item_name = db.Column(db.String(200), nullable=False)
    quantity = db.Column(db.Numeric(10, 2), nullable=False)
    unit_price = db.Column(db.Numeric(15, 2), nullable=False)
    total_price = db.Column(db.Numeric(15, 2), nullable=False)
    
    # Relationships
    purchase = db.relationship('Purchase', back_populates='items')
    
    def __repr__(self):
        return f'<PurchaseItem {self.item_name}>'


class Payment(db.Model):
    """Payment model for vendor payments"""
    __tablename__ = 'payment'
    
    payment_id = db.Column(db.Integer, primary_key=True)
    company_id = db.Column(db.Integer, db.ForeignKey('company.company_id'), nullable=False)
    vendor_id = db.Column(db.Integer, db.ForeignKey('vendor.vendor_id'), nullable=False)
    project_id = db.Column(db.Integer, db.ForeignKey('project.project_id'), nullable=False)
    purchase_id = db.Column(db.Integer, db.ForeignKey('purchase.purchase_id'), nullable=True)
    amount = db.Column(db.Numeric(15, 2), nullable=False)
    payment_date = db.Column(db.Date, nullable=False)
    payment_mode = db.Column(db.String(20), nullable=False)  # CASH, BANK, UPI
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    company = db.relationship('Company', back_populates='payments')
    vendor = db.relationship('Vendor', back_populates='payments')
    project = db.relationship('Project', back_populates='payments')
    purchase = db.relationship('Purchase', back_populates='payments')
    
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
