import psycopg2

conn = psycopg2.connect(
    host='dpg-d7i7as5ckfvc73evmme0-a.oregon-postgres.render.com',
    port=5432, dbname='cons_java',
    user='cons_java_user',
    password='LJWQRjMU2t42LPfJAyGqgT8Z97ukL4oq',
    sslmode='require'
)
cur = conn.cursor()

projects = ['DSP', 'Ganapathy Residence', 'Happy Town', 'SK', 'Velavan Clinic']
cur.execute("SELECT project_id FROM project WHERE name IN %s", (tuple(projects),))
p_ids = tuple(r[0] for r in cur.fetchall())

cur.execute("UPDATE expense SET payment_mode = 'CREDIT' WHERE expense_type = 'Material Purchase' AND project_id IN %s", (p_ids,))
updated = cur.rowcount
conn.commit()

print(f"Successfully updated {updated} material purchases to CREDIT payment mode.")

cur.close()
conn.close()
