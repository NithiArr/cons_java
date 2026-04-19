package com.construction.service;

import com.construction.domain.*;
import com.construction.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final ProjectRepository projectRepository;
    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;
    private final ClientPaymentRepository clientPaymentRepository;
    private final VendorRepository vendorRepository;

    // ─── OWNER KPIs ──────────────────────────────────────────────────────
    public Map<String, Object> getOwnerKpis(Company company, String statusFilter) {
        List<Project> projects;
        if (statusFilter != null && !statusFilter.isEmpty()) {
            projects = projectRepository.findByCompanyOrderByCreatedAtDesc(company)
                    .stream().filter(p -> p.getStatus().equalsIgnoreCase(statusFilter)).toList();
        } else {
            projects = projectRepository.findByCompanyOrderByCreatedAtDesc(company);
        }

        long totalProjects = projects.size();

        BigDecimal totalBudget = projects.stream()
                .map(p -> p.getBudget() != null ? p.getBudget() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Expense> expenses = expenseRepository.findByCompanyAndProjectIn(company, projects);
        List<Payment> payments = paymentRepository.findByCompanyAndProjectIn(company, projects);
        List<ClientPayment> clientPayments = clientPaymentRepository.findByCompanyAndProjectIn(company, projects);

        BigDecimal totalSpent = expenses.stream()
                .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expensesCredit = expenses.stream()
                .filter(e -> "CREDIT".equalsIgnoreCase(e.getPaymentMode()))
                .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expensesPaid = totalSpent.subtract(expensesCredit);

        BigDecimal totalPayments = payments.stream()
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal vendorOutstanding = expensesCredit.subtract(totalPayments);
        if (vendorOutstanding.compareTo(BigDecimal.ZERO) < 0) vendorOutstanding = BigDecimal.ZERO;

        BigDecimal totalReceived = clientPayments.stream()
                .map(ClientPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutflow = expensesPaid.add(totalPayments);
        BigDecimal balanceInHand = totalReceived.subtract(totalOutflow);

        Map<String, Long> statusBreakdown = new HashMap<>();
        projectRepository.findByCompanyOrderByCreatedAtDesc(company)
                .forEach(p -> statusBreakdown.merge(p.getStatus(), 1L, Long::sum));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total_projects", totalProjects);
        data.put("total_budget", totalBudget);
        data.put("total_spent", totalSpent);
        data.put("total_received", totalReceived);
        data.put("expenses_paid", expensesPaid);
        data.put("expenses_credit", expensesCredit);
        data.put("vendor_outstanding", vendorOutstanding);
        data.put("balance_in_hand", balanceInHand);
        data.put("status_breakdown", statusBreakdown);
        return data;
    }

    // ─── DAILY CASH BALANCE ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getDailyCashBalance(Company company, List<Long> projectIdList,
                                                    LocalDate fromDate, LocalDate toDate) {
        boolean allProjects = (projectIdList == null || projectIdList.isEmpty());

        // Opening balance (before fromDate)
        List<ClientPayment> opClientPayments = allProjects
                ? clientPaymentRepository.findByCompanyAndPaymentDateBefore(company, fromDate)
                : clientPaymentRepository.findByCompanyAndProjectIdsBeforeDate(company, projectIdList, fromDate);

        List<Expense> opExpenses = allProjects
                ? expenseRepository.findByCompanyAndExpenseDateBefore(company, fromDate)
                : expenseRepository.findByCompanyAndProjectIdsBeforeDate(company, projectIdList, fromDate);

        List<Payment> opPayments = allProjects
                ? paymentRepository.findByCompanyAndPaymentDateBefore(company, fromDate)
                : paymentRepository.findByCompanyAndProjectIdsBeforeDate(company, projectIdList, fromDate);

        BigDecimal opInflow = opClientPayments.stream().map(ClientPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal opExpOutflow = opExpenses.stream()
                .filter(e -> !e.getPaymentMode().equalsIgnoreCase("CREDIT"))
                .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal opPayOutflow = opPayments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal openingBalance = opInflow.subtract(opExpOutflow).subtract(opPayOutflow);

        // Period data (fromDate..toDate)
        List<ClientPayment> periodClientPayments = allProjects
                ? clientPaymentRepository.findByCompanyAndPaymentDateBetween(company, fromDate, toDate)
                : clientPaymentRepository.findByCompanyAndProjectIdsBetween(company, projectIdList, fromDate, toDate);

        List<Expense> periodExpenses = allProjects
                ? expenseRepository.findByCompanyAndExpenseDateBetween(company, fromDate, toDate)
                : expenseRepository.findByCompanyAndProjectIdsBetween(company, projectIdList, fromDate, toDate);

        List<Payment> periodPayments = allProjects
                ? paymentRepository.findByCompanyAndPaymentDateBetween(company, fromDate, toDate)
                : paymentRepository.findByCompanyAndProjectIdsBetween(company, projectIdList, fromDate, toDate);

        BigDecimal clientReceipts = periodClientPayments.stream().map(ClientPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vendorPayments = periodPayments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expensesCredit = periodExpenses.stream()
                .filter(e -> "CREDIT".equalsIgnoreCase(e.getPaymentMode()))
                .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expensesCash = periodExpenses.stream()
                .filter(e -> "CASH".equalsIgnoreCase(e.getPaymentMode()))
                .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expensesBankUpi = periodExpenses.stream()
                .filter(e -> "BANK".equalsIgnoreCase(e.getPaymentMode()) || "UPI".equalsIgnoreCase(e.getPaymentMode()))
                .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal periodOutflow = expensesCash.add(expensesBankUpi).add(vendorPayments);
        BigDecimal closingBalance = openingBalance.add(clientReceipts).subtract(periodOutflow);

        // Build transactions list
        List<Map<String, Object>> transactions = new ArrayList<>();

        for (ClientPayment cp : periodClientPayments) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("date", cp.getPaymentDate().toString());
            t.put("type", "client_payment");
            t.put("id", cp.getClientPaymentId());
            t.put("amount", cp.getAmount());
            t.put("payment_mode", cp.getPaymentMode());
            t.put("project_name", cp.getProject() != null ? cp.getProject().getName() : null);
            t.put("reference", cp.getReferenceNumber());
            t.put("remarks", cp.getRemarks());
            transactions.add(t);
        }

        for (Expense exp : periodExpenses) {
            // items for material purchases
            List<Map<String, Object>> items = new ArrayList<>();
            String subcategory = "";
            if ("Material Purchase".equals(exp.getExpenseType()) && exp.getItems() != null) {
                for (ExpenseItem item : exp.getItems()) {
                    Map<String, Object> im = new LinkedHashMap<>();
                    im.put("item_name", item.getItemName());
                    im.put("quantity", item.getQuantity());
                    im.put("measuring_unit", item.getMeasuringUnit());
                    im.put("unit_price", item.getUnitPrice());
                    im.put("total_price", item.getTotalPrice());
                    items.add(im);
                }
            }
            if ("Regular Expense".equals(exp.getExpenseType()) && exp.getItems() != null && !exp.getItems().isEmpty()) {
                subcategory = exp.getItems().get(0).getItemName() != null ? exp.getItems().get(0).getItemName() : "";
            }

            Map<String, Object> t = new LinkedHashMap<>();
            t.put("date", exp.getExpenseDate().toString());
            t.put("type", "expense");
            t.put("expense_type", exp.getExpenseType());
            t.put("id", exp.getExpenseId());
            t.put("amount", exp.getAmount());
            t.put("payment_mode", exp.getPaymentMode());
            t.put("project_name", exp.getProject() != null ? exp.getProject().getName() : null);
            t.put("category", exp.getCategory());
            t.put("subcategory", subcategory);
            t.put("description", exp.getDescription());
            t.put("items", items);
            transactions.add(t);
        }

        for (Payment pay : periodPayments) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("date", pay.getPaymentDate().toString());
            t.put("type", "vendor_payment");
            t.put("id", pay.getPaymentId());
            t.put("amount", pay.getAmount());
            t.put("payment_mode", pay.getPaymentMode());
            t.put("project_name", pay.getProject() != null ? pay.getProject().getName() : null);
            t.put("vendor_name", pay.getVendor() != null ? pay.getVendor().getName() : null);
            t.put("invoice_number", pay.getExpense() != null ? pay.getExpense().getInvoiceNumber() : null);
            t.put("category", pay.getExpense() != null ? pay.getExpense().getCategory() : null);
            transactions.add(t);
        }

        transactions.sort(Comparator.comparing(t -> (String) t.get("date"), Comparator.reverseOrder()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("opening_balance", openingBalance);
        result.put("closing_balance", closingBalance);
        result.put("client_receipts", clientReceipts);
        result.put("vendor_payments", vendorPayments);
        result.put("expenses_credit", expensesCredit);
        result.put("expenses_cash", expensesCash);
        result.put("expenses_bank_upi", expensesBankUpi);
        result.put("transactions", transactions);
        return result;
    }

    // ─── PROJECT PAYMENT DETAILS ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getProjectPaymentDetails(Company company, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.getCompany().getCompanyId().equals(company.getCompanyId()))
                .orElseThrow(() -> new NoSuchElementException("Project not found"));

        List<Expense> expenses = expenseRepository.findByCompanyAndProject(company, project)
                .stream().sorted(Comparator.comparing(Expense::getExpenseDate).reversed()).toList();
        List<Payment> payments = paymentRepository.findByCompanyAndProject(company, project)
                .stream().sorted(Comparator.comparing(Payment::getPaymentDate).reversed()).toList();
        List<ClientPayment> clientPayments = clientPaymentRepository.findByCompanyAndProject(company, project)
                .stream().sorted(Comparator.comparing(ClientPayment::getPaymentDate).reversed()).toList();

        List<Map<String, Object>> purchaseHistory = new ArrayList<>();
        List<Map<String, Object>> expensesList = new ArrayList<>();
        List<Map<String, Object>> expenseItemsList = new ArrayList<>();
        BigDecimal totalPurchasesAmount = BigDecimal.ZERO;
        BigDecimal totalExpensesAmount = BigDecimal.ZERO;

        for (Expense expense : expenses) {
            if ("Material Purchase".equals(expense.getExpenseType())) {
                BigDecimal displayedPaid;
                BigDecimal displayedBalance;
                if (List.of("CASH", "UPI", "BANK").contains(expense.getPaymentMode().toUpperCase())) {
                    displayedPaid = expense.getAmount();
                    displayedBalance = BigDecimal.ZERO;
                } else {
                    BigDecimal paymentSum = payments.stream()
                            .filter(p -> p.getExpense() != null && p.getExpense().getExpenseId().equals(expense.getExpenseId()))
                            .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    displayedPaid = paymentSum;
                    displayedBalance = expense.getAmount().subtract(paymentSum);
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("purchase_id", expense.getExpenseId());
                m.put("vendor_name", expense.getVendor() != null ? expense.getVendor().getName() : "Unknown");
                m.put("invoice_number", expense.getInvoiceNumber() != null ? expense.getInvoiceNumber() : "-");
                m.put("invoice_date", expense.getExpenseDate().toString());
                m.put("category", expense.getCategory());
                m.put("amount", expense.getAmount());
                m.put("payment_mode", expense.getPaymentMode());
                m.put("paid", displayedPaid);
                m.put("balance", displayedBalance);
                purchaseHistory.add(m);
                totalPurchasesAmount = totalPurchasesAmount.add(expense.getAmount());

                // Expense items breakdown
                if (expense.getItems() != null) {
                    for (ExpenseItem item : expense.getItems()) {
                        Map<String, Object> im = new LinkedHashMap<>();
                        im.put("item_id", item.getExpenseItemId());
                        im.put("expense_id", expense.getExpenseId());
                        im.put("category", expense.getCategory());
                        im.put("subcategory", item.getItemName());
                        im.put("quantity", item.getQuantity());
                        im.put("unit_price", item.getUnitPrice());
                        im.put("total_price", item.getTotalPrice());
                        im.put("measuring_unit", item.getMeasuringUnit());
                        expenseItemsList.add(im);
                    }
                }
            } else {
                // Regular Expense
                String subcategory = "";
                if (expense.getItems() != null && !expense.getItems().isEmpty()) {
                    subcategory = expense.getItems().get(0).getItemName() != null ? expense.getItems().get(0).getItemName() : "";
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("expense_id", expense.getExpenseId());
                m.put("expense_type", expense.getExpenseType());
                m.put("expense_date", expense.getExpenseDate().toString());
                m.put("category", expense.getCategory());
                m.put("subcategory", subcategory);
                m.put("amount", expense.getAmount());
                m.put("payment_mode", expense.getPaymentMode());
                m.put("description", expense.getDescription());
                expensesList.add(m);
                totalExpensesAmount = totalExpensesAmount.add(expense.getAmount());
            }
        }

        List<Map<String, Object>> vendorPaymentsList = new ArrayList<>();
        for (Payment p : payments) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("payment_id", p.getPaymentId());
            m.put("payment_date", p.getPaymentDate().toString());
            m.put("vendor_name", p.getVendor() != null ? p.getVendor().getName() : "Unknown");
            m.put("amount", p.getAmount());
            m.put("payment_mode", p.getPaymentMode());
            m.put("purchase_invoice", p.getExpense() != null ? p.getExpense().getInvoiceNumber() : "-");
            vendorPaymentsList.add(m);
        }

        List<Map<String, Object>> clientPaymentsList = new ArrayList<>();
        for (ClientPayment cp : clientPayments) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("client_payment_id", cp.getClientPaymentId());
            m.put("payment_date", cp.getPaymentDate().toString());
            m.put("amount", cp.getAmount());
            m.put("payment_mode", cp.getPaymentMode());
            m.put("reference_number", cp.getReferenceNumber());
            m.put("remarks", cp.getRemarks());
            clientPaymentsList.add(m);
        }

        BigDecimal totalSpent = totalPurchasesAmount.add(totalExpensesAmount);
        BigDecimal totalReceived = clientPayments.stream().map(ClientPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVendorPayments = payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditPurchases = expenses.stream()
                .filter(e -> "Material Purchase".equals(e.getExpenseType()) && "CREDIT".equalsIgnoreCase(e.getPaymentMode()))
                .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vendorOutstanding = creditPurchases.subtract(totalVendorPayments);
        if (vendorOutstanding.compareTo(BigDecimal.ZERO) < 0) vendorOutstanding = BigDecimal.ZERO;
        BigDecimal clientOutstanding = totalSpent.subtract(totalReceived);
        BigDecimal remainingBudget = project.getBudget() != null ? project.getBudget().subtract(totalSpent) : BigDecimal.ZERO;

        Map<String, Object> financialSummary = new LinkedHashMap<>();
        financialSummary.put("total_purchases", totalPurchasesAmount);
        financialSummary.put("total_expenses", totalExpensesAmount);
        financialSummary.put("total_spent", totalSpent);
        financialSummary.put("total_received", totalReceived);
        financialSummary.put("vendor_outstanding", vendorOutstanding);
        financialSummary.put("client_outstanding", clientOutstanding);
        financialSummary.put("total_vendor_payments", totalVendorPayments);
        financialSummary.put("remaining_budget", remainingBudget);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("project_name", project.getName());
        result.put("project_status", project.getStatus());
        result.put("purchase_history", purchaseHistory);
        result.put("vendor_payments", vendorPaymentsList);
        result.put("expenses", expensesList);
        result.put("expense_items", expenseItemsList);
        result.put("client_payments", clientPaymentsList);
        result.put("financial_summary", financialSummary);
        return result;
    }

    // ─── VENDOR SUMMARY ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVendorSummary(Company company) {
        List<Vendor> vendors = vendorRepository.findByCompanyOrderByName(company);
        List<Map<String, Object>> data = new ArrayList<>();

        for (Vendor vendor : vendors) {
            List<Expense> expenses = expenseRepository.findByCompanyAndVendor(company, vendor);
            List<Payment> payments = paymentRepository.findByCompanyAndVendor(company, vendor);

            BigDecimal totalPurchases = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalPaid = payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal creditPurchases = expenses.stream()
                    .filter(e -> "CREDIT".equalsIgnoreCase(e.getPaymentMode()))
                    .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal outstanding = creditPurchases.subtract(totalPaid);
            if (outstanding.compareTo(BigDecimal.ZERO) < 0) outstanding = BigDecimal.ZERO;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("vendor_id", vendor.getVendorId());
            m.put("vendor_name", vendor.getName());
            m.put("total_purchases", totalPurchases);
            m.put("total_paid", totalPaid);
            m.put("outstanding", outstanding);
            data.add(m);
        }
        return data;
    }

    // ─── VENDOR MATERIAL SUMMARY ─────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVendorMaterialSummary(Company company, Long vendorId, String projectName) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new NoSuchElementException("Vendor not found"));

        List<Expense> expenses = expenseRepository.findByCompanyAndVendorAndExpenseType(company, vendor, "Material Purchase");
        if (projectName != null && !projectName.isBlank()) {
            expenses = expenses.stream()
                    .filter(e -> e.getProject() != null && projectName.equals(e.getProject().getName()))
                    .toList();
        }

        Map<String, Map<String, Object>> materialMap = new LinkedHashMap<>();
        for (Expense expense : expenses) {
            String mat = expense.getCategory() != null ? expense.getCategory() : "Uncategorized";
            materialMap.computeIfAbsent(mat, k -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("material", k);
                entry.put("total_quantity", BigDecimal.ZERO);
                entry.put("total_amount", BigDecimal.ZERO);
                entry.put("purchases", new ArrayList<Map<String, Object>>());
                return entry;
            });
            Map<String, Object> entry = materialMap.get(mat);
            entry.put("total_amount", ((BigDecimal) entry.get("total_amount")).add(expense.getAmount()));

            if (expense.getItems() != null) {
                for (ExpenseItem item : expense.getItems()) {
                    entry.put("total_quantity", ((BigDecimal) entry.get("total_quantity")).add(
                            item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO));
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> purchases = (List<Map<String, Object>>) entry.get("purchases");
                    Map<String, Object> p = new LinkedHashMap<>();
                    p.put("project", expense.getProject() != null ? expense.getProject().getName() : null);
                    p.put("date", expense.getExpenseDate().toString());
                    p.put("invoice_number", expense.getInvoiceNumber() != null ? expense.getInvoiceNumber() : "-");
                    p.put("item_name", item.getItemName());
                    p.put("quantity", item.getQuantity());
                    p.put("unit", item.getMeasuringUnit());
                    p.put("unit_price", item.getUnitPrice());
                    p.put("total_price", item.getTotalPrice());
                    p.put("payment_mode", expense.getPaymentMode());
                    purchases.add(p);
                }
            }
        }
        return new ArrayList<>(materialMap.values());
    }

    // ─── VENDOR PURCHASE HISTORY ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVendorPurchaseHistory(Company company, Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new NoSuchElementException("Vendor not found"));

        List<Expense> expenses = expenseRepository.findByCompanyAndVendor(company, vendor)
                .stream().sorted(Comparator.comparing(Expense::getExpenseDate).reversed()).toList();

        List<Map<String, Object>> data = new ArrayList<>();
        for (Expense expense : expenses) {
            BigDecimal paid;
            BigDecimal balance;
            if (List.of("CASH", "UPI", "BANK").contains(expense.getPaymentMode().toUpperCase())) {
                paid = expense.getAmount();
                balance = BigDecimal.ZERO;
            } else {
                // Proportional estimate based on project credit vs payments
                List<Expense> projCredit = expenseRepository.findByCompanyAndVendorAndExpenseType(company, vendor, "Material Purchase")
                        .stream().filter(e -> "CREDIT".equalsIgnoreCase(e.getPaymentMode())
                                && e.getProject() != null && expense.getProject() != null
                                && e.getProject().getProjectId().equals(expense.getProject().getProjectId()))
                        .toList();
                BigDecimal totalProjectCredit = projCredit.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

                List<Payment> projPayments = expense.getProject() != null
                        ? paymentRepository.findByCompanyAndProject(company, expense.getProject())
                        .stream().filter(p -> p.getVendor() != null && p.getVendor().getVendorId().equals(vendorId)).toList()
                        : List.of();
                BigDecimal totalProjectPayments = projPayments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalProjectCredit.compareTo(BigDecimal.ZERO) > 0) {
                    double proportion = expense.getAmount().doubleValue() / totalProjectCredit.doubleValue();
                    paid = BigDecimal.valueOf(totalProjectPayments.doubleValue() * proportion);
                } else {
                    paid = BigDecimal.ZERO;
                }
                balance = expense.getAmount().subtract(paid);
                if (balance.compareTo(BigDecimal.ZERO) < 0) balance = BigDecimal.ZERO;
            }

            List<Map<String, Object>> items = new ArrayList<>();
            if (expense.getItems() != null) {
                for (ExpenseItem item : expense.getItems()) {
                    Map<String, Object> im = new LinkedHashMap<>();
                    im.put("category", expense.getCategory());
                    im.put("item_name", item.getItemName());
                    im.put("quantity", item.getQuantity());
                    im.put("unit", item.getMeasuringUnit());
                    im.put("unit_price", item.getUnitPrice());
                    im.put("total_price", item.getTotalPrice());
                    items.add(im);
                }
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("purchase_id", expense.getExpenseId());
            m.put("project_name", expense.getProject() != null ? expense.getProject().getName() : null);
            m.put("invoice_number", expense.getInvoiceNumber() != null ? expense.getInvoiceNumber() : "-");
            m.put("invoice_date", expense.getExpenseDate().toString());
            m.put("amount", expense.getAmount());
            m.put("payment_mode", expense.getPaymentMode());
            m.put("category", expense.getCategory());
            m.put("paid", paid);
            m.put("balance", balance);
            m.put("items", items);
            data.add(m);
        }
        return data;
    }
}
