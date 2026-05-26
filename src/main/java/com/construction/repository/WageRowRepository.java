package com.construction.repository;

import com.construction.domain.WageRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WageRowRepository extends JpaRepository<WageRow, Long> {
}
