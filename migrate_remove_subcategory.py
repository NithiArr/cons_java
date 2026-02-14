import os
from mongoengine import connect
from dotenv import load_dotenv
import datetime
from pymongo import MongoClient

# Load environment variables
load_dotenv()

# MongoDB Connection
mongodb_host = os.environ.get('MONGODB_HOST', 'mongodb://localhost:27017/construction_db')
connect(host=mongodb_host)

print(f"Connected to MongoDB at {mongodb_host}")

client = MongoClient(mongodb_host)
db = client.get_database()
expenses_collection = db['expenses']

def migrate_remove_subcategory():
    print("Starting Phase 2 migration (Remove Subcategory)...")
    
    # 1. Backup
    backup_name = f"expenses_backup_phase2_{datetime.datetime.now().strftime('%Y%m%d%H%M%S')}"
    expenses = list(expenses_collection.find())
    if expenses:
        db[backup_name].insert_many(expenses)
        print(f"Backup created: {backup_name} with {len(expenses)} records")
    
    count = 0
    for expense in expenses_collection.find():
        updates = {}
        unset = {}
        
        # Move subcategory to description if present
        subcategory = expense.get('subcategory')
        description = expense.get('description', '')
        
        if subcategory:
            if description:
                new_description = f"{description} ({subcategory})"
            else:
                new_description = subcategory
            
            # Update description
            updates['description'] = new_description
            
        # Remove subcategory field
        unset['subcategory'] = ""
        
        update_op = {}
        if updates:
            update_op['$set'] = updates
        if unset:
            update_op['$unset'] = unset
            
        if update_op:
            expenses_collection.update_one({'_id': expense['_id']}, update_op)
            count += 1
            
    print(f"Migration completed. Updated {count} records.")

if __name__ == "__main__":
    migrate_remove_subcategory()
