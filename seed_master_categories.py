import os
from mongoengine import connect
from dotenv import load_dotenv
from models_mongo import MasterCategory, SubCategoryItem

# Load environment variables
load_dotenv()

# MongoDB Connection
mongodb_host = os.environ.get('MONGODB_HOST', 'mongodb://localhost:27017/construction_db')
connect(host=mongodb_host)

print(f"Connected to MongoDB at {mongodb_host}")

def seed_master_categories():
    print("Seeding Master Categories...")
    
    # Clear existing
    MasterCategory.objects.delete()
    
    # 1. Material Categories
    materials = {
        "Steel": {"items": ["8mm", "10mm", "12mm", "16mm", "20mm", "25mm", "32mm", "36mm"], "unit": "kg"},
        "Building Materials": {"items": ["Cement", "M-sand", "P-sand", "20mm Jally", "40mm Jally", "Gravel", "Baby Chips", "Wet Mix", "Others"], "unit": "unit"},
        "Brick": {"items": ["Red Brick-wirecut", "Red Brick-chamber", "Fly ash", "Solid block (4\", 6\", 12\")", "AAC block", "Hollow block"], "unit": "nos"},
        "Painting": {"items": ["Primer", "Emulsion", "Putty", "White cement", "Other"], "unit": "ltr"},
        "Plumbing": {"items": ["Fitting", "PVC", "UPVC", "CPVC", "Other"], "unit": "nos"},
        "Electrical": {"items": ["Wires", "Switches", "Pipes"], "unit": "nos"}
    }
    
    for name, data in materials.items():
        items = [SubCategoryItem(name=item, default_unit=data['unit']) for item in data['items']]
        MasterCategory(name=name, type='MATERIAL', subcategories=items).save()
        print(f"Added Material: {name}")

    # 2. Expense Categories
    expenses = {
        "Labour": ["Mason", "Centring", "Painter", "Electrician", "Plumber"],
        "Rental": ["Centring Rental", "JCB", "Bobcat"],
        "Transport": ["Labour", "Material", "Other"],
        "Petty Cash": ["Tea", "Others"],
        "TSK Expense": ["Salary", "Others", "Withdrawal", "Chit"]
    }
    
    for name, items in expenses.items():
        sub_items = [SubCategoryItem(name=item) for item in items]
        MasterCategory(name=name, type='EXPENSE', subcategories=sub_items).save()
        print(f"Added Expense: {name}")
        
    print("Seeding completed successfully.")

if __name__ == "__main__":
    seed_master_categories()
