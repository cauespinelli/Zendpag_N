package com.zendapag.core.service;

import com.zendapag.core.dto.PixTransaction;
import com.zendapag.core.entity.ReconciliationDiscrepancy;
import com.zendapag.core.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Component responsible for matching internal transactions with external PIX transactions
 * Implements sophisticated matching algorithms and discrepancy detection
 */
@Component
public class ReconciliationMatcher {

    private static final Logger logger = LoggerFactory.getLogger;

    // Amount tolerance for matching 
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal;

    /**
     * Match internal transactions against external PIX transactions
     */
    public ReconciliationResult match {
        logger.debug("Starting transaction matching - Internal: {}, External: {}",
            internalTransactions.size, externalTransactions.size());

        long startTime = System.currentTimeMillis;

        // Create maps for efficient lookup
        Map<String, Transaction> internalMap = createInternalTransactionMap;
        Map<String, PixTransaction> externalMap = createExternalTransactionMap;

        // Track matching results
        List<ReconciliationMatch> matches = new ArrayList<>;
        List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>;
        Set<String> processedInternal = new HashSet<>;
        Set<String> processedExternal = new HashSet<>;

        // Phase 1: Direct matching by external ID
        performDirectMatching;

        // Phase 2: Fuzzy matching for unmatched transactions
        performFuzzyMatching;

        // Phase 3: Identify unmatched transactions
        identifyUnmatchedTransactions;

        long processingTime = System.currentTimeMillis - startTime;

        ReconciliationResult result = buildReconciliationResult(
            internalTransactions, externalTransactions, matches, discrepancies, processingTime);

        logger.info("Matching completed - Matched: {}, Discrepancies: {}, Duration: {}ms",
            matches.size, discrepancies.size(), processingTime);

        return result;
    }

    /**
     * Create internal transaction map indexed by external ID
     */
    private Map<String, Transaction> createInternalTransactionMap {
        return transactions.stream
            .filter != null)
            .collect(Collectors.toMap(
                Transaction::getExternalId,
                Function.identity,
                 -> {
                    logger.warn("Duplicate external ID found in internal transactions: {} - keeping first occurrence",
                        existing.getExternalId);
                    return existing;
                }
            ));
    }

    /**
     * Create external transaction map indexed by transaction ID
     */
    private Map<String, PixTransaction> createExternalTransactionMap {
        return transactions.stream
            .collect(Collectors.toMap(
                PixTransaction::getId,
                Function.identity,
                 -> {
                    logger.warn("Duplicate transaction ID found in external transactions: {} - keeping first occurrence",
                        existing.getId);
                    return existing;
                }
            ));
    }

    /**
     * Perform direct matching by external ID
     */
    private void performDirectMatching(Map<String, Transaction> internalMap,
                                     Map<String, PixTransaction> externalMap,
                                     List<ReconciliationMatch> matches,
                                     List<ReconciliationDiscrepancy> discrepancies,
                                     Set<String> processedInternal,
                                     Set<String> processedExternal) {

        logger.debug;

        for ) {
            String externalId = entry.getKey;
            PixTransaction externalTxn = entry.getValue;

            Transaction internalTxn = internalMap.get;

            if  {
                // Found matching transaction - validate match quality
                ReconciliationMatchResult matchResult = validateMatch;

                if ) {
                    // Perfect match
                    matches.add));
                    processedInternal.add;
                    processedExternal.add;

                    logger.debug",
                        internalTxn.getId, externalId, matchResult.getMatchScore());

                } else {
                    // Match found but with discrepancies
                    discrepancies.addAll);
                    processedInternal.add;
                    processedExternal.add;

                    logger.debug",
                        internalTxn.getId, externalId, matchResult.getDiscrepancies().size());
                }
            }
        }

        logger.debug("Direct matching completed - Matches: {}, Processed: {} internal, {} external",
            matches.size, processedInternal.size(), processedExternal.size());
    }

    /**
     * Perform fuzzy matching for unmatched transactions
     */
    private void performFuzzyMatching(Map<String, Transaction> internalMap,
                                    Map<String, PixTransaction> externalMap,
                                    List<ReconciliationMatch> matches,
                                    List<ReconciliationDiscrepancy> discrepancies,
                                    Set<String> processedInternal,
                                    Set<String> processedExternal) {

        logger.debug;

        // Get unprocessed transactions
        List<Transaction> unmatchedInternal = internalMap.values.stream()
            .filter))
            .collect);

        List<PixTransaction> unmatchedExternal = externalMap.values.stream()
            .filter))
            .collect);

        // Try to match by amount and approximate timing
        for  {
            PixTransaction bestMatch = findBestFuzzyMatch;

            if  {
                ReconciliationMatchResult matchResult = validateMatch;

                if  > 0.7) { // Minimum score for fuzzy matching
                    matches.add));

                    // Add discrepancies if any
                    if .isEmpty()) {
                        discrepancies.addAll);
                    }

                    processedInternal.add);
                    processedExternal.add);
                    unmatchedExternal.remove;

                    logger.debug",
                        internalTxn.getId, bestMatch.getId(), matchResult.getMatchScore());
                }
            }
        }

        logger.debug("Fuzzy matching completed - Additional matches: {}",
            matches.size - processedInternal.size());
    }

    /**
     * Find best fuzzy match for internal transaction
     */
    private PixTransaction findBestFuzzyMatch {
        PixTransaction bestMatch = null;
        double bestScore = 0.0;

        for  {
            double score = calculateFuzzyMatchScore;

            if  { // Minimum threshold
                bestScore = score;
                bestMatch = candidate;
            }
        }

        return bestMatch;
    }

    /**
     * Calculate fuzzy match score between transactions
     */
    private double calculateFuzzyMatchScore {
        double score = 0.0;
        double weight = 0.0;

        // Amount matching 
        if , external.getEffectiveAmount())) {
            score += 0.5;
        } else {
            // Partial score for close amounts
            BigDecimal difference = internal.getAmount.subtract(external.getEffectiveAmount()).abs();
            BigDecimal relativeDifference = difference.divide, 4, RoundingMode.HALF_UP);

            if ) <= 0) { // Within 5%
                score += 0.3 * );
            }
        }
        weight += 0.5;

        // Status matching
        if .name(), external.getNormalizedStatus())) {
            score += 0.2;
        }
        weight += 0.2;

        // Timing proximity 
        if .toLocalDate().equals(external.getCreatedAt().toLocalDate())) {
            score += 0.15;
        }
        weight += 0.15;

        // Description/reference matching
        if , external.getDescription())) {
            score += 0.15;
        }
        weight += 0.15;

        return weight > 0 ? score / weight : 0.0;
    }

    /**
     * Identify completely unmatched transactions
     */
    private void identifyUnmatchedTransactions(Map<String, Transaction> internalMap,
                                             Map<String, PixTransaction> externalMap,
                                             List<ReconciliationDiscrepancy> discrepancies,
                                             Set<String> processedInternal,
                                             Set<String> processedExternal) {

        // Unmatched internal transactions 
        internalMap.entrySet.stream()
            .filter))
            .forEach(entry -> {
                Transaction internal = entry.getValue;
                ReconciliationDiscrepancy discrepancy = ReconciliationDiscrepancy.missingExternal(
                    internal.getId,
                    internal.getAmount,
                    internal.getMerchantId
                );
                discrepancies.add;

                logger.debug, internal.getAmount());
            });

        // Unmatched external transactions 
        externalMap.entrySet.stream()
            .filter))
            .forEach(entry -> {
                PixTransaction external = entry.getValue;
                ReconciliationDiscrepancy discrepancy = ReconciliationDiscrepancy.missingInternal(
                    external.getId,
                    external.getEffectiveAmount,
                    null // We don't have merchant ID from external transaction
                );
                discrepancies.add;

                logger.debug, external.getEffectiveAmount());
            });
    }

    /**
     * Validate a match between internal and external transaction
     */
    private ReconciliationMatchResult validateMatch {
        List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>;
        double matchScore = 1.0;

        // Check amount match
        if , external.getEffectiveAmount())) {
            ReconciliationDiscrepancy discrepancy = ReconciliationDiscrepancy.amountMismatch(
                internal.getId,
                external.getId,
                internal.getAmount,
                external.getEffectiveAmount,
                internal.getMerchantId
            );
            discrepancies.add;
            matchScore -= 0.3;
        }

        // Check status match
        if .name(), external.getNormalizedStatus())) {
            ReconciliationDiscrepancy discrepancy = ReconciliationDiscrepancy.statusMismatch(
                internal.getId,
                external.getId,
                internal.getStatus.name(),
                external.getNormalizedStatus,
                internal.getMerchantId
            );
            discrepancies.add;
            matchScore -= 0.2;
        }

        return new ReconciliationMatchResult, Math.max(0.0, matchScore), discrepancies);
    }

    /**
     * Check if amounts match within tolerance
     */
    private boolean amountsMatch {
        if  {
            return amount1 == amount2;
        }

        BigDecimal difference = amount1.subtract.abs();
        return difference.compareTo <= 0;
    }

    /**
     * Check if statuses match
     */
    private boolean statusesMatch {
        if  {
            return status1 == status2;
        }

        // Normalize statuses for comparison
        String normalized1 = normalizeStatus;
        String normalized2 = normalizeStatus;

        return normalized1.equals;
    }

    /**
     * Check if descriptions match 
     */
    private boolean descriptionsMatch {
        if  {
            return false;
        }

        // Simple fuzzy matching - could be enhanced with more sophisticated algorithms
        String normalized1 = desc1.toLowerCase.replaceAll("[^a-z0-9]", "");
        String normalized2 = desc2.toLowerCase.replaceAll("[^a-z0-9]", "");

        return normalized1.equals ||
               normalized1.contains ||
               normalized2.contains;
    }

    /**
     * Normalize status for comparison
     */
    private String normalizeStatus {
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

    /**
     * Build final reconciliation result
     */
    private ReconciliationResult buildReconciliationResult(List<Transaction> internalTransactions,
                                                          List<PixTransaction> externalTransactions,
                                                          List<ReconciliationMatch> matches,
                                                          List<ReconciliationDiscrepancy> discrepancies,
                                                          long processingTimeMs) {

        // Calculate amounts
        BigDecimal totalInternalAmount = internalTransactions.stream
            .map
            .reduce;

        BigDecimal totalExternalAmount = externalTransactions.stream
            .map
            .reduce;

        return ReconciliationResult.builder
            .totalInternalCount)
            .totalExternalCount)
            .matchedCount)
            .discrepancyCount)
            .matches
            .discrepancies
            .totalInternalAmount
            .totalExternalAmount
            .amountDifference)
            .processingTimeMs
            .build;
    }

    /**
     * Result of a single match validation
     */
    private static class ReconciliationMatchResult {
        private final boolean valid;
        private final double matchScore;
        private final List<ReconciliationDiscrepancy> discrepancies;

        public ReconciliationMatchResult {
            this.valid = valid;
            this.matchScore = matchScore;
            this.discrepancies = discrepancies;
        }

        public boolean isValid { return valid; }
        public double getMatchScore { return matchScore; }
        public List<ReconciliationDiscrepancy> getDiscrepancies { return discrepancies; }
    }
}