import os
import sys
from dotenv import load_dotenv
from models import db, SubCategory
from sqlalchemy import text

load_dotenv()
sys.path.append(os.getcwd())

from app import create_app

def check_data():
    app = create_app()
    with app.app_context():
        try:
            print("\n--- SubCategories ---")
            subs = SubCategory.query.all()
            for s in subs:
                print(f"ID: {s.subcategory_id}, Name: {s.name}, Unit: {s.default_unit}")
                
        except Exception as e:
            print(f"Error: {e}")

if __name__ == "__main__":
    check_data()
