import sqlite3
import os

# Flask creates database in instance folder
db_path = 'instance/construction.db'

if not os.path.exists(db_path):
    print("Database file not found!")
    print(f"Looking for: {os.path.abspath(db_path)}")
    print("\nMake sure you've run 'python app.py' at least once to create the database.")
else:
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    print("=" * 60)
    print("REGISTERED USERS")
    print("=" * 60)
    
    # Get all users with company info
    cursor.execute("""
        SELECT u.user_id, u.name, u.email, u.role, c.name as company_name
        FROM user u
        JOIN company c ON u.company_id = c.company_id
        ORDER BY u.user_id
    """)
    
    users = cursor.fetchall()
    
    if not users:
        print("\nNO USERS FOUND IN DATABASE!")
        print("\nThis means:")
        print("1. The database exists but is empty")
        print("2. Registration may have failed")
        print("3. Or you haven't registered yet")
        print("\nPlease try registering again at: http://127.0.0.1:5000")
    else:
        print(f"\nFound {len(users)} user(s):\n")
        for user in users:
            user_id, name, email, role, company = user
            print(f"User ID: {user_id}")
            print(f"  Name:     {name}")
            print(f"  Email:    {email}  <- USE THIS TO LOGIN")
            print(f"  Role:     {role}")
            print(f"  Company:  {company}")
            print(f"  Password: (hashed - use the password you set during registration)")
            print("-" * 60)
        
        print("\n" + "=" * 60)
        print("TO LOGIN:")
        print("=" * 60)
        print(f"Email:    {users[0][2]}")
        print("Password: The password YOU chose during registration")
        print("\nGo to: http://127.0.0.1:5000")
        print("=" * 60)
    
    # Also show companies
    print("\n" + "=" * 60)
    print("REGISTERED COMPANIES")
    print("=" * 60)
    cursor.execute("SELECT company_id, name, email, phone FROM company")
    companies = cursor.fetchall()
    
    for company in companies:
        comp_id, name, email, phone = company
        print(f"\nCompany ID: {comp_id}")
        print(f"Name:       {name}")
        print(f"Email:      {email}")
        print(f"Phone:      {phone}")
    
    conn.close()
