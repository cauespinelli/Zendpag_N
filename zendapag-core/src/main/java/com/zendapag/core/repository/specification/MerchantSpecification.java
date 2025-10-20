package com.zendapag.core.repository.specification;

import com.zendapag.core.entity.Merchant;
import com.zendapag.core.entity.enums.MerchantStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MerchantSpecification {

    public static Specification<Merchant> withFilters(String name,
                                                       String email,
                                                       String document,
                                                       MerchantStatus status,
                                                       Boolean kycVerified,
                                                       Integer minRiskScore,
                                                       Integer maxRiskScore,
                                                       BigDecimal minFeeRate,
                                                       BigDecimal maxFeeRate,
                                                       Instant createdAfter,
                                                       Instant createdBefore,
                                                       String country) {
        return (Root<Merchant> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude deleted merchants
            predicates.add(cb.equal(root.get("deleted"), false));

            // Name filter (case-insensitive partial match)
            if (name != null && !name.trim().isEmpty()) {
                predicates.add(cb.like(
                    cb.lower(root.get("name")),
                    "%" + name.toLowerCase().trim() + "%"
                ));
            }

            // Email filter (case-insensitive partial match)
            if (email != null && !email.trim().isEmpty()) {
                predicates.add(cb.like(
                    cb.lower(root.get("email")),
                    "%" + email.toLowerCase().trim() + "%"
                ));
            }

            // Document filter (exact match)
            if (document != null && !document.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("document"), document.trim()));
            }

            // Status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // KYC verification filter
            if (kycVerified != null) {
                predicates.add(cb.equal(root.get("kycVerified"), kycVerified));
            }

            // Risk score range filter
            if (minRiskScore != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("riskScore"), minRiskScore));
            }
            if (maxRiskScore != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("riskScore"), maxRiskScore));
            }

            // Fee rate range filter
            if (minFeeRate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("feeRate"), minFeeRate));
            }
            if (maxFeeRate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("feeRate"), maxFeeRate));
            }

            // Created date range filter
            if (createdAfter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
            }
            if (createdBefore != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), createdBefore));
            }

            // Country filter
            if (country != null && !country.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("country"), country.trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Merchant> hasName(String name) {
        return (root, query, cb) -> name == null ? null :
            cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Merchant> hasEmail(String email) {
        return (root, query, cb) -> email == null ? null :
            cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<Merchant> hasDocument(String document) {
        return (root, query, cb) -> document == null ? null :
            cb.equal(root.get("document"), document);
    }

    public static Specification<Merchant> hasStatus(MerchantStatus status) {
        return (root, query, cb) -> status == null ? null :
            cb.equal(root.get("status"), status);
    }

    public static Specification<Merchant> isKycVerified(Boolean kycVerified) {
        return (root, query, cb) -> kycVerified == null ? null :
            cb.equal(root.get("kycVerified"), kycVerified);
    }

    public static Specification<Merchant> hasRiskScoreBetween(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get("riskScore"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("riskScore"), min);
            return cb.between(root.get("riskScore"), min, max);
        };
    }

    public static Specification<Merchant> hasFeeRateBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min == null) return cb.lessThanOrEqualTo(root.get("feeRate"), max);
            if (max == null) return cb.greaterThanOrEqualTo(root.get("feeRate"), min);
            return cb.between(root.get("feeRate"), min, max);
        };
    }

    public static Specification<Merchant> createdBetween(Instant startDate, Instant endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) return null;
            if (startDate == null) return cb.lessThan(root.get("createdAt"), endDate);
            if (endDate == null) return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            return cb.between(root.get("createdAt"), startDate, endDate);
        };
    }

    public static Specification<Merchant> hasCountry(String country) {
        return (root, query, cb) -> country == null ? null :
            cb.equal(root.get("country"), country);
    }

    public static Specification<Merchant> isNotDeleted() {
        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<Merchant> hasAutoSettle(Boolean autoSettle) {
        return (root, query, cb) -> autoSettle == null ? null :
            cb.equal(root.get("autoSettle"), autoSettle);
    }

    public static Specification<Merchant> lastLoginBefore(Instant before) {
        return (root, query, cb) -> before == null ? null :
            cb.or(
                cb.isNull(root.get("lastLoginAt")),
                cb.lessThan(root.get("lastLoginAt"), before)
            );
    }

    public static Specification<Merchant> searchByText(String searchText) {
        return (root, query, cb) -> {
            if (searchText == null || searchText.trim().isEmpty()) return null;

            String searchPattern = "%" + searchText.toLowerCase().trim() + "%";

            return cb.or(
                cb.like(cb.lower(root.get("name")), searchPattern),
                cb.like(cb.lower(root.get("email")), searchPattern),
                cb.like(cb.lower(root.get("tradingName")), searchPattern),
                cb.like(root.get("document"), searchPattern)
            );
        };
    }

    // Complex specifications
    public static Specification<Merchant> isActiveAndVerified() {
        return hasStatus(MerchantStatus.ACTIVE)
            .and(isKycVerified(true))
            .and(isNotDeleted());
    }

    public static Specification<Merchant> requiresAttention() {
        return (root, query, cb) -> cb.or(
            cb.and(
                cb.equal(root.get("status"), MerchantStatus.PENDING_APPROVAL),
                cb.lessThan(root.get("createdAt"), Instant.now().minusSeconds(86400)) // 24 hours ago
            ),
            cb.and(
                cb.equal(root.get("kycVerified"), false),
                cb.equal(root.get("status"), MerchantStatus.ACTIVE)
            ),
            cb.greaterThan(root.get("riskScore"), 80)
        );
    }

    public static Specification<Merchant> isHighRisk() {
        return (root, query, cb) -> cb.or(
            cb.greaterThan(root.get("riskScore"), 70),
            cb.equal(root.get("status"), MerchantStatus.SUSPENDED),
            cb.equal(root.get("status"), MerchantStatus.BLOCKED)
        );
    }
}