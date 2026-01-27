import sqlite3
from werkzeug.security import generate_password_hash

# Connect to database
db_path = 'instance/construction.db'
conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# Get the company ID (Sorgavasal)
cursor.execute("SELECT company_id FROM company WHERE name = 'Sorgavasal'")
company = cursor.fetchone()

if not company:
    print("Company not found! Please check database.")
    exit()

company_id = company[0]

# Admin user details
admin_email = "admin@sorgavasal.com"
admin_password = "admin123"  # Simple password for testing
admin_name = "Admin User"


admin_role = "ADMIN"

# Check if admin already exists
cursor.execute("SELECT email FROM user WHERE email = ?", (admin_email,))
existing = cursor.fetchone()

if existing:
    print(f"Admin user already exists with email: {admin_email}")
else:
    # Hash the password
    password_hash = generate_password_hash(admin_password)
    
    # Insert new admin user
    cursor.execute("""
        INSERT INTO user (company_id, name, email, password_hash, role, created_at)
        VALUES (?, ?, ?, ?, ?, datetime('now'))
    """, (company_id, admin_name, admin_email, password_hash, admin_role))
    
    conn.commit()
    print("=" * 60)
    print("ADMIN ACCOUNT CREATED SUCCESSFULLY!")
    print("=" * 60)
    print(f"\nEmail:    {admin_email}")
    print(f"Password: {admin_password}")
    print(f"Role:     {admin_role}")
    print(f"Company:  Sorgavasal")
    print("\n" + "=" * 60)
    print("LOGIN INSTRUCTIONS:")
    print("=" * 60)
    print("1. Go to: http://127.0.0.1:5000")
    print("2. Enter the credentials above")
    print("3. As ADMIN, you have FULL access to:")
    print("   - Create/Edit/Delete Projects")
    print("   - Manage Vendors")
    print("   - Record Purchases")
    print("   - Add Expenses")
    print("   - Record Payments")
    print("   - Manage Users")
    print("=" * 60)

conn.close()
