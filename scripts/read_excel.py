import openpyxl

wb = openpyxl.load_workbook(r"e:\Personal\Con_java\Echanari Material Expense.xlsx", data_only=True)
ws = wb['Material Details']

categories = set()
for i, row in enumerate(ws.iter_rows(values_only=True)):
    if i == 0:
        continue
    cat = row[1]
    if cat is not None:
        categories.add(str(cat).strip().upper())

print("Unique categories in Excel:")
for c in sorted(categories):
    print(f"- {c}")
