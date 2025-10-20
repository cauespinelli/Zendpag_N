package com.zendapag.core.repository.specification;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.Payment;
import com.zendapag.core.entity.enums.PaymentStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PaymentSpecification {

    public static Specification<Payment> withFilters(String referenceId,
                                                     Merchant merchant,
                                                     PaymentStatus status,
                                                     BigDecimal minAmount,
                                                     BigDecimal maxAmount,
                                                     String currency,
                                                     String customerEmail,
                                                     String customerDocument,
                                                     String gateway,
                                                     Instant createdAfter,
                                                     Instant createdBefore,
                                                     String description) {
        return (Root<Payment> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude deleted payments
            predicates.add(cb.equal(root.get("deleted"), false));

            // Reference ID filter (case-insensitive partial match)
            if (referenceId != null && !referenceId.trim().isEmpty()) {
                predicates.add(cb.like(
                    cb.lower(root.get("referenceId")),
                    "%" + referenceId.toLowerCase().trim() + "%"
                ));
            }

            // Merchant filter
            if (merchant != null) {
                predicates.add(cb.equal(root.get("merchant"), merchant));
            }

            // Status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Amount range filter
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            // Currency filter
            if (currency != null && !currency.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("currency"), currency.trim().toUpperCase()));
            }

            // Customer email filter (case-insensitive partial match)
            if (customerEmail != null && !customerEmail.trim().isEmpty()) {
                predicates.add(cb.like(
                    cb.lower(root.get("customerEmail")),
                    "%" + customerEmail.toLowerCase().trim() + "%"
                ));
            }

            // Customer document filter
            if (customerDocument != null && !customerDocument.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("customerDocument"), customerDocument.trim()));
            }

            // Gateway filter
            if (gateway != null && !gateway.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("gateway"), gateway.trim()));
            }

            // Created date range filter
            if (createdAfter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
            }
            if (createdBefore != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), createdBefore));
            }

            // Description filter (case-insensitive partial match)
            if (description != null && !description.trim().isEmpty()) {
                predicates.add(cb.like(
                    cb.lower(root.get("description")),
                    "%" + description.toLowerCase().trim() + "%"
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Payment> hasReferenceId(String referenceId) {
        return (root, query, cb) -> referenceId == null ? null :
            cb.equal(root.get("referenceId"), referenceId);
    }

    public static Specification<Payment> belongsToMerchant(Merchant merchant) {
        return (root, query, cb) -> merchant == null ? null :
            cb.equal(root.get("merchant"), merchant);
    }

    public static Specification<Payment> hasStatus(PaymentStatus status) {
        return (root, query, cb) -> status == null ? null :
            cb.equal(root.get("status"), status);
    }

    public static Specification<Payment> hasStatusIn(List<PaymentStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) return null;
            return root.get("status").in(statuses);
        };
    }

    public static Specification<Payment> hasAmountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get("amount"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("amount"), min);
            return cb.between(root.get("amount"), min, max);
        };
    }

    public static Specification<Payment> hasCurrency(String currency) {
        return (root, query, cb) -> currency == null ? null :
            cb.equal(root.get("currency"), currency);
    }

    public static Specification<Payment> hasCustomerEmail(String email) {
        return (root, query, cb) -> email == null ? null :
            cb.like(cb.lower(root.get("customerEmail")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<Payment> hasCustomerDocument(String document) {
        return (root, query, cb) -> document == null ? null :
            cb.equal(root.get("customerDocument"), document);
    }

    public static Specification<Payment> hasGateway(String gateway) {
        return (root, query, cb) -> gateway == null ? null :
            cb.equal(root.get("gateway"), gateway);
    }

    public static Specification<Payment> createdBetween(Instant startDate, Instant endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) return null;
            if (startDate == null) return cb.lessThan(root.get("createdAt"), endDate);
            if (endDate == null) return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            return cb.between(root.get("createdAt"), startDate, endDate);
        };
    }

    public static Specification<Payment> processedBetween(Instant startDate, Instant endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) return null;
            if (startDate == null) return cb.lessThan(root.get("processedAt"), endDate);
            if (endDate == null) return cb.greaterThanOrEqualTo(root.get("processedAt"), startDate);
            return cb.between(root.get("processedAt"), startDate, endDate);
        };
    }

    public static Specification<Payment> hasDescription(String description) {
        return (root, query, cb) -> description == null ? null :
            cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
    }

    public static Specification<Payment> isNotDeleted() {
        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<Payment> isExpired() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("expiresAt")),
            cb.lessThan(root.get("expiresAt"), Instant.now())
        );
    }

    public static Specification<Payment> isRefundable() {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("status"), PaymentStatus.APPROVED),
            cb.greaterThan(root.get("refundableAmount"), BigDecimal.ZERO)
        );
    }

    public static Specification<Payment> hasPixKey(String pixKey) {
        return (root, query, cb) -> pixKey == null ? null :
            cb.equal(root.get("pixKey"), pixKey);
    }

    public static Specification<Payment> hasPixTransactionId(String pixTransactionId) {
        return (root, query, cb) -> pixTransactionId == null ? null :
            cb.equal(root.get("pixTransactionId"), pixTransactionId);
    }

    public static Specification<Payment> hasGatewayTransactionId(String gatewayTransactionId) {
        return (root, query, cb) -> gatewayTransactionId == null ? null :
            cb.equal(root.get("gatewayTransactionId"), gatewayTransactionId);
    }

    public static Specification<Payment> hasExternalId(String externalId) {
        return (root, query, cb) -> externalId == null ? null :
            cb.equal(root.get("externalId"), externalId);
    }

    public static Specification<Payment> hasIpAddress(String ipAddress) {
        return (root, query, cb) -> ipAddress == null ? null :
            cb.equal(root.get("ipAddress"), ipAddress);
    }

    public static Specification<Payment> searchByText(String searchText) {
        return (root, query, cb) -> {
            if (searchText == null || searchText.trim().isEmpty()) return null;

            String searchPattern = "%" + searchText.toLowerCase().trim() + "%";

            return cb.or(
                cb.like(cb.lower(root.get("referenceId")), searchPattern),
                cb.like(cb.lower(root.get("description")), searchPattern),
                cb.like(cb.lower(root.get("customerEmail")), searchPattern),
                cb.like(cb.lower(root.get("customerName")), searchPattern),
                cb.like(root.get("customerDocument"), searchPattern),
                cb.like(root.get("externalId"), searchPattern),
                cb.like(root.get("gatewayTransactionId"), searchPattern)
            );
        };
    }

    // Complex specifications
    public static Specification<Payment> isPending() {
        return hasStatusIn(List.of(PaymentStatus.PENDING, PaymentStatus.PROCESSING))
            .and(isNotDeleted());
    }

    public static Specification<Payment> isSuccessful() {
        return hasStatus(PaymentStatus.APPROVED)
            .and(isNotDeleted());
    }

    public static Specification<Payment> isFailed() {
        return hasStatusIn(List.of(PaymentStatus.REJECTED, PaymentStatus.CANCELLED, PaymentStatus.EXPIRED))
            .and(isNotDeleted());
    }

    public static Specification<Payment> requiresAttention() {
        return (root, query, cb) -> cb.or(
            // Payments pending for more than 1 hour
            cb.and(
                cb.equal(root.get("status"), PaymentStatus.PENDING),
                cb.lessThan(root.get("createdAt"), Instant.now().minusSeconds(3600))
            ),
            // Processing payments for more than 30 minutes
            cb.and(
                cb.equal(root.get("status"), PaymentStatus.PROCESSING),
                cb.isNotNull(root.get("processedAt")),
                cb.lessThan(root.get("processedAt"), Instant.now().minusSeconds(1800))
            ),
            // Expired but not marked as expired
            cb.and(
                cb.in(root.get("status")).value(List.of(PaymentStatus.PENDING, PaymentStatus.PROCESSING)),
                cb.isNotNull(root.get("expiresAt")),
                cb.lessThan(root.get("expiresAt"), Instant.now())
            )
        );
    }

    public static Specification<Payment> isHighValue(BigDecimal threshold) {
        return hasAmountBetween(threshold, null)
            .and(isNotDeleted());
    }

    public static Specification<Payment> hasSuspiciousActivity() {
        return (root, query, cb) -> {
            // Multiple payments from same IP in short time
            // This would typically be implemented with a subquery or additional logic
            return cb.isNotNull(root.get("ipAddress"));
        };
    }

    public static Specification<Payment> isRecent(int hours) {
        return createdBetween(Instant.now().minusSeconds(hours * 3600), null)
            .and(isNotDeleted());
    }
}