import psycopg2

conn = psycopg2.connect(
    host='dpg-d7i7as5ckfvc73evmme0-a.oregon-postgres.render.com',
    port=5432,
    dbname='cons_java',
    user='cons_java_user',
    password='LJWQRjMU2t42LPfJAyGqgT8Z97ukL4oq',
    sslmode='require'
)
cur = conn.cursor()

cur.execute("SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name")
print('Tables:', [r[0] for r in cur.fetchall()])

cur.execute("SELECT column_name, data_type FROM information_schema.columns WHERE table_name='master_category' ORDER BY ordinal_position")
print('master_category cols:', cur.fetchall())

cur.execute("SELECT column_name, data_type FROM information_schema.columns WHERE table_name='sub_category' ORDER BY ordinal_position")
print('sub_category cols:', cur.fetchall())

cur.execute("SELECT column_name, data_type FROM information_schema.columns WHERE table_name='project' ORDER BY ordinal_position")
print('project cols:', cur.fetchall())

print()
print('=== MASTER CATEGORIES ===')
cur.execute('SELECT * FROM master_category ORDER BY category_id')
for r in cur.fetchall():
    print(' ', r)

print()
print('=== SUBCATEGORIES ===')
cur.execute('SELECT * FROM sub_category ORDER BY parent_category_id, subcategory_id')
for r in cur.fetchall():
    print(' ', r)

print()
print('=== PROJECTS ===')
cur.execute('SELECT project_id, name, status FROM project ORDER BY project_id')
for r in cur.fetchall():
    print(' ', r)

print()
print('=== VENDORS ===')
cur.execute('SELECT vendor_id, name FROM vendor ORDER BY name')
for r in cur.fetchall():
    print(' ', r)

cur.close()
conn.close()
