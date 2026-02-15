import os
import sys
from dotenv import load_dotenv

load_dotenv()

# Add project root to path
sys.path.append(os.getcwd())

try:
    from app import create_app
    from models import db, User, Project
    from sqlalchemy import text
    
    app = create_app()
    with app.app_context():
        print("Database URI:", app.config['SQLALCHEMY_DATABASE_URI'])
        try:
            # Test connection
            db.session.execute(text('SELECT 1'))
            print("Database connection successful.")
            
            # Test querying
            user_count = db.session.query(User).count()
            project_count = db.session.query(Project).count()
            print(f"Users: {user_count}")
            print(f"Projects: {project_count}")
            
        except Exception as e:
            print(f"Database error: {e}")
            sys.exit(1)
            
    print("Application verification successful.")
    
except Exception as e:
    print(f"Application import error: {e}")
    # Print full traceback
    import traceback
    traceback.print_exc()
    sys.exit(1)
