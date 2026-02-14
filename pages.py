from flask import Blueprint, request, jsonify, render_template
from flask_login import login_required, current_user

# Create a simple routes blueprint for HTML pages
pages_bp = Blueprint('pages', __name__)

@pages_bp.route('/projects')
@login_required
def projects_page():
    return render_template('projects.html', user=current_user)

@pages_bp.route('/vendors')
@login_required
def vendors_page():
    return render_template('vendors.html', user=current_user)

@pages_bp.route('/purchases')
@login_required
def purchases_page():
    return render_template('purchases.html', user=current_user)

@pages_bp.route('/expenses')
@login_required
def expenses_page():
    return render_template('expenses.html', user=current_user)

@pages_bp.route('/payments')
@login_required
def payments_page():
    return render_template('payments.html', user=current_user)

@pages_bp.route('/client-payments')
@login_required
def client_payments_page():
    return render_template('client_payments.html', user=current_user)
@pages_bp.route('/master-categories')
@login_required
def master_categories_page():
    return render_template('master_categories.html', user=current_user)
