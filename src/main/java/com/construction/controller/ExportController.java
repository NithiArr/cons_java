package com.construction.controller;

import com.construction.domain.*;
import com.construction.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
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
        s.setDataFormat(wb.createDataFormat().getFormat("[$₹-en-IN]##,##,##0.00"));
        return s;
    }

    private CellStyle totalAmountStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.cloneStyleFrom(totalStyle(wb));
        s.setDataFormat(wb.createDataFormat().getFormat("[$₹-en-IN]##,##,##0.00"));
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

    private String fmtRupee(BigDecimal amt) {
        if (amt == null) return "₹0.00";
        boolean isNegative = amt.compareTo(BigDecimal.ZERO) < 0;
        BigDecimal absAmt = amt.abs();
        String s = String.format(java.util.Locale.US, "%.2f", absAmt.doubleValue());
        String[] parts = s.split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts.length > 1 ? parts[1] : "00";

        int len = integerPart.length();
        if (len > 3) {
            String lastThree = integerPart.substring(len - 3);
            String otherNumbers = integerPart.substring(0, len - 3);
            StringBuilder res = new StringBuilder();
            int count = 0;
            for (int i = otherNumbers.length() - 1; i >= 0; i--) {
                res.insert(0, otherNumbers.charAt(i));
                count++;
                if (count % 2 == 0 && i > 0) {
                    res.insert(0, ",");
                }
            }
            integerPart = res.toString() + "," + lastThree;
        }
        return (isNegative ? "-₹" : "₹") + integerPart + "." + decimalPart;
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
    //   No project filter → Sheet 1: All Purchases + one sheet per project
    //   Project filtered  → Single sheet (original behaviour)
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

        boolean noProjectFilter = (project == null || project.isBlank());

        if (!noProjectFilter) {
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
        CellStyle hStyle = headerStyle(wb), aStyle = amountStyle(wb),
                  tStyle = totalStyle(wb), tAmt = totalAmountStyle(wb);

        if (noProjectFilter) {
            // ── Sheet 1: All Purchases ────────────────────────────────────
            Sheet allSheet = wb.createSheet("All Purchases");
            hdr(allSheet, hStyle, "Date", "Project", "Vendor", "Category", "Item", "Qty", "Unit", "Unit Price", "Total", "Invoice #", "Payment Type");
            int rn = 1; BigDecimal grand = BigDecimal.ZERO;
            for (Expense e : rows) {
                List<ExpenseItem> items = e.getItems();
                if (items != null && !items.isEmpty()) {
                    for (ExpenseItem item : items) {
                        Row r = allSheet.createRow(rn++);
                        str(r, 0, fmt(e.getExpenseDate()));
                        str(r, 1, e.getProject() != null ? e.getProject().getName() : "");
                        str(r, 2, e.getVendor()  != null ? e.getVendor().getName()  : "");
                        str(r, 3, e.getCategory() != null ? e.getCategory() : "");
                        str(r, 4, item.getItemName() != null ? item.getItemName() : "");
                        Cell c5 = r.createCell(5);
                        c5.setCellValue(item.getQuantity() != null ? item.getQuantity().doubleValue() : 0.0);
                        str(r, 6, item.getMeasuringUnit() != null ? item.getMeasuringUnit() : "");
                        amt(r, 7, item.getUnitPrice(), aStyle);
                        amt(r, 8, item.getTotalPrice(), aStyle);
                        str(r, 9, e.getInvoiceNumber() != null ? e.getInvoiceNumber() : "");
                        str(r, 10, e.getPaymentMode() != null ? e.getPaymentMode() : "");
                        grand = grand.add(item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO);
                    }
                } else {
                    Row r = allSheet.createRow(rn++);
                    str(r, 0, fmt(e.getExpenseDate()));
                    str(r, 1, e.getProject() != null ? e.getProject().getName() : "");
                    str(r, 2, e.getVendor()  != null ? e.getVendor().getName()  : "");
                    str(r, 3, e.getCategory() != null ? e.getCategory() : "");
                    str(r, 4, ""); r.createCell(5).setCellValue(0.0); str(r, 6, "");
                    amt(r, 7, BigDecimal.ZERO, aStyle);
                    amt(r, 8, e.getAmount(), aStyle);
                    str(r, 9, e.getInvoiceNumber() != null ? e.getInvoiceNumber() : "");
                    str(r, 10, e.getPaymentMode() != null ? e.getPaymentMode() : "");
                    grand = grand.add(e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO);
                }
            }
            totalRow(allSheet, rn, 0, 8, 10, grand, tStyle, tAmt);
            for (int i = 0; i < 11; i++) allSheet.autoSizeColumn(i);

            // ── One sheet per project ─────────────────────────────────────
            Map<String, List<Expense>> byProject = rows.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getProject() != null ? e.getProject().getName() : "No Project",
                            LinkedHashMap::new, Collectors.toList()));

            for (Map.Entry<String, List<Expense>> entry : byProject.entrySet()) {
                String sheetName = entry.getKey().replaceAll("[\\[\\]\\*\\?:/\\\\]", "");
                if (sheetName.length() > 31) sheetName = sheetName.substring(0, 31);

                Sheet ps = wb.createSheet(sheetName);
                hdr(ps, hStyle, "Date", "Vendor", "Category", "Item", "Qty", "Unit", "Unit Price", "Total", "Invoice #", "Payment Type");
                int pn = 1; BigDecimal pTot = BigDecimal.ZERO;
                for (Expense e : entry.getValue()) {
                    List<ExpenseItem> items = e.getItems();
                    if (items != null && !items.isEmpty()) {
                        for (ExpenseItem item : items) {
                            Row r = ps.createRow(pn++);
                            str(r, 0, fmt(e.getExpenseDate()));
                            str(r, 1, e.getVendor()  != null ? e.getVendor().getName()  : "");
                            str(r, 2, e.getCategory() != null ? e.getCategory() : "");
                            str(r, 3, item.getItemName() != null ? item.getItemName() : "");
                            Cell c4 = r.createCell(4);
                            c4.setCellValue(item.getQuantity() != null ? item.getQuantity().doubleValue() : 0.0);
                            str(r, 5, item.getMeasuringUnit() != null ? item.getMeasuringUnit() : "");
                            amt(r, 6, item.getUnitPrice(), aStyle);
                            amt(r, 7, item.getTotalPrice(), aStyle);
                            str(r, 8, e.getInvoiceNumber() != null ? e.getInvoiceNumber() : "");
                            str(r, 9, e.getPaymentMode() != null ? e.getPaymentMode() : "");
                            pTot = pTot.add(item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO);
                        }
                    } else {
                        Row r = ps.createRow(pn++);
                        str(r, 0, fmt(e.getExpenseDate()));
                        str(r, 1, e.getVendor()  != null ? e.getVendor().getName()  : "");
                        str(r, 2, e.getCategory() != null ? e.getCategory() : "");
                        str(r, 3, ""); r.createCell(4).setCellValue(0.0); str(r, 5, "");
                        amt(r, 6, BigDecimal.ZERO, aStyle);
                        amt(r, 7, e.getAmount(), aStyle);
                        str(r, 8, e.getInvoiceNumber() != null ? e.getInvoiceNumber() : "");
                        str(r, 9, e.getPaymentMode() != null ? e.getPaymentMode() : "");
                        pTot = pTot.add(e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO);
                    }
                }
                totalRow(ps, pn, 0, 7, 9, pTot, tStyle, tAmt);
                for (int i = 0; i < 10; i++) ps.autoSizeColumn(i);
            }

        } else {
            // ── Single sheet (project filtered) ──────────────────────────
            Sheet sheet = wb.createSheet("Purchases");
            hdr(sheet, hStyle, "Date", "Project", "Vendor", "Category", "Item", "Qty", "Unit", "Unit Price", "Total", "Invoice #", "Payment Type");
            int rn = 1; BigDecimal grand = BigDecimal.ZERO;
            for (Expense e : rows) {
                List<ExpenseItem> items = e.getItems();
                if (items != null && !items.isEmpty()) {
                    for (ExpenseItem item : items) {
                        Row r = sheet.createRow(rn++);
                        str(r, 0, fmt(e.getExpenseDate()));
                        str(r, 1, e.getProject() != null ? e.getProject().getName() : "");
                        str(r, 2, e.getVendor()  != null ? e.getVendor().getName()  : "");
                        str(r, 3, e.getCategory() != null ? e.getCategory() : "");
                        str(r, 4, item.getItemName() != null ? item.getItemName() : "");
                        Cell c5 = r.createCell(5);
                        c5.setCellValue(item.getQuantity() != null ? item.getQuantity().doubleValue() : 0.0);
                        str(r, 6, item.getMeasuringUnit() != null ? item.getMeasuringUnit() : "");
                        amt(r, 7, item.getUnitPrice(), aStyle);
                        amt(r, 8, item.getTotalPrice(), aStyle);
                        str(r, 9, e.getInvoiceNumber() != null ? e.getInvoiceNumber() : "");
                        str(r, 10, e.getPaymentMode() != null ? e.getPaymentMode() : "");
                        grand = grand.add(item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO);
                    }
                } else {
                    Row r = sheet.createRow(rn++);
                    str(r, 0, fmt(e.getExpenseDate()));
                    str(r, 1, e.getProject() != null ? e.getProject().getName() : "");
                    str(r, 2, e.getVendor()  != null ? e.getVendor().getName()  : "");
                    str(r, 3, e.getCategory() != null ? e.getCategory() : "");
                    str(r, 4, ""); r.createCell(5).setCellValue(0.0); str(r, 6, "");
                    amt(r, 7, BigDecimal.ZERO, aStyle);
                    amt(r, 8, e.getAmount(), aStyle);
                    str(r, 9, e.getInvoiceNumber() != null ? e.getInvoiceNumber() : "");
                    str(r, 10, e.getPaymentMode() != null ? e.getPaymentMode() : "");
                    grand = grand.add(e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO);
                }
            }
            totalRow(sheet, rn, 0, 8, 10, grand, tStyle, tAmt);
            for (int i = 0; i < 11; i++) sheet.autoSizeColumn(i);
        }

        return respond(wb, "purchases_" + LocalDate.now() + ".xlsx");
    }

    // ─────────────────────────────────────────────────────────────────────
    // EXPORT EXPENSES  →  /export_expenses
    //   No project filter → Sheet 1: All Expenses + one sheet per project
    //   Project filtered  → Single sheet (original behaviour)
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

        boolean noProjectFilter = (project == null || project.isBlank());

        if (!noProjectFilter) {
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
        CellStyle hStyle = headerStyle(wb), aStyle = amountStyle(wb),
                  tStyle = totalStyle(wb), tAmt = totalAmountStyle(wb);

        // ── Helper: write expenses into a sheet ──────────────────────────
        // (reused for All-sheet and per-project sheets)

        if (noProjectFilter) {
            // ── Sheet 1: All Expenses ────────────────────────────────────
            Sheet allSheet = wb.createSheet("All Expenses");
            hdr(allSheet, hStyle, "Date", "Project", "Category", "Subcategory", "Description", "Amount", "Payment Mode");
            int rn = 1; BigDecimal grand = BigDecimal.ZERO;
            for (Expense e : rows) {
                String subcat = (e.getItems() != null && !e.getItems().isEmpty() && e.getItems().get(0).getItemName() != null)
                        ? e.getItems().get(0).getItemName() : "";
                Row r = allSheet.createRow(rn++);
                str(r, 0, fmt(e.getExpenseDate()));
                str(r, 1, e.getProject() != null ? e.getProject().getName() : "");
                str(r, 2, e.getCategory());
                str(r, 3, subcat);
                str(r, 4, e.getDescription());
                amt(r, 5, e.getAmount(), aStyle);
                str(r, 6, e.getPaymentMode());
                grand = grand.add(e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO);
            }
            totalRow(allSheet, rn, 0, 5, 6, grand, tStyle, tAmt);
            for (int i = 0; i < 7; i++) allSheet.autoSizeColumn(i);

            // ── One sheet per project ────────────────────────────────────
            Map<String, List<Expense>> byProject = rows.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getProject() != null ? e.getProject().getName() : "No Project",
                            LinkedHashMap::new, Collectors.toList()));

            for (Map.Entry<String, List<Expense>> entry : byProject.entrySet()) {
                // Excel sheet names max 31 chars, strip illegal chars
                String sheetName = entry.getKey().replaceAll("[\\[\\]\\*\\?:/\\\\]", "");
                if (sheetName.length() > 31) sheetName = sheetName.substring(0, 31);

                Sheet ps = wb.createSheet(sheetName);
                hdr(ps, hStyle, "Date", "Category", "Subcategory", "Description", "Amount", "Payment Mode");
                int pn = 1; BigDecimal pTot = BigDecimal.ZERO;
                for (Expense e : entry.getValue()) {
                    String subcat = (e.getItems() != null && !e.getItems().isEmpty() && e.getItems().get(0).getItemName() != null)
                            ? e.getItems().get(0).getItemName() : "";
                    Row r = ps.createRow(pn++);
                    str(r, 0, fmt(e.getExpenseDate()));
                    str(r, 1, e.getCategory());
                    str(r, 2, subcat);
                    str(r, 3, e.getDescription());
                    amt(r, 4, e.getAmount(), aStyle);
                    str(r, 5, e.getPaymentMode());
                    pTot = pTot.add(e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO);
                }
                totalRow(ps, pn, 0, 4, 5, pTot, tStyle, tAmt);
                for (int i = 0; i < 6; i++) ps.autoSizeColumn(i);
            }

        } else {
            // ── Single sheet (project filtered) ──────────────────────────
            Sheet sheet = wb.createSheet("Expenses");
            hdr(sheet, hStyle, "Date", "Project", "Category", "Subcategory", "Description", "Amount", "Payment Mode");
            int rn = 1; BigDecimal grand = BigDecimal.ZERO;
            for (Expense e : rows) {
                String subcat = (e.getItems() != null && !e.getItems().isEmpty() && e.getItems().get(0).getItemName() != null)
                        ? e.getItems().get(0).getItemName() : "";
                Row r = sheet.createRow(rn++);
                str(r, 0, fmt(e.getExpenseDate()));
                str(r, 1, e.getProject() != null ? e.getProject().getName() : "");
                str(r, 2, e.getCategory());
                str(r, 3, subcat);
                str(r, 4, e.getDescription());
                amt(r, 5, e.getAmount(), aStyle);
                str(r, 6, e.getPaymentMode());
                grand = grand.add(e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO);
            }
            totalRow(sheet, rn, 0, 5, 6, grand, tStyle, tAmt);
            for (int i = 0; i < 7; i++) sheet.autoSizeColumn(i);
        }

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
            {"Budget",       project.getBudget()    != null ? fmtRupee(project.getBudget()) : "N/A"},
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
            {"Total Spent (All)",        fmtRupee(totalSpent)},
            {"Total Paid to Vendors",    fmtRupee(totalVendorPd)},
            {"Total Received (Client)",  fmtRupee(totalReceived)},
            {"Net Balance",              fmtRupee(totalReceived.subtract(totalSpent))},
        };
        for (String[] pair : fin) {
            Row r = sumSheet.createRow(sn++);
            Cell k = r.createCell(0); k.setCellValue(pair[0]); k.setCellStyle(boldStyle);
            r.createCell(1).setCellValue(pair[1]);
        }
        sumSheet.setColumnWidth(0, 12000); sumSheet.setColumnWidth(1, 8000);

        // ── Sheet 2: Material Purchases ─────────
        Sheet ps = wb.createSheet("Material Purchases");

        String[] psHdr = {"Date", "Category", "Sub Category", "Vendor", "Qty", "Unit", "Payment", "Unit Price", "Total Amount"};
        hdr(ps, hStyle, psHdr);

        materialRows.sort(Comparator.comparing(e -> e.getExpenseDate() != null ? e.getExpenseDate() : LocalDate.MIN));
        int pn = 1;
        BigDecimal pTot = BigDecimal.ZERO;

        for (Expense e : materialRows) {
            List<ExpenseItem> items = e.getItems();
            if (items != null && !items.isEmpty()) {
                for (ExpenseItem item : items) {
                    Row ir = ps.createRow(pn++);
                    str(ir, 0, fmt(e.getExpenseDate()));
                    str(ir, 1, e.getCategory() != null ? e.getCategory() : "");
                    str(ir, 2, item.getItemName() != null ? item.getItemName() : "");
                    str(ir, 3, e.getVendor() != null ? e.getVendor().getName() : "");
                    
                    Cell c4 = ir.createCell(4);
                    c4.setCellValue(item.getQuantity() != null ? item.getQuantity().doubleValue() : 0.0);
                    
                    str(ir, 5, item.getMeasuringUnit() != null ? item.getMeasuringUnit() : "");
                    str(ir, 6, e.getPaymentMode() != null ? e.getPaymentMode() : "");
                    amt(ir, 7, item.getUnitPrice(), aStyle);
                    amt(ir, 8, item.getTotalPrice(), aStyle);
                    pTot = pTot.add(item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO);
                }
            } else {
                Row ir = ps.createRow(pn++);
                str(ir, 0, fmt(e.getExpenseDate()));
                str(ir, 1, e.getCategory() != null ? e.getCategory() : "");
                str(ir, 2, "");
                str(ir, 3, e.getVendor() != null ? e.getVendor().getName() : "");
                
                Cell c4 = ir.createCell(4);
                c4.setCellValue(0.0);
                
                str(ir, 5, "");
                str(ir, 6, e.getPaymentMode() != null ? e.getPaymentMode() : "");
                amt(ir, 7, BigDecimal.ZERO, aStyle);
                amt(ir, 8, e.getAmount(), aStyle);
                pTot = pTot.add(e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO);
            }
        }

        totalRow(ps, pn, 0, 8, 8, pTot, tStyle, tAmt);
        for (int i = 0; i < 9; i++) ps.autoSizeColumn(i);


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

        // ── Sheet 6: Material Wise Summary ──────────────────────────
        Sheet mws = wb.createSheet("Material Wise Summary");
        hdr(mws, hStyle, "MATERIALS", "QTY", "MEASURING IN", "TOTAL AMOUNT");
        
        Map<String, List<ExpenseItem>> matMap = new LinkedHashMap<>();
        for (Expense e : materialRows) {
            if (e.getItems() != null) {
                for (ExpenseItem item : e.getItems()) {
                    String name = item.getItemName() != null && !item.getItemName().isBlank() ? item.getItemName() : "Unknown";
                    matMap.computeIfAbsent(name, k -> new ArrayList<>()).add(item);
                }
            }
        }
        
        int mwsRow = 1;
        BigDecimal mwsTotal = BigDecimal.ZERO;
        for (Map.Entry<String, List<ExpenseItem>> entry : matMap.entrySet()) {
            Row r = mws.createRow(mwsRow++);
            str(r, 0, entry.getKey());
            
            BigDecimal qty = entry.getValue().stream()
                .map(i -> i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Cell c1 = r.createCell(1);
            c1.setCellValue(qty.doubleValue());
            
            String unit = entry.getValue().isEmpty() ? "" : 
                          (entry.getValue().get(0).getMeasuringUnit() != null ? entry.getValue().get(0).getMeasuringUnit() : "");
            str(r, 2, unit);
            
            BigDecimal amt = entry.getValue().stream()
                .map(i -> i.getTotalPrice() != null ? i.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            amt(r, 3, amt, aStyle);
            
            mwsTotal = mwsTotal.add(amt);
        }
        totalRow(mws, mwsRow, 0, 3, 3, mwsTotal, tStyle, tAmt);
        for (int i = 0; i < 4; i++) mws.autoSizeColumn(i);
        
        // ── Sheet 7: Vendor Wise Summary ──────────────────────────
        Sheet vws = wb.createSheet("Vendor Wise Summary");
        hdr(vws, hStyle, "VENDOR NAME", "TOTAL BILL AMOUNT", "TOTAL AMOUNT PAID", "BALANCE AMOUNT");
        
        Map<String, BigDecimal> vendorBillMap = new LinkedHashMap<>();
        for (Expense e : materialRows) {
            if (e.getVendor() != null) {
                String vName = e.getVendor().getName();
                BigDecimal amt = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
                vendorBillMap.put(vName, vendorBillMap.getOrDefault(vName, BigDecimal.ZERO).add(amt));
            }
        }
        
        Map<String, BigDecimal> vendorPaidMap = new LinkedHashMap<>();
        for (Payment p : vendorPayRows) {
            if (p.getVendor() != null) {
                String vName = p.getVendor().getName();
                BigDecimal amt = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
                vendorPaidMap.put(vName, vendorPaidMap.getOrDefault(vName, BigDecimal.ZERO).add(amt));
            }
        }
        for (Expense e : materialRows) {
            if (e.getVendor() != null && e.getPaymentMode() != null && !e.getPaymentMode().equalsIgnoreCase("CREDIT")) {
                String vName = e.getVendor().getName();
                BigDecimal amt = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
                vendorPaidMap.put(vName, vendorPaidMap.getOrDefault(vName, BigDecimal.ZERO).add(amt));
            }
        }
        
        Set<String> allVendors = new TreeSet<>(vendorBillMap.keySet());
        allVendors.addAll(vendorPaidMap.keySet());
        
        int vwsRow = 1;
        BigDecimal vwsBillTotal = BigDecimal.ZERO;
        BigDecimal vwsPaidTotal = BigDecimal.ZERO;
        BigDecimal vwsBalTotal = BigDecimal.ZERO;
        
        for (String vName : allVendors) {
            Row r = vws.createRow(vwsRow++);
            str(r, 0, vName);
            
            BigDecimal bill = vendorBillMap.getOrDefault(vName, BigDecimal.ZERO);
            BigDecimal paid = vendorPaidMap.getOrDefault(vName, BigDecimal.ZERO);
            BigDecimal bal = bill.subtract(paid);
            if (bal.compareTo(BigDecimal.ZERO) < 0) bal = BigDecimal.ZERO;
            
            amt(r, 1, bill, aStyle);
            amt(r, 2, paid, aStyle);
            amt(r, 3, bal, aStyle);
            
            vwsBillTotal = vwsBillTotal.add(bill);
            vwsPaidTotal = vwsPaidTotal.add(paid);
            vwsBalTotal = vwsBalTotal.add(bal);
        }
        
        Row rVwsTot = vws.createRow(vwsRow);
        Cell cellTot = rVwsTot.createCell(0); cellTot.setCellValue("TOTAL"); cellTot.setCellStyle(tStyle);
        amt(rVwsTot, 1, vwsBillTotal, tAmt);
        amt(rVwsTot, 2, vwsPaidTotal, tAmt);
        amt(rVwsTot, 3, vwsBalTotal, tAmt);
        
        for (int i = 0; i < 4; i++) vws.autoSizeColumn(i);

        String safe = project.getName().replaceAll("[^a-zA-Z0-9 ]", "").trim().replace(' ', '_');
        return respond(wb, safe + "_report_" + LocalDate.now() + ".xlsx");
    }

    // ─────────────────────────────────────────────────────────────────────
    // EXPORT CASH BALANCE  →  /export_cash_balance
    //   Sheet 1: Balance Summary | Sheet 2: Transactions
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping("/export_cash_balance")
    public ResponseEntity<byte[]> exportCashBalance(
            Authentication auth,
            @RequestParam(required = false) String project_ids,
            @RequestParam String from_date,
            @RequestParam String to_date) throws Exception {

        Company company = currentCompany(auth);
        LocalDate from = LocalDate.parse(from_date);
        LocalDate to   = LocalDate.parse(to_date);

        // Resolve project filter
        List<Long> pidList = null;
        if (project_ids != null && !project_ids.equalsIgnoreCase("all") && !project_ids.isBlank()) {
            pidList = Arrays.stream(project_ids.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(Long::parseLong).collect(Collectors.toList());
        }

        final List<Long> finalPidList = pidList;
        boolean allProjects = (finalPidList == null);

        // Fetch period data
        List<ClientPayment> clientPays = clientPaymentRepository.findByCompanyAndPaymentDateBetween(company, from, to);
        List<Payment>       vendorPays = paymentRepository.findByCompanyAndPaymentDateBetween(company, from, to);
        List<Expense>       expenses   = expenseRepository.findByCompanyAndExpenseDateBetween(company, from, to);

        if (!allProjects) {
            clientPays = clientPays.stream().filter(cp -> cp.getProject() != null && finalPidList.contains(cp.getProject().getProjectId())).collect(Collectors.toList());
            vendorPays = vendorPays.stream().filter(p  -> p.getProject()  != null && finalPidList.contains(p.getProject().getProjectId())).collect(Collectors.toList());
            expenses   = expenses.stream().filter(e   -> e.getProject()   != null && finalPidList.contains(e.getProject().getProjectId())).collect(Collectors.toList());
        }

        // Fetch opening balance data (before from_date)
        List<ClientPayment> opClientPays = clientPaymentRepository.findByCompanyAndPaymentDateBefore(company, from);
        List<Payment>       opVendorPays = paymentRepository.findByCompanyAndPaymentDateBefore(company, from);
        List<Expense>       opExpenses   = expenseRepository.findByCompanyAndExpenseDateBefore(company, from);
        if (!allProjects) {
            opClientPays = opClientPays.stream().filter(cp -> cp.getProject() != null && finalPidList.contains(cp.getProject().getProjectId())).collect(Collectors.toList());
            opVendorPays = opVendorPays.stream().filter(p  -> p.getProject()  != null && finalPidList.contains(p.getProject().getProjectId())).collect(Collectors.toList());
            opExpenses   = opExpenses.stream().filter(e   -> e.getProject()   != null && finalPidList.contains(e.getProject().getProjectId())).collect(Collectors.toList());
        }

        BigDecimal opInflow   = opClientPays.stream().map(ClientPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal opExpOut   = opExpenses.stream().filter(e -> !"CREDIT".equalsIgnoreCase(e.getPaymentMode())).map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal opPayOut   = opVendorPays.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal openingBal = opInflow.subtract(opExpOut).subtract(opPayOut);

        BigDecimal clientReceipts = clientPays.stream().map(ClientPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vendorPayTotal = vendorPays.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expCredit      = expenses.stream().filter(e -> "CREDIT".equalsIgnoreCase(e.getPaymentMode())).map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expCash        = expenses.stream().filter(e -> "CASH".equalsIgnoreCase(e.getPaymentMode())).map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expBankUpi     = expenses.stream().filter(e -> "BANK".equalsIgnoreCase(e.getPaymentMode()) || "UPI".equalsIgnoreCase(e.getPaymentMode())).map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal closingBal     = openingBal.add(clientReceipts).subtract(expCash).subtract(expBankUpi).subtract(vendorPayTotal);

        // Resolve list of projects to process
        List<Project> projectsToProcess;
        if (allProjects) {
            projectsToProcess = projectRepository.findByCompanyOrderByCreatedAtDesc(company);
        } else {
            List<Long> safeIds = finalPidList != null ? finalPidList : Collections.emptyList();
            projectsToProcess = projectRepository.findAllById(safeIds).stream()
                    .filter(p -> p.getCompany().getCompanyId().equals(company.getCompanyId()))
                    .collect(Collectors.toList());
        }

        // Local class to store project summary values
        class ProjectSummary {
            Project proj;
            BigDecimal op = BigDecimal.ZERO;
            BigDecimal receipts = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;
            BigDecimal cash = BigDecimal.ZERO;
            BigDecimal bankUpi = BigDecimal.ZERO;
            BigDecimal vendorPaid = BigDecimal.ZERO;
            BigDecimal cl = BigDecimal.ZERO;
        }

        List<ProjectSummary> projSummaries = new ArrayList<>();
        BigDecimal totalOp = BigDecimal.ZERO;
        BigDecimal totalCl = BigDecimal.ZERO;
        BigDecimal totalReceipts = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal totalCash = BigDecimal.ZERO;
        BigDecimal totalBankUpi = BigDecimal.ZERO;
        BigDecimal totalVendorPaid = BigDecimal.ZERO;

        for (Project pObj : projectsToProcess) {
            ProjectSummary ps = new ProjectSummary();
            ps.proj = pObj;
            Long pid = pObj.getProjectId();

            // Calculate opening balance for this project
            BigDecimal pOpInflow = opClientPays.stream()
                    .filter(cp -> cp.getProject() != null && cp.getProject().getProjectId().equals(pid))
                    .map(ClientPayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal pOpExpOut = opExpenses.stream()
                    .filter(e -> e.getProject() != null && e.getProject().getProjectId().equals(pid))
                    .filter(e -> !"CREDIT".equalsIgnoreCase(e.getPaymentMode()))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal pOpPayOut = opVendorPays.stream()
                    .filter(p -> p.getProject() != null && p.getProject().getProjectId().equals(pid))
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ps.op = pOpInflow.subtract(pOpExpOut).subtract(pOpPayOut);

            // Calculate period data for this project
            ps.receipts = clientPays.stream()
                    .filter(cp -> cp.getProject() != null && cp.getProject().getProjectId().equals(pid))
                    .map(ClientPayment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ps.vendorPaid = vendorPays.stream()
                    .filter(p -> p.getProject() != null && p.getProject().getProjectId().equals(pid))
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ps.credit = expenses.stream()
                    .filter(e -> e.getProject() != null && e.getProject().getProjectId().equals(pid))
                    .filter(e -> "CREDIT".equalsIgnoreCase(e.getPaymentMode()))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ps.cash = expenses.stream()
                    .filter(e -> e.getProject() != null && e.getProject().getProjectId().equals(pid))
                    .filter(e -> "CASH".equalsIgnoreCase(e.getPaymentMode()))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            ps.bankUpi = expenses.stream()
                    .filter(e -> e.getProject() != null && e.getProject().getProjectId().equals(pid))
                    .filter(e -> "BANK".equalsIgnoreCase(e.getPaymentMode()) || "UPI".equalsIgnoreCase(e.getPaymentMode()))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            ps.cl = ps.op.add(ps.receipts).subtract(ps.cash).subtract(ps.bankUpi).subtract(ps.vendorPaid);

            projSummaries.add(ps);

            // Accumulate totals
            totalOp = totalOp.add(ps.op);
            totalCl = totalCl.add(ps.cl);
            totalReceipts = totalReceipts.add(ps.receipts);
            totalCredit = totalCredit.add(ps.credit);
            totalCash = totalCash.add(ps.cash);
            totalBankUpi = totalBankUpi.add(ps.bankUpi);
            totalVendorPaid = totalVendorPaid.add(ps.vendorPaid);
        }

        Workbook wb = new XSSFWorkbook();
        XSSFWorkbook xwb = (XSSFWorkbook) wb;
        CellStyle hStyle  = headerStyle(wb);
        CellStyle aStyle  = amountStyle(wb);
        CellStyle tStyle  = totalStyle(wb);
        CellStyle tAmt    = totalAmountStyle(wb);

        // ── Reliable color styles using XSSFFont ──────────────────────
        // Green — Client Payment (income)
        XSSFFont greenFont2 = xwb.createFont();
        greenFont2.setColor(new XSSFColor(new byte[]{(byte)0, (byte)130, (byte)0}, null));
        CellStyle greenText = xwb.createCellStyle(); greenText.setFont(greenFont2);
        CellStyle greenAmt  = xwb.createCellStyle(); greenAmt.setFont(greenFont2);
        greenAmt.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

        // Red — Vendor Payment / Expenses (outflow)
        XSSFFont redFont2 = xwb.createFont();
        redFont2.setColor(new XSSFColor(new byte[]{(byte)192, (byte)0, (byte)0}, null));
        CellStyle redText = xwb.createCellStyle(); redText.setFont(redFont2);
        CellStyle redAmt  = xwb.createCellStyle(); redAmt.setFont(redFont2);
        redAmt.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

        // Bold for key rows
        Font bold = wb.createFont(); bold.setBold(true);
        CellStyle boldStyle = wb.createCellStyle(); boldStyle.setFont(bold);
        CellStyle boldAmt   = wb.createCellStyle(); boldAmt.setFont(bold);
        boldAmt.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

        // ── Sheet 1: Balance Summary ──────────────────────────────────
        Sheet sum = wb.createSheet("Balance Summary");

        String periodLabel = from_date + " to " + to_date;
        // Resolve project names for display
        String projectsLabel;
        if (allProjects) {
            projectsLabel = "All Projects";
        } else {
            List<Long> safeIds = finalPidList != null ? finalPidList : Collections.emptyList();
            projectsLabel = projectRepository.findAllById(safeIds).stream()
                    .filter(p -> p.getCompany().getCompanyId().equals(company.getCompanyId()))
                    .map(Project::getName)
                    .collect(Collectors.joining(", "));
        }

        // Each entry: { label, value, "green"|"red"|"bold"|null }
        int sn = 0;
        Object[][] summaryRows = {
            {"Period",                  periodLabel,                    null},
            {"Projects",               projectsLabel,                  null},
            {""},
            {"Opening Balance",         openingBal,                    "bold"},
            {"  Client Receipts",       clientReceipts,                "green"},
            {"  Expenses - Credit",     expCredit.negate(),            "red"},
            {"  Expenses - Cash",       expCash.negate(),              "red"},
            {"  Expenses - Bank/UPI",   expBankUpi.negate(),           "red"},
            {"  Vendor Payments Made",  vendorPayTotal.negate(),       "red"},
            {"Closing Balance",         closingBal,                    "bold"},
        };
        for (Object[] row : summaryRows) {
            Row r = sum.createRow(sn++);
            if (row.length == 0 || (row[0] instanceof String && ((String) row[0]).isEmpty())) continue;
            String color = row.length > 2 ? (String) row[2] : null;
            Cell k = r.createCell(0); k.setCellValue((String) row[0]);
            if (row[1] instanceof BigDecimal) {
                BigDecimal val = (BigDecimal) row[1];
                Cell v = r.createCell(1); v.setCellValue(val.doubleValue());
                if ("bold".equals(color)) {
                    k.setCellStyle(boldStyle); v.setCellStyle(boldAmt);
                } else if ("green".equals(color)) {
                    k.setCellStyle(greenText); v.setCellStyle(greenAmt);
                } else if ("red".equals(color)) {
                    k.setCellStyle(redText);   v.setCellStyle(redAmt);
                } else {
                    v.setCellStyle(aStyle);
                }
            } else if (row[1] != null) {
                r.createCell(1).setCellValue((String) row[1]);
            }
        }

        // Add spacing before project summary table
        sn += 2;

        // Styles for project breakdown table
        CellStyle cellBorder = wb.createCellStyle();
        cellBorder.setBorderTop(BorderStyle.THIN);
        cellBorder.setBorderBottom(BorderStyle.THIN);
        cellBorder.setBorderLeft(BorderStyle.THIN);
        cellBorder.setBorderRight(BorderStyle.THIN);

        CellStyle projDataStyle = wb.createCellStyle();
        projDataStyle.cloneStyleFrom(cellBorder);

        CellStyle opDataStyle = wb.createCellStyle();
        opDataStyle.cloneStyleFrom(cellBorder);
        opDataStyle.setFont(bold);
        opDataStyle.setDataFormat(wb.createDataFormat().getFormat("[$₹-en-IN]##,##,##0.00"));

        CellStyle greenDataStyle = wb.createCellStyle();
        greenDataStyle.cloneStyleFrom(cellBorder);
        greenDataStyle.setFont(greenFont2);
        greenDataStyle.setDataFormat(wb.createDataFormat().getFormat("[$₹-en-IN]##,##,##0.00"));

        CellStyle redDataStyle = wb.createCellStyle();
        redDataStyle.cloneStyleFrom(cellBorder);
        redDataStyle.setFont(redFont2);
        redDataStyle.setDataFormat(wb.createDataFormat().getFormat("[$₹-en-IN]##,##,##0.00"));

        // Header styles for project breakdown table
        CellStyle headerCommon = wb.createCellStyle();
        headerCommon.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerCommon.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerCommon.setBorderTop(BorderStyle.THIN);
        headerCommon.setBorderBottom(BorderStyle.THIN);
        headerCommon.setBorderLeft(BorderStyle.THIN);
        headerCommon.setBorderRight(BorderStyle.THIN);
        headerCommon.setAlignment(HorizontalAlignment.CENTER);

        CellStyle projHdr = wb.createCellStyle();
        projHdr.cloneStyleFrom(headerCommon);
        projHdr.setFont(bold);

        CellStyle opHdr = wb.createCellStyle();
        opHdr.cloneStyleFrom(headerCommon);
        opHdr.setFont(bold);

        CellStyle clHdr = wb.createCellStyle();
        clHdr.cloneStyleFrom(headerCommon);
        clHdr.setFont(bold);

        XSSFFont greenFont2Bold = xwb.createFont();
        greenFont2Bold.setBold(true);
        greenFont2Bold.setColor(new XSSFColor(new byte[]{(byte)0, (byte)130, (byte)0}, null));

        XSSFFont redFont2Bold = xwb.createFont();
        redFont2Bold.setBold(true);
        redFont2Bold.setColor(new XSSFColor(new byte[]{(byte)192, (byte)0, (byte)0}, null));

        CellStyle greenHdr = wb.createCellStyle();
        greenHdr.cloneStyleFrom(headerCommon);
        greenHdr.setFont(greenFont2Bold);

        CellStyle redHdr = wb.createCellStyle();
        redHdr.cloneStyleFrom(headerCommon);
        redHdr.setFont(redFont2Bold);

        // Write Project Breakdown Table headers
        Row tblHdrRow = sum.createRow(sn++);
        String[] headers = {
            "Project", "Opening Balance", "Closing Balance", "Client Receipts",
            "Expenses - Credit", "Expenses - Cash", "Expenses - Bank/UPI", "Vendor Payments Made"
        };
        CellStyle[] headerStyles = {
            projHdr, opHdr, clHdr, greenHdr, redHdr, redHdr, redHdr, redHdr
        };
        for (int i = 0; i < headers.length; i++) {
            Cell c = tblHdrRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyles[i]);
        }

        // Render project data rows
        for (ProjectSummary ps : projSummaries) {
            Row r = sum.createRow(sn++);
            Cell c0 = r.createCell(0); c0.setCellValue(ps.proj.getName()); c0.setCellStyle(projDataStyle);
            Cell c1 = r.createCell(1); c1.setCellValue(ps.op.doubleValue()); c1.setCellStyle(opDataStyle);
            Cell c2 = r.createCell(2); c2.setCellValue(ps.cl.doubleValue()); c2.setCellStyle(opDataStyle);
            Cell c3 = r.createCell(3); c3.setCellValue(ps.receipts.doubleValue()); c3.setCellStyle(greenDataStyle);
            Cell c4 = r.createCell(4); c4.setCellValue(-ps.credit.doubleValue()); c4.setCellStyle(redDataStyle);
            Cell c5 = r.createCell(5); c5.setCellValue(-ps.cash.doubleValue()); c5.setCellStyle(redDataStyle);
            Cell c6 = r.createCell(6); c6.setCellValue(-ps.bankUpi.doubleValue()); c6.setCellStyle(redDataStyle);
            Cell c7 = r.createCell(7); c7.setCellValue(-ps.vendorPaid.doubleValue()); c7.setCellStyle(redDataStyle);
        }

        // Render project data total row
        Row rTot = sum.createRow(sn++);
        CellStyle totalLabelStyle = wb.createCellStyle();
        totalLabelStyle.cloneStyleFrom(cellBorder);
        totalLabelStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        totalLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalLabelStyle.setFont(bold);

        CellStyle totalOpStyle = wb.createCellStyle();
        totalOpStyle.cloneStyleFrom(totalLabelStyle);
        totalOpStyle.setDataFormat(wb.createDataFormat().getFormat("[$₹-en-IN]##,##,##0.00"));

        CellStyle totalGreenStyle = wb.createCellStyle();
        totalGreenStyle.cloneStyleFrom(totalLabelStyle);
        totalGreenStyle.setFont(greenFont2Bold);
        totalGreenStyle.setDataFormat(wb.createDataFormat().getFormat("[$₹-en-IN]##,##,##0.00"));

        CellStyle totalRedStyle = wb.createCellStyle();
        totalRedStyle.cloneStyleFrom(totalLabelStyle);
        totalRedStyle.setFont(redFont2Bold);
        totalRedStyle.setDataFormat(wb.createDataFormat().getFormat("[$₹-en-IN]##,##,##0.00"));

        Cell t0 = rTot.createCell(0); t0.setCellValue("TOTAL"); t0.setCellStyle(totalLabelStyle);
        Cell t1 = rTot.createCell(1); t1.setCellValue(totalOp.doubleValue()); t1.setCellStyle(totalOpStyle);
        Cell t2 = rTot.createCell(2); t2.setCellValue(totalCl.doubleValue()); t2.setCellStyle(totalOpStyle);
        Cell t3 = rTot.createCell(3); t3.setCellValue(totalReceipts.doubleValue()); t3.setCellStyle(totalGreenStyle);
        Cell t4 = rTot.createCell(4); t4.setCellValue(-totalCredit.doubleValue()); t4.setCellStyle(totalRedStyle);
        Cell t5 = rTot.createCell(5); t5.setCellValue(-totalCash.doubleValue()); t5.setCellStyle(totalRedStyle);
        Cell t6 = rTot.createCell(6); t6.setCellValue(-totalBankUpi.doubleValue()); t6.setCellStyle(totalRedStyle);
        Cell t7 = rTot.createCell(7); t7.setCellValue(-totalVendorPaid.doubleValue()); t7.setCellStyle(totalRedStyle);

        // Auto-fit all 8 summary sheet columns
        for (int i = 0; i < 8; i++) {
            sum.autoSizeColumn(i);
        }

        // ── Sheet 2 onwards: Individual Project Transaction Sheets ────
        Set<String> usedSheetNames = new HashSet<>();
        for (ProjectSummary ps : projSummaries) {
            Project pObj = ps.proj;
            String baseSheetName = pObj.getName().replaceAll("[\\[\\]\\*\\?:/\\\\]", "");
            if (baseSheetName.length() > 31) {
                baseSheetName = baseSheetName.substring(0, 31);
            }
            String finalSheetName = baseSheetName;
            int counter = 1;
            while (usedSheetNames.contains(finalSheetName.toLowerCase())) {
                String suffix = " (" + counter + ")";
                int maxLen = 31 - suffix.length();
                if (baseSheetName.length() > maxLen) {
                    finalSheetName = baseSheetName.substring(0, maxLen) + suffix;
                } else {
                    finalSheetName = baseSheetName + suffix;
                }
                counter++;
            }
            usedSheetNames.add(finalSheetName.toLowerCase());
            Sheet projTxSheet = wb.createSheet(finalSheetName);

            // Write Headers
            hdr(projTxSheet, hStyle, "Date", "Type", "Project", "Details", "Payment Mode", "Amount");

            // Write Opening Balance row
            Row opRow = projTxSheet.createRow(1);
            Cell opCell0 = opRow.createCell(0); opCell0.setCellValue(from.format(DATE_FMT)); opCell0.setCellStyle(boldStyle);
            Cell opCell1 = opRow.createCell(1); opCell1.setCellValue("Opening Balance"); opCell1.setCellStyle(boldStyle);
            Cell opCell2 = opRow.createCell(2); opCell2.setCellValue(pObj.getName()); opCell2.setCellStyle(boldStyle);
            Cell opCell3 = opRow.createCell(3); opCell3.setCellValue("Opening Balance"); opCell3.setCellStyle(boldStyle);
            Cell opCell4 = opRow.createCell(4); opCell4.setCellValue(""); opCell4.setCellStyle(boldStyle);
            Cell opCell5 = opRow.createCell(5); opCell5.setCellValue(ps.op.doubleValue()); opCell5.setCellStyle(boldAmt);

            // Filter and sort transactions for this project
            List<Object[]> projTxRows = new ArrayList<>();
            for (ClientPayment cp : clientPays) {
                if (cp.getProject() != null && cp.getProject().getProjectId().equals(pObj.getProjectId())) {
                    projTxRows.add(new Object[]{ cp.getPaymentDate(), "Client Payment",
                        pObj.getName(),
                        cp.getReferenceNumber() != null ? "Ref: " + cp.getReferenceNumber() : "",
                        cp.getPaymentMode(), cp.getAmount() });
                }
            }
            for (Payment p : vendorPays) {
                if (p.getProject() != null && p.getProject().getProjectId().equals(pObj.getProjectId())) {
                    projTxRows.add(new Object[]{ p.getPaymentDate(), "Vendor Payment",
                        pObj.getName(),
                        p.getVendor() != null ? p.getVendor().getName() : "",
                        p.getPaymentMode(), p.getAmount() });
                }
            }
            for (Expense e : expenses) {
                if (e.getProject() != null && e.getProject().getProjectId().equals(pObj.getProjectId())) {
                    projTxRows.add(new Object[]{ e.getExpenseDate(), e.getExpenseType(),
                        pObj.getName(),
                        e.getCategory() != null ? e.getCategory() : "",
                        e.getPaymentMode(), e.getAmount() });
                }
            }

            projTxRows.sort(Comparator.comparing(r -> (LocalDate) r[0]));

            int tn = 2; // Rows start at index 2 since index 1 is Opening Balance
            for (Object[] tx : projTxRows) {
                String type = (String) tx[1];
                boolean isIncome = "Client Payment".equals(type);
                CellStyle textStyle = isIncome ? greenText : redText;
                CellStyle amtStyle2 = isIncome ? greenAmt  : redAmt;

                Row r = projTxSheet.createRow(tn++);
                for (int col = 0; col < 5; col++) {
                    Cell c = r.createCell(col);
                    c.setCellValue(col == 0
                            ? ((LocalDate) tx[0]).format(DATE_FMT)
                            : (String) tx[col]);
                    c.setCellStyle(textStyle);
                }
                BigDecimal amt = (BigDecimal) tx[5];
                double displayAmt = amt != null ? (isIncome ? amt.doubleValue() : -amt.doubleValue()) : 0.0;
                Cell amtCell = r.createCell(5);
                amtCell.setCellValue(displayAmt);
                amtCell.setCellStyle(amtStyle2);
            }

            // Write Closing Balance row at the end (first line after all transactions)
            Row clRow = projTxSheet.createRow(tn++);
            Cell clCell0 = clRow.createCell(0); clCell0.setCellValue(to.format(DATE_FMT)); clCell0.setCellStyle(boldStyle);
            Cell clCell1 = clRow.createCell(1); clCell1.setCellValue("Closing Balance"); clCell1.setCellStyle(boldStyle);
            Cell clCell2 = clRow.createCell(2); clCell2.setCellValue(pObj.getName()); clCell2.setCellStyle(boldStyle);
            Cell clCell3 = clRow.createCell(3); clCell3.setCellValue("Closing Balance"); clCell3.setCellStyle(boldStyle);
            Cell clCell4 = clRow.createCell(4); clCell4.setCellValue(""); clCell4.setCellStyle(boldStyle);
            Cell clCell5 = clRow.createCell(5); clCell5.setCellValue(ps.cl.doubleValue()); clCell5.setCellStyle(boldAmt);

            // Auto-size columns for this project sheet
            for (int i = 0; i < 6; i++) {
                projTxSheet.autoSizeColumn(i);
            }
        }

        // Use project name in filename
        String safeName = projectsLabel.replaceAll("[^a-zA-Z0-9 ,]", "").trim()
                .replace(", ", "_").replace(" ", "_");
        if (safeName.length() > 40) safeName = safeName.substring(0, 40);
        return respond(wb, safeName + "_" + from_date + "_to_" + to_date + ".xlsx");
    }
}
