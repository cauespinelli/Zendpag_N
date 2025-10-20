package com.zendapag.core.service;

import com.zendapag.core.entity.ReconciliationDiscrepancy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of a reconciliation matching process
 * Contains all matches, discrepancies, and summary statistics
 */
public class ReconciliationResult {

    private int totalInternalCount;
    private int totalExternalCount;
    private int matchedCount;
    private int unmatchedInternalCount;
    private int unmatchedExternalCount;
    private int discrepancyCount;

    private List<ReconciliationMatch> matches = new ArrayList<>;
    private List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>;

    private BigDecimal totalInternalAmount = BigDecimal.ZERO;
    private BigDecimal totalExternalAmount = BigDecimal.ZERO;
    private BigDecimal amountDifference = BigDecimal.ZERO;

    private long processingTimeMs;

    public ReconciliationResult {}

    public static Builder builder {
        return new Builder;
    }

    /**
     * Check if there are any discrepancies
     */
    public boolean hasDiscrepancies {
        return discrepancyCount > 0;
    }

    /**
     * Check if reconciliation is balanced 
     */
    public boolean isBalanced {
        return amountDifference.compareTo == 0;
    }

    /**
     * Calculate match percentage
     */
    public double getMatchPercentage {
        if  {
            return 0.0;
        }
        return  matchedCount / totalInternalCount * 100.0;
    }

    /**
     * Get summary description
     */
    public String getSummary {
        return String.format(
            "Reconciliation: %d/%d matched , %d discrepancies, amount diff: %s",
            matchedCount,
            totalInternalCount,
            getMatchPercentage,
            discrepancyCount,
            amountDifference
        );
    }

    // Getters and setters
    public int getTotalInternalCount { return totalInternalCount; }
    public void setTotalInternalCount { this.totalInternalCount = totalInternalCount; }

    public int getTotalExternalCount { return totalExternalCount; }
    public void setTotalExternalCount { this.totalExternalCount = totalExternalCount; }

    public int getMatchedCount { return matchedCount; }
    public void setMatchedCount { this.matchedCount = matchedCount; }

    public int getUnmatchedInternalCount { return unmatchedInternalCount; }
    public void setUnmatchedInternalCount { this.unmatchedInternalCount = unmatchedInternalCount; }

    public int getUnmatchedExternalCount { return unmatchedExternalCount; }
    public void setUnmatchedExternalCount { this.unmatchedExternalCount = unmatchedExternalCount; }

    public int getDiscrepancyCount { return discrepancyCount; }
    public void setDiscrepancyCount { this.discrepancyCount = discrepancyCount; }

    public List<ReconciliationMatch> getMatches { return matches; }
    public void setMatches { this.matches = matches; }

    public List<ReconciliationDiscrepancy> getDiscrepancies { return discrepancies; }
    public void setDiscrepancies { this.discrepancies = discrepancies; }

    public BigDecimal getTotalInternalAmount { return totalInternalAmount; }
    public void setTotalInternalAmount { this.totalInternalAmount = totalInternalAmount; }

    public BigDecimal getTotalExternalAmount { return totalExternalAmount; }
    public void setTotalExternalAmount { this.totalExternalAmount = totalExternalAmount; }

    public BigDecimal getAmountDifference { return amountDifference; }
    public void setAmountDifference { this.amountDifference = amountDifference; }

    public long getProcessingTimeMs { return processingTimeMs; }
    public void setProcessingTimeMs { this.processingTimeMs = processingTimeMs; }

    /**
     * Builder pattern for ReconciliationResult
     */
    public static class Builder {
        private ReconciliationResult result = new ReconciliationResult;

        public Builder totalInternalCount {
            result.totalInternalCount = count;
            result.unmatchedInternalCount = count; // Will be adjusted when matches are set
            return this;
        }

        public Builder totalExternalCount {
            result.totalExternalCount = count;
            result.unmatchedExternalCount = count; // Will be adjusted when matches are set
            return this;
        }

        public Builder matchedCount {
            result.matchedCount = count;
            result.unmatchedInternalCount = result.totalInternalCount - count;
            result.unmatchedExternalCount = result.totalExternalCount - count;
            return this;
        }

        public Builder discrepancyCount {
            result.discrepancyCount = count;
            return this;
        }

        public Builder matches {
            result.matches = matches != null ? matches : new ArrayList<>;
            result.matchedCount = result.matches.size;
            return this;
        }

        public Builder discrepancies {
            result.discrepancies = discrepancies != null ? discrepancies : new ArrayList<>;
            result.discrepancyCount = result.discrepancies.size;
            return this;
        }

        public Builder totalInternalAmount {
            result.totalInternalAmount = amount != null ? amount : BigDecimal.ZERO;
            return this;
        }

        public Builder totalExternalAmount {
            result.totalExternalAmount = amount != null ? amount : BigDecimal.ZERO;
            return this;
        }

        public Builder amountDifference {
            result.amountDifference = difference != null ? difference : BigDecimal.ZERO;
            return this;
        }

        public Builder processingTimeMs {
            result.processingTimeMs = timeMs;
            return this;
        }

        public ReconciliationResult build {
            // Auto-calculate derived values
            if  {
                result.matchedCount = result.matches.size;
            }

            if  {
                result.discrepancyCount = result.discrepancies.size;
            }

            result.unmatchedInternalCount = Math.max;
            result.unmatchedExternalCount = Math.max;

            if  &&
                !result.totalInternalAmount.equals &&
                !result.totalExternalAmount.equals) {
                result.amountDifference = result.totalExternalAmount.subtract;
            }

            return result;
        }
    }
}