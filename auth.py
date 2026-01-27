from flask import Blueprint, render_template, request, redirect, url_for, flash
from flask_login import login_user, logout_user, login_required, current_user
from models import db, User, Company
from functools import wraps

auth_bp = Blueprint('auth', __name__)

def logout_required(f):
    """Decorator to ensure user is NOT logged in"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if current_user.is_authenticated:
            return redirect(url_for('index'))
        return f(*args, **kwargs)
    return decorated_function

@auth_bp.route('/login', methods=['GET', 'POST'])
@logout_required
def login():
    """User login"""
    if request.method == 'POST':
        email = request.form.get('email')
        password = request.form.get('password')
        
        user = User.query.filter_by(email=email).first()
        
        if user and user.check_password(password):
            login_user(user)
            flash(f'Welcome back, {user.name}!', 'success')
            return redirect(url_for('index'))
        else:
            flash('Invalid email or password', 'error')
    
    return render_template('login.html')

@auth_bp.route('/register', methods=['GET', 'POST'])
@logout_required
def register():
    """User registration"""
    if request.method == 'POST':
        # Get form data
        name = request.form.get('name')
        email = request.form.get('email')
        password = request.form.get('password')
        role = request.form.get('role', 'ENGINEER')
        
        # Company handling
        company_option = request.form.get('company_option')
        
        if company_option == 'new':
            # Create new company
            company_name = request.form.get('company_name')
            company_address = request.form.get('company_address')
            company_phone = request.form.get('company_phone')
            company_email = request.form.get('company_email')
            
            company = Company(
                name=company_name,
                address=company_address,
                phone=company_phone,
                email=company_email
            )
            db.session.add(company)
            db.session.flush()  # Get company_id
            
            # First user of a new company should be OWNER
            role = 'OWNER'
        else:
            # Join existing company
            company_id = request.form.get('company_id')
            company = Company.query.get(company_id)
            
            if not company:
                flash('Invalid company selected', 'error')
                return render_template('login.html')
        
        # Check if email already exists
        if User.query.filter_by(email=email).first():
            flash('Email already registered', 'error')
            return render_template('login.html')
        
        # Create user
        user = User(
            name=name,
            email=email,
            role=role,
            company_id=company.company_id
        )
        user.set_password(password)
        
        db.session.add(user)
        db.session.commit()
        
        flash(f'Account created successfully! You can now login.', 'success')
        return redirect(url_for('auth.login'))
    
    # GET request - show companies for selection
    companies = Company.query.all()
    return render_template('login.html', companies=companies)

@auth_bp.route('/logout')
@login_required
def logout():
    """User logout"""
    logout_user()
    flash('You have been logged out', 'info')
    return redirect(url_for('auth.login'))
