package com.construction.service;

import com.construction.domain.AuditLog;
import com.construction.domain.User;
import com.construction.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Record an audit event. Called fire-and-forget so it never slows down API responses.
     *
     * @param user         the user who performed the action
     * @param action       CREATE | UPDATE | DELETE
     * @param module       PURCHASE | EXPENSE | PAYMENT | CLIENT_PAYMENT | PROJECT | VENDOR | USER | MASTER_CATEGORY
     * @param resourceName human-readable label of the affected record
     * @param details      one-line description of what changed
     */
    @Async
    public void log(User user, String action, String module, String resourceName, Long resourceId, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setCompany(user.getCompany());
            entry.setPerformedBy(user);
            entry.setAction(action);
            entry.setModule(module);
            entry.setResourceName(resourceName);
            entry.setResourceId(resourceId);
            entry.setDetails(details);
            auditLogRepository.save(entry);
        } catch (Exception ignored) {
            // Audit failures must never break the main request
        }
    }

    @Async
    public void log(User user, String action, String module, String resourceName, String details) {
        log(user, action, module, resourceName, null, details);
    }
}
