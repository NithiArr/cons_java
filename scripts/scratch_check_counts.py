import psycopg2

DB_LOCAL = "postgresql://postgres:admin123@localhost:5432/construction_db"
DB_RAILWAY = "postgresql://postgres:WodrjpqAtVDCnpfffPRiqIIsPQcjfZLg@autorack.proxy.rlwy.net:47353/railway"
DB_RENDER = "postgresql://cons_java_user:LJWQRjMU2t42LPfJAyGqgT8Z97ukL4oq@dpg-d7i7as5ckfvc73evmme0-a.oregon-postgres.render.com/cons_java"

tables = ['project', 'vendor', 'expense', 'expense_item', 'payment', 'client_payment']

def get_counts(conn_str, name):
    print(f"Connecting to {name}...")
    try:
        conn = psycopg2.connect(conn_str, connect_timeout=5)
        cur = conn.cursor()
        counts = {}
        for t in tables:
            try:
                cur.execute(f"SELECT COUNT(*) FROM {t}")
                counts[t] = cur.fetchone()[0]
            except Exception as e:
                counts[t] = f"Error: {e}"
                conn.rollback()
        cur.close()
        conn.close()
        return counts
    except Exception as e:
        print(f"Failed to connect to {name}: {e}")
        return None

local_counts = get_counts(DB_LOCAL, "Local DB")
railway_counts = get_counts(DB_RAILWAY, "Railway DB")
render_counts = get_counts(DB_RENDER, "Render DB")

print("\n--- ROW COUNTS COMPARISON ---")
print(f"{'TABLE':<20} | {'LOCAL':<10} | {'RAILWAY':<10} | {'RENDER':<10}")
print("-" * 60)
for t in tables:
    l_val = local_counts.get(t, "N/A") if local_counts else "N/A"
    rw_val = railway_counts.get(t, "N/A") if railway_counts else "N/A"
    rn_val = render_counts.get(t, "N/A") if render_counts else "N/A"
    print(f"{t:<20} | {str(l_val):<10} | {str(rw_val):<10} | {str(rn_val):<10}")
