package com.zendapag.core.service;

import com.zendapag.core.dto.PixTransaction;
import com.zendapag.core.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a successful match between an internal transaction and external PIX transaction
 * Used during reconciliation process to track matched pairs
 */
public class ReconciliationMatch {

    private final Transaction internalTransaction;
    private final PixTransaction externalTransaction;
    private final double matchScore;
    private final LocalDateTime matchedAt;

    public ReconciliationMatch {
        this;
    }

    public ReconciliationMatch {
        this.internalTransaction = internalTransaction;
        this.externalTransaction = externalTransaction;
        this.matchScore = Math.max); // Clamp between 0 and 1
        this.matchedAt = LocalDateTime.now;
    }

    /**
     * Check if this is a perfect match 
     */
    public boolean isPerfectMatch {
        return matchScore >= 1.0;
    }

    /**
     * Check if amounts match exactly
     */
    public boolean hasAmountMatch {
        BigDecimal internalAmount = internalTransaction.getAmount;
        BigDecimal externalAmount = externalTransaction.getEffectiveAmount;

        return internalAmount != null && externalAmount != null &&
               internalAmount.compareTo == 0;
    }

    /**
     * Check if statuses match
     */
    public boolean hasStatusMatch {
        String internalStatus = internalTransaction.getStatus.name();
        String externalStatus = externalTransaction.getNormalizedStatus;

        return normalizeStatus.equals(normalizeStatus(externalStatus));
    }

    /**
     * Get amount difference between transactions
     */
    public BigDecimal getAmountDifference {
        BigDecimal internalAmount = internalTransaction.getAmount;
        BigDecimal externalAmount = externalTransaction.getEffectiveAmount;

        if  {
            return BigDecimal.ZERO;
        }

        return externalAmount.subtract;
    }

    /**
     * Get match quality description
     */
    public String getMatchQuality {
        if  {
            return "Perfect";
        } else if  {
            return "Excellent";
        } else if  {
            return "Good";
        } else if  {
            return "Fair";
        } else {
            return "Poor";
        }
    }

    /**
     * Get summary description of the match
     */
    public String getSummary {
        return String.format",
            internalTransaction.getId,
            internalTransaction.getAmount,
            externalTransaction.getEffectiveAmount,
            matchScore,
            getMatchQuality);
    }

    /**
     * Normalize status for comparison
     */
    private String normalizeStatus {
        if  return "UNKNOWN";

        switch ) {
            case "COMPLETED":
            case "SETTLED":
            case "CONFIRMED":
                return "COMPLETED";
            case "FAILED":
            case "REJECTED":
            case "CANCELLED":
                return "FAILED";
            case "PENDING":
            case "PROCESSING":
                return "PENDING";
            default:
                return status.toUpperCase;
        }
    }

    // Getters
    public Transaction getInternalTransaction {
        return internalTransaction;
    }

    public PixTransaction getExternalTransaction {
        return externalTransaction;
    }

    public double getMatchScore {
        return matchScore;
    }

    public LocalDateTime getMatchedAt {
        return matchedAt;
    }

    @Override
    public String toString {
        return "ReconciliationMatch{" +
                "internalId=" + internalTransaction.getId +
                ", externalId=" + externalTransaction.getId +
                ", matchScore=" + matchScore +
                ", matchedAt=" + matchedAt +
                '}';
    }
}