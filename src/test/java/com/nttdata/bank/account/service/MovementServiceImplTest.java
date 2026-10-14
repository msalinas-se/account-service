package com.nttdata.bank.account.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nttdata.bank.account.dto.MovementRequest;
import com.nttdata.bank.account.exception.BusinessRuleException;
import com.nttdata.bank.account.mapper.AccountMapper;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.AccountType;
import com.nttdata.bank.account.model.Movement;
import com.nttdata.bank.account.repository.AccountRepository;
import com.nttdata.bank.account.repository.MovementRepository;
import com.nttdata.bank.account.service.impl.MovementServiceImpl;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovementServiceImplTest {

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private MovementRepository movementRepository;

  private MovementServiceImpl movementService;

  @BeforeEach
  void setUp() {
    movementService = new MovementServiceImpl(accountRepository, movementRepository,
        new AccountMapper());
  }

  private Account checkingAccount(String balance) {
    return Account.builder().id("a1").type(AccountType.CHECKING)
        .balance(new BigDecimal(balance)).build();
  }

  @Test
  void depositIncreasesBalance() {
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(checkingAccount("100")));
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
    when(movementRepository.save(any(Movement.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));

    movementService.deposit("a1", new MovementRequest(new BigDecimal("50"))).test()
        .assertValue(movement -> movement.balance().compareTo(new BigDecimal("150")) == 0);
  }

  @Test
  void withdrawalWithoutFunds() {
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(checkingAccount("10")));

    movementService.withdraw("a1", new MovementRequest(new BigDecimal("50"))).test()
        .assertError(BusinessRuleException.class);
  }
}
