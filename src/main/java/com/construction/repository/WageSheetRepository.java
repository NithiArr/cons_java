package com.construction.repository;

import com.construction.domain.WageSheet;
import com.construction.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WageSheetRepository extends JpaRepository<WageSheet, Long> {
    List<WageSheet> findByProjectInOrderByWeekStartDesc(List<Project> projects);
    List<WageSheet> findByProjectOrderByWeekStartDesc(Project project);
    Optional<WageSheet> findByProjectAndWeekStart(Project project, LocalDate weekStart);
    List<WageSheet> findByProjectInAndWeekStart(List<Project> projects, LocalDate weekStart);
}
