package com.construction.repository;

import com.construction.domain.ClientPayment;
import com.construction.domain.Company;
import com.construction.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClientPaymentRepository extends JpaRepository<ClientPayment, Long> {
    List<ClientPayment> findByCompany(Company company);
    List<ClientPayment> findByCompanyAndProject(Company company, Project project);
    List<ClientPayment> findByCompanyAndPaymentDateBefore(Company company, LocalDate date);

    List<ClientPayment> findByCompanyAndProjectIn(Company company, List<Project> projects);
    List<ClientPayment> findByCompanyAndProjectInAndPaymentDateBefore(Company company, List<Project> projects, LocalDate date);
    List<ClientPayment> findByCompanyAndProjectInAndPaymentDateBetween(Company company, List<Project> projects, LocalDate from, LocalDate to);

    List<ClientPayment> findByCompanyAndPaymentDateBetween(Company company, LocalDate from, LocalDate to);

    @Query("SELECT cp FROM ClientPayment cp WHERE cp.company = :company AND cp.project.projectId IN :projectIds AND cp.paymentDate < :date")
    List<ClientPayment> findByCompanyAndProjectIdsBeforeDate(@Param("company") Company company,
                                                              @Param("projectIds") List<Long> projectIds,
                                                              @Param("date") LocalDate date);

    @Query("SELECT cp FROM ClientPayment cp WHERE cp.company = :company AND cp.project.projectId IN :projectIds AND cp.paymentDate BETWEEN :from AND :to")
    List<ClientPayment> findByCompanyAndProjectIdsBetween(@Param("company") Company company,
                                                           @Param("projectIds") List<Long> projectIds,
                                                           @Param("from") LocalDate from,
                                                           @Param("to") LocalDate to);
}
