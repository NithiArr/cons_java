import os
import sys
from dotenv import load_dotenv
from sqlalchemy import create_engine, inspect

load_dotenv()
sys.path.append(os.getcwd())

def check_schema():
    uri = os.getenv("SQLALCHEMY_DATABASE_URI")
    engine = create_engine(uri)
    inspector = inspect(engine)
    
    print("\n--- SubCategory Columns ---")
    columns = inspector.get_columns('sub_category')
    for c in columns:
        print(f"{c['name']} ({c['type']})")

    print("\n--- ExpenseItem Columns ---")
    columns = inspector.get_columns('expense_item')
    for c in columns:
        print(f"{c['name']} ({c['type']})")

if __name__ == "__main__":
    check_schema()
