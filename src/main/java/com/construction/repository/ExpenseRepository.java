package com.construction.repository;

import com.construction.domain.Expense;
import com.construction.domain.Project;
import com.construction.domain.Vendor;
import com.construction.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByCompany(Company company);
    List<Expense> findByCompanyAndProject(Company company, Project project);
    List<Expense> findByCompanyAndVendor(Company company, Vendor vendor);
    List<Expense> findByCompanyAndVendorAndExpenseType(Company company, Vendor vendor, String expenseType);
    List<Expense> findByCompanyAndExpenseDateBefore(Company company, LocalDate date);

    List<Expense> findByCompanyAndProjectIn(Company company, List<Project> projects);
    List<Expense> findByCompanyAndProjectInAndExpenseDateBefore(Company company, List<Project> projects, LocalDate date);
    List<Expense> findByCompanyAndProjectInAndExpenseDateBetween(Company company, List<Project> projects, LocalDate from, LocalDate to);

    List<Expense> findByCompanyAndExpenseDateBetween(Company company, LocalDate from, LocalDate to);

    @Query("SELECT e FROM Expense e WHERE e.company = :company AND e.project.projectId IN :projectIds AND e.expenseDate < :date")
    List<Expense> findByCompanyAndProjectIdsBeforeDate(@Param("company") Company company,
                                                        @Param("projectIds") List<Long> projectIds,
                                                        @Param("date") LocalDate date);

    @Query("SELECT e FROM Expense e WHERE e.company = :company AND e.project.projectId IN :projectIds AND e.expenseDate BETWEEN :from AND :to")
    List<Expense> findByCompanyAndProjectIdsBetween(@Param("company") Company company,
                                                     @Param("projectIds") List<Long> projectIds,
                                                     @Param("from") LocalDate from,
                                                     @Param("to") LocalDate to);
}
