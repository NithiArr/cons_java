import os
import subprocess
import psycopg2

# ── CONFIG ────────────────────────────────────────────────────────────────────
# Railway Production Database
RW_HOST = "autorack.proxy.rlwy.net"
RW_PORT = "47353"
RW_DB   = "railway"
RW_USER = "postgres"
RW_PASS = "WodrjpqAtVDCnpfffPRiqIIsPQcjfZLg"

# Local Development Database
LC_HOST = "localhost"
LC_PORT = "5432"
LC_DB   = "construction_db"
LC_USER = "postgres"
LC_PASS = "admin123"

BACKUP_FILE = "railway_backup.sql"
TABLES = ['project', 'vendor', 'expense', 'expense_item', 'payment', 'client_payment', 'sub_category', 'master_category']

# ── FUNCTIONS ─────────────────────────────────────────────────────────────────
def run_command(args, env):
    try:
        # Run process and capture output
        res = subprocess.run(args, env=env, capture_output=True, text=True, check=True)
        return True, res.stdout, res.stderr
    except subprocess.CalledProcessError as e:
        return False, e.stdout, e.stderr

def get_row_counts(host, port, db, user, pwd, name):
    try:
        conn = psycopg2.connect(host=host, port=port, dbname=db, user=user, password=pwd, connect_timeout=5)
        cur = conn.cursor()
        counts = {}
        for t in TABLES:
            try:
                cur.execute(f"SELECT COUNT(*) FROM {t}")
                counts[t] = cur.fetchone()[0]
            except Exception as e:
                counts[t] = "Error"
                conn.rollback()
        cur.close()
        conn.close()
        return counts
    except Exception as e:
        print(f"  ❌ Could not connect to {name}: {e}")
        return None

def main():
    print("======================================================================")
    print("  🚀 CLONING RAILWAY DATABASE TO LOCAL DEVELOPMENT DATABASE")
    print("======================================================================")

    # ── 1. Dump Railway Database ──
    print("\n[1/4] Dumping Railway database...")
    dump_env = os.environ.copy()
    dump_env["PGPASSWORD"] = RW_PASS
    
    # pg_dump command
    dump_args = [
        "pg_dump",
        "-h", RW_HOST,
        "-p", RW_PORT,
        "-U", RW_USER,
        "-d", RW_DB,
        "-F", "p",  # Plain text SQL format
        "-f", BACKUP_FILE
    ]
    
    success, stdout, stderr = run_command(dump_args, dump_env)
    if not success:
        print(f"  ❌ Failed to dump database:\n{stderr}")
        return
    print(f"  ✓ Dumped database successfully to: {BACKUP_FILE}")

    # ── 2. Wipe Local Database public schema ──
    print("\n[2/4] Wiping local database (public schema)...")
    try:
        conn = psycopg2.connect(host=LC_HOST, port=LC_PORT, dbname=LC_DB, user=LC_USER, password=LC_PASS)
        conn.autocommit = True
        cur = conn.cursor()
        
        # Drop public schema and recreate it to cleanly delete all tables, views, etc.
        cur.execute("DROP SCHEMA public CASCADE;")
        cur.execute("CREATE SCHEMA public;")
        cur.execute("GRANT ALL ON SCHEMA public TO postgres;")
        cur.execute("GRANT ALL ON SCHEMA public TO public;")
        
        cur.close()
        conn.close()
        print("  ✓ Local database public schema wiped cleanly.")
    except Exception as e:
        print(f"  ❌ Failed to wipe local database: {e}")
        return

    # ── 3. Restore to Local Database ──
    print("\n[3/4] Restoring backup to local database...")
    restore_env = os.environ.copy()
    restore_env["PGPASSWORD"] = LC_PASS
    
    # psql command
    restore_args = [
        "psql",
        "-h", LC_HOST,
        "-p", LC_PORT,
        "-U", LC_USER,
        "-d", LC_DB,
        "-f", BACKUP_FILE
    ]
    
    success, stdout, stderr = run_command(restore_args, restore_env)
    # Note: restore might output some warnings/errors about owner/grant checks, but it's typically fine.
    if not success:
        print(f"  ❌ Failed to restore database:\n{stderr}")
        return
    print("  ✓ Restored backup to local database.")

    # ── 4. Verify Row Counts ──
    print("\n[4/4] Verifying data row counts...")
    rw_counts = get_row_counts(RW_HOST, RW_PORT, RW_DB, RW_USER, RW_PASS, "Railway DB")
    lc_counts = get_row_counts(LC_HOST, LC_PORT, LC_DB, LC_USER, LC_PASS, "Local DB")
    
    if rw_counts and lc_counts:
        print("\n--- CLONE VERIFICATION ---")
        print(f"{'TABLE':<20} | {'RAILWAY (PROD)':<16} | {'LOCAL (DEV)':<12} | {'STATUS'}")
        print("-" * 65)
        mismatch = False
        for t in TABLES:
            rw_val = rw_counts.get(t, "N/A")
            lc_val = lc_counts.get(t, "N/A")
            status = "✅ MATCH" if rw_val == lc_val else "❌ MISMATCH"
            if rw_val != lc_val:
                mismatch = True
            print(f"{t:<20} | {str(rw_val):<16} | {str(lc_val):<12} | {status}")
        
        print("-" * 65)
        if mismatch:
            print("  ⚠️ Warning: Some row counts mismatch. Please verify manually.")
        else:
            print("  🎉 Database clone completed successfully! All data matches perfectly.")
    else:
        print("  ⚠️ Could not verify row counts due to connection issues.")

    # Clean up temporary backup file
    if os.path.exists(BACKUP_FILE):
        try:
            os.remove(BACKUP_FILE)
            print(f"\n  ✓ Removed temporary backup file: {BACKUP_FILE}")
        except Exception as e:
            print(f"\n  ⚠️ Could not remove temporary file {BACKUP_FILE}: {e}")

if __name__ == "__main__":
    main()
