package com.construction.controller;

import com.construction.domain.AuditLog;
import com.construction.domain.User;
import com.construction.repository.AuditLogRepository;
import com.construction.repository.UserRepository;
import com.construction.service.AuditService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))) {
                return "redirect:/purchases";
            } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MANAGER"))) {
                return "redirect:/dashboard/daily-cash";
            } else {
                return "redirect:/dashboard/owner";
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access_denied";
    }

    // Core Pages
    @GetMapping({"/dashboard/owner", "/dashboard/main"})
    public String ownerDashboard() {
        return "dashboard/owner_dashboard"; 
    }

    @GetMapping({"/dashboard/daily-cash", "/daily_cash", "/daily-cash"})
    public String dailyCash() {
        return "dashboard/daily_cash";
    }

    @GetMapping({"/dashboard/vendor-analytics", "/vendor_analytics", "/vendor-analytics"})
    public String vendorAnalytics() {
        return "dashboard/vendor_analytics";
    }

    @GetMapping({"/master-categories", "/master_categories"})
    public String masterCategories() {
        return "master_categories";
    }

    @GetMapping("/projects")
    public String projects() {
        return "projects";
    }

    @GetMapping("/vendors")
    public String vendors() {
        return "vendors";
    }

    // Finance Pages
    @GetMapping("/expenses")
    public String expenses() {
        return "expenses";
    }

    @GetMapping("/purchases")
    public String purchases() {
        return "purchases";
    }

    @GetMapping("/payments")
    public String payments() {
        return "payments";
    }

    @GetMapping({"/client-payments", "/client_payments"})
    public String clientPayments() {
        return "client_payments";
    }

    // User Pages
    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String createUser(
            Authentication auth,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirm_password,
            @RequestParam String role,
            Model model,
            RedirectAttributes redirectAttrs) {

        User admin = (User) auth.getPrincipal();

        if (!password.equals(confirm_password)) {
            model.addAttribute("error", "Passwords do not match.");
            return "register";
        }
        if (userRepository.findByEmail(email).isPresent()) {
            model.addAttribute("error", "Email '" + email + "' is already registered.");
            return "register";
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setCompany(admin.getCompany());
        user.setActive(true);
        userRepository.save(user);
        auditService.log(admin, "CREATE", "USER", name, "Created user: " + name + " (" + role + ")");

        redirectAttrs.addFlashAttribute("success", "User '" + name + "' created successfully!");
        return "redirect:/users";
    }

    @GetMapping({"/users", "/manage_users", "/manage-users"})
    public String manageUsers(Authentication auth, Model model) {
        User currentUser = (User) auth.getPrincipal();
        model.addAttribute("users", userRepository.findByCompanyOrderByRoleAscNameAsc(currentUser.getCompany()));
        model.addAttribute("currentUserId", currentUser.getUserId());
        model.addAttribute("companyName", currentUser.getCompany().getName());
        return "manage_users";
    }

    @GetMapping("/activity-log")
    public String activityLog(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            Model model) {

        User admin = (User) auth.getPrincipal();
        var company = admin.getCompany();
        ZoneId ist = ZoneId.of("Asia/Kolkata");

        String actionF = (action  != null && !action.isBlank())  ? action  : null;
        String moduleF = (module  != null && !module.isBlank())  ? module  : null;
        OffsetDateTime from = (dateFrom != null && !dateFrom.isBlank())
                ? LocalDate.parse(dateFrom).atStartOfDay(ist).toOffsetDateTime() : null;
        OffsetDateTime to = (dateTo != null && !dateTo.isBlank())
                ? LocalDate.parse(dateTo).atTime(23, 59, 59).atZone(ist).toOffsetDateTime() : null;

        // Build Specification dynamically — null filters are simply omitted
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("company"), company));
            if (actionF != null) p.add(cb.equal(root.get("action"),  actionF));
            if (moduleF != null) p.add(cb.equal(root.get("module"),  moduleF));
            if (userId  != null) p.add(cb.equal(root.get("performedBy").get("userId"), userId));
            if (from    != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to      != null) p.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            if (query != null) query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(p.toArray(new Predicate[0]));
        };

        var pageable = PageRequest.of(page, 50);
        var logs = auditLogRepository.findAll(spec, pageable);

        model.addAttribute("logs",        logs);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages",  logs.getTotalPages());
        model.addAttribute("fAction",   action   != null ? action   : "");
        model.addAttribute("fModule",   module   != null ? module   : "");
        model.addAttribute("fUserId",   userId);
        model.addAttribute("fDateFrom", dateFrom != null ? dateFrom : "");
        model.addAttribute("fDateTo",   dateTo   != null ? dateTo   : "");
        model.addAttribute("companyUsers", userRepository.findByCompanyOrderByRoleAscNameAsc(company));
        return "activity_log";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@org.springframework.web.bind.annotation.PathVariable Long id,
                               Authentication auth, Model model) {
        User currentAdmin = (User) auth.getPrincipal();
        User target = userRepository.findById(id)
                .filter(u -> u.getCompany().getCompanyId().equals(currentAdmin.getCompany().getCompanyId()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        model.addAttribute("editUser", target);
        return "edit_user";
    }

    @PostMapping("/users/{id}/edit")
    public String editUserSave(@org.springframework.web.bind.annotation.PathVariable Long id,
                               Authentication auth,
                               @RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String role,
                               @RequestParam(value = "is_active", required = false) String isActiveStr,
                               @RequestParam(value = "new_password", required = false) String newPassword,
                               @RequestParam(value = "confirm_password", required = false) String confirmPassword,
                               Model model,
                               RedirectAttributes redirectAttrs) {

        User currentAdmin = (User) auth.getPrincipal();
        User target = userRepository.findById(id)
                .filter(u -> u.getCompany().getCompanyId().equals(currentAdmin.getCompany().getCompanyId()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));

        // Validate password if provided
        if (newPassword != null && !newPassword.isBlank()) {
            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("error", "Passwords do not match.");
                model.addAttribute("editUser", target);
                return "edit_user";
            }
            if (newPassword.length() < 8) {
                model.addAttribute("error", "Password must be at least 8 characters.");
                model.addAttribute("editUser", target);
                return "edit_user";
            }
            target.setPassword(passwordEncoder.encode(newPassword));
        }

        // Check email uniqueness if changed
        if (!target.getEmail().equalsIgnoreCase(email)) {
            if (userRepository.findByEmail(email).isPresent()) {
                model.addAttribute("error", "Email '" + email + "' is already in use.");
                model.addAttribute("editUser", target);
                return "edit_user";
            }
        }

        target.setName(name);
        target.setEmail(email);
        target.setRole(role);
        target.setActive("true".equals(isActiveStr));
        userRepository.save(target);
        auditService.log(currentAdmin, "UPDATE", "USER", name,
                "Updated user: " + name + " → role=" + role + ", active=" + target.isActive());

        redirectAttrs.addFlashAttribute("success", "User '" + name + "' updated successfully!");
        return "redirect:/users";
    }
}
