package com.construction.controller.api;

import com.construction.domain.Company;
import com.construction.domain.User;
import com.construction.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard/api")
@RequiredArgsConstructor
public class DashboardApiController {

    private final FinanceService financeService;

    private User currentUser(Authentication auth) {
        return (User) auth.getPrincipal();
    }

    // ─── OWNER KPIs ───────────────────────────────────────────────────────
    @GetMapping("/owner-kpis")
    public ResponseEntity<?> getOwnerKpis(
            Authentication authentication,
            @RequestParam(required = false) String status) {

        User user = currentUser(authentication);
        if (!"ADMIN".equals(user.getRole()) && !"MANAGER".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Permission denied"));
        }
        Company company = user.getCompany();
        Map<String, Object> kpis = financeService.getOwnerKpis(company, status);
        return ResponseEntity.ok(kpis);
    }

    // ─── DAILY CASH BALANCE ───────────────────────────────────────────────
    @GetMapping("/daily-cash-balance")
    public ResponseEntity<?> getDailyCashBalance(
            Authentication authentication,
            @RequestParam(required = false) String project_ids,
            @RequestParam(required = false) String project_id,
            @RequestParam String from_date,
            @RequestParam String to_date) {

        User user = currentUser(authentication);
        if (!"ADMIN".equals(user.getRole()) && !"MANAGER".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Permission denied"));
        }

        try {
            LocalDate fromDate = LocalDate.parse(from_date);
            LocalDate toDate = LocalDate.parse(to_date);

            // Support both comma-separated project_ids and single project_id
            String rawIds = project_ids != null ? project_ids : project_id;
            List<Long> projectIdList = null;
            if (rawIds != null && !rawIds.equalsIgnoreCase("all") && !rawIds.isBlank()) {
                projectIdList = Arrays.stream(rawIds.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            }

            Map<String, Object> result = financeService.getDailyCashBalance(
                    user.getCompany(), projectIdList, fromDate, toDate);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── PROJECT PAYMENT DETAILS ──────────────────────────────────────────
    @GetMapping("/project-payment-details/{projectId}")
    public ResponseEntity<?> getProjectPaymentDetails(
            Authentication authentication,
            @PathVariable Long projectId) {

        User user = currentUser(authentication);
        if (!"ADMIN".equals(user.getRole()) && !"MANAGER".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Permission denied"));
        }

        try {
            Map<String, Object> result = financeService.getProjectPaymentDetails(user.getCompany(), projectId);
            return ResponseEntity.ok(result);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── VENDOR SUMMARY ───────────────────────────────────────────────────
    @GetMapping("/vendor-summary")
    public ResponseEntity<?> getVendorSummary(Authentication authentication) {
        User user = currentUser(authentication);
        if (!"ADMIN".equals(user.getRole()) && !"MANAGER".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Permission denied"));
        }
        return ResponseEntity.ok(financeService.getVendorSummary(user.getCompany()));
    }

    // ─── VENDOR MATERIAL SUMMARY ──────────────────────────────────────────
    @GetMapping("/vendor-material-summary/{vendorId}")
    public ResponseEntity<?> getVendorMaterialSummary(
            Authentication authentication,
            @PathVariable Long vendorId,
            @RequestParam(required = false) String project) {

        User user = currentUser(authentication);
        if (!"ADMIN".equals(user.getRole()) && !"MANAGER".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Permission denied"));
        }

        try {
            return ResponseEntity.ok(financeService.getVendorMaterialSummary(user.getCompany(), vendorId, project));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── VENDOR PURCHASE HISTORY ──────────────────────────────────────────
    @GetMapping("/vendor-purchase-history/{vendorId}")
    public ResponseEntity<?> getVendorPurchaseHistory(
            Authentication authentication,
            @PathVariable Long vendorId) {

        User user = currentUser(authentication);
        if (!"ADMIN".equals(user.getRole()) && !"MANAGER".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Permission denied"));
        }

        try {
            return ResponseEntity.ok(financeService.getVendorPurchaseHistory(user.getCompany(), vendorId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
