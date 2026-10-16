package com.nttdata.bank.account.service.impl;

import com.nttdata.bank.account.dto.MovementRequest;
import com.nttdata.bank.account.dto.MovementResponse;
import com.nttdata.bank.account.dto.TransferRequest;
import com.nttdata.bank.account.exception.BusinessRuleException;
import com.nttdata.bank.account.exception.ResourceNotFoundException;
import com.nttdata.bank.account.mapper.AccountMapper;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.Movement;
import com.nttdata.bank.account.model.MovementType;
import com.nttdata.bank.account.repository.AccountRepository;
import com.nttdata.bank.account.repository.MovementRepository;
import com.nttdata.bank.account.service.MovementService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Implementation of MovementService. */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovementServiceImpl implements MovementService {
  private static final List<MovementType> TRANSACTIONS =
      List.of(MovementType.DEPOSIT, MovementType.WITHDRAWAL);

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

  /** Transfers money between two accounts of the bank. */
  @Override
  public Single<MovementResponse> transfer(TransferRequest request) {
    if (request.fromAccountId().equals(request.toAccountId())) {
      return Single.error(new BusinessRuleException("SAME_ACCOUNT",
          "Origin and destination accounts must be different"));
    }
    return Single.zip(findAccount(request.fromAccountId()), findAccount(request.toAccountId()),
            (from, to) -> List.of(from, to))
        .flatMap(accounts -> {
          Account from = accounts.get(0);
          Account to = accounts.get(1);
          if (from.getBalance().compareTo(request.amount()) < 0) {
            return Single.error(new BusinessRuleException("INSUFFICIENT_FUNDS",
                "Insufficient funds"));
          }
          from.setBalance(from.getBalance().subtract(request.amount()));
          to.setBalance(to.getBalance().add(request.amount()));
          return accountRepository.save(to)
              .flatMap(saved -> save(to, MovementType.TRANSFER_IN, request.amount(),
                  BigDecimal.ZERO))
              .flatMap(in -> accountRepository.save(from))
              .flatMap(saved -> save(from, MovementType.TRANSFER_OUT, request.amount(),
                  BigDecimal.ZERO));
        })
        .doOnSuccess(movement -> log.info("Transfer of {} from {} to {}", request.amount(),
            request.fromAccountId(), request.toAccountId()))
        .map(accountMapper::toResponse);
  }

  /** Lists the movements of an account. */
  @Override
  public Flowable<MovementResponse> findByAccount(String accountId) {
    return movementRepository.findByAccountIdOrderByDateDesc(accountId)
        .map(accountMapper::toResponse);
  }

  private Single<MovementResponse> register(String accountId, MovementType type,
      BigDecimal amount) {
    return findAccount(accountId)
        .flatMap(account -> process(account, type, amount))
        .doOnSuccess(movement -> log.info("{} of {} on account {}", type, amount, accountId))
        .map(accountMapper::toResponse);
  }

  private Single<Movement> process(Account account, MovementType type, BigDecimal amount) {
    LocalDate today = LocalDate.now();
    if (account.getMovementDay() != null && today.getDayOfMonth() != account.getMovementDay()) {
      return Single.error(new BusinessRuleException("MOVEMENT_DAY_NOT_ALLOWED",
          "Movements are only allowed on day " + account.getMovementDay() + " of the month"));
    }
    LocalDateTime from = today.withDayOfMonth(1).atStartOfDay();
    return movementRepository.countByAccountIdAndTypeInAndDateBetween(account.getId(),
            TRANSACTIONS, from, from.plusMonths(1))
        .flatMap(count -> {
          if (account.getMonthlyMovementLimit() != null
              && count >= account.getMonthlyMovementLimit()) {
            return Single.error(new BusinessRuleException("MOVEMENT_LIMIT_REACHED",
                "Monthly movement limit reached"));
          }
          BigDecimal fee = account.getFreeTransactions() != null
              && count >= account.getFreeTransactions()
              ? account.getTransactionFee() : BigDecimal.ZERO;
          BigDecimal balance = type == MovementType.DEPOSIT
              ? account.getBalance().add(amount).subtract(fee)
              : account.getBalance().subtract(amount).subtract(fee);
          if (balance.signum() < 0) {
            return Single.error(new BusinessRuleException("INSUFFICIENT_FUNDS",
                "Insufficient funds"));
          }
          account.setBalance(balance);
          return accountRepository.save(account)
              .flatMap(saved -> save(saved, type, amount, fee));
        });
  }

  private Single<Movement> save(Account account, MovementType type, BigDecimal amount,
      BigDecimal fee) {
    return movementRepository.save(Movement.builder()
        .accountId(account.getId())
        .type(type)
        .amount(amount)
        .fee(fee)
        .balance(account.getBalance())
        .date(LocalDateTime.now())
        .build());
  }

  private Single<Account> findAccount(String id) {
    return accountRepository.findById(id)
        .switchIfEmpty(Single.error(new ResourceNotFoundException("Account not found: " + id)));
  }
}
