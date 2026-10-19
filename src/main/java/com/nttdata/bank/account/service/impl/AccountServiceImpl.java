package com.nttdata.bank.account.service.impl;

import com.nttdata.bank.account.client.CustomerClient;
import com.nttdata.bank.account.client.dto.CustomerDto;
import com.nttdata.bank.account.config.AccountProperties;
import com.nttdata.bank.account.dto.AccountRequest;
import com.nttdata.bank.account.dto.AccountResponse;
import com.nttdata.bank.account.dto.AccountUpdateRequest;
import com.nttdata.bank.account.dto.BalanceResponse;
import com.nttdata.bank.account.exception.BusinessRuleException;
import com.nttdata.bank.account.exception.ResourceNotFoundException;
import com.nttdata.bank.account.mapper.AccountMapper;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.AccountHolder;
import com.nttdata.bank.account.model.AccountType;
import com.nttdata.bank.account.model.CustomerType;
import com.nttdata.bank.account.repository.AccountRepository;
import com.nttdata.bank.account.repository.CustomerDebtRepository;
import com.nttdata.bank.account.repository.MovementRepository;
import com.nttdata.bank.account.rule.ProfileRule;
import com.nttdata.bank.account.rule.ProfileRuleFactory;
import com.nttdata.bank.account.service.AccountService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Implementation of AccountService backed by MongoDB. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
  private final AccountRepository accountRepository;
  private final MovementRepository movementRepository;
  private final CustomerClient customerClient;
  private final AccountProperties accountProperties;
  private final AccountMapper accountMapper;
  private final ProfileRuleFactory profileRuleFactory;
  private final CustomerDebtRepository customerDebtRepository;

  /** Opens a new account. */
  @Override
  public Single<AccountResponse> create(AccountRequest request) {
    return customerClient.findById(request.customerId())
        .flatMap(customer -> {
          ProfileRule profileRule = profileRuleFactory.get(customer.profile());
          return validateNoOverdueDebt(customer.id())
              .andThen(Completable.defer(() -> validate(customer, request)))
              .andThen(Completable.defer(() -> profileRule.validate(customer, request)))
              .andThen(Single.defer(() -> {
                Account account = newAccount(customer, request);
                profileRule.apply(account);
                return accountRepository.save(account);
              }));
        })
        .doOnSuccess(account -> log.info("Account created: {}", account.getAccountNumber()))
        .map(accountMapper::toResponse);
  }

  /** Lists accounts, optionally by customer. */
  @Override
  public Flowable<AccountResponse> findAll(String customerId) {
    Flowable<Account> accounts = customerId == null
        ? accountRepository.findAll()
        : accountRepository.findByCustomerId(customerId);
    return accounts.map(accountMapper::toResponse);
  }

  /** Finds an account by its identifier. */
  @Override
  public Single<AccountResponse> findById(String id) {
    return findAccount(id).map(accountMapper::toResponse);
  }

  /** Updates the holders and signers of an account. */
  @Override
  public Single<AccountResponse> update(String id, AccountUpdateRequest request) {
    return findAccount(id)
        .flatMap(account -> {
          if (request.holders() != null) {
            account.setHolders(request.holders());
          }
          if (request.authorizedSigners() != null) {
            account.setAuthorizedSigners(request.authorizedSigners());
          }
          return accountRepository.save(account);
        })
        .map(accountMapper::toResponse);
  }

  /** Deletes an account. */
  @Override
  public Completable delete(String id) {
    return findAccount(id)
        .flatMapCompletable(account -> movementRepository.deleteByAccountId(id).ignoreElement()
            .andThen(accountRepository.delete(account)))
        .doOnComplete(() -> log.info("Account deleted: {}", id));
  }

  /** Returns the available balance of an account. */
  @Override
  public Single<BalanceResponse> getBalance(String id) {
    return findAccount(id).map(accountMapper::toBalance);
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

  /** Validates the opening amount and the account rules of the customer type. */
  private Completable validate(CustomerDto customer, AccountRequest request) {
    BigDecimal minimum = accountProperties.products().get(request.type()).minimumOpeningAmount();
    if (minimum != null && request.openingAmountOrZero().compareTo(minimum) < 0) {
      return Completable.error(new BusinessRuleException("MINIMUM_OPENING_AMOUNT",
          "The minimum opening amount for " + request.type() + " is " + minimum));
    }
    if (customer.type() == CustomerType.BUSINESS) {
      if (request.type() != AccountType.CHECKING) {
        return Completable.error(new BusinessRuleException("ACCOUNT_TYPE_NOT_ALLOWED",
            "Business customers can only have checking accounts"));
      }
      if (request.holders() == null || request.holders().isEmpty()) {
        return Completable.error(new BusinessRuleException("HOLDER_REQUIRED",
            "Business accounts need at least one holder"));
      }
      return Completable.complete();
    }
    if (request.type() == AccountType.FIXED_TERM) {
      return Completable.complete();
    }
    return accountRepository.countByCustomerIdAndType(customer.id(), request.type())
        .flatMapCompletable(count -> count > 0
            ? Completable.error(new BusinessRuleException("ACCOUNT_LIMIT_REACHED",
                "Personal customers can only have one " + request.type() + " account"))
            : Completable.complete());
  }

  /** Builds the account with the conditions of its product. */
  private Account newAccount(CustomerDto customer, AccountRequest request) {
    AccountProperties.ProductRule rule = accountProperties.products().get(request.type());
    boolean business = customer.type() == CustomerType.BUSINESS;
    return Account.builder()
        .accountNumber(String.valueOf(
            ThreadLocalRandom.current().nextLong(10_000_000_000_000L, 99_999_999_999_999L)))
        .customerId(customer.id())
        .customerType(customer.type())
        .type(request.type())
        .balance(request.openingAmountOrZero())
        .maintenanceFee(rule.maintenanceFee())
        .monthlyMovementLimit(rule.monthlyMovementLimit())
        .movementDay(rule.movementDay())
        .freeTransactions(rule.freeTransactions())
        .transactionFee(rule.transactionFee())
        .holders(business
            ? request.holders()
            : List.of(new AccountHolder(customer.documentNumber(), customer.name())))
        .authorizedSigners(business && request.authorizedSigners() != null
            ? request.authorizedSigners()
            : List.of())
        .build();
  }

  /** Finds an account or fails if it does not exist. */
  private Single<Account> findAccount(String id) {
    return accountRepository.findById(id)
        .switchIfEmpty(Single.error(new ResourceNotFoundException("Account not found: " + id)));
  }
}
