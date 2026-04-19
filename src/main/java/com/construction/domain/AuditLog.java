package com.construction.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id")
    private User performedBy;

    /** CREATE | UPDATE | DELETE */
    @Column(nullable = false, length = 20)
    private String action;

    /** PURCHASE | EXPENSE | PAYMENT | CLIENT_PAYMENT | PROJECT | VENDOR | USER | MASTER_CATEGORY */
    @Column(nullable = false, length = 40)
    private String module;

    /** Human-readable name of the affected record, e.g. "Project Alpha", "Ravi Kumar" */
    @Column(name = "resource_name")
    private String resourceName;

    /** Short human-readable description of the change */
    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
