import os
import sys
from dotenv import load_dotenv
from flask import Flask
from models import db, User

load_dotenv()
sys.path.append(os.getcwd())

from app import create_app
from api import get_purchases

def debug_purchases():
    app = create_app()
    
    with app.app_context():
        # Mock login
        user = db.session.query(User).first()
        if not user:
            print("No users found.")
            return

        print(f"Testing purchases for user: {user.name}")
        
        from flask_login import login_user
        
        with app.test_request_context('/api/purchases'):
            login_user(user)
            
            print("\n--- Testing get_purchases ---")
            try:
                response = get_purchases()
                print("Status Code:", response.status_code)
                print("Data length:", len(response.get_json()))
                # print("First item:", response.get_json()[0])
            except Exception as e:
                print("ERROR in get_purchases:")
                import traceback
                traceback.print_exc()

if __name__ == "__main__":
    debug_purchases()
