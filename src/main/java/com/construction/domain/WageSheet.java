package com.construction.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "wage_sheet")
public class WageSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;   // Always a Saturday

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;     // Always a Friday (weekStart + 6)

    @Column(precision = 12, scale = 2)
    private BigDecimal advance = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "wageSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "row_order")
    private List<WageRow> rows = new ArrayList<>();
}
