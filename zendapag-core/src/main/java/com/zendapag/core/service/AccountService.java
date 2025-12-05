package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.User;
import com.zendapag.core.entity.Account.AccountStatus;
import com.zendapag.core.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account createAccount(Account account) {
        log.info("Creating account for user: {}", account.getUser().getId());

        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        if (account.getPendingBalance() == null) {
            account.setPendingBalance(BigDecimal.ZERO);
        }
        if (account.getStatus() == null) {
            account.setStatus(AccountStatus.ACTIVE);
        }

        Account saved = accountRepository.save(account);
        log.info("Account created with ID: {}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Account findById(Long id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));
    }

    @Transactional(readOnly = true)
    public List<Account> findByUser(User user) {
        return accountRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public List<Account> findByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    @Transactional(readOnly = true)
    public Optional<Account> findByPixKey(String pixKey) {
        return accountRepository.findByPixKey(pixKey);
    }

    @Transactional(readOnly = true)
    public Page<Account> findAll(Pageable pageable) {
        return accountRepository.findAll(pageable);
    }

    @Transactional
    public Account updateBalance(Long accountId, BigDecimal amount) {
        Account account = findById(accountId);
        BigDecimal newBalance = account.getBalance().add(amount);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("INSUFFICIENT_BALANCE", "Insufficient balance for this operation");
        }

        account.setBalance(newBalance);
        return accountRepository.save(account);
    }

    @Transactional
    public Account addPendingBalance(Long accountId, BigDecimal amount) {
        Account account = findById(accountId);
        account.setPendingBalance(account.getPendingBalance().add(amount));
        return accountRepository.save(account);
    }

    @Transactional
    public Account releasePendingBalance(Long accountId, BigDecimal amount) {
        Account account = findById(accountId);
        BigDecimal newPending = account.getPendingBalance().subtract(amount);

        if (newPending.compareTo(BigDecimal.ZERO) < 0) {
            newPending = BigDecimal.ZERO;
        }

        account.setPendingBalance(newPending);
        return accountRepository.save(account);
    }

    @Transactional
    public Account updateStatus(Long accountId, AccountStatus status) {
        Account account = findById(accountId);
        account.setStatus(status);
        return accountRepository.save(account);
    }

    @Transactional
    public Account credit(Long accountId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Credit amount must be positive");
        }
        log.info("Crediting account {} with amount: {}", accountId, amount);
        return updateBalance(accountId, amount);
    }

    @Transactional
    public Account debit(Long accountId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Debit amount must be positive");
        }
        log.info("Debiting account {} with amount: {}", accountId, amount);
        return updateBalance(accountId, amount.negate());
    }

    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalance(Long accountId) {
        Account account = findById(accountId);
        return account.getBalance().subtract(account.getPendingBalance());
    }

    @Transactional(readOnly = true)
    public boolean hasAvailableBalance(Long accountId, BigDecimal requiredAmount) {
        BigDecimal available = getAvailableBalance(accountId);
        return available.compareTo(requiredAmount) >= 0;
    }
}
