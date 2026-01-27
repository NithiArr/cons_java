import sqlite3
from datetime import datetime, timedelta

# Connect to database
db_path = 'instance/construction.db'
conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# Get the company ID (Sorgavasal)
cursor.execute("SELECT company_id FROM company WHERE name = 'Sorgavasal'")
company = cursor.fetchone()

if not company:
    print("Company not found! Please ensure you have registered first.")
    exit()

company_id = company[0]

print("=" * 60)
print("CREATING SAMPLE DATA FOR SORGAVASAL")
print("=" * 60)

# 1. CREATE PROJECTS
print("\n1. Creating Projects...")
projects_data = [
    ('Skyline Tower', 'Bandra West, Mumbai', '2025-01-01', '2025-12-31', 15000000, 'ACTIVE'),
    ('Green Valley Apartments', 'Powai, Mumbai', '2025-02-15', '2026-03-15', 8000000, 'ACTIVE'),
    ('Corporate Office Complex', 'Andheri East, Mumbai', '2024-11-01', '2025-10-30', 12000000, 'ACTIVE'),
    ('Luxury Villa Project', 'Lonavala', '2025-03-01', '2025-09-30', 5000000, 'PLANNING'),
    ('Old City Mall Renovation', 'Dadar', '2024-08-01', '2024-12-31', 3000000, 'COMPLETED'),
]

for project in projects_data:
    cursor.execute("""
        INSERT INTO project (company_id, name, location, start_date, end_date, budget, status, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'))
    """, (company_id,) + project)

print(f"   Created {len(projects_data)} projects")

# 2. CREATE VENDORS
print("\n2. Creating Vendors...")
vendors_data = [
    ('Steel Suppliers Ltd', '9876543210', 'steel@suppliers.com', '27AABCS1234F1Z1'),
    ('Cement Corporation', '9876543211', 'info@cement.com', '27AABCC5678G2Z2'),
    ('Brick Masters', '9876543212', 'sales@brickmasters.com', '27AABCB9012H3Z3'),
    ('Electrical Solutions', '9876543213', 'contact@electrical.com', '27AABCE3456I4Z4'),
    ('Plumbing Pro', '9876543214', 'info@plumbingpro.com', '27AABCP7890J5Z5'),
    ('Paint & Finishes Co', '9876543215', 'sales@paints.com', '27AABCF1122K6Z6'),
    ('Hardware Traders', '9876543216', 'orders@hardware.com', '27AABCH3344L7Z7'),
]

for vendor in vendors_data:
    cursor.execute("""
        INSERT INTO vendor (company_id, name, phone, email, gst_number, created_at)
        VALUES (?, ?, ?, ?, ?, datetime('now'))
    """, (company_id,) + vendor)

print(f"   Created {len(vendors_data)} vendors")

# Get created IDs
cursor.execute("SELECT project_id FROM project WHERE company_id = ? ORDER BY project_id", (company_id,))
project_ids = [row[0] for row in cursor.fetchall()]

cursor.execute("SELECT vendor_id FROM vendor WHERE company_id = ? ORDER BY vendor_id", (company_id,))
vendor_ids = [row[0] for row in cursor.fetchall()]

# 3. CREATE PURCHASES WITH ITEMS
print("\n3. Creating Purchases with Items...")
purchases_data = [
    # Project 1 - Skyline Tower
    (project_ids[0], vendor_ids[0], 'INV-2025-001', '2025-01-15', 500000, 'CREDIT', [
        ('Steel Bars (12mm)', 1000, 450, 450000),
        ('Steel Plates', 50, 1000, 50000)
    ]),
    (project_ids[0], vendor_ids[1], 'INV-2025-002', '2025-01-20', 300000, 'CREDIT', [
        ('Cement (50kg bags)', 1000, 300, 300000)
    ]),
    (project_ids[0], vendor_ids[2], 'INV-2025-003', '2025-02-01', 200000, 'CASH', [
        ('Red Bricks', 10000, 15, 150000),
        ('AAC Blocks', 500, 100, 50000)
    ]),
    
    # Project 2 - Green Valley
    (project_ids[1], vendor_ids[0], 'INV-2025-010', '2025-02-20', 350000, 'CREDIT', [
        ('Steel Bars (16mm)', 800, 400, 320000),
        ('Binding Wire', 100, 300, 30000)
    ]),
    (project_ids[1], vendor_ids[3], 'INV-2025-011', '2025-03-01', 180000, 'CREDIT', [
        ('Electrical Cables', 500, 200, 100000),
        ('Switch Boards', 100, 800, 80000)
    ]),
    
    # Project 3 - Corporate Office
    (project_ids[2], vendor_ids[1], 'INV-2025-020', '2025-01-10', 400000, 'CREDIT', [
        ('Cement (50kg bags)', 1200, 300, 360000),
        ('Sand (Truckload)', 4, 10000, 40000)
    ]),
    (project_ids[2], vendor_ids[5], 'INV-2025-021', '2025-02-15', 250000, 'CREDIT', [
        ('Asian Paints - Exterior', 500, 400, 200000),
        ('Interior Emulsion', 250, 200, 50000)
    ]),
]

for purchase in purchases_data:
    project_id, vendor_id, invoice_num, invoice_date, total, payment_type, items = purchase
    
    # Insert purchase
    cursor.execute("""
        INSERT INTO purchase (company_id, project_id, vendor_id, invoice_number, invoice_date, 
                            total_amount, payment_type, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'))
    """, (company_id, project_id, vendor_id, invoice_num, invoice_date, total, payment_type))
    
    purchase_id = cursor.lastrowid
    
    # Insert purchase items
    for item in items:
        cursor.execute("""
            INSERT INTO purchase_item (purchase_id, item_name, quantity, unit_price, total_price)
            VALUES (?, ?, ?, ?, ?)
        """, (purchase_id,) + item)

print(f"   Created {len(purchases_data)} purchases with items")

# 4. CREATE EXPENSES
print("\n4. Creating Expenses...")
expenses_data = [
    (project_ids[0], 'Labor - Skilled', 150000, 'BANK', '2025-01-25', 'Monthly wages for masons'),
    (project_ids[0], 'Labor - Helper', 80000, 'CASH', '2025-01-25', 'Monthly wages for helpers'),
    (project_ids[0], 'Equipment Rental', 45000, 'UPI', '2025-02-01', 'Crane rental for 5 days'),
    (project_ids[0], 'Transportation', 25000, 'CASH', '2025-02-05', 'Material transportation'),
    
    (project_ids[1], 'Labor - Skilled', 120000, 'BANK', '2025-02-28', 'Monthly wages'),
    (project_ids[1], 'Site Supervision', 60000, 'BANK', '2025-03-01', 'Engineer salary'),
    (project_ids[1], 'Safety Equipment', 35000, 'UPI', '2025-03-05', 'Helmets, boots, safety nets'),
    
    (project_ids[2], 'Labor - Skilled', 180000, 'BANK', '2025-01-31', 'Monthly wages for masons'),
    (project_ids[2], 'Miscellaneous', 15000, 'CASH', '2025-02-10', 'Small tools and supplies'),
    (project_ids[2], 'Electricity Bill', 42000, 'BANK', '2025-02-20', 'Site electricity charges'),
]

for expense in expenses_data:
    cursor.execute("""
        INSERT INTO expense (company_id, project_id, category, amount, payment_mode, 
                           expense_date, description, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'))
    """, (company_id,) + expense)

print(f"   Created {len(expenses_data)} expenses")

# Get purchase IDs for payments
cursor.execute("SELECT purchase_id, vendor_id, project_id, total_amount FROM purchase WHERE company_id = ?", (company_id,))
all_purchases = cursor.fetchall()

# 5. CREATE VENDOR PAYMENTS
print("\n5. Creating Vendor Payments...")
payments_data = [
    # Partial payments for purchases
    (vendor_ids[0], project_ids[0], all_purchases[0][0], 250000, '2025-02-01', 'BANK'),  # Steel payment
    (vendor_ids[1], project_ids[0], all_purchases[1][0], 150000, '2025-02-05', 'BANK'),  # Cement payment
    (vendor_ids[2], project_ids[0], all_purchases[2][0], 200000, '2025-02-10', 'CASH'),  # Bricks full payment
    
    (vendor_ids[0], project_ids[1], all_purchases[3][0], 200000, '2025-03-10', 'BANK'),  # Steel partial
    (vendor_ids[3], project_ids[1], all_purchases[4][0], 100000, '2025-03-15', 'UPI'),   # Electrical partial
    
    (vendor_ids[1], project_ids[2], all_purchases[5][0], 400000, '2025-02-15', 'BANK'),  # Cement full
]

for payment in payments_data:
    cursor.execute("""
        INSERT INTO payment (company_id, vendor_id, project_id, purchase_id, amount, 
                           payment_date, payment_mode, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'))
    """, (company_id,) + payment)

print(f"   Created {len(payments_data)} vendor payments")

# 6. CREATE CLIENT PAYMENTS
print("\n6. Creating Client Payments...")
client_payments_data = [
    (project_ids[0], 5000000, '2025-01-10', 'BANK', 'CHQ-001234', 'Initial advance payment'),
    (project_ids[0], 3000000, '2025-02-15', 'BANK', 'CHQ-001456', 'First milestone payment'),
    
    (project_ids[1], 2500000, '2025-02-25', 'BANK', 'NEFT-789012', 'Booking amount'),
    (project_ids[1], 1500000, '2025-03-20', 'BANK', 'NEFT-789345', 'Construction start payment'),
    
    (project_ids[2], 4000000, '2025-01-05', 'BANK', 'CHQ-002345', 'Advance payment'),
    (project_ids[2], 3500000, '2025-02-25', 'BANK', 'CHQ-002678', 'Progress payment'),
    
    (project_ids[4], 3000000, '2024-12-30', 'BANK', 'CHQ-000123', 'Final payment on completion'),
]

for payment in client_payments_data:
    cursor.execute("""
        INSERT INTO client_payment (company_id, project_id, amount, payment_date, 
                                   payment_mode, reference_number, remarks, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'))
    """, (company_id,) + payment)

print(f"   Created {len(client_payments_data)} client payments")

# Commit all changes
conn.commit()
conn.close()

print("\n" + "=" * 60)
print("SAMPLE DATA CREATED SUCCESSFULLY!")
print("=" * 60)
print("\nSummary:")
print(f"  - {len(projects_data)} Projects")
print(f"  - {len(vendors_data)} Vendors")
print(f"  - {len(purchases_data)} Purchases with multiple items")
print(f"  - {len(expenses_data)} Expenses")
print(f"  - {len(payments_data)} Vendor Payments")
print(f"  - {len(client_payments_data)} Client Payments")
print("\nYou can now:")
print("  1. View projects on Owner Dashboard")
print("  2. Check Vendor Analytics")
print("  3. See Daily Cash Balance")
print("  4. Explore all CRUD pages with real data")
print("\nLogin at: http://127.0.0.1:5000")
print("=" * 60)
