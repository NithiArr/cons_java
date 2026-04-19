package com.construction.repository;

import com.construction.domain.MasterCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MasterCategoryRepository extends JpaRepository<MasterCategory, Long> {
    List<MasterCategory> findByActiveOrderByNameAsc(boolean active);
    List<MasterCategory> findByActiveAndTypeOrderByNameAsc(boolean active, String type);
    Optional<MasterCategory> findByName(String name);
}
