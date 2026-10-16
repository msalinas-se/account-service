package com.nttdata.bank.account.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.nttdata.bank.account.dto.MovementRequest;
import com.nttdata.bank.account.dto.TransferRequest;
import com.nttdata.bank.account.exception.BusinessRuleException;
import com.nttdata.bank.account.mapper.AccountMapper;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.AccountType;
import com.nttdata.bank.account.model.Movement;
import com.nttdata.bank.account.model.MovementType;
import com.nttdata.bank.account.repository.AccountRepository;
import com.nttdata.bank.account.repository.MovementRepository;
import com.nttdata.bank.account.service.impl.MovementServiceImpl;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDate;
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

  private Account account(String id, String balance) {
    return Account.builder().id(id).type(AccountType.SAVINGS).balance(new BigDecimal(balance))
        .monthlyMovementLimit(20).freeTransactions(2).transactionFee(new BigDecimal("2"))
        .build();
  }

  private void monthlyTransactions(long count) {
    when(movementRepository.countByAccountIdAndTypeInAndDateBetween(eq("a1"), anyCollection(),
        any(), any())).thenReturn(Single.just(count));
  }

  private void saveAll() {
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
    when(movementRepository.save(any(Movement.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
  }

  @Test
  void depositWithoutFee() {
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account("a1", "100")));
    monthlyTransactions(0);
    saveAll();

    movementService.deposit("a1", new MovementRequest(new BigDecimal("50"))).test()
        .assertValue(movement -> movement.balance().intValue() == 150
            && movement.fee().signum() == 0);
  }

  @Test
  void depositWithFeeAfterFreeTransactions() {
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account("a1", "100")));
    monthlyTransactions(2);
    saveAll();

    movementService.deposit("a1", new MovementRequest(new BigDecimal("50"))).test()
        .assertValue(movement -> movement.balance().intValue() == 148
            && movement.fee().intValue() == 2);
  }

  @Test
  void withdrawal() {
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account("a1", "100")));
    monthlyTransactions(0);
    saveAll();

    movementService.withdraw("a1", new MovementRequest(new BigDecimal("30"))).test()
        .assertValue(movement -> movement.balance().intValue() == 70);
  }

  @Test
  void withdrawalWithoutFunds() {
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account("a1", "10")));
    monthlyTransactions(0);

    movementService.withdraw("a1", new MovementRequest(new BigDecimal("50"))).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void monthlyLimitReached() {
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account("a1", "100")));
    monthlyTransactions(20);

    movementService.deposit("a1", new MovementRequest(BigDecimal.ONE)).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void fixedTermOutsideMovementDay() {
    Account account = account("a1", "100");
    account.setMovementDay(LocalDate.now().getDayOfMonth() == 1 ? 2 : 1);
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account));

    movementService.deposit("a1", new MovementRequest(BigDecimal.ONE)).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void transferBetweenAccounts() {
    Account from = account("a1", "100");
    Account to = account("a2", "0");
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(from));
    when(accountRepository.findById("a2")).thenReturn(Maybe.just(to));
    saveAll();

    movementService.transfer(new TransferRequest("a1", "a2", new BigDecimal("40"))).test()
        .assertValue(movement -> movement.type() == MovementType.TRANSFER_OUT
            && movement.balance().intValue() == 60);
  }

  @Test
  void transferWithoutFunds() {
    when(accountRepository.findById("a1")).thenReturn(Maybe.just(account("a1", "10")));
    when(accountRepository.findById("a2")).thenReturn(Maybe.just(account("a2", "0")));

    movementService.transfer(new TransferRequest("a1", "a2", new BigDecimal("40"))).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void transferToSameAccount() {
    movementService.transfer(new TransferRequest("a1", "a1", BigDecimal.ONE)).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void accountMovements() {
    when(movementRepository.findByAccountIdOrderByDateDesc("a1"))
        .thenReturn(Flowable.just(new Movement()));

    movementService.findByAccount("a1").test().assertValueCount(1);
  }
}
