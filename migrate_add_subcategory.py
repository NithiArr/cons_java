import sqlite3

# Database migration: Add subcategory to expense table
db_path = 'instance/construction.db'
conn = sqlite3.connect(db_path)
cursor = conn.cursor()

print("=" * 60)
print("DATABASE MIGRATION: Adding Subcategory to Expenses")
print("=" * 60)

try:
    # Check if column already exists
    cursor.execute("PRAGMA table_info(expense)")
    columns = [col[1] for col in cursor.fetchall()]
    
    if 'subcategory' in columns:
        print("\nSubcategory column already exists. Skipping migration.")
    else:
        # Add subcategory column
        cursor.execute("""
            ALTER TABLE expense 
            ADD COLUMN subcategory VARCHAR(100)
        """)
        
        conn.commit()
        print("\nSUCCESS: Added subcategory column to expense table")
        print("  - Column: subcategory VARCHAR(100)")
        print("  - Nullable: Yes (existing records will have NULL)")
        print("  - Default: NULL")
    
    # Verify the change
    cursor.execute("PRAGMA table_info(expense)")
    print("\nUpdated Expense Table Schema:")
    print("-" * 60)
    for col in cursor.fetchall():
        col_id, name, col_type, not_null, default, pk = col
        print(f"  {name:20} {col_type:15} {'NOT NULL' if not_null else 'NULL':10} {'PK' if pk else ''}")
    
    print("\n" + "=" * 60)
    print("MIGRATION COMPLETED SUCCESSFULLY!")
    print("=" * 60)
    
except Exception as e:
    print(f"\nERROR: Migration failed - {e}")
    conn.rollback()
finally:
    conn.close()
