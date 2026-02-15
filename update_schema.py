import os
import sys
from dotenv import load_dotenv
from flask import Flask
from models import db
from sqlalchemy import text

load_dotenv()
sys.path.append(os.getcwd())

from app import create_app

def update_schema():
    app = create_app()
    with app.app_context():
        try:
            # Check if column exists
            result = db.session.execute(text(
                "SELECT column_name FROM information_schema.columns WHERE table_name='expense_item' AND column_name='measuring_unit'"
            ))
            if result.fetchone():
                print("Column 'measuring_unit' already exists.")
            else:
                print("Adding 'measuring_unit' column to 'expense_item' table...")
                db.session.execute(text("ALTER TABLE expense_item ADD COLUMN measuring_unit VARCHAR(20) DEFAULT 'Unit'"))
                db.session.commit()
                print("Column added successfully.")
                
        except Exception as e:
            print(f"Error updating schema: {e}")
            import traceback
            traceback.print_exc()

if __name__ == "__main__":
    update_schema()
