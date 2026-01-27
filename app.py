from flask import Flask, render_template, redirect, url_for
from flask_login import LoginManager, current_user
from models import db, User
import os

def create_app():
    """Application factory"""
    app = Flask(__name__)
    
    # Configuration
    app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'dev-secret-key-change-in-production')
    app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///construction.db'
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    app.config['UPLOAD_FOLDER'] = 'uploads'
    
    # Initialize extensions
    db.init_app(app)
    
    # Setup Flask-Login
    login_manager = LoginManager()
    login_manager.init_app(app)
    login_manager.login_view = 'auth.login'
    
    @login_manager.user_loader
    def load_user(user_id):
        return User.query.get(int(user_id))
    
    # Register blueprints
    from auth import auth_bp
    from api import api_bp
    from dashboard_api import dashboard_bp
    from export import export_bp
    from pages import pages_bp
    
    app.register_blueprint(auth_bp)
    app.register_blueprint(api_bp, url_prefix='/api')
    app.register_blueprint(dashboard_bp, url_prefix='/dashboard')
    app.register_blueprint(export_bp, url_prefix='/export')
    app.register_blueprint(pages_bp)
    
    # Home route
    @app.route('/')
    def index():
        if current_user.is_authenticated:
            # Redirect based on role
            if current_user.role == 'OWNER':
                return redirect(url_for('dashboard.owner_dashboard'))
            else:
                return redirect(url_for('dashboard.main_dashboard'))
        return redirect(url_for('auth.login'))
    
    # Create tables
    with app.app_context():
        db.create_all()
        # Create upload directory if it doesn't exist
        os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
    
    return app

if __name__ == '__main__':
    app = create_app()
    print("Construction Management System starting...")
    print("Access the application at: http://127.0.0.1:5000")
    app.run(debug=True, host='0.0.0.0', port=5000)
