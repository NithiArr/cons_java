package com.construction.controller.api;

import com.construction.domain.*;
import com.construction.repository.*;
import com.construction.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class DataApiController {

    private final ProjectRepository projectRepository;
    private final VendorRepository vendorRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseItemRepository expenseItemRepository;
    private final PaymentRepository paymentRepository;
    private final ClientPaymentRepository clientPaymentRepository;
    private final MasterCategoryRepository masterCategoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    // ─── HELPER ─────────────────────────────────────────────────────────
    private User currentUser(Authentication auth) {
        return (User) auth.getPrincipal();
    }

    private Company currentCompany(Authentication auth) {
        return currentUser(auth).getCompany();
    }

    // ─── PROJECTS ────────────────────────────────────────────────────────
    @GetMapping("/api_projects_list")
    public ResponseEntity<?> listProjects(Authentication auth) {
        Company company = currentCompany(auth);
        List<Project> projects = projectRepository.findByCompanyOrderByCreatedAtDesc(company);
        List<Map<String, Object>> result = projects.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("project_id", p.getProjectId());
            m.put("name", p.getName());
            m.put("location", p.getLocation());
            m.put("start_date", p.getStartDate());
            m.put("end_date", p.getEndDate());
            m.put("budget", p.getBudget());
            m.put("status", p.getStatus());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api_projects_list")
    public ResponseEntity<?> createProject(@RequestBody Map<String, Object> body, Authentication auth) {
        Company company = currentCompany(auth);
        Project project = new Project();
        project.setCompany(company);
        project.setName((String) body.get("name"));
        project.setLocation((String) body.get("location"));
        project.setStatus((String) body.getOrDefault("status", "PLANNING"));
        if (body.get("budget") != null)
            project.setBudget(new BigDecimal(body.get("budget").toString()));
        if (body.get("start_date") != null && !body.get("start_date").toString().isBlank())
            project.setStartDate(java.time.LocalDate.parse(body.get("start_date").toString()));
        if (body.get("end_date") != null && !body.get("end_date").toString().isBlank())
            project.setEndDate(java.time.LocalDate.parse(body.get("end_date").toString()));
        projectRepository.save(project);
        auditService.log(currentUser(auth), "CREATE", "PROJECT", project.getName(),
                "Created project: " + project.getName());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/api_projects_list/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        Company company = currentCompany(auth);
        return projectRepository.findById(id)
            .filter(p -> p.getCompany().getCompanyId().equals(company.getCompanyId()))
            .map(p -> {
                if (body.get("name") != null) p.setName((String) body.get("name"));
                if (body.get("location") != null) p.setLocation((String) body.get("location"));
                if (body.get("status") != null) p.setStatus((String) body.get("status"));
                if (body.get("budget") != null) p.setBudget(new BigDecimal(body.get("budget").toString()));
                if (body.get("start_date") != null && !body.get("start_date").toString().isBlank())
                    p.setStartDate(java.time.LocalDate.parse(body.get("start_date").toString()));
                if (body.get("end_date") != null && !body.get("end_date").toString().isBlank())
                    p.setEndDate(java.time.LocalDate.parse(body.get("end_date").toString()));
                projectRepository.save(p);
                auditService.log(currentUser(auth), "UPDATE", "PROJECT", p.getName(),
                        "Updated project: " + p.getName());
                return ResponseEntity.ok(Map.of("success", true));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api_projects_list/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id, Authentication auth) {
        Company company = currentCompany(auth);
        return projectRepository.findById(id)
            .filter(p -> p.getCompany().getCompanyId().equals(company.getCompanyId()))
            .map(p -> { projectRepository.delete(p);
                auditService.log(currentUser(auth), "DELETE", "PROJECT", p.getName(),
                        "Deleted project: " + p.getName());
                return ResponseEntity.ok(Map.of("success", true)); })
            .orElse(ResponseEntity.notFound().build());
    }

    // ─── VENDORS ─────────────────────────────────────────────────────────
    @GetMapping("/api_vendors_list")
    public ResponseEntity<?> listVendors(Authentication auth) {
        Company company = currentCompany(auth);
        List<Vendor> vendors = vendorRepository.findByCompanyOrderByName(company);
        List<Map<String, Object>> result = vendors.stream().map(v -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("vendor_id", v.getVendorId());
            m.put("name", v.getName());
            m.put("phone", v.getPhone());
            m.put("email", v.getEmail());
            m.put("gst_number", v.getGstNumber());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api_vendors_list")
    public ResponseEntity<?> createVendor(@RequestBody Map<String, Object> body, Authentication auth) {
        Company company = currentCompany(auth);
        Vendor vendor = new Vendor();
        vendor.setCompany(company);
        vendor.setName((String) body.get("name"));
        vendor.setPhone((String) body.get("phone"));
        vendor.setEmail((String) body.get("email"));
        if (body.get("gst_number") != null) vendor.setGstNumber((String) body.get("gst_number"));
        vendorRepository.save(vendor);
        auditService.log(currentUser(auth), "CREATE", "VENDOR", vendor.getName(),
                "Created vendor: " + vendor.getName());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/api_vendors_list/{id}")
    public ResponseEntity<?> updateVendor(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        Company company = currentCompany(auth);
        return vendorRepository.findById(id)
            .filter(v -> v.getCompany().getCompanyId().equals(company.getCompanyId()))
            .map(v -> {
                if (body.get("name") != null) v.setName((String) body.get("name"));
                if (body.get("phone") != null) v.setPhone((String) body.get("phone"));
                if (body.get("email") != null) v.setEmail((String) body.get("email"));
                if (body.get("gst_number") != null) v.setGstNumber((String) body.get("gst_number"));
                vendorRepository.save(v);
                auditService.log(currentUser(auth), "UPDATE", "VENDOR", v.getName(),
                        "Updated vendor: " + v.getName());
                return ResponseEntity.ok(Map.of("success", true));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api_vendors_list/{id}")
    public ResponseEntity<?> deleteVendor(@PathVariable Long id, Authentication auth) {
        Company company = currentCompany(auth);
        return vendorRepository.findById(id)
            .filter(v -> v.getCompany().getCompanyId().equals(company.getCompanyId()))
            .map(v -> { vendorRepository.delete(v);
                auditService.log(currentUser(auth), "DELETE", "VENDOR", v.getName(),
                        "Deleted vendor: " + v.getName());
                return ResponseEntity.ok(Map.of("success", true)); })
            .orElse(ResponseEntity.notFound().build());
    }

    // ─── PURCHASES ───────────────────────────────────────────────────────
    @GetMapping("/api_purchases_list")
    public ResponseEntity<?> listPurchases(Authentication auth) {
        Company company = currentCompany(auth);
        List<Expense> expenses = expenseRepository.findByCompany(company)
            .stream().filter(e -> "Material Purchase".equals(e.getExpenseType()))
            .sorted(Comparator.comparing(Expense::getExpenseDate).reversed())
            .collect(Collectors.toList());

        List<Map<String, Object>> result = expenses.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("expense_id", e.getExpenseId());
            m.put("purchase_id", e.getExpenseId());
            m.put("project_id", e.getProject() != null ? e.getProject().getProjectId() : null);
            m.put("project_name", e.getProject() != null ? e.getProject().getName() : null);
            m.put("vendor_id", e.getVendor() != null ? e.getVendor().getVendorId() : null);
            m.put("vendor_name", e.getVendor() != null ? e.getVendor().getName() : null);
            m.put("category", e.getCategory());
            m.put("invoice_number", e.getInvoiceNumber());
            m.put("invoice_date", e.getExpenseDate());
            m.put("expense_date", e.getExpenseDate());
            m.put("total_amount", e.getAmount());
            m.put("payment_type", e.getPaymentMode());
            m.put("payment_mode", e.getPaymentMode());
            List<Map<String, Object>> items = e.getItems() != null ? e.getItems().stream().map(item -> {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("item_name", item.getItemName());
                im.put("brand", item.getBrand());
                im.put("quantity", item.getQuantity());
                im.put("measuring_unit", item.getMeasuringUnit());
                im.put("unit_price", item.getUnitPrice());
                im.put("total_price", item.getTotalPrice());
                return im;
            }).collect(Collectors.toList()) : List.of();
            m.put("items", items);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/api_purchases_list/{id}")
    public ResponseEntity<?> getPurchase(@PathVariable Long id, Authentication auth) {
        Company company = currentCompany(auth);
        return expenseRepository.findById(id)
            .filter(e -> e.getCompany().getCompanyId().equals(company.getCompanyId()))
            .map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("expense_id", e.getExpenseId());
                m.put("purchase_id", e.getExpenseId());
                m.put("project_id", e.getProject() != null ? e.getProject().getProjectId() : null);
                m.put("project_name", e.getProject() != null ? e.getProject().getName() : null);
                m.put("vendor_id", e.getVendor() != null ? e.getVendor().getVendorId() : null);
                m.put("vendor_name", e.getVendor() != null ? e.getVendor().getName() : null);
                m.put("category", e.getCategory());
                m.put("invoice_number", e.getInvoiceNumber());
                m.put("invoice_date", e.getExpenseDate());
                m.put("expense_date", e.getExpenseDate());
                m.put("total_amount", e.getAmount());
                m.put("payment_type", e.getPaymentMode());
                m.put("payment_mode", e.getPaymentMode());
                List<Map<String, Object>> items = e.getItems() != null ? e.getItems().stream().map(item -> {
                    Map<String, Object> im = new LinkedHashMap<>();
                    im.put("item_name", item.getItemName());
                    im.put("brand", item.getBrand());
                    im.put("quantity", item.getQuantity());
                    im.put("measuring_unit", item.getMeasuringUnit());
                    im.put("unit_price", item.getUnitPrice());
                    im.put("total_price", item.getTotalPrice());
                    return im;
                }).collect(Collectors.toList()) : List.of();
                m.put("items", items);
                return ResponseEntity.ok(m);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api_purchases_list")
    public ResponseEntity<?> createPurchase(@RequestBody Map<String, Object> body, Authentication auth) {
        Company company = currentCompany(auth);
        Expense expense = buildExpense(body, company, "Material Purchase");
        expenseRepository.save(expense);
        String proj = expense.getProject() != null ? expense.getProject().getName() : "—";
        auditService.log(currentUser(auth), "CREATE", "PURCHASE",
                proj, "Created purchase for project: " + proj + " | ₹" + expense.getAmount());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/api_purchases_list/{id}")
    public ResponseEntity<?> updatePurchase(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        Company company = currentCompany(auth);
        return expenseRepository.findById(id)
            .filter(e -> e.getCompany().getCompanyId().equals(company.getCompanyId()))
            .map(e -> {
                populateExpense(e, body, company);
                if (e.getItems() != null) e.getItems().clear();
                addItems(e, body);
                expenseRepository.save(e);
                String pName = e.getProject() != null ? e.getProject().getName() : "—";
                auditService.log(currentUser(auth), "UPDATE", "PURCHASE",
                        pName, "Updated purchase for project: " + pName + " | ₹" + e.getAmount());
                return ResponseEntity.ok(Map.of("success", true));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api_purchases_list/{id}")
    public ResponseEntity<?> deletePurchase(@PathVariable Long id, Authentication auth) {
        Company company = currentCompany(auth);
        return expenseRepository.findById(id)
            .filter(e -> e.getCompany().getCompanyId().equals(company.getCompanyId()))
            .map(e -> { expenseRepository.delete(e);
                auditService.log(currentUser(auth), "DELETE", "PURCHASE",
                        e.getProject() != null ? e.getProject().getName() : "—",
                        "Deleted purchase #" + e.getExpenseId());
                return ResponseEntity.ok(Map.of("success", true)); })
            .orElse(ResponseEntity.notFound().build());
    }

    // ─── EXPENSES ────────────────────────────────────────────────────────
    @GetMapping("/api_expenses_list")
    public ResponseEntity<?> listExpenses(Authentication auth) {
        Company company = currentCompany(auth);
        List<Expense> expenses = expenseRepository.findByCompany(company)
            .stream().filter(e -> "Regular Expense".equals(e.getExpenseType()))
            .sorted(Comparator.comparing(Expense::getExpenseDate).reversed())
            .collect(Collectors.toList());

        List<Map<String, Object>> result = expenses.stream().map(this::buildExpenseMap).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api_expenses_list/{id}")
    public ResponseEntity<?> getExpense(@PathVariable Long id, Authentication auth) {
        Company company = currentCompany(auth);
        return expenseRepository.findById(id)
            .filter(e -> e.getCompany().getCompanyId().equals(company.getCompanyId()))
            .map(e -> ResponseEntity.ok(buildExpenseMap(e)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api_expenses_list")
    public ResponseEntity<?> createExpense(@RequestBody Map<String, Object> body, Authentication auth) {
        Company company = currentCompany(auth);
        Expense expense = buildExpense(body, company, "Regular Expense");
        // For regular expenses, also save subcategory as ExpenseItem
        String subcategory = (String) body.get("subcategory");
        if (subcategory != null && !subcategory.isBlank() && (expense.getItems() == null || expense.getItems().isEmpty())) {
            ExpenseItem item = new ExpenseItem();
            item.setExpense(expense);
            item.setItemName(subcategory);
            item.setQuantity(BigDecimal.ONE);
            item.setMeasuringUnit("unit");
            item.setUnitPrice(expense.getAmount());
            item.setTotalPrice(expense.getAmount());
            if (expense.getItems() == null) expense.setItems(new ArrayList<>());
            expense.getItems().add(item);
        }
        expenseRepository.save(expense);
        String cat = expense.getCategory() != null ? expense.getCategory() : "Expense";
        auditService.log(currentUser(auth), "CREATE", "EXPENSE", cat,
                "Created expense: " + cat + " | ₹" + expense.getAmount());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/api_expenses_list/{id}")
    public ResponseEntity<?> updateExpense(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        Company company = currentCompany(auth);
        return expenseRepository.findById(id)
            .filter(e -> e.getCompany().getCompanyId().equals(company.getCompanyId()))
            .map(e -> {
                populateExpense(e, body, company);
                // Update subcategory item
                String subcategory = (String) body.get("subcategory");
                if (subcategory != null) {
                    if (e.getItems() != null) e.getItems().clear();
                    else e.setItems(new ArrayList<>());
                    ExpenseItem item = new ExpenseItem();
                    item.setExpense(e);
                    item.setItemName(subcategory);
                    item.setQuantity(BigDecimal.ONE);
                    item.setMeasuringUnit("unit");
                    item.setUnitPrice(e.getAmount());
                    item.setTotalPrice(e.getAmount());
                    e.getItems().add(item);
                }
                expenseRepository.save(e);
                auditService.log(currentUser(auth), "UPDATE", "EXPENSE",
                        e.getCategory() != null ? e.getCategory() : "Expense",
                        "Updated expense #" + e.getExpenseId() + " | ₹" + e.getAmount());
                return ResponseEntity.ok(Map.of("success", true));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api_expenses_list/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable Long id, Authentication auth) {
        Company company = currentCompany(auth);
        return expenseRepository.findById(id)
            .filter(e -> e.getCompany().getCompanyId().equals(company.getCompanyId()))
            .map(e -> { expenseRepository.delete(e);
                auditService.log(currentUser(auth), "DELETE", "EXPENSE",
                        e.getCategory() != null ? e.getCategory() : "Expense",
                        "Deleted expense #" + e.getExpenseId());
                return ResponseEntity.ok(Map.of("success", true)); })
            .orElse(ResponseEntity.notFound().build());
    }

    // ─── PAYMENTS ────────────────────────────────────────────────────────
    @GetMapping("/api_payments_list")
    public ResponseEntity<?> listPayments(Authentication auth) {
        Company company = currentCompany(auth);
        List<Payment> payments = paymentRepository.findByCompany(company);
        List<Map<String, Object>> result = payments.stream()
            .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("payment_id", p.getPaymentId());
                m.put("vendor_id", p.getVendor() != null ? p.getVendor().getVendorId() : null);
                m.put("vendor_name", p.getVendor() != null ? p.getVendor().getName() : null);
                m.put("project_id", p.getProject() != null ? p.getProject().getProjectId() : null);
                m.put("project_name", p.getProject() != null ? p.getProject().getName() : null);
                m.put("amount", p.getAmount());
                m.put("payment_date", p.getPaymentDate());
                m.put("payment_mode", p.getPaymentMode());
                m.put("purchase_invoice", p.getExpense() != null ? p.getExpense().getInvoiceNumber() : "-");
                return m;
            }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api_payments_list")
    public ResponseEntity<?> createPayment(@RequestBody Map<String, Object> body, Authentication auth) {
        Company company = currentCompany(auth);
        Payment payment = new Payment();
        payment.setCompany(company);
        if (body.get("vendor_id") != null)
            vendorRepository.findById(Long.parseLong(body.get("vendor_id").toString()))
                .ifPresent(payment::setVendor);
        if (body.get("project_id") != null)
            projectRepository.findById(Long.parseLong(body.get("project_id").toString()))
                .ifPresent(payment::setProject);
        if (body.get("expense_id") != null)
            expenseRepository.findById(Long.parseLong(body.get("expense_id").toString()))
                .ifPresent(payment::setExpense);
        payment.setAmount(new BigDecimal(body.get("amount").toString()));
        payment.setPaymentDate(java.time.LocalDate.parse(body.get("payment_date").toString()));
        payment.setPaymentMode((String) body.getOrDefault("payment_mode", "CASH"));
        paymentRepository.save(payment);
        String vName = payment.getVendor() != null ? payment.getVendor().getName() : "—";
        auditService.log(currentUser(auth), "CREATE", "PAYMENT", vName,
                "Vendor payment to: " + vName + " | ₹" + payment.getAmount());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/api_payments_list/{id}")
    public ResponseEntity<?> updatePayment(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        Company company = currentCompany(auth);
        return paymentRepository.findById(id)
            .filter(p -> p.getCompany().getCompanyId().equals(company.getCompanyId()))
            .map(p -> {
                if (body.get("vendor_id") != null)
                    vendorRepository.findById(Long.parseLong(body.get("vendor_id").toString())).ifPresent(p::setVendor);
                if (body.get("project_id") != null)
                    projectRepository.findById(Long.parseLong(body.get("project_id").toString())).ifPresent(p::setProject);
                if (body.get("expense_id") != null)
                    expenseRepository.findById(Long.parseLong(body.get("expense_id").toString())).ifPresent(p::setExpense);
                if (body.get("amount") != null) p.setAmount(new BigDecimal(body.get("amount").toString()));
                if (body.get("payment_date") != null) p.setPaymentDate(java.time.LocalDate.parse(body.get("payment_date").toString()));
                if (body.get("payment_mode") != null) p.setPaymentMode((String) body.get("payment_mode"));
                paymentRepository.save(p);
                auditService.log(currentUser(auth), "UPDATE", "PAYMENT",
                        p.getVendor() != null ? p.getVendor().getName() : "—",
                        "Updated vendor payment #" + p.getPaymentId() + " | ₹" + p.getAmount());
                return ResponseEntity.ok(Map.of("success", true));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api_payments_list/{id}")
    public ResponseEntity<?> deletePayment(@PathVariable Long id, Authentication auth) {
        Company company = currentCompany(auth);
        return paymentRepository.findById(id)
            .filter(p -> p.getCompany().getCompanyId().equals(company.getCompanyId()))
            .map(p -> { paymentRepository.delete(p);
                auditService.log(currentUser(auth), "DELETE", "PAYMENT",
                        p.getVendor() != null ? p.getVendor().getName() : "—",
                        "Deleted payment #" + p.getPaymentId());
                return ResponseEntity.ok(Map.of("success", true)); })
            .orElse(ResponseEntity.notFound().build());
    }

    // ─── CLIENT PAYMENTS ─────────────────────────────────────────────────
    @GetMapping("/api_client_payments_list")
    public ResponseEntity<?> listClientPayments(Authentication auth) {
        Company company = currentCompany(auth);
        List<ClientPayment> payments = clientPaymentRepository.findByCompany(company);
        List<Map<String, Object>> result = payments.stream()
            .sorted(Comparator.comparing(ClientPayment::getPaymentDate).reversed())
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("client_payment_id", p.getClientPaymentId());
                m.put("payment_id", p.getClientPaymentId()); // legacy alias
                m.put("project_id", p.getProject() != null ? p.getProject().getProjectId() : null);
                m.put("project_name", p.getProject() != null ? p.getProject().getName() : null);
                m.put("amount", p.getAmount());
                m.put("payment_date", p.getPaymentDate());
                m.put("payment_mode", p.getPaymentMode());
                m.put("reference_number", p.getReferenceNumber());
                m.put("remarks", p.getRemarks());
                return m;
            }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api_client_payments_list")
    public ResponseEntity<?> createClientPayment(@RequestBody Map<String, Object> body, Authentication auth) {
        Company company = currentCompany(auth);
        ClientPayment cp = new ClientPayment();
        cp.setCompany(company);
        if (body.get("project_id") != null)
            projectRepository.findById(Long.parseLong(body.get("project_id").toString()))
                .ifPresent(cp::setProject);
        cp.setAmount(new BigDecimal(body.get("amount").toString()));
        cp.setPaymentDate(java.time.LocalDate.parse(body.get("payment_date").toString()));
        cp.setPaymentMode((String) body.getOrDefault("payment_mode", "CASH"));
        cp.setRemarks((String) body.get("remarks"));
        cp.setReferenceNumber((String) body.get("reference_number"));
        clientPaymentRepository.save(cp);
        String cpProj = cp.getProject() != null ? cp.getProject().getName() : "—";
        auditService.log(currentUser(auth), "CREATE", "CLIENT_PAYMENT", cpProj,
                "Client payment for: " + cpProj + " | ₹" + cp.getAmount());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/api_client_payments_list/{id}")
    public ResponseEntity<?> updateClientPayment(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        Company company = currentCompany(auth);
        return clientPaymentRepository.findById(id)
            .filter(p -> p.getCompany().getCompanyId().equals(company.getCompanyId()))
            .map(p -> {
                if (body.get("project_id") != null)
                    projectRepository.findById(Long.parseLong(body.get("project_id").toString())).ifPresent(p::setProject);
                if (body.get("amount") != null) p.setAmount(new BigDecimal(body.get("amount").toString()));
                if (body.get("payment_date") != null) p.setPaymentDate(java.time.LocalDate.parse(body.get("payment_date").toString()));
                if (body.get("payment_mode") != null) p.setPaymentMode((String) body.get("payment_mode"));
                if (body.containsKey("reference_number")) p.setReferenceNumber((String) body.get("reference_number"));
                if (body.containsKey("remarks")) p.setRemarks((String) body.get("remarks"));
                clientPaymentRepository.save(p);
                auditService.log(currentUser(auth), "UPDATE", "CLIENT_PAYMENT",
                        p.getProject() != null ? p.getProject().getName() : "—",
                        "Updated client payment #" + p.getClientPaymentId() + " | ₹" + p.getAmount());
                return ResponseEntity.ok(Map.of("success", true));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api_client_payments_list/{id}")
    public ResponseEntity<?> deleteClientPayment(@PathVariable Long id, Authentication auth) {
        Company company = currentCompany(auth);
        return clientPaymentRepository.findById(id)
            .filter(p -> p.getCompany().getCompanyId().equals(company.getCompanyId()))
            .map(p -> { clientPaymentRepository.delete(p);
                auditService.log(currentUser(auth), "DELETE", "CLIENT_PAYMENT",
                        p.getProject() != null ? p.getProject().getName() : "—",
                        "Deleted client payment #" + p.getClientPaymentId());
                return ResponseEntity.ok(Map.of("success", true)); })
            .orElse(ResponseEntity.notFound().build());
    }

    // ─── MASTER CATEGORIES ───────────────────────────────────────────────
    @GetMapping("/api_master_categories_list")
    public ResponseEntity<?> listMasterCategories(@RequestParam(required = false) String type) {
        List<MasterCategory> cats;
        if (type != null && !type.isBlank()) {
            cats = masterCategoryRepository.findByActiveAndTypeOrderByNameAsc(true, type);
        } else {
            cats = masterCategoryRepository.findByActiveOrderByNameAsc(true);
        }
        List<Map<String, Object>> result = cats.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("category_id", c.getCategoryId());
            m.put("name", c.getName());
            m.put("type", c.getType());
            List<Map<String, Object>> subs = c.getSubcategories() != null ? c.getSubcategories().stream().map(s -> {
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("subcategory_id", s.getSubcategoryId());
                sm.put("name", s.getName());
                sm.put("default_unit", s.getDefaultUnit());
                return sm;
            }).collect(Collectors.toList()) : List.of();
            m.put("subcategories", subs);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api_master_categories_list")
    public ResponseEntity<?> createMasterCategory(@RequestBody Map<String, Object> body, Authentication auth) {
        User user = currentUser(auth);
        if (!"ADMIN".equals(user.getRole()) && !"MANAGER".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Permission denied"));
        }
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Category name is required"));
        }
        if (masterCategoryRepository.findAll().stream().anyMatch(c -> c.getName().equalsIgnoreCase(name))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Category with this name already exists"));
        }
        MasterCategory category = new MasterCategory();
        category.setName(name);
        category.setType((String) body.getOrDefault("type", "MATERIAL"));
        category.setActive(true);
        masterCategoryRepository.save(category);

        // Save subcategories
        addSubcategories(category, body);
        masterCategoryRepository.save(category);

        auditService.log(currentUser(auth), "CREATE", "MASTER_CATEGORY", name,
                "Created category: " + name);
        return ResponseEntity.ok(Map.of("success", true, "category_id", category.getCategoryId()));
    }

    @PutMapping("/api_master_categories_list/{id}")
    public ResponseEntity<?> updateMasterCategory(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        User user = currentUser(auth);
        if (!"ADMIN".equals(user.getRole()) && !"MANAGER".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Permission denied"));
        }
        return masterCategoryRepository.findById(id)
            .map(category -> {
                String newName = (String) body.getOrDefault("name", category.getName());
                category.setName(newName);
                if (body.get("type") != null) category.setType((String) body.get("type"));

                if (body.containsKey("subcategories")) {
                    if (category.getSubcategories() != null) category.getSubcategories().clear();
                    addSubcategories(category, body);
                }
                masterCategoryRepository.save(category);
                auditService.log(currentUser(auth), "UPDATE", "MASTER_CATEGORY", category.getName(),
                        "Updated category: " + category.getName());
                return ResponseEntity.ok(Map.of("success", true));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api_master_categories_list/{id}")
    public ResponseEntity<?> deleteMasterCategory(@PathVariable Long id, Authentication auth) {
        User user = currentUser(auth);
        if (!"ADMIN".equals(user.getRole()) && !"MANAGER".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Permission denied"));
        }
        return masterCategoryRepository.findById(id)
            .map(c -> { masterCategoryRepository.delete(c);
                auditService.log(currentUser(auth), "DELETE", "MASTER_CATEGORY", c.getName(),
                        "Deleted category: " + c.getName());
                return ResponseEntity.ok(Map.of("success", true)); })
            .orElse(ResponseEntity.notFound().build());
    }

    // ─── DASHBOARD: PROJECT FINANCIAL TABLE ──────────────────────────────
    @GetMapping("/dashboard/api/project-financial-table")
    public ResponseEntity<?> projectFinancialTable(Authentication auth) {
        Company company = currentCompany(auth);
        List<Project> projects = projectRepository.findByCompanyOrderByCreatedAtDesc(company);
        List<Expense> allExpenses = expenseRepository.findByCompany(company);
        List<Payment> allPayments = paymentRepository.findByCompanyAndProjectIn(company, projects);
        List<ClientPayment> allClientPayments = clientPaymentRepository.findByCompanyAndProjectIn(company, projects);

        List<Map<String, Object>> result = projects.stream().map(p -> {
            List<Expense> pExpenses = allExpenses.stream()
                .filter(e -> e.getProject() != null && e.getProject().getProjectId().equals(p.getProjectId()))
                .collect(Collectors.toList());

            BigDecimal totalSpent = pExpenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal creditAmount = pExpenses.stream()
                .filter(e -> "CREDIT".equalsIgnoreCase(e.getPaymentMode()))
                .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalPaidToVendors = allPayments.stream()
                .filter(pay -> pay.getProject() != null && pay.getProject().getProjectId().equals(p.getProjectId()))
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal vendorOutstanding = creditAmount.subtract(totalPaidToVendors);
            if (vendorOutstanding.compareTo(BigDecimal.ZERO) < 0) vendorOutstanding = BigDecimal.ZERO;

            BigDecimal amountReceived = allClientPayments.stream()
                .filter(cp -> cp.getProject() != null && cp.getProject().getProjectId().equals(p.getProjectId()))
                .map(ClientPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal budget = p.getBudget() != null ? p.getBudget() : BigDecimal.ZERO;
            BigDecimal balanceBudget = budget.subtract(totalSpent);

            String indicator;
            if (balanceBudget.compareTo(BigDecimal.ZERO) < 0) indicator = "over";
            else if (balanceBudget.compareTo(budget.multiply(new BigDecimal("0.1"))) < 0) indicator = "near";
            else indicator = "good";

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("project_id", p.getProjectId());
            m.put("project_name", p.getName());
            m.put("status", p.getStatus());
            m.put("budget", budget);
            m.put("amount_spent", totalSpent);
            m.put("amount_received", amountReceived);
            m.put("vendor_outstanding", vendorOutstanding);
            m.put("balance_budget", balanceBudget);
            m.put("indicator", indicator);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ─── VENDOR OUTSTANDING ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    @GetMapping("/api_vendor_outstanding")
    public ResponseEntity<?> vendorOutstanding(
            @RequestParam Long vendor_id,
            @RequestParam(required = false) Long project_id,
            Authentication auth) {
        Company company = currentCompany(auth);
        Optional<Vendor> vendor = vendorRepository.findById(vendor_id);
        if (vendor.isEmpty()) return ResponseEntity.ok(Map.of("outstanding", 0, "total_outstanding", 0));

        List<Expense> expenses = expenseRepository.findByCompanyAndVendor(company, vendor.get())
            .stream().filter(e -> "CREDIT".equalsIgnoreCase(e.getPaymentMode()))
            .filter(e -> project_id == null || (e.getProject() != null && e.getProject().getProjectId().equals(project_id)))
            .collect(Collectors.toList());

        BigDecimal totalCredit = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Payment> payments = paymentRepository.findByCompanyAndVendor(company, vendor.get())
            .stream()
            .filter(p -> project_id == null || (p.getProject() != null && p.getProject().getProjectId().equals(project_id)))
            .collect(Collectors.toList());

        BigDecimal totalPaid = payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = totalCredit.subtract(totalPaid);
        if (outstanding.compareTo(BigDecimal.ZERO) < 0) outstanding = BigDecimal.ZERO;

        // Also compute total outstanding (all projects) for the vendor
        BigDecimal allCredit = expenseRepository.findByCompanyAndVendor(company, vendor.get())
            .stream().filter(e -> "CREDIT".equalsIgnoreCase(e.getPaymentMode()))
            .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal allPaid = paymentRepository.findByCompanyAndVendor(company, vendor.get())
            .stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOutstanding = allCredit.subtract(allPaid);
        if (totalOutstanding.compareTo(BigDecimal.ZERO) < 0) totalOutstanding = BigDecimal.ZERO;

        // Resolve project name if project_id provided
        String projectName = null;
        if (project_id != null) {
            projectName = projectRepository.findById(project_id)
                .map(p -> p.getName()).orElse(null);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("vendor_name", vendor.get().getName());
        response.put("outstanding", outstanding);
        response.put("total_outstanding", totalOutstanding);
        if (project_id != null) {
            response.put("project_outstanding", outstanding);
            response.put("project_name", projectName);
        }
        return ResponseEntity.ok(response);
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────
    private Map<String, Object> buildExpenseMap(Expense e) {
        // Extract subcategory from first ExpenseItem
        String subcategory = "";
        if (e.getItems() != null && !e.getItems().isEmpty()) {
            subcategory = e.getItems().get(0).getItemName() != null ? e.getItems().get(0).getItemName() : "";
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("expense_id", e.getExpenseId());
        m.put("project_id", e.getProject() != null ? e.getProject().getProjectId() : null);
        m.put("project_name", e.getProject() != null ? e.getProject().getName() : null);
        m.put("vendor_id", e.getVendor() != null ? e.getVendor().getVendorId() : null);
        m.put("vendor_name", e.getVendor() != null ? e.getVendor().getName() : null);
        m.put("expense_type", e.getExpenseType());
        m.put("category", e.getCategory());
        m.put("subcategory", subcategory);
        m.put("amount", e.getAmount());
        m.put("payment_mode", e.getPaymentMode());
        m.put("expense_date", e.getExpenseDate());
        m.put("description", e.getDescription());
        m.put("invoice_number", e.getInvoiceNumber());
        m.put("bill_url", e.getBillUrl());
        return m;
    }

    private Expense buildExpense(Map<String, Object> body, Company company, String type) {
        Expense e = new Expense();
        e.setCompany(company);
        e.setExpenseType(type);
        populateExpense(e, body, company);
        addItems(e, body);
        return e;
    }

    @SuppressWarnings("unchecked")
    private void addSubcategories(MasterCategory category, Map<String, Object> body) {
        if (body.get("subcategories") instanceof List<?> rawSubs) {
            if (category.getSubcategories() == null) category.setSubcategories(new ArrayList<>());
            for (Object rawSub : rawSubs) {
                if (rawSub instanceof Map<?, ?> subMap) {
                    Map<String, Object> sm = (Map<String, Object>) subMap;
                    String subName = (String) sm.get("name");
                    if (subName != null && !subName.isBlank()) {
                        SubCategory sub = new SubCategory();
                        sub.setParentCategory(category);
                        sub.setName(subName);
                        sub.setDefaultUnit((String) sm.get("default_unit"));
                        category.getSubcategories().add(sub);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addItems(Expense e, Map<String, Object> body) {
        if (body.get("items") instanceof List<?> rawItems) {
            if (e.getItems() == null) e.setItems(new java.util.ArrayList<>());
            for (Object rawItem : rawItems) {
                if (rawItem instanceof Map<?, ?> itemMap) {
                    Map<String, Object> im = (Map<String, Object>) itemMap;
                    ExpenseItem item = new ExpenseItem();
                    item.setExpense(e);
                    item.setItemName((String) im.get("item_name"));
                    String brand = (String) im.get("brand");
                    item.setBrand(brand != null ? brand.toUpperCase() : null);
                    if (im.get("quantity") != null) item.setQuantity(new BigDecimal(im.get("quantity").toString()));
                    if (im.get("measuring_unit") != null) item.setMeasuringUnit((String) im.get("measuring_unit"));
                    if (im.get("unit_price") != null) item.setUnitPrice(new BigDecimal(im.get("unit_price").toString()));
                    if (im.get("total_price") != null) item.setTotalPrice(new BigDecimal(im.get("total_price").toString()));
                    e.getItems().add(item);
                }
            }
        }
    }

    private void populateExpense(Expense e, Map<String, Object> body, Company company) {
        if (body.get("project_id") != null && !body.get("project_id").toString().isBlank())
            projectRepository.findById(Long.parseLong(body.get("project_id").toString())).ifPresent(e::setProject);
        if (body.get("vendor_id") != null && !body.get("vendor_id").toString().isBlank())
            vendorRepository.findById(Long.parseLong(body.get("vendor_id").toString())).ifPresent(e::setVendor);
        if (body.get("category") != null) e.setCategory((String) body.get("category"));
        if (body.get("description") != null) e.setDescription((String) body.get("description"));
        if (body.get("invoice_number") != null) e.setInvoiceNumber((String) body.get("invoice_number"));
        if (body.get("bill_url") != null) e.setBillUrl((String) body.get("bill_url"));

        String dateStr = (String) body.getOrDefault("invoice_date", body.get("expense_date"));
        if (dateStr != null && !dateStr.isBlank()) e.setExpenseDate(java.time.LocalDate.parse(dateStr));

        if (body.get("total_amount") != null) e.setAmount(new BigDecimal(body.get("total_amount").toString()));
        else if (body.get("amount") != null) e.setAmount(new BigDecimal(body.get("amount").toString()));

        String mode = (String) body.getOrDefault("payment_type", body.getOrDefault("payment_mode", "CASH"));
        e.setPaymentMode(mode);
    }
}
