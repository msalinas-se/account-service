package com.nttdata.bank.account.service.impl;

import com.nttdata.bank.account.dto.MovementResponse;
import com.nttdata.bank.account.dto.ProductReport;
import com.nttdata.bank.account.mapper.AccountMapper;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.AccountType;
import com.nttdata.bank.account.model.Movement;
import com.nttdata.bank.account.repository.AccountRepository;
import com.nttdata.bank.account.repository.MovementRepository;
import com.nttdata.bank.account.service.ReportService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Implementation of ReportService. */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
  private final AccountRepository accountRepository;
  private final MovementRepository movementRepository;
  private final AccountMapper accountMapper;

  /** General report by product between two dates. */
  @Override
  public Single<Map<AccountType, ProductReport>> productReport(LocalDate from, LocalDate to) {
    Single<Map<String, AccountType>> types = accountRepository.findAll()
        .toMap(Account::getId, Account::getType);
    Single<List<Movement>> movements = movementRepository
        .findByDateBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay())
        .toList();
    return Single.zip(types, movements, (accountTypes, list) -> list.stream()
        .filter(movement -> accountTypes.containsKey(movement.getAccountId()))
        .collect(Collectors.groupingBy(movement -> accountTypes.get(movement.getAccountId()),
            Collectors.collectingAndThen(Collectors.toList(), this::toReport))));
  }

  /** Last 10 movements of an account. */
  @Override
  public Flowable<MovementResponse> lastMovements(String accountId) {
    return movementRepository.findTop10ByAccountIdOrderByDateDesc(accountId)
        .map(accountMapper::toResponse);
  }

  private ProductReport toReport(List<Movement> movements) {
    long accounts = movements.stream().map(Movement::getAccountId).distinct().count();
    var totals = movements.stream()
        .collect(Collectors.groupingBy(Movement::getType,
            Collectors.reducing(BigDecimal.ZERO, Movement::getAmount, BigDecimal::add)));
    BigDecimal fees = movements.stream()
        .map(movement -> movement.getFee() == null ? BigDecimal.ZERO : movement.getFee())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new ProductReport(accounts, movements.size(), totals, fees,
        movements.stream().map(accountMapper::toResponse).toList());
  }
}
