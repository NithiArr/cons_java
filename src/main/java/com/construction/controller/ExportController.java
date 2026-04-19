package com.construction.controller;

import com.construction.domain.*;
import com.construction.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ExportController {

    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;
    private final ClientPaymentRepository clientPaymentRepository;
    private final ProjectRepository projectRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ── Domain helpers ────────────────────────────────────────────────────
    private User currentUser(Authentication auth) { return (User) auth.getPrincipal(); }
    private Company currentCompany(Authentication auth) { return currentUser(auth).getCompany(); }

    // ── Style helpers ─────────────────────────────────────────────────────
    private CellStyle headerStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont();
        f.setColor(IndexedColors.WHITE.getIndex());
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        s.setFont(f);
        s.setBorderBottom(BorderStyle.THIN);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private CellStyle totalStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont(); f.setBold(true); s.setFont(f);
        s.setBorderTop(BorderStyle.MEDIUM);
        return s;
    }

    private CellStyle amountStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return s;
    }

    private CellStyle totalAmountStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.cloneStyleFrom(totalStyle(wb));
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return s;
    }

    private void hdr(Sheet sheet, CellStyle style, String... cols) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell c = row.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(style);
        }
    }

    private void str(Row row, int col, String val) {
        row.createCell(col).setCellValue(val != null ? val : "");
    }

    private void amt(Row row, int col, BigDecimal val, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(val != null ? val.doubleValue() : 0.0);
        c.setCellStyle(style);
    }

    private void totalRow(Sheet sheet, int rowNum, int labelCol, int amtCol, int lastCol,
                          BigDecimal total, CellStyle tStyle, CellStyle tAmt) {
        Row r = sheet.createRow(rowNum);
        for (int i = labelCol; i <= lastCol; i++) {
            Cell c = r.createCell(i);
            if (i == labelCol) { c.setCellValue("TOTAL"); c.setCellStyle(tStyle); }
            else if (i == amtCol) { c.setCellValue(total.doubleValue()); c.setCellStyle(tAmt); }
            else { c.setCellStyle(tStyle); }
        }
    }

    private ResponseEntity<byte[]> respond(Workbook wb, String filename) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out); wb.close();
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        h.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        return ResponseEntity.ok().headers(h).body(out.toByteArray());
    }

    private String fmt(LocalDate d) { return d != null ? d.format(DATE_FMT) : ""; }

    // ─────────────────────────────────────────────────────────────────────
    // EXPORT PURCHASES  →  /export_purchases
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping("/export_purchases")
    public ResponseEntity<byte[]> exportPurchases(
            Authentication auth,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String date_from,
            @RequestParam(required = false) String date_to) throws Exception {

        Company company = currentCompany(auth);
        // Purchases are stored with expenseType = "Material Purchase"
        List<Expense> rows = expenseRepository.findByCompany(company).stream()
                .filter(e -> "Material Purchase".equals(e.getExpenseType()))
                .collect(Collectors.toList());

        if (project != null && !project.isBlank()) {
            Long pid = Long.parseLong(project);
            rows = rows.stream().filter(e -> e.getProject() != null && e.getProject().getProjectId().equals(pid))
                    .collect(Collectors.toList());
        }
        if (vendor != null && !vendor.isBlank()) {
            Long vid = Long.parseLong(vendor);
            rows = rows.stream().filter(e -> e.getVendor() != null && e.getVendor().getVendorId().equals(vid))
                    .collect(Collectors.toList());
        }
        if (category != null && !category.isBlank()) {
            rows = rows.stream().filter(e -> category.equals(e.getCategory())).collect(Collectors.toList());
        }
        if (date_from != null && !date_from.isBlank()) {
            LocalDate from = LocalDate.parse(date_from);
            rows = rows.stream().filter(e -> e.getExpenseDate() != null && !e.getExpenseDate().isBefore(from))
                    .collect(Collectors.toList());
        }
        if (date_to != null && !date_to.isBlank()) {
            LocalDate to = LocalDate.parse(date_to);
            rows = rows.stream().filter(e -> e.getExpenseDate() != null && !e.getExpenseDate().isAfter(to))
                    .collect(Collectors.toList());
        }
        rows.sort(Comparator.comparing(e -> e.getExpenseDate() != null ? e.getExpenseDate() : LocalDate.MIN));

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Purchases");
        CellStyle hStyle = headerStyle(wb), aStyle = amountStyle(wb),
                  tStyle = totalStyle(wb), tAmt = totalAmountStyle(wb);

        hdr(sheet, hStyle, "Date", "Project", "Vendor", "Category", "Invoice #", "Amount", "Payment Type");

        int rn = 1; BigDecimal grand = BigDecimal.ZERO;
        for (Expense e : rows) {
            Row r = sheet.createRow(rn++);
            str(r, 0, fmt(e.getExpenseDate()));
            str(r, 1, e.getProject() != null ? e.getProject().getName() : "");
            str(r, 2, e.getVendor()  != null ? e.getVendor().getName()  : "");
            str(r, 3, e.getCategory());
            str(r, 4, e.getInvoiceNumber());
            amt(r, 5, e.getAmount(), aStyle);
            str(r, 6, e.getPaymentMode());
            grand = grand.add(e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO);
        }
        totalRow(sheet, rn, 0, 5, 6, grand, tStyle, tAmt);
        for (int i = 0; i < 7; i++) sheet.autoSizeColumn(i);
        return respond(wb, "purchases_" + LocalDate.now() + ".xlsx");
    }

    // ─────────────────────────────────────────────────────────────────────
    // EXPORT EXPENSES  →  /export_expenses
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping("/export_expenses")
    public ResponseEntity<byte[]> exportExpenses(
            Authentication auth,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String date_from,
            @RequestParam(required = false) String date_to) throws Exception {

        Company company = currentCompany(auth);
        List<Expense> rows = expenseRepository.findByCompany(company).stream()
                .filter(e -> "Regular Expense".equals(e.getExpenseType()))
                .collect(Collectors.toList());

        if (project != null && !project.isBlank()) {
            Long pid = Long.parseLong(project);
            rows = rows.stream().filter(e -> e.getProject() != null && e.getProject().getProjectId().equals(pid))
                    .collect(Collectors.toList());
        }
        if (category != null && !category.isBlank()) {
            rows = rows.stream().filter(e -> category.equals(e.getCategory())).collect(Collectors.toList());
        }
        if (date_from != null && !date_from.isBlank()) {
            LocalDate from = LocalDate.parse(date_from);
            rows = rows.stream().filter(e -> e.getExpenseDate() != null && !e.getExpenseDate().isBefore(from))
                    .collect(Collectors.toList());
        }
        if (date_to != null && !date_to.isBlank()) {
            LocalDate to = LocalDate.parse(date_to);
            rows = rows.stream().filter(e -> e.getExpenseDate() != null && !e.getExpenseDate().isAfter(to))
                    .collect(Collectors.toList());
        }
        rows.sort(Comparator.comparing(e -> e.getExpenseDate() != null ? e.getExpenseDate() : LocalDate.MIN));

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Expenses");
        CellStyle hStyle = headerStyle(wb), aStyle = amountStyle(wb),
                  tStyle = totalStyle(wb), tAmt = totalAmountStyle(wb);

        hdr(sheet, hStyle, "Date", "Project", "Category", "Description", "Amount", "Payment Mode");

        int rn = 1; BigDecimal grand = BigDecimal.ZERO;
        for (Expense e : rows) {
            Row r = sheet.createRow(rn++);
            str(r, 0, fmt(e.getExpenseDate()));
            str(r, 1, e.getProject() != null ? e.getProject().getName() : "");
            str(r, 2, e.getCategory());
            str(r, 3, e.getDescription());
            amt(r, 4, e.getAmount(), aStyle);
            str(r, 5, e.getPaymentMode());
            grand = grand.add(e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO);
        }
        totalRow(sheet, rn, 0, 4, 5, grand, tStyle, tAmt);
        for (int i = 0; i < 6; i++) sheet.autoSizeColumn(i);
        return respond(wb, "expenses_" + LocalDate.now() + ".xlsx");
    }

    // ─────────────────────────────────────────────────────────────────────
    // EXPORT VENDOR PAYMENTS  →  /export_vendor_payments
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping("/export_vendor_payments")
    public ResponseEntity<byte[]> exportVendorPayments(
            Authentication auth,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) String date_from,
            @RequestParam(required = false) String date_to) throws Exception {

        Company company = currentCompany(auth);
        List<Payment> rows = paymentRepository.findByCompany(company);

        if (project != null && !project.isBlank()) {
            Long pid = Long.parseLong(project);
            rows = rows.stream().filter(p -> p.getProject() != null && p.getProject().getProjectId().equals(pid))
                    .collect(Collectors.toList());
        }
        if (vendor != null && !vendor.isBlank()) {
            Long vid = Long.parseLong(vendor);
            rows = rows.stream().filter(p -> p.getVendor() != null && p.getVendor().getVendorId().equals(vid))
                    .collect(Collectors.toList());
        }
        if (date_from != null && !date_from.isBlank()) {
            LocalDate from = LocalDate.parse(date_from);
            rows = rows.stream().filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().isBefore(from))
                    .collect(Collectors.toList());
        }
        if (date_to != null && !date_to.isBlank()) {
            LocalDate to = LocalDate.parse(date_to);
            rows = rows.stream().filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().isAfter(to))
                    .collect(Collectors.toList());
        }
        rows.sort(Comparator.comparing(p -> p.getPaymentDate() != null ? p.getPaymentDate() : LocalDate.MIN));

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Vendor Payments");
        CellStyle hStyle = headerStyle(wb), aStyle = amountStyle(wb),
                  tStyle = totalStyle(wb), tAmt = totalAmountStyle(wb);

        hdr(sheet, hStyle, "Date", "Vendor", "Project", "Amount", "Payment Mode");

        int rn = 1; BigDecimal grand = BigDecimal.ZERO;
        for (Payment p : rows) {
            Row r = sheet.createRow(rn++);
            str(r, 0, fmt(p.getPaymentDate()));
            str(r, 1, p.getVendor()  != null ? p.getVendor().getName()  : "");
            str(r, 2, p.getProject() != null ? p.getProject().getName() : "");
            amt(r, 3, p.getAmount(), aStyle);
            str(r, 4, p.getPaymentMode());
            grand = grand.add(p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO);
        }
        totalRow(sheet, rn, 0, 3, 4, grand, tStyle, tAmt);
        for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);
        return respond(wb, "vendor_payments_" + LocalDate.now() + ".xlsx");
    }

    // ─────────────────────────────────────────────────────────────────────
    // EXPORT CLIENT PAYMENTS  →  /export_client_payments
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping("/export_client_payments")
    public ResponseEntity<byte[]> exportClientPayments(
            Authentication auth,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String date_from,
            @RequestParam(required = false) String date_to) throws Exception {

        Company company = currentCompany(auth);
        List<ClientPayment> rows = clientPaymentRepository.findByCompany(company);

        if (project != null && !project.isBlank()) {
            Long pid = Long.parseLong(project);
            rows = rows.stream().filter(cp -> cp.getProject() != null && cp.getProject().getProjectId().equals(pid))
                    .collect(Collectors.toList());
        }
        if (date_from != null && !date_from.isBlank()) {
            LocalDate from = LocalDate.parse(date_from);
            rows = rows.stream().filter(cp -> cp.getPaymentDate() != null && !cp.getPaymentDate().isBefore(from))
                    .collect(Collectors.toList());
        }
        if (date_to != null && !date_to.isBlank()) {
            LocalDate to = LocalDate.parse(date_to);
            rows = rows.stream().filter(cp -> cp.getPaymentDate() != null && !cp.getPaymentDate().isAfter(to))
                    .collect(Collectors.toList());
        }
        rows.sort(Comparator.comparing(cp -> cp.getPaymentDate() != null ? cp.getPaymentDate() : LocalDate.MIN));

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Client Payments");
        CellStyle hStyle = headerStyle(wb), aStyle = amountStyle(wb),
                  tStyle = totalStyle(wb), tAmt = totalAmountStyle(wb);

        hdr(sheet, hStyle, "Date", "Project", "Amount", "Payment Mode", "Reference #", "Remarks");

        int rn = 1; BigDecimal grand = BigDecimal.ZERO;
        for (ClientPayment cp : rows) {
            Row r = sheet.createRow(rn++);
            str(r, 0, fmt(cp.getPaymentDate()));
            str(r, 1, cp.getProject() != null ? cp.getProject().getName() : "");
            amt(r, 2, cp.getAmount(), aStyle);
            str(r, 3, cp.getPaymentMode());
            str(r, 4, cp.getReferenceNumber());
            str(r, 5, cp.getRemarks());
            grand = grand.add(cp.getAmount() != null ? cp.getAmount() : BigDecimal.ZERO);
        }
        totalRow(sheet, rn, 0, 2, 5, grand, tStyle, tAmt);
        for (int i = 0; i < 6; i++) sheet.autoSizeColumn(i);
        return respond(wb, "client_payments_" + LocalDate.now() + ".xlsx");
    }

    // ─────────────────────────────────────────────────────────────────────
    // EXPORT FULL PROJECT REPORT  →  /export_project_excel/{projectId}
    //   Sheet 1: Summary  |  Sheet 2: Material Purchases
    //   Sheet 3: Regular Expenses  |  Sheet 4: Vendor Payments
    //   Sheet 5: Client Payments
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping("/export_project_excel/{projectId}")
    public ResponseEntity<byte[]> exportProjectDetail(
            Authentication auth,
            @PathVariable Long projectId) throws Exception {

        Company company = currentCompany(auth);
        Project project = projectRepository.findById(projectId)
                .filter(p -> p.getCompany().getCompanyId().equals(company.getCompanyId()))
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<Expense>       allExpenses     = expenseRepository.findByCompanyAndProject(company, project);
        List<Expense>       materialRows    = allExpenses.stream().filter(e -> "Material Purchase".equals(e.getExpenseType())).collect(Collectors.toList());
        List<Expense>       expenseRows     = allExpenses.stream().filter(e -> "Regular Expense".equals(e.getExpenseType())).collect(Collectors.toList());
        List<Payment>       vendorPayRows   = paymentRepository.findByCompanyAndProject(company, project);
        List<ClientPayment> clientPayRows   = clientPaymentRepository.findByCompanyAndProject(company, project);

        BigDecimal totalSpent    = allExpenses.stream().map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVendorPd = vendorPayRows.stream().map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalReceived = clientPayRows.stream().map(cp -> cp.getAmount() != null ? cp.getAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

        Workbook wb = new XSSFWorkbook();
        CellStyle hStyle = headerStyle(wb);
        CellStyle aStyle = amountStyle(wb);
        CellStyle tStyle = totalStyle(wb);
        CellStyle tAmt   = totalAmountStyle(wb);

        // ── Sheet 1: Summary ──────────────────────────────────────────
        Sheet sumSheet = wb.createSheet("Summary");

        Font titleFont = wb.createFont(); titleFont.setBold(true); titleFont.setFontHeightInPoints((short) 14);
        CellStyle titleStyle = wb.createCellStyle(); titleStyle.setFont(titleFont);
        Font boldFont = wb.createFont(); boldFont.setBold(true);
        CellStyle boldStyle = wb.createCellStyle(); boldStyle.setFont(boldFont);

        Row r0 = sumSheet.createRow(0);
        Cell tc = r0.createCell(0); tc.setCellValue("Project Report: " + project.getName()); tc.setCellStyle(titleStyle);
        sumSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        sumSheet.createRow(1).createCell(0).setCellValue("Generated: " + LocalDate.now().format(DATE_FMT));

        int sn = 3;
        String[][] info = {
            {"Project Name", project.getName()},
            {"Location",     project.getLocation() != null ? project.getLocation() : ""},
            {"Status",       project.getStatus()   != null ? project.getStatus()   : ""},
            {"Start Date",   project.getStartDate() != null ? project.getStartDate().format(DATE_FMT) : ""},
            {"End Date",     project.getEndDate()   != null ? project.getEndDate().format(DATE_FMT)   : ""},
            {"Budget",       project.getBudget()    != null ? "₹" + project.getBudget().toPlainString() : "N/A"},
        };
        for (String[] pair : info) {
            Row r = sumSheet.createRow(sn++);
            Cell k = r.createCell(0); k.setCellValue(pair[0]); k.setCellStyle(boldStyle);
            r.createCell(1).setCellValue(pair[1]);
        }

        sn++;
        Row fh = sumSheet.createRow(sn++);
        Cell fhc = fh.createCell(0); fhc.setCellValue("Financial Summary"); fhc.setCellStyle(hStyle);

        String[][] fin = {
            {"Total Spent (All)",        "₹" + totalSpent.toPlainString()},
            {"Total Paid to Vendors",    "₹" + totalVendorPd.toPlainString()},
            {"Total Received (Client)", "₹" + totalReceived.toPlainString()},
            {"Net Balance",              "₹" + totalReceived.subtract(totalSpent).toPlainString()},
        };
        for (String[] pair : fin) {
            Row r = sumSheet.createRow(sn++);
            Cell k = r.createCell(0); k.setCellValue(pair[0]); k.setCellStyle(boldStyle);
            r.createCell(1).setCellValue(pair[1]);
        }
        sumSheet.setColumnWidth(0, 12000); sumSheet.setColumnWidth(1, 8000);

        // ── Sheet 2: Material Purchases (with items expanded) ─────────
        Sheet ps = wb.createSheet("Material Purchases");

        // Invoice header: Date | Vendor | Invoice # | Category | Payment Type | Invoice Total
        // Item rows:      Item | Brand  | Qty       | Unit     | Unit Price   | Total Price
        String[] psHdr = {"Date / Item", "Vendor / Brand", "Invoice # / Qty", "Category / Unit", "Payment / Unit Price", "Total Amount"};
        hdr(ps, hStyle, psHdr);

        // Sub-header style (light blue, bold) for invoice grouped rows
        CellStyle invoiceRowStyle = wb.createCellStyle();
        invoiceRowStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
        invoiceRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font invFont = wb.createFont(); invFont.setBold(true); invFont.setColor(IndexedColors.WHITE.getIndex());
        invoiceRowStyle.setFont(invFont);

        // Item row style (alternating light grey)
        CellStyle itemRowStyle = wb.createCellStyle();
        itemRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        itemRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle itemAmtStyle = wb.createCellStyle();
        itemAmtStyle.cloneStyleFrom(itemRowStyle);
        itemAmtStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

        CellStyle invAmtStyle = wb.createCellStyle();
        invAmtStyle.cloneStyleFrom(invoiceRowStyle);
        invAmtStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

        materialRows.sort(Comparator.comparing(e -> e.getExpenseDate() != null ? e.getExpenseDate() : LocalDate.MIN));
        int pn = 1;
        BigDecimal pTot = BigDecimal.ZERO;

        for (Expense e : materialRows) {
            // ── Invoice summary row ───────────────────────────────────
            Row invRow = ps.createRow(pn++);
            String[] invCells = {
                fmt(e.getExpenseDate()),
                e.getVendor() != null ? e.getVendor().getName() : "",
                e.getInvoiceNumber() != null ? e.getInvoiceNumber() : "",
                e.getCategory() != null ? e.getCategory() : "",
                e.getPaymentMode() != null ? e.getPaymentMode() : "",
                ""
            };
            for (int i = 0; i < 5; i++) {
                Cell c = invRow.createCell(i);
                c.setCellValue(invCells[i]);
                c.setCellStyle(invoiceRowStyle);
            }
            Cell invAmt = invRow.createCell(5);
            invAmt.setCellValue(e.getAmount() != null ? e.getAmount().doubleValue() : 0.0);
            invAmt.setCellStyle(invAmtStyle);
            pTot = pTot.add(e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO);

            // ── Item rows ─────────────────────────────────────────────
            List<ExpenseItem> items = e.getItems();
            if (items != null && !items.isEmpty()) {
                for (ExpenseItem item : items) {
                    Row ir = ps.createRow(pn++);
                    // Col 0: item name
                    Cell c0 = ir.createCell(0); c0.setCellValue("  ↳ " + (item.getItemName() != null ? item.getItemName() : "")); c0.setCellStyle(itemRowStyle);
                    // Col 1: brand
                    Cell c1 = ir.createCell(1); c1.setCellValue(item.getBrand() != null ? item.getBrand() : ""); c1.setCellStyle(itemRowStyle);
                    // Col 2: quantity
                    Cell c2 = ir.createCell(2);
                    c2.setCellValue(item.getQuantity() != null ? item.getQuantity().doubleValue() : 0.0);
                    CellStyle qtyStyle = wb.createCellStyle(); qtyStyle.cloneStyleFrom(itemRowStyle);
                    qtyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.##"));
                    c2.setCellStyle(qtyStyle);
                    // Col 3: measuring unit
                    Cell c3 = ir.createCell(3); c3.setCellValue(item.getMeasuringUnit() != null ? item.getMeasuringUnit() : ""); c3.setCellStyle(itemRowStyle);
                    // Col 4: unit price
                    Cell c4 = ir.createCell(4); c4.setCellValue(item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0.0); c4.setCellStyle(itemAmtStyle);
                    // Col 5: total price
                    Cell c5 = ir.createCell(5); c5.setCellValue(item.getTotalPrice() != null ? item.getTotalPrice().doubleValue() : 0.0); c5.setCellStyle(itemAmtStyle);
                }
            }
        }

        totalRow(ps, pn, 0, 5, 5, pTot, tStyle, tAmt);
        for (int i = 0; i < 6; i++) ps.autoSizeColumn(i);
        // Minimum column widths for readability
        if (ps.getColumnWidth(0) < 4000) ps.setColumnWidth(0, 6000);
        if (ps.getColumnWidth(1) < 3000) ps.setColumnWidth(1, 4000);


        // ── Sheet 3: Regular Expenses ─────────────────────────────────
        Sheet es = wb.createSheet("Regular Expenses");
        hdr(es, hStyle, "Date", "Category", "Description", "Amount", "Payment Mode");
        expenseRows.sort(Comparator.comparing(e -> e.getExpenseDate() != null ? e.getExpenseDate() : LocalDate.MIN));
        int en = 1; BigDecimal eTot = BigDecimal.ZERO;
        for (Expense e : expenseRows) {
            Row r = es.createRow(en++);
            str(r, 0, fmt(e.getExpenseDate()));
            str(r, 1, e.getCategory());
            str(r, 2, e.getDescription());
            amt(r, 3, e.getAmount(), aStyle);
            str(r, 4, e.getPaymentMode());
            eTot = eTot.add(e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO);
        }
        totalRow(es, en, 0, 3, 4, eTot, tStyle, tAmt);
        for (int i = 0; i < 5; i++) es.autoSizeColumn(i);

        // ── Sheet 4: Vendor Payments ──────────────────────────────────
        Sheet vs = wb.createSheet("Vendor Payments");
        hdr(vs, hStyle, "Date", "Vendor", "Amount", "Payment Mode");
        vendorPayRows.sort(Comparator.comparing(p -> p.getPaymentDate() != null ? p.getPaymentDate() : LocalDate.MIN));
        int vn = 1; BigDecimal vTot = BigDecimal.ZERO;
        for (Payment p : vendorPayRows) {
            Row r = vs.createRow(vn++);
            str(r, 0, fmt(p.getPaymentDate()));
            str(r, 1, p.getVendor() != null ? p.getVendor().getName() : "");
            amt(r, 2, p.getAmount(), aStyle);
            str(r, 3, p.getPaymentMode());
            vTot = vTot.add(p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO);
        }
        totalRow(vs, vn, 0, 2, 3, vTot, tStyle, tAmt);
        for (int i = 0; i < 4; i++) vs.autoSizeColumn(i);

        // ── Sheet 5: Client Payments ──────────────────────────────────
        Sheet cs = wb.createSheet("Client Payments");
        hdr(cs, hStyle, "Date", "Amount", "Payment Mode", "Reference #", "Remarks");
        clientPayRows.sort(Comparator.comparing(cp -> cp.getPaymentDate() != null ? cp.getPaymentDate() : LocalDate.MIN));
        int cn = 1; BigDecimal cTot = BigDecimal.ZERO;
        for (ClientPayment cp : clientPayRows) {
            Row r = cs.createRow(cn++);
            str(r, 0, fmt(cp.getPaymentDate()));
            amt(r, 1, cp.getAmount(), aStyle);
            str(r, 2, cp.getPaymentMode());
            str(r, 3, cp.getReferenceNumber());
            str(r, 4, cp.getRemarks());
            cTot = cTot.add(cp.getAmount() != null ? cp.getAmount() : BigDecimal.ZERO);
        }
        totalRow(cs, cn, 0, 1, 4, cTot, tStyle, tAmt);
        for (int i = 0; i < 5; i++) cs.autoSizeColumn(i);

        String safe = project.getName().replaceAll("[^a-zA-Z0-9 ]", "").trim().replace(' ', '_');
        return respond(wb, safe + "_report_" + LocalDate.now() + ".xlsx");
    }
}
