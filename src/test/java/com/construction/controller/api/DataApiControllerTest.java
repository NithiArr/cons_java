package com.construction.controller.api;

import com.construction.domain.*;
import com.construction.repository.*;
import com.construction.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataApiControllerTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private ExpenseItemRepository expenseItemRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ClientPaymentRepository clientPaymentRepository;
    @Mock
    private MasterCategoryRepository masterCategoryRepository;
    @Mock
    private SubCategoryRepository subCategoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private WageSheetRepository wageSheetRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DataApiController dataApiController;

    private Company testCompany;
    private User testUser;
    private Project testProject1;
    private Project testProject2;
    private Vendor testVendor1;
    private Vendor testVendor2;

    @BeforeEach
    public void setUp() {
        testCompany = new Company();
        testCompany.setCompanyId(1L);
        testCompany.setName("Test Construction Co");

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setCompany(testCompany);
        testUser.setEmail("test@construction.com");
        testUser.setRole("ADMIN");

        testProject1 = new Project();
        testProject1.setProjectId(101L);
        testProject1.setName("Project Alpha");
        testProject1.setCompany(testCompany);

        testProject2 = new Project();
        testProject2.setProjectId(102L);
        testProject2.setName("Project Beta");
        testProject2.setCompany(testCompany);

        testVendor1 = new Vendor();
        testVendor1.setVendorId(201L);
        testVendor1.setName("Steel Vendor Ltd");
        testVendor1.setCompany(testCompany);

        testVendor2 = new Vendor();
        testVendor2.setVendorId(202L);
        testVendor2.setName("Cement Supplier Corp");
        testVendor2.setCompany(testCompany);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetWeeklySummaryData_MaterialsSummaryCalculation() {
        // Setup authentication
        when(authentication.getPrincipal()).thenReturn(testUser);

        // Date setup: we query for week of 2026-06-08 (Mon) to 2026-06-14 (Sun)
        String weekStartStr = "2026-06-08";
        LocalDate start = LocalDate.parse(weekStartStr);
        LocalDate end = start.plusDays(6); // 2026-06-14

        // Setup repository mock responses
        List<Project> projects = Arrays.asList(testProject1, testProject2);
        when(projectRepository.findByCompanyOrderByCreatedAtDesc(testCompany)).thenReturn(projects);

        // Purchases within week (not strictly used for materials_summary calculation but for weekly purchases list)
        Expense weeklyPurchase = new Expense();
        weeklyPurchase.setExpenseId(1L);
        weeklyPurchase.setExpenseType("Material Purchase");
        weeklyPurchase.setExpenseDate(LocalDate.of(2026, 6, 10));
        weeklyPurchase.setAmount(new BigDecimal("1500.00"));
        weeklyPurchase.setVendor(testVendor1);
        weeklyPurchase.setProject(testProject1);
        weeklyPurchase.setCompany(testCompany);

        List<Expense> weeklyPurchases = Collections.singletonList(weeklyPurchase);
        when(expenseRepository.findByCompanyAndExpenseDateBetween(testCompany, start, end)).thenReturn(weeklyPurchases);

        // All purchases up to end of week
        // 1. Purchase of 2000.00 from Vendor 1 on Project Alpha (past)
        Expense pastPurchase = new Expense();
        pastPurchase.setExpenseId(2L);
        pastPurchase.setExpenseType("Material Purchase");
        pastPurchase.setExpenseDate(LocalDate.of(2026, 5, 20));
        pastPurchase.setAmount(new BigDecimal("2000.00"));
        pastPurchase.setVendor(testVendor1);
        pastPurchase.setProject(testProject1);
        pastPurchase.setCompany(testCompany);

        // 2. Purchase of 1500.00 from Vendor 1 on Project Alpha (during selected week, same as above)
        // 3. Purchase of 3000.00 from Vendor 2 on Project Beta (past)
        Expense pastPurchase2 = new Expense();
        pastPurchase2.setExpenseId(3L);
        pastPurchase2.setExpenseType("Material Purchase");
        pastPurchase2.setExpenseDate(LocalDate.of(2026, 5, 25));
        pastPurchase2.setAmount(new BigDecimal("3000.00"));
        pastPurchase2.setVendor(testVendor2);
        pastPurchase2.setProject(testProject2);
        pastPurchase2.setCompany(testCompany);

        List<Expense> allPurchasesUpTo = Arrays.asList(pastPurchase, weeklyPurchase, pastPurchase2);
        when(expenseRepository.findByCompanyAndExpenseDateBetween(eq(testCompany), eq(LocalDate.of(2000, 1, 1)), eq(end)))
                .thenReturn(allPurchasesUpTo);

        // Payments within week
        Payment weeklyPayment = new Payment();
        weeklyPayment.setPaymentId(1L);
        weeklyPayment.setPaymentDate(LocalDate.of(2026, 6, 12));
        weeklyPayment.setAmount(new BigDecimal("800.00"));
        weeklyPayment.setVendor(testVendor1);
        weeklyPayment.setProject(testProject1);
        weeklyPayment.setCompany(testCompany);

        List<Payment> weeklyPayments = Collections.singletonList(weeklyPayment);
        when(paymentRepository.findByCompanyAndPaymentDateBetween(testCompany, start, end)).thenReturn(weeklyPayments);

        // All payments up to end of week
        // 1. Payment of 500.00 to Vendor 1 for Project Alpha (past)
        Payment pastPayment = new Payment();
        pastPayment.setPaymentId(2L);
        pastPayment.setPaymentDate(LocalDate.of(2026, 5, 22));
        pastPayment.setAmount(new BigDecimal("500.00"));
        pastPayment.setVendor(testVendor1);
        pastPayment.setProject(testProject1);
        pastPayment.setCompany(testCompany);

        // 2. Payment of 800.00 to Vendor 1 for Project Alpha (weekly, same as above)
        // 3. Payment of 1000.00 to Vendor 2 for Project Beta (past)
        Payment pastPayment2 = new Payment();
        pastPayment2.setPaymentId(3L);
        pastPayment2.setPaymentDate(LocalDate.of(2026, 5, 26));
        pastPayment2.setAmount(new BigDecimal("1000.00"));
        pastPayment2.setVendor(testVendor2);
        pastPayment2.setProject(testProject2);
        pastPayment2.setCompany(testCompany);

        List<Payment> allPaymentsUpTo = Arrays.asList(pastPayment, weeklyPayment, pastPayment2);
        when(paymentRepository.findByCompanyAndPaymentDateBetween(eq(testCompany), eq(LocalDate.of(2000, 1, 1)), eq(end)))
                .thenReturn(allPaymentsUpTo);

        // Empty list responses for others
        when(clientPaymentRepository.findByCompanyAndPaymentDateBetween(any(), any(), any())).thenReturn(Collections.emptyList());
        when(wageSheetRepository.findByProjectInAndWeekStart(anyList(), any())).thenReturn(Collections.emptyList());

        // Invoke controller method
        ResponseEntity<?> response = dataApiController.getWeeklySummaryData(weekStartStr, null, authentication);

        // Verify response
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());

        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("materials_summary"));

        List<Map<String, Object>> summary = (List<Map<String, Object>>) body.get("materials_summary");
        assertNotNull(summary);
        assertEquals(2, summary.size());

        // Check calculations for Vendor 1, Project Alpha
        // Cumulative Purchases = past (2000) + weekly (1500) = 3500
        // Cumulative Payments before week = past (500)
        // Payments this week = weekly (800)
        Map<String, Object> item1 = summary.stream()
                .filter(item -> "Steel Vendor Ltd".equals(item.get("vendor_name")) && "Project Alpha".equals(item.get("project_name")))
                .findFirst()
                .orElse(null);
        assertNotNull(item1);
        assertEquals(new BigDecimal("3500.00"), item1.get("total_purchases"));
        assertEquals(new BigDecimal("500.00"), item1.get("total_payments_before"));
        assertEquals(new BigDecimal("800.00"), item1.get("payments_this_week"));

        // Check calculations for Vendor 2, Project Beta
        // Cumulative Purchases = past (3000) = 3000
        // Cumulative Payments before week = past (1000)
        // Payments this week = 0
        Map<String, Object> item2 = summary.stream()
                .filter(item -> "Cement Supplier Corp".equals(item.get("vendor_name")) && "Project Beta".equals(item.get("project_name")))
                .findFirst()
                .orElse(null);
        assertNotNull(item2);
        assertEquals(new BigDecimal("3000.00"), item2.get("total_purchases"));
        assertEquals(new BigDecimal("1000.00"), item2.get("total_payments_before"));
        assertEquals(BigDecimal.ZERO, item2.get("payments_this_week"));
    }
}
