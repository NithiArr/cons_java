import os
from mongoengine import connect, Document, StringField, FloatField, DateField, ReferenceField, EmbeddedDocumentListField, EmbeddedDocument
from dotenv import load_dotenv
import datetime

# Load environment variables
load_dotenv()

# MongoDB Connection
mongodb_host = os.environ.get('MONGODB_HOST', 'mongodb://localhost:27017/construction_db')
connect(host=mongodb_host)

print(f"Connected to MongoDB at {mongodb_host}")

# Define a raw pymongo connection for direct manipulation
from pymongo import MongoClient
client = MongoClient(mongodb_host)
db = client.get_database()
expenses_collection = db['expenses']

def migrate_expenses():
    print("Starting migration...")
    
    # 1. Backup
    backup_name = f"expenses_backup_{datetime.datetime.now().strftime('%Y%m%d%H%M%S')}"
    expenses = list(expenses_collection.find())
    if expenses:
        db[backup_name].insert_many(expenses)
        print(f"Backup created: {backup_name} with {len(expenses)} records")
    else:
        print("No expenses found to migrate.")
        return

    count = 0
    for expense in expenses_collection.find():
        updates = {}
        rename = {}
        
        # Current data:
        # category -> (Old: 'Material Purchase' / 'Regular Expense')
        # subcategory -> (Old: 'Cement', 'Travel')
        
        # New data:
        # expense_type -> (Old category)
        # category -> (Old subcategory)
        # subcategory -> (New: Item Name or Description)
        
        current_category = expense.get('category')
        current_subcategory = expense.get('subcategory')
        
        # 1. Prepare new values
        new_expense_type = current_category
        new_category_value = current_subcategory
        
        # 2. Determine new subcategory (Item Name)
        new_subcategory = None
        
        if current_category == 'Material Purchase':
            # Use the first item's name if available
            items = expense.get('items', [])
            if items and len(items) > 0:
                new_subcategory = items[0].get('item_name')
            else:
                new_subcategory = 'Unknown Material'
                
            # Update items with defaults if needed
            new_items = []
            for item in items:
                if 'measuring_unit' not in item:
                    item['measuring_unit'] = 'Unit' # Default
                new_items.append(item)
            updates['items'] = new_items
            
        else:
            # Regular Expense
            # For regular expenses, the old 'subcategory' was the category (e.g. 'Travel')
            # The new 'category' will be 'Travel'
            # The new 'subcategory' can be the description or 'General'
            if expense.get('description'):
                new_subcategory = expense.get('description')[:50]
            else:
                new_subcategory = 'General'
        
        # 3. Construct $set update
        # We set ALL fields explicitly to avoid conflicts
        updates['expense_type'] = new_expense_type
        updates['category'] = new_category_value
        updates['subcategory'] = new_subcategory
        
        # Apply updates
        if updates:
            expenses_collection.update_one({'_id': expense['_id']}, {'$set': updates})
            count += 1
            
    print(f"Migration completed. Updated {count} records.")

if __name__ == "__main__":
    migrate_expenses()
