DO $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN SELECT expense_id, expense_type, amount FROM expense WHERE project_id = 1 AND expense_type != 'Regular Expense' AND expense_type != 'Material Purchase'
    LOOP
        INSERT INTO expense_item (expense_id, item_name, quantity, measuring_unit, unit_price, total_price)
        VALUES (rec.expense_id, rec.expense_type, 1, 'unit', rec.amount, rec.amount);
        
        UPDATE expense SET expense_type = 'Regular Expense' WHERE expense_id = rec.expense_id;
    END LOOP;
END $$;
