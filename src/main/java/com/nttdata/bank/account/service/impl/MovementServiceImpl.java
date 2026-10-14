package com.nttdata.bank.account.service.impl;

import com.nttdata.bank.account.dto.MovementRequest;
import com.nttdata.bank.account.dto.MovementResponse;
import com.nttdata.bank.account.exception.BusinessRuleException;
import com.nttdata.bank.account.exception.ResourceNotFoundException;
import com.nttdata.bank.account.mapper.AccountMapper;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.Movement;
import com.nttdata.bank.account.model.MovementType;
import com.nttdata.bank.account.repository.AccountRepository;
import com.nttdata.bank.account.repository.MovementRepository;
import com.nttdata.bank.account.service.MovementService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Implementation of MovementService. */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovementServiceImpl implements MovementService {
  private final AccountRepository accountRepository;
  private final MovementRepository movementRepository;
  private final AccountMapper accountMapper;

  /** Deposits money into an account. */
  @Override
  public Single<MovementResponse> deposit(String accountId, MovementRequest request) {
    return register(accountId, MovementType.DEPOSIT, request.amount());
  }

  /** Withdraws money from an account. */
  @Override
  public Single<MovementResponse> withdraw(String accountId, MovementRequest request) {
    return register(accountId, MovementType.WITHDRAWAL, request.amount());
  }

  /** Lists the movements of an account. */
  @Override
  public Flowable<MovementResponse> findByAccount(String accountId) {
    return movementRepository.findByAccountIdOrderByDateDesc(accountId)
        .map(accountMapper::toResponse);
  }

  private Single<MovementResponse> register(String accountId, MovementType type,
      BigDecimal amount) {
    return accountRepository.findById(accountId)
        .switchIfEmpty(Single.error(new ResourceNotFoundException("Account not found: " + accountId)))
        .flatMap(account -> validate(account, type, amount)
            .andThen(Single.defer(() -> apply(account, type, amount))))
        .doOnSuccess(movement -> log.info("{} of {} on account {}", type, amount, accountId))
        .map(accountMapper::toResponse);
  }

  private Completable validate(Account account, MovementType type, BigDecimal amount) {
    LocalDate today = LocalDate.now();
    if (account.getMovementDay() != null && today.getDayOfMonth() != account.getMovementDay()) {
      return Completable.error(new BusinessRuleException("MOVEMENT_DAY_NOT_ALLOWED",
          "Movements are only allowed on day " + account.getMovementDay() + " of the month"));
    }
    if (type == MovementType.WITHDRAWAL && account.getBalance().compareTo(amount) < 0) {
      return Completable.error(new BusinessRuleException("INSUFFICIENT_FUNDS",
          "Insufficient funds"));
    }
    if (account.getMonthlyMovementLimit() == null) {
      return Completable.complete();
    }
    LocalDateTime from = today.withDayOfMonth(1).atStartOfDay();
    return movementRepository.countByAccountIdAndDateBetween(account.getId(), from,
            from.plusMonths(1))
        .flatMapCompletable(count -> count >= account.getMonthlyMovementLimit()
            ? Completable.error(new BusinessRuleException("MOVEMENT_LIMIT_REACHED",
                "Monthly movement limit reached"))
            : Completable.complete());
  }

  private Single<Movement> apply(Account account, MovementType type, BigDecimal amount) {
    BigDecimal balance = type == MovementType.DEPOSIT
        ? account.getBalance().add(amount)
        : account.getBalance().subtract(amount);
    account.setBalance(balance);
    return accountRepository.save(account)
        .flatMap(saved -> movementRepository.save(Movement.builder()
            .accountId(saved.getId())
            .type(type)
            .amount(amount)
            .balance(balance)
            .date(LocalDateTime.now())
            .build()));
  }
}
