import psycopg2
from psycopg2.extras import execute_values

# ── CONFIG ────────────────────────────────────────────────────────────────────
DB_RAILWAY = "postgresql://postgres:WodrjpqAtVDCnpfffPRiqIIsPQcjfZLg@autorack.proxy.rlwy.net:47353/railway"
DB_LOCAL = "postgresql://postgres:admin123@localhost:5432/construction_db"

def main():
    print("======================================================================")
    print("  🚀 CLONING RAILWAY DATABASE TO LOCAL DB (Table-by-Table via Python)")
    print("======================================================================")

    # 1. Connect to both databases
    try:
        src_conn = psycopg2.connect(DB_RAILWAY)
        src_cur = src_conn.cursor()
        print("  ✓ Connected to source database (Railway)")
    except Exception as e:
        print(f"  ❌ Failed to connect to Railway database: {e}")
        return

    try:
        tgt_conn = psycopg2.connect(DB_LOCAL)
        tgt_cur = tgt_conn.cursor()
        print("  ✓ Connected to target database (Local)")
    except Exception as e:
        print(f"  ❌ Failed to connect to Local database: {e}")
        src_conn.close()
        return

    # 2. Get list of tables in public schema
    src_cur.execute("""
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = 'public' 
          AND table_type = 'BASE TABLE'
        ORDER BY table_name;
    """)
    tables = [r[0] for r in src_cur.fetchall()]
    print(f"  Found {len(tables)} tables to copy.")

    # 3. Disable all triggers locally to bypass foreign key checks
    print("\n[1/5] Disabling triggers on local tables...")
    for t in tables:
        try:
            tgt_cur.execute(f"ALTER TABLE {t} DISABLE TRIGGER ALL;")
        except Exception as e:
            print(f"  ⚠️ Could not disable triggers on {t}: {e}")
            tgt_conn.rollback()

    # 3b. Align nullability of columns locally to match production (Railway)
    print("\n[1b/5] Aligning column nullability with Railway...")
    for t in tables:
        src_cur.execute(f"""
            SELECT column_name 
            FROM information_schema.columns 
            WHERE table_name = '{t}' 
              AND table_schema = 'public' 
              AND is_nullable = 'YES'
        """)
        nullable_cols = [r[0] for r in src_cur.fetchall()]
        for col in nullable_cols:
            try:
                # Alter local table to drop NOT NULL constraint
                tgt_cur.execute(f"ALTER TABLE {t} ALTER COLUMN {col} DROP NOT NULL;")
            except Exception as e:
                tgt_conn.rollback()
    tgt_conn.commit()


    # 4. Truncate all local tables
    print("\n[2/5] Wiping local tables...")
    for t in tables:
        try:
            # CASCADE ensures dependent rows are deleted, RESTART IDENTITY resets sequences
            tgt_cur.execute(f"TRUNCATE TABLE {t} RESTART IDENTITY CASCADE;")
            print(f"  ✓ Wiped table: {t}")
        except Exception as e:
            print(f"  ❌ Failed to truncate {t}: {e}")
            tgt_conn.rollback()
            # If truncate fails, we abort
            src_conn.close()
            tgt_conn.close()
            return
    tgt_conn.commit()

    # 5. Copy data table-by-table
    print("\n[3/5] Copying data from Railway to Local...")
    for t in tables:
        print(f"  Copying table {t} ...", end="", flush=True)
        try:
            # Get column names
            src_cur.execute(f"SELECT * FROM {t} LIMIT 0")
            colnames = [desc[0] for desc in src_cur.description]
            
            # Query all rows
            src_cur.execute(f"SELECT * FROM {t}")
            rows = src_cur.fetchall()
            
            if rows:
                col_str = ", ".join(f'"{c}"' for c in colnames)
                placeholders = ", ".join(["%s"] * len(colnames))
                insert_query = f"INSERT INTO {t} ({col_str}) VALUES ({placeholders})"
                
                # Execute insert locally
                execute_values_list = []
                for row in rows:
                    execute_values_list.append(row)
                
                # Use psycopg2 execute_values for fast bulk insertion
                # Note: execute_values expects cursor, query, and values
                # We can do this in chunks of 1000
                chunk_size = 1000
                for i in range(0, len(execute_values_list), chunk_size):
                    chunk = execute_values_list[i:i+chunk_size]
                    tgt_cur.executemany(insert_query, chunk)
                
                print(f" ✓ Copied {len(rows)} rows")
            else:
                print(" (empty table)")
        except Exception as e:
            print(f"\n  ❌ Failed to copy table {t}: {e}")
            tgt_conn.rollback()
            src_conn.close()
            tgt_conn.close()
            return
    tgt_conn.commit()

    # 6. Re-enable triggers locally
    print("\n[4/5] Re-enabling triggers on local tables...")
    for t in tables:
        try:
            tgt_cur.execute(f"ALTER TABLE {t} ENABLE TRIGGER ALL;")
        except Exception as e:
            print(f"  ❌ Could not enable triggers on {t}: {e}")
            tgt_conn.rollback()
    tgt_conn.commit()

    # 7. Reset all sequences locally
    print("\n[5/5] Resetting sequence values...")
    for t in tables:
        # Check if table has columns
        tgt_cur.execute(f"SELECT column_name FROM information_schema.columns WHERE table_name = '{t}' AND table_schema = 'public'")
        cols = [r[0] for r in tgt_cur.fetchall()]
        
        # Check if there is an id column with sequence
        id_col = None
        for c in cols:
            if c in (f'{t}_id', 'id', 'subcategory_id', 'category_id', 'expense_id', 'expense_item_id', 'payment_id', 'client_payment_id'):
                id_col = c
                break
        
        if id_col:
            try:
                # Find the sequence name associated with the column
                tgt_cur.execute(f"SELECT pg_get_serial_sequence('{t}', '{id_col}')")
                seq = tgt_cur.fetchone()[0]
                if seq:
                    # Find maximum value in the column
                    tgt_cur.execute(f"SELECT COALESCE(MAX({id_col}), 0) FROM {t}")
                    max_val = tgt_cur.fetchone()[0]
                    next_val = max(1, max_val + 1)
                    
                    tgt_cur.execute(f"ALTER SEQUENCE {seq} RESTART WITH {next_val}")
                    print(f"  ✓ Sequence {seq} set to restart with {next_val} (max = {max_val})")
            except Exception as e:
                # Ignore sequence errors for non-serial PKs
                tgt_conn.rollback()

    tgt_conn.commit()
    print("\n======================================================================")
    print("  🎉 Database cloning completed successfully!")
    print("  Local database is now a 1:1 copy of Railway database.")
    print("======================================================================")

    src_conn.close()
    tgt_conn.close()

if __name__ == "__main__":
    main()
