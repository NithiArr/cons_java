import os
import sys
import re
from dotenv import load_dotenv
from models import db, Expense
from app import create_app

load_dotenv()
sys.path.append(os.getcwd())

def clean_descriptions():
    app = create_app()
    with app.app_context():
        # Fetch expenses with parentheses
        expenses = Expense.query.filter(Expense.description.like('%(%)%')).all()
        
        count = 0
        for e in expenses:
            old_desc = e.description
            # Regex to remove space followed by (content) at end of string
            new_desc = re.sub(r'\s*\([^)]*\)$', '', old_desc)
            
            # Trim just in case
            new_desc = new_desc.strip()
            
            if old_desc != new_desc:
                print(f"Updating ID {e.expense_id}: '{old_desc}' -> '{new_desc}'")
                e.description = new_desc
                count += 1
        
        if count > 0:
            db.session.commit()
            print(f"\nSuccessfully updated {count} expenses.")
        else:
            print("\nNo expenses needed updating.")

if __name__ == "__main__":
    clean_descriptions()
