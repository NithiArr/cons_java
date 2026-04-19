package com.construction.repository;

import com.construction.domain.Project;
import com.construction.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByCompanyOrderByCreatedAtDesc(Company company);
    long countByCompany(Company company);
    long countByCompanyAndStatus(Company company, String status);
}
