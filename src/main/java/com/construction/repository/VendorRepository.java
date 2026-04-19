package com.construction.repository;

import com.construction.domain.Vendor;
import com.construction.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    List<Vendor> findByCompanyOrderByName(Company company);
}
