package com.construction.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.ToString;

@Data
@Entity
@Table(name = "expense_item")
public class ExpenseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_item_id")
    private Long expenseItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Expense expense;

    @Column(name = "item_name", length = 200, nullable = false)
    private String itemName;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal quantity;

    @Column(name = "measuring_unit", length = 20, nullable = false)
    private String measuringUnit = "Unit";

    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    @Column(length = 100)
    private String brand;
}
