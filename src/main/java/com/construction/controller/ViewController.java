package com.construction.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ViewController {

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

    @GetMapping({"/users", "/manage_users", "/manage-users"})
    public String manageUsers() {
        return "manage_users";
    }
}
