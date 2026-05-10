import psycopg2
from collections import defaultdict

conn = psycopg2.connect(
    host='dpg-d7i7as5ckfvc73evmme0-a.oregon-postgres.render.com',
    port=5432, dbname='cons_java',
    user='cons_java_user',
    password='LJWQRjMU2t42LPfJAyGqgT8Z97ukL4oq',
    sslmode='require'
)
cur = conn.cursor()

# Get the project IDs for the 5 projects
projects = ['DSP', 'Ganapathy Residence', 'Happy Town', 'SK', 'Velavan Clinic']
cur.execute("SELECT project_id, name FROM project WHERE name IN %s ORDER BY name", (tuple(projects),))
project_map = {r[0]: r[1] for r in cur.fetchall()}

# Get vendor names
cur.execute("SELECT vendor_id, name FROM vendor")
vendor_map = {r[0]: r[1] for r in cur.fetchall()}

# Material Purchases per project per vendor
cur.execute("""
    SELECT project_id, vendor_id, SUM(amount) 
    FROM expense 
    WHERE expense_type = 'Material Purchase' AND vendor_id IS NOT NULL 
    GROUP BY project_id, vendor_id
""")
purchases = cur.fetchall()

# Payments per project per vendor
cur.execute("""
    SELECT project_id, vendor_id, SUM(amount) 
    FROM payment 
    GROUP BY project_id, vendor_id
""")
payments = cur.fetchall()

cur.close()
conn.close()

# Calculate outstanding
# structure: dict[project_name] -> dict[vendor_name] -> {'purchases': 0, 'payments': 0}
data = defaultdict(lambda: defaultdict(lambda: {'purchases': 0, 'payments': 0}))

for p_id, v_id, amt in purchases:
    p_name = project_map.get(p_id)
    v_name = vendor_map.get(v_id)
    if p_name and v_name:
        data[p_name][v_name]['purchases'] += float(amt)

for p_id, v_id, amt in payments:
    p_name = project_map.get(p_id)
    v_name = vendor_map.get(v_id)
    if p_name and v_name:
        data[p_name][v_name]['payments'] += float(amt)

# Print Summary
grand_total_outstanding = 0

for p_name in sorted(projects):
    if p_name not in data:
        continue
    
    print(f"\n{'='*50}")
    print(f" PROJECT: {p_name.upper()}")
    print(f"{'='*50}")
    print(f"{'VENDOR'.ljust(22)} | {'PURCHASED'.rjust(10)} | {'PAID'.rjust(10)} | {'OUTSTANDING'.rjust(12)}")
    print("-" * 61)
    
    project_outstanding = 0
    
    for v_name in sorted(data[p_name].keys()):
        p_amt = data[p_name][v_name]['purchases']
        pay_amt = data[p_name][v_name]['payments']
        out = p_amt - pay_amt
        project_outstanding += out
        
        # Only print if there is some activity
        if p_amt > 0 or pay_amt > 0:
            print(f"{v_name.ljust(22)} | {p_amt:10,.0f} | {pay_amt:10,.0f} | {out:12,.0f}")
            
    print("-" * 61)
    print(f"{'TOTAL OUTSTANDING FOR PROJECT:'.rjust(47)} {project_outstanding:12,.0f}")
    grand_total_outstanding += project_outstanding

print(f"\n{'='*61}")
print(f"{'GRAND TOTAL OUTSTANDING ACROSS 5 PROJECTS:'.rjust(47)} {grand_total_outstanding:12,.0f}")
print(f"{'='*61}\n")
