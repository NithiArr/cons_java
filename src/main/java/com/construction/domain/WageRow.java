package com.construction.domain;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "wage_row")
public class WageRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wage_sheet_id", nullable = false)
    @JsonIgnore
    private WageSheet wageSheet;

    @Column(name = "row_order")
    private Integer rowOrder;

    @Column(length = 50)
    private String category;   // Free text: e.g. Mas, Men, Fem

    @Column(name = "employee_name", length = 200)
    private String employeeName;

    @Column(name = "row_type", length = 20)
    private String rowType; // MASON, FITTER, ADDITIONAL

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_head_labour")
    private Boolean isHeadLabour = false;

    // day1 = Saturday, day2 = Sunday, ..., day7 = Friday
    // Values: 0, 0.5, 1, 1.5, 2
    @Column(precision = 3, scale = 1)
    private BigDecimal day1 = BigDecimal.ZERO;
    @Column(precision = 3, scale = 1)
    private BigDecimal day2 = BigDecimal.ZERO;
    @Column(precision = 3, scale = 1)
    private BigDecimal day3 = BigDecimal.ZERO;
    @Column(precision = 3, scale = 1)
    private BigDecimal day4 = BigDecimal.ZERO;
    @Column(precision = 3, scale = 1)
    private BigDecimal day5 = BigDecimal.ZERO;
    @Column(precision = 3, scale = 1)
    private BigDecimal day6 = BigDecimal.ZERO;
    @Column(precision = 3, scale = 1)
    private BigDecimal day7 = BigDecimal.ZERO;

    @Column(name = "wage_per_day", precision = 10, scale = 2)
    private BigDecimal wagePerDay = BigDecimal.ZERO;

    // ── Calculated helpers (not stored) ──────────────────────────────────
    @Transient
    public BigDecimal getNoOfDays() {
        if ("ADDITIONAL".equals(rowType)) {
            return BigDecimal.ZERO;
        }
        return safeVal(day1).add(safeVal(day2)).add(safeVal(day3))
                .add(safeVal(day4)).add(safeVal(day5)).add(safeVal(day6))
                .add(safeVal(day7));
    }

    @Transient
    public BigDecimal getAmount() {
        if ("ADDITIONAL".equals(rowType)) {
            return safeVal(wagePerDay);
        }
        return getNoOfDays().multiply(safeVal(wagePerDay));
    }

    private BigDecimal safeVal(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
