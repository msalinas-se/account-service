package com.nttdata.bank.account.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nttdata.bank.account.client.CustomerClient;
import com.nttdata.bank.account.client.dto.CustomerDto;
import com.nttdata.bank.account.config.AccountProperties;
import com.nttdata.bank.account.dto.AccountRequest;
import com.nttdata.bank.account.exception.BusinessRuleException;
import com.nttdata.bank.account.mapper.AccountMapper;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.AccountType;
import com.nttdata.bank.account.model.CustomerType;
import com.nttdata.bank.account.repository.AccountRepository;
import com.nttdata.bank.account.repository.MovementRepository;
import com.nttdata.bank.account.service.impl.AccountServiceImpl;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private MovementRepository movementRepository;

  @Mock
  private CustomerClient customerClient;

  private AccountServiceImpl accountService;

  @BeforeEach
  void setUp() {
    AccountProperties properties = new AccountProperties(Map.of(
        AccountType.SAVINGS, new AccountProperties.ProductRule(BigDecimal.ZERO, 20, null)));
    accountService = new AccountServiceImpl(accountRepository, movementRepository,
        customerClient, properties, new AccountMapper());
  }

  @Test
  void createSavingsAccount() {
    when(customerClient.findById("1"))
        .thenReturn(Single.just(new CustomerDto("1", CustomerType.PERSONAL, "45678912", "Ana")));
    when(accountRepository.countByCustomerIdAndType("1", AccountType.SAVINGS))
        .thenReturn(Single.just(0L));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

    accountService.create(new AccountRequest("1", AccountType.SAVINGS, null, null)).test()
        .assertValue(account -> account.monthlyMovementLimit() == 20);
  }

  @Test
  void personalCustomerCannotHaveTwoSavingsAccounts() {
    when(customerClient.findById("1"))
        .thenReturn(Single.just(new CustomerDto("1", CustomerType.PERSONAL, "45678912", "Ana")));
    when(accountRepository.countByCustomerIdAndType("1", AccountType.SAVINGS))
        .thenReturn(Single.just(1L));

    accountService.create(new AccountRequest("1", AccountType.SAVINGS, null, null)).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void businessCustomerCannotHaveSavingsAccount() {
    when(customerClient.findById("2"))
        .thenReturn(Single.just(new CustomerDto("2", CustomerType.BUSINESS, "20123456789", "Acme")));

    accountService.create(new AccountRequest("2", AccountType.SAVINGS, null, null)).test()
        .assertError(BusinessRuleException.class);
  }
}
