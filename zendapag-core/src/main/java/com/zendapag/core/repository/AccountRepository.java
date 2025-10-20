package com.zendapag.core.repository;

import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUser(User user);

    List<Account> findByUserId(Long userId);

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByPixKey(String pixKey);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByPixKey(String pixKey);

    @Query("SELECT a FROM Account a WHERE a.user.id = ?1 AND a.status = 'ACTIVE'")
    List<Account> findActiveAccountsByUserId(Long userId);

    @Query("SELECT a FROM Account a WHERE a.pixKey = ?1 AND a.status = 'ACTIVE'")
    Optional<Account> findActiveAccountByPixKey(String pixKey);
}