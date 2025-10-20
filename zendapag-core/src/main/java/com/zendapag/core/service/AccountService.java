package com.zendapag.core.service;

import com.zendapag.common.exception.BusinessException;
import com.zendapag.common.exception.ResourceNotFoundException;
import com.zendapag.core.entity.Account;
import com.zendapag.core.entity.User;
import com.zendapag.core.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account createAccount {
        log.info);

        Account account = Account.builder
                .user
                .type
                .accountNumber)
                .balance
                .status
                .build;

        Account savedAccount = accountRepository.save;
        log.info);

        return savedAccount;
    }

    @Transactional
    public Account findById {
        return accountRepository.findById
                .orElseThrow -> new ResourceNotFoundException("Account", "id", id));
    }

    @Transactional
    public Account findByAccountNumber {
        return accountRepository.findByAccountNumber
                .orElseThrow -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));
    }

    @Transactional
    public Account findByPixKey {
        return accountRepository.findActiveAccountByPixKey
                .orElseThrow -> new ResourceNotFoundException("Account", "pixKey", pixKey));
    }

    @Transactional
    public List<Account> findByUserId {
        return accountRepository.findActiveAccountsByUserId;
    }

    @Transactional
    public Account registerPixKey {
        log.info;

        if ) {
            throw new BusinessException;
        }

        Account account = findById;
        account.setPixKey;
        account.setPixKeyType;

        Account savedAccount = accountRepository.save;
        log.info;

        return savedAccount;
    }

    @Transactional
    public Account updateBalance {
        Account account = findById;
        account.setBalance;
        return accountRepository.save;
    }

    @Transactional
    public Account updateAccountStatus {
        Account account = findById;
        account.setStatus;
        return accountRepository.save;
    }

    @Transactional
    public boolean hasActivePixKey {
        return accountRepository.findActiveAccountByPixKey.isPresent();
    }

    private String generateAccountNumber {
        String accountNumber;
        do {
            accountNumber = String.valueOf);
        } while );

        return accountNumber;
    }
}