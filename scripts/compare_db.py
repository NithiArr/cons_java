import psycopg2
import psycopg2.extras

print("Connecting to source (Render) database...")
src_conn = psycopg2.connect(
    host='dpg-d7i7as5ckfvc73evmme0-a.oregon-postgres.render.com',
    port=5432, dbname='cons_java', user='cons_java_user',
    password='LJWQRjMU2t42LPfJAyGqgT8Z97ukL4oq', sslmode='require'
)

print("Connecting to destination (Local) database...")
dest_conn = psycopg2.connect(
    host='localhost', port=5432, dbname='construction_db', user='postgres',
    password='admin123'
)

tables = [
    'company', 'app_user', 'project', 'vendor', 'master_category', 'sub_category',
    'client_payment', 'payment', 'expense', 'expense_item', 'audit_log'
]

dest_cur = dest_conn.cursor()
src_cur = src_conn.cursor()

diff_found = False

print("\n--- Comparing Tables ---")
for table in tables:
    try:
        src_cur.execute(f"SELECT COUNT(*) FROM {table}")
        src_count = src_cur.fetchone()[0]
    except psycopg2.errors.UndefinedTable:
        src_count = 0
        src_conn.rollback()

    try:
        dest_cur.execute(f"SELECT COUNT(*) FROM {table}")
        dest_count = dest_cur.fetchone()[0]
    except psycopg2.errors.UndefinedTable:
        dest_count = 0
        dest_conn.rollback()

    if src_count != dest_count:
        diff_found = True
        print(f"DIFFERENCE in '{table}': Render has {src_count} rows, Local has {dest_count} rows. Diff: {src_count - dest_count}")
        
        # If there's an id column, find the max id
        try:
            # Let's try to find primary key differences
            # Fallback to general difference check
            pass
        except:
            pass
    else:
        print(f"OK '{table}': Both have {src_count} rows")

if not diff_found:
    print("\nNo differences found in row counts.")

src_conn.close()
dest_conn.close()
