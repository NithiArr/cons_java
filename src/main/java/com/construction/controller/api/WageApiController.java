package com.construction.controller.api;

import com.construction.domain.*;
import com.construction.repository.ProjectRepository;
import com.construction.repository.WageRowRepository;
import com.construction.repository.WageSheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class WageApiController {

    private final WageSheetRepository wageSheetRepository;
    private final WageRowRepository wageRowRepository;
    private final ProjectRepository projectRepository;

    private User currentUser(Authentication auth) {
        return (User) auth.getPrincipal();
    }

    // ─── LIST all wage sheets (optionally filtered by project) ────────────
    @GetMapping("/api/wages")
    public ResponseEntity<?> listSheets(
            @RequestParam(required = false) Long projectId,
            Authentication auth) {

        Company company = currentUser(auth).getCompany();
        List<Project> projects = projectRepository.findByCompanyOrderByCreatedAtDesc(company);

        List<WageSheet> sheets;
        if (projectId != null) {
            sheets = projects.stream()
                    .filter(p -> p.getProjectId().equals(projectId))
                    .findFirst()
                    .map(wageSheetRepository::findByProjectOrderByWeekStartDesc)
                    .orElse(Collections.emptyList());
        } else {
            sheets = wageSheetRepository.findByProjectInOrderByWeekStartDesc(projects);
        }

        List<Map<String, Object>> result = sheets.stream().map(this::toSheetSummary)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // ─── GET single wage sheet with full rows ─────────────────────────────
    @GetMapping("/api/wages/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSheet(@PathVariable Long id, Authentication auth) {
        Company company = currentUser(auth).getCompany();
        return wageSheetRepository.findById(id)
                .filter(s -> s.getProject().getCompany().getCompanyId().equals(company.getCompanyId()))
                .map(s -> ResponseEntity.ok(toSheetDetail(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── CREATE wage sheet ────────────────────────────────────────────────
    @PostMapping("/api/wages")
    @Transactional
    public ResponseEntity<?> createSheet(@RequestBody Map<String, Object> body, Authentication auth) {
        Company company = currentUser(auth).getCompany();

        Long projectId = Long.parseLong(body.get("project_id").toString());
        Optional<Project> projectOpt = projectRepository.findById(projectId)
                .filter(p -> p.getCompany().getCompanyId().equals(company.getCompanyId()));
        if (projectOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Invalid project"));

        LocalDate weekStart = LocalDate.parse(body.get("week_start").toString());
        LocalDate weekEnd = weekStart.plusDays(6);

        WageSheet sheet = new WageSheet();
        sheet.setProject(projectOpt.get());
        sheet.setWeekStart(weekStart);
        sheet.setWeekEnd(weekEnd);
        BigDecimal masonAdvance = body.get("mason_advance") != null ? new BigDecimal(body.get("mason_advance").toString()) : BigDecimal.ZERO;
        BigDecimal fitterAdvance = body.get("fitter_advance") != null ? new BigDecimal(body.get("fitter_advance").toString()) : BigDecimal.ZERO;
        sheet.setMasonAdvance(masonAdvance);
        sheet.setFitterAdvance(fitterAdvance);
        sheet.setAdvance(masonAdvance.add(fitterAdvance));

        applyRows(sheet, body);
        wageSheetRepository.save(sheet);

        return ResponseEntity.ok(Map.of("success", true, "id", sheet.getId()));
    }

    // ─── UPDATE wage sheet ────────────────────────────────────────────────
    @PutMapping("/api/wages/{id}")
    @Transactional
    public ResponseEntity<?> updateSheet(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body,
                                         Authentication auth) {
        Company company = currentUser(auth).getCompany();
        return wageSheetRepository.findById(id)
                .filter(s -> s.getProject().getCompany().getCompanyId().equals(company.getCompanyId()))
                .map(sheet -> {
                    // Update project if changed
                    if (body.get("project_id") != null) {
                        Long pid = Long.parseLong(body.get("project_id").toString());
                        projectRepository.findById(pid)
                                .filter(p -> p.getCompany().getCompanyId().equals(company.getCompanyId()))
                                .ifPresent(sheet::setProject);
                    }
                    if (body.get("week_start") != null) {
                        LocalDate ws = LocalDate.parse(body.get("week_start").toString());
                        sheet.setWeekStart(ws);
                        sheet.setWeekEnd(ws.plusDays(6));
                    }
                    BigDecimal mAdv = sheet.getMasonAdvance() != null ? sheet.getMasonAdvance() : BigDecimal.ZERO;
                    BigDecimal fAdv = sheet.getFitterAdvance() != null ? sheet.getFitterAdvance() : BigDecimal.ZERO;
                    if (body.get("mason_advance") != null) {
                        mAdv = new BigDecimal(body.get("mason_advance").toString());
                        sheet.setMasonAdvance(mAdv);
                    }
                    if (body.get("fitter_advance") != null) {
                        fAdv = new BigDecimal(body.get("fitter_advance").toString());
                        sheet.setFitterAdvance(fAdv);
                    }
                    sheet.setAdvance(mAdv.add(fAdv));
                    // Replace all rows
                    sheet.getRows().clear();
                    applyRows(sheet, body);
                    wageSheetRepository.save(sheet);
                    return ResponseEntity.ok(Map.of("success", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── DELETE wage sheet ────────────────────────────────────────────────
    @DeleteMapping("/api/wages/{id}")
    @Transactional
    public ResponseEntity<?> deleteSheet(@PathVariable Long id, Authentication auth) {
        Company company = currentUser(auth).getCompany();
        return wageSheetRepository.findById(id)
                .filter(s -> s.getProject().getCompany().getCompanyId().equals(company.getCompanyId()))
                .map(s -> {
                    wageSheetRepository.delete(s);
                    return ResponseEntity.ok(Map.of("success", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void applyRows(WageSheet sheet, Map<String, Object> body) {
        List<Map<String, Object>> rowsData = (List<Map<String, Object>>) body.get("rows");
        if (rowsData == null) return;
        for (int i = 0; i < rowsData.size(); i++) {
            Map<String, Object> rd = rowsData.get(i);
            WageRow row = new WageRow();
            row.setWageSheet(sheet);
            row.setRowOrder(i);
            row.setCategory(strOrEmpty(rd.get("category")));
            row.setEmployeeName(strOrEmpty(rd.get("employee_name")));
            row.setRowType(strOrEmpty(rd.get("row_type")));
            row.setDescription(strOrEmpty(rd.get("description")));
            row.setIsHeadLabour(bool(rd.get("is_head_labour")));
            row.setDay1(dec(rd.get("day1")));
            row.setDay2(dec(rd.get("day2")));
            row.setDay3(dec(rd.get("day3")));
            row.setDay4(dec(rd.get("day4")));
            row.setDay5(dec(rd.get("day5")));
            row.setDay6(dec(rd.get("day6")));
            row.setDay7(dec(rd.get("day7")));
            row.setWagePerDay(dec(rd.get("wage_per_day")));
            sheet.getRows().add(row);
        }
    }

    private Map<String, Object> toSheetSummary(WageSheet s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("project_id", s.getProject().getProjectId());
        m.put("project_name", s.getProject().getName());
        m.put("week_start", s.getWeekStart().toString());
        m.put("week_end", s.getWeekEnd().toString());
        m.put("advance", s.getAdvance());
        m.put("mason_advance", s.getMasonAdvance());
        m.put("fitter_advance", s.getFitterAdvance());
        // grand total
        BigDecimal total = s.getRows().stream()
                .map(WageRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal masonTotal = s.getRows().stream()
                .filter(r -> "MASON".equals(r.getRowType()) || r.getRowType() == null || r.getRowType().isEmpty())
                .map(WageRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fitterTotal = s.getRows().stream()
                .filter(r -> "FITTER".equals(r.getRowType()))
                .map(WageRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal additionalTotal = s.getRows().stream()
                .filter(r -> "ADDITIONAL".equals(r.getRowType()))
                .map(WageRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        m.put("grand_total", total);
        m.put("net_total", total.subtract(s.getAdvance() == null ? BigDecimal.ZERO : s.getAdvance()));
        m.put("mason_total", masonTotal);
        m.put("fitter_total", fitterTotal);
        m.put("additional_total", additionalTotal);
        m.put("row_count", s.getRows().size());
        return m;
    }

    private Map<String, Object> toSheetDetail(WageSheet s) {
        Map<String, Object> m = toSheetSummary(s);
        // rows
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < s.getRows().size(); i++) {
            WageRow r = s.getRows().get(i);
            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("id", r.getId());
            rm.put("sl_no", i + 1);
            rm.put("category", r.getCategory());
            rm.put("employee_name", r.getEmployeeName());
            rm.put("row_type", r.getRowType());
            rm.put("description", r.getDescription());
            rm.put("is_head_labour", r.getIsHeadLabour() != null && r.getIsHeadLabour());
            rm.put("day1", r.getDay1());
            rm.put("day2", r.getDay2());
            rm.put("day3", r.getDay3());
            rm.put("day4", r.getDay4());
            rm.put("day5", r.getDay5());
            rm.put("day6", r.getDay6());
            rm.put("day7", r.getDay7());
            rm.put("wage_per_day", r.getWagePerDay());
            rm.put("no_of_days", r.getNoOfDays());
            rm.put("amount", r.getAmount());
            rows.add(rm);
        }
        m.put("rows", rows);
        // category summary
        m.put("category_summary", buildCategorySummary(s.getRows()));
        return m;
    }

    private List<Map<String, Object>> buildCategorySummary(List<WageRow> rows) {
        // group by category and rowType
        Map<String, List<WageRow>> grouped = new LinkedHashMap<>();
        for (WageRow r : rows) {
            String key = (r.getCategory() == null ? "" : r.getCategory().toUpperCase())
                    + ":" + (r.getRowType() == null ? "MASON" : r.getRowType().toUpperCase());
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        List<Map<String, Object>> summary = new ArrayList<>();
        for (Map.Entry<String, List<WageRow>> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split(":");
            String cat = parts[0];
            String type = parts[1];
            List<WageRow> catRows = entry.getValue();

            BigDecimal totalDays = BigDecimal.ZERO;
            BigDecimal wageRate = BigDecimal.ZERO;
            BigDecimal subtotal = BigDecimal.ZERO;

            if ("ADDITIONAL".equals(type)) {
                subtotal = catRows.stream()
                        .map(r -> r.getWagePerDay() == null ? BigDecimal.ZERO : r.getWagePerDay())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else {
                totalDays = catRows.stream().map(WageRow::getNoOfDays).reduce(BigDecimal.ZERO, BigDecimal::add);
                wageRate = catRows.stream()
                        .map(r -> r.getWagePerDay() == null ? BigDecimal.ZERO : r.getWagePerDay())
                        .filter(w -> w.compareTo(BigDecimal.ZERO) > 0)
                        .findFirst().orElse(BigDecimal.ZERO);
                subtotal = totalDays.multiply(wageRate);
            }

            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("category", cat);
            sm.put("row_type", type);
            sm.put("total_days", totalDays);
            sm.put("wage_rate", wageRate);
            sm.put("subtotal", subtotal);
            summary.add(sm);
        }
        return summary;
    }

    private BigDecimal dec(Object val) {
        if (val == null) return BigDecimal.ZERO;
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private String strOrEmpty(Object val) {
        return val == null ? "" : val.toString();
    }

    private Boolean bool(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(val.toString());
    }
}
