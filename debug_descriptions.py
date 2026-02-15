import os
import sys
from dotenv import load_dotenv
from models import db, Expense
from app import create_app

load_dotenv()
sys.path.append(os.getcwd())

def check_descriptions():
    app = create_app()
    with app.app_context():
        expenses = Expense.query.filter(Expense.description.like('%(%)%')).all()
        print(f"Found {len(expenses)} expenses with parentheses in description:")
        for e in expenses:
            print(f"ID: {e.expense_id}, Desc: '{e.description}'")

if __name__ == "__main__":
    check_descriptions()
