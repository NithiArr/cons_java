package com.construction.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "master_category")
public class MasterCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(unique = true, length = 100)
    private String name;

    @Column(length = 50)
    private String type; // MATERIAL, EXPENSE

    @Column(name = "is_active")
    private boolean active = true;

    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubCategory> subcategories;
}
