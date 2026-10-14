package com.nttdata.bank.account.repository;

import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.AccountType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;

/** Reactive repository for accounts. */
public interface AccountRepository extends RxJava3CrudRepository<Account, String> {

  /** Lists the accounts of a customer. */
  Flowable<Account> findByCustomerId(String customerId);

  /** Counts the accounts of a type owned by a customer. */
  Single<Long> countByCustomerIdAndType(String customerId, AccountType type);
}
