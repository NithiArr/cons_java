import sqlite3
from werkzeug.security import generate_password_hash

# Connect to database
db_path = 'instance/construction.db'
conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# Get the company ID (TS Constructions)
cursor.execute("SELECT company_id FROM company LIMIT 1")
company = cursor.fetchone()

if not company:
    print("Company not found!")
    exit()

company_id = company[0]

# Define users for each role
users_to_create = [
    {
        'name': 'Manager User',
        'email': 'manager@tsconst.com',
        'password': 'manager123',
        'role': 'MANAGER'
    },
    {
        'name': 'Accountant User',
        'email': 'accountant@tsconst.com',
        'password': 'accountant123',
        'role': 'ACCOUNTANT'
    },
    {
        'name': 'Engineer User',
        'email': 'engineer@tsconst.com',
        'password': 'engineer123',
        'role': 'ENGINEER'
    }
]

print("=" * 70)
print("CREATING ROLE-SPECIFIC USER ACCOUNTS")
print("=" * 70)

created_users = []

for user_data in users_to_create:
    # Check if user already exists
    cursor.execute("SELECT email FROM user WHERE email = ?", (user_data['email'],))
    existing = cursor.fetchone()
    
    if existing:
        print(f"\nUser {user_data['email']} already exists - skipping")
        # Get user_id for existing user
        cursor.execute("SELECT user_id FROM user WHERE email = ?", (user_data['email'],))
        user_id = cursor.fetchone()[0]
        created_users.append({**user_data, 'user_id': user_id, 'status': 'EXISTS'})
    else:
        # Hash the password
        password_hash = generate_password_hash(user_data['password'])
        
        # Insert new user
        cursor.execute("""
            INSERT INTO user (company_id, name, email, password_hash, role, created_at)
            VALUES (?, ?, ?, ?, ?, datetime('now'))
        """, (company_id, user_data['name'], user_data['email'], password_hash, user_data['role']))
        
        user_id = cursor.lastrowid
        created_users.append({**user_data, 'user_id': user_id, 'status': 'CREATED'})
        print(f"\nCreated {user_data['role']}: {user_data['email']}")

conn.commit()

print("\n" + "=" * 70)
print("USER ACCOUNTS SUMMARY")
print("=" * 70)

for user in created_users:
    print(f"\n{user['status']}: {user['role']}")
    print(f"  User ID:  {user['user_id']}")
    print(f"  Name:     {user['name']}")
    print(f"  Email:    {user['email']}")
    print(f"  Password: {user['password']}")
    print(f"  Access:   ", end="")
    
    if user['role'] == 'MANAGER':
        print("Projects (CRUD), Purchases (CRUD), Vendors (CRUD)")
    elif user['role'] == 'ACCOUNTANT':
        print("Payments (CRUD), Expenses (CRUD), Client Payments (CRUD)")
    elif user['role'] == 'ENGINEER':
        print("Projects (View Only), Dashboard (View Only)")
    
    print("-" * 70)

# Get all users for final summary
cursor.execute("""
    SELECT u.user_id, u.name, u.email, u.role
    FROM user u
    WHERE u.company_id = ?
    ORDER BY u.user_id
""", (company_id,))

all_users = cursor.fetchall()

print("\n" + "=" * 70)
print("ALL USERS IN YOUR COMPANY")
print("=" * 70)

for user in all_users:
    user_id, name, email, role = user
    print(f"\nID: {user_id} | {role:12} | {email:30} | {name}")

print("\n" + "=" * 70)
print("QUICK REFERENCE - LOGIN CREDENTIALS")
print("=" * 70)
print("\nOWNER (Read-only, all data):")
print("  Email: nithiarrvind@gmail.com")
print("  Password: [Your original password]")

print("\nADMIN (Full CRUD access):")
print("  Email: admin@sorgavasal.com")
print("  Password: admin123")

print("\nMANAGER (Projects & Purchases):")
print("  Email: manager@sorgavasal.com")
print("  Password: manager123")

print("\nACCOUNTANT (Payments & Expenses):")
print("  Email: accountant@sorgavasal.com")
print("  Password: accountant123")

print("\nENGINEER (View-only):")
print("  Email: engineer@sorgavasal.com")
print("  Password: engineer123")

print("\n" + "=" * 70)
print("Login at: http://127.0.0.1:5000")
print("=" * 70)

conn.close()
