package com.construction.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Entity
@Table(name = "sub_category")
public class SubCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subcategory_id")
    private Long subcategoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private MasterCategory parentCategory;

    @Column(length = 100)
    private String name;

    @Column(name = "default_unit", length = 20)
    private String defaultUnit;
}
