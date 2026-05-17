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

# Order of tables for referential integrity (though we use replica mode, it's good practice)
tables = [
    'company', 'app_user', 'project', 'vendor', 'master_category', 'sub_category',
    'client_payment', 'payment', 'expense', 'expense_item', 'audit_log'
]

dest_cur = dest_conn.cursor()
src_cur = src_conn.cursor(cursor_factory=psycopg2.extras.DictCursor)

print("Disabling foreign key checks on destination...")
dest_cur.execute("SET session_replication_role = replica;")

for table in tables:
    print(f"Truncating local table {table}...")
    dest_cur.execute(f"TRUNCATE TABLE {table} RESTART IDENTITY CASCADE;")

for table in tables:
    print(f"Fetching data from {table}...")
    try:
        src_cur.execute(f"SELECT * FROM {table}")
        rows = src_cur.fetchall()
        if not rows:
            print(f"  -> No data in {table}, skipping.")
            continue
            
        cols = list(rows[0].keys())
        col_str = ','.join(cols)
        placeholders = ','.join(['%s'] * len(cols))
        insert_query = f"INSERT INTO {table} ({col_str}) VALUES ({placeholders})"
        
        print(f"  -> Inserting {len(rows)} rows into local {table}...")
        psycopg2.extras.execute_batch(dest_cur, insert_query, rows)
    except psycopg2.errors.UndefinedTable:
        print(f"  -> Table {table} does not exist in source, skipping.")
        src_conn.rollback()

print("Re-enabling foreign key checks...")
dest_cur.execute("SET session_replication_role = DEFAULT;")

print("Updating sequences...")
for table in tables:
    dest_cur.execute(f"""
        SELECT column_name FROM information_schema.columns 
        WHERE table_name = '{table}' AND column_default LIKE 'nextval%%'
    """)
    seq_col = dest_cur.fetchone()
    if seq_col:
        col = seq_col[0]
        dest_cur.execute(f"SELECT pg_get_serial_sequence('{table}', '{col}')")
        seq_res = dest_cur.fetchone()
        if seq_res and seq_res[0]:
            seq_name = seq_res[0]
            dest_cur.execute(f"SELECT MAX({col}) FROM {table}")
            max_val = dest_cur.fetchone()[0]
            if max_val is not None:
                dest_cur.execute(f"SELECT setval('{seq_name}', {max_val})")

dest_conn.commit()
src_conn.close()
dest_conn.close()
print("🎉 Sync completed successfully!")
