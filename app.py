from flask import Flask, render_template, redirect, url_for
from flask_login import LoginManager, current_user
from models import db
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

def create_app():
    """Application factory"""
    app = Flask(__name__)
    
    # Configuration from environment variables
    app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'dev-secret-key-change-in-production')
    app.config['UPLOAD_FOLDER'] = os.environ.get('UPLOAD_FOLDER', 'uploads')
    app.config['MAX_CONTENT_LENGTH'] = int(os.environ.get('MAX_CONTENT_LENGTH', 16777216))
    
    
    # PostgreSQL Connection
    database_uri = os.environ.get('SQLALCHEMY_DATABASE_URI', 'postgresql://postgres:admin123@localhost:5432/construction_db')
    # Fix for SQLAlchemy requiring 'postgresql://' instead of 'postgres://' (common in cloud providers like Supabase/Heroku)
    if database_uri and database_uri.startswith('postgres://'):
        database_uri = database_uri.replace('postgres://', 'postgresql://', 1)
    
    app.config['SQLALCHEMY_DATABASE_URI'] = database_uri
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    db.init_app(app)

    # Setup Flask-Login
    login_manager = LoginManager()
    login_manager.init_app(app)
    login_manager.login_view = 'auth.login'
    
    @login_manager.user_loader
    def load_user(user_id):
        from models import User
        return db.session.get(User, int(user_id)) if user_id.isdigit() else None
    
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
    
    # Health check for DB
    @app.route('/health-db')
    def health_db():
        try:
            # Simple query to check connection
            db.session.execute(db.text('SELECT 1'))
            return {"status": "success", "message": "Database is connected!"}, 200
        except Exception as e:
            return {"status": "error", "message": str(e)}, 500

    @app.route('/init-db')
    def init_db():
        try:
            db.create_all()
            
            # Check if Owner exists
            from models import Company, User
            from werkzeug.security import generate_password_hash
            
            if not Company.query.first():
                # Create Default Company
                company = Company(name="Construction Co", address="Head Office")
                db.session.add(company)
                db.session.flush() # Get ID
                
                # Create Owner
                owner = User(
                    company_id=company.company_id,
                    name="Owner",
                    email="owner@example.com",
                    role="OWNER"
                )
                owner.set_password("admin123")
                db.session.add(owner)
                db.session.commit()
                return "Database initialized! Tables created and default Owner (owner@example.com / admin123) created.", 200
            
            return "Database already initialized.", 200
        except Exception as e:
            return f"Error initializing database: {str(e)}", 500

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
    
    # Set upload folder - use /tmp on serverless platforms
    app.config['UPLOAD_FOLDER'] = '/tmp/uploads'
    
    return app

# Create app instance for production servers (gunicorn, etc.)
app = create_app()

if __name__ == '__main__':
    debug_mode = os.environ.get('DEBUG', 'True').lower() == 'true'
    port = int(os.environ.get('PORT', 5000))
    
    print("Construction Management System starting...")
    print(f"Access the application at: http://127.0.0.1:{port}")
    app.run(debug=debug_mode, host='0.0.0.0', port=port)

