package com.nttdata.bank.account.service.impl;

import com.nttdata.bank.account.dto.BalanceResponse;
import com.nttdata.bank.account.dto.DebitCardRequest;
import com.nttdata.bank.account.dto.MovementRequest;
import com.nttdata.bank.account.dto.MovementResponse;
import com.nttdata.bank.account.exception.BusinessRuleException;
import com.nttdata.bank.account.exception.ResourceNotFoundException;
import com.nttdata.bank.account.mapper.AccountMapper;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.DebitCard;
import com.nttdata.bank.account.model.Movement;
import com.nttdata.bank.account.model.MovementType;
import com.nttdata.bank.account.repository.AccountRepository;
import com.nttdata.bank.account.repository.CustomerDebtRepository;
import com.nttdata.bank.account.repository.DebitCardRepository;
import com.nttdata.bank.account.repository.MovementRepository;
import com.nttdata.bank.account.service.DebitCardService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Implementation of DebitCardService. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DebitCardServiceImpl implements DebitCardService {
  private final DebitCardRepository debitCardRepository;
  private final AccountRepository accountRepository;
  private final MovementRepository movementRepository;
  private final AccountMapper accountMapper;
  private final CustomerDebtRepository customerDebtRepository;

  /** Creates a debit card associated to accounts of the customer. */
  @Override
  public Single<DebitCard> create(DebitCardRequest request) {
    List<String> accountIds = orderedAccounts(request);
    return validateNoOverdueDebt(request.customerId())
        .andThen(Completable.defer(() -> validateAccounts(request.customerId(), accountIds)))
        .andThen(Single.defer(() -> debitCardRepository.save(DebitCard.builder()
            .cardNumber(String.valueOf(ThreadLocalRandom.current()
                .nextLong(5_000_000_000_000_000L, 5_999_999_999_999_999L)))
            .customerId(request.customerId())
            .mainAccountId(request.mainAccountId())
            .accountIds(accountIds)
            .build())))
        .doOnSuccess(card -> log.info("Debit card created: {}", card.getCardNumber()));
  }

  /** Lists debit cards, optionally by customer. */
  @Override
  public Flowable<DebitCard> findAll(String customerId) {
    return customerId == null
        ? debitCardRepository.findAll()
        : debitCardRepository.findByCustomerId(customerId);
  }

  /** Finds a debit card by its identifier. */
  @Override
  public Single<DebitCard> findById(String id) {
    return debitCardRepository.findById(id)
        .switchIfEmpty(Single.error(new ResourceNotFoundException("Debit card not found: " + id)));
  }

  /** Updates the accounts associated to a debit card. */
  @Override
  public Single<DebitCard> update(String id, DebitCardRequest request) {
    List<String> accountIds = orderedAccounts(request);
    return findById(id)
        .flatMap(card -> validateAccounts(card.getCustomerId(), accountIds)
            .andThen(Single.defer(() -> {
              card.setMainAccountId(request.mainAccountId());
              card.setAccountIds(accountIds);
              return debitCardRepository.save(card);
            })));
  }

  /** Deletes a debit card. */
  @Override
  public Completable delete(String id) {
    return findById(id).flatMapCompletable(debitCardRepository::delete);
  }

  /** Pays with the debit card, from the main account or the next one with enough balance. */
  @Override
  public Single<MovementResponse> pay(String id, MovementRequest request) {
    BigDecimal amount = request.amount();
    return findById(id)
        .flatMapPublisher(card -> Flowable.fromIterable(card.getAccountIds()))
        .concatMapMaybe(accountRepository::findById)
        .filter(account -> account.getBalance().compareTo(amount) >= 0)
        .firstElement()
        .switchIfEmpty(Single.error(new BusinessRuleException("INSUFFICIENT_FUNDS",
            "No associated account has enough balance")))
        .flatMap(account -> {
          account.setBalance(account.getBalance().subtract(amount));
          return accountRepository.save(account);
        })
        .flatMap(account -> movementRepository.save(Movement.builder()
            .accountId(account.getId())
            .type(MovementType.DEBIT_CARD_PAYMENT)
            .amount(amount)
            .fee(BigDecimal.ZERO)
            .balance(account.getBalance())
            .date(LocalDateTime.now())
            .build()))
        .doOnSuccess(movement -> log.info("Debit card payment of {} from account {}", amount,
            movement.getAccountId()))
        .map(accountMapper::toResponse);
  }

  /** Balance of the main account of the debit card. */
  @Override
  public Single<BalanceResponse> getBalance(String id) {
    return findById(id)
        .flatMap(card -> accountRepository.findById(card.getMainAccountId())
            .switchIfEmpty(Single.error(new ResourceNotFoundException("Main account not found"))))
        .map(accountMapper::toBalance);
  }

  /** Last 10 movements of the main account of the debit card. */
  @Override
  public Flowable<MovementResponse> lastMovements(String id) {
    return findById(id)
        .flatMapPublisher(card ->
            movementRepository.findTop10ByAccountIdOrderByDateDesc(card.getMainAccountId()))
        .map(accountMapper::toResponse);
  }

  /** Returns the account ids with the main account first and no duplicates. */
  private List<String> orderedAccounts(DebitCardRequest request) {
    return Stream.concat(Stream.of(request.mainAccountId()),
            Optional.ofNullable(request.accountIds()).orElse(List.of()).stream())
        .distinct()
        .toList();
  }

  /** Fails if the customer has an overdue debt. */
  private Completable validateNoOverdueDebt(String customerId) {
    return customerDebtRepository.findById(customerId)
        .filter(debt -> debt.isOverdue())
        .isEmpty()
        .flatMapCompletable(noDebt -> noDebt
            ? Completable.complete()
            : Completable.error(new BusinessRuleException("OVERDUE_DEBT",
                "The customer has an overdue debt in a credit product")));
  }

  /** Validates that all the accounts exist and belong to the customer. */
  private Completable validateAccounts(String customerId, List<String> accountIds) {
    return Flowable.fromIterable(accountIds)
        .concatMapSingle(accountId -> accountRepository.findById(accountId)
            .switchIfEmpty(Single.error(
                new ResourceNotFoundException("Account not found: " + accountId))))
        .all(account -> customerId.equals(account.getCustomerId()))
        .flatMapCompletable(valid -> valid
            ? Completable.complete()
            : Completable.error(new BusinessRuleException("ACCOUNT_NOT_OWNED",
                "All the accounts must belong to the customer")));
  }
}
