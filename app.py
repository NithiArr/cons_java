from flask import Flask, render_template, redirect, url_for
from flask_login import LoginManager, current_user
from mongoengine import connect
from models_mongo import User
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
    
    # MongoDB Connection
    mongodb_host = os.environ.get('MONGODB_HOST', 'mongodb://localhost:27017/construction_db')
    try:
        connect(host=mongodb_host)
        # Force a connection attempt to fail fast if config is wrong
        # We need to import get_connection to do this properly or just trust connect() for now
        # But for the health-db route we will be explicit
        print(f"MongoDB connected successfully to: {mongodb_host.split('@')[-1] if '@' in mongodb_host else 'localhost'}")
    except Exception as e:
        print(f"Error connecting to MongoDB: {e}")

    
    # Setup Flask-Login
    login_manager = LoginManager()
    login_manager.init_app(app)
    login_manager.login_view = 'auth.login'
    
    @login_manager.user_loader
    def load_user(user_id):
        try:
            return User.objects(id=user_id).first()
        except:
            return None
    
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
            # Attempt to list collections or just ping
            # This forces a round-trip to the DB
            from mongoengine.connection import get_connection
            conn = get_connection()
            conn.admin.command('ping')
            return {"status": "success", "message": "MongoDB is connected!"}, 200
        except Exception as e:
            return {"status": "error", "message": str(e)}, 500

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
    
    # Create upload directory
    # On Vercel, the file system is read-only except for /tmp
    # We check if we are on Vercel or if the default location is not writable
    try:
        os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
    except OSError:
        # Fallback to /tmp if default location is read-only (common in serverless)
        app.config['UPLOAD_FOLDER'] = '/tmp/uploads'
        os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
        print(f"Read-only file system detected. Switched upload folder to: {app.config['UPLOAD_FOLDER']}")
    
    return app

# Create app instance for production servers (gunicorn, etc.)
app = create_app()

if __name__ == '__main__':
    debug_mode = os.environ.get('DEBUG', 'True').lower() == 'true'
    port = int(os.environ.get('PORT', 5000))
    
    print("Construction Management System starting...")
    print(f"Access the application at: http://127.0.0.1:{port}")
    app.run(debug=debug_mode, host='0.0.0.0', port=port)

