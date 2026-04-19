package com.construction.repository;

import com.construction.domain.Payment;
import com.construction.domain.Company;
import com.construction.domain.Project;
import com.construction.domain.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByCompany(Company company);
    List<Payment> findByCompanyAndProject(Company company, Project project);
    List<Payment> findByCompanyAndVendor(Company company, Vendor vendor);
    List<Payment> findByCompanyAndPaymentDateBefore(Company company, LocalDate date);

    List<Payment> findByCompanyAndProjectIn(Company company, List<Project> projects);
    List<Payment> findByCompanyAndProjectInAndPaymentDateBefore(Company company, List<Project> projects, LocalDate date);
    List<Payment> findByCompanyAndProjectInAndPaymentDateBetween(Company company, List<Project> projects, LocalDate from, LocalDate to);

    List<Payment> findByCompanyAndPaymentDateBetween(Company company, LocalDate from, LocalDate to);

    @Query("SELECT p FROM Payment p WHERE p.company = :company AND p.project.projectId IN :projectIds AND p.paymentDate < :date")
    List<Payment> findByCompanyAndProjectIdsBeforeDate(@Param("company") Company company,
                                                        @Param("projectIds") List<Long> projectIds,
                                                        @Param("date") LocalDate date);

    @Query("SELECT p FROM Payment p WHERE p.company = :company AND p.project.projectId IN :projectIds AND p.paymentDate BETWEEN :from AND :to")
    List<Payment> findByCompanyAndProjectIdsBetween(@Param("company") Company company,
                                                     @Param("projectIds") List<Long> projectIds,
                                                     @Param("from") LocalDate from,
                                                     @Param("to") LocalDate to);
}
