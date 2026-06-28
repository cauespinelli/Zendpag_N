package com.zendapag.core.repository;

import com.zendapag.core.entity.PayoutRule;
import com.zendapag.core.entity.enums.PaymentMethodType;
import com.zendapag.core.entity.enums.PayoutScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutRuleRepository extends JpaRepository<PayoutRule, Long> {

    List<PayoutRule> findByScope(PayoutScope scope);

    List<PayoutRule> findByScopeAndMerchantId(PayoutScope scope, UUID merchantId);

    Optional<PayoutRule> findByScopeAndMerchantIdAndMethod(PayoutScope scope, UUID merchantId, PaymentMethodType method);

    /** Regra GLOBAL de um método (merchantId nulo). */
    Optional<PayoutRule> findByScopeAndMethodAndMerchantIdIsNull(PayoutScope scope, PaymentMethodType method);
}
