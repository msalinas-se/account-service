package com.nttdata.bank.account.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nttdata.bank.account.mapper.AccountMapper;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.AccountType;
import com.nttdata.bank.account.model.Movement;
import com.nttdata.bank.account.model.MovementType;
import com.nttdata.bank.account.repository.AccountRepository;
import com.nttdata.bank.account.repository.MovementRepository;
import com.nttdata.bank.account.service.impl.ReportServiceImpl;
import io.reactivex.rxjava3.core.Flowable;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private MovementRepository movementRepository;

  private ReportServiceImpl reportService;

  @BeforeEach
  void setUp() {
    reportService = new ReportServiceImpl(accountRepository, movementRepository,
        new AccountMapper());
  }

  private Movement movement(String accountId, MovementType type, String amount, String fee) {
    return Movement.builder().accountId(accountId).type(type).amount(new BigDecimal(amount))
        .fee(new BigDecimal(fee)).build();
  }

  @Test
  void reportGroupsMovementsByProduct() {
    when(accountRepository.findAll()).thenReturn(Flowable.just(
        Account.builder().id("a1").type(AccountType.SAVINGS).build(),
        Account.builder().id("a2").type(AccountType.CHECKING).build()));
    when(movementRepository.findByDateBetween(any(), any())).thenReturn(Flowable.just(
        movement("a1", MovementType.DEPOSIT, "100", "0"),
        movement("a1", MovementType.DEPOSIT, "50", "2"),
        movement("a2", MovementType.WITHDRAWAL, "30", "1")));

    reportService.productReport(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)).test()
        .assertValue(report -> report.get(AccountType.SAVINGS).totalMovements() == 2
            && report.get(AccountType.SAVINGS).totals().get(MovementType.DEPOSIT).intValue() == 150
            && report.get(AccountType.SAVINGS).totalFees().intValue() == 2
            && report.get(AccountType.CHECKING).accounts() == 1);
  }

  @Test
  void lastMovements() {
    when(movementRepository.findTop10ByAccountIdOrderByDateDesc("a1"))
        .thenReturn(Flowable.just(new Movement(), new Movement()));

    reportService.lastMovements("a1").test().assertValueCount(2);
  }
}
