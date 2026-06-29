package com.zendapag.core.repository;

import com.zendapag.core.entity.Origin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OriginRepository extends JpaRepository<Origin, Long> {

    Optional<Origin> findByCode(String code);

    /** Resolução por API Key (auth): busca a origem ativa pelo hash da key. */
    Optional<Origin> findByApiKeyHashAndActiveTrue(String apiKeyHash);

    boolean existsByCode(String code);
}
