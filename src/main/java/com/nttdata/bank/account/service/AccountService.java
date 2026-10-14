package com.nttdata.bank.account.service;

import com.nttdata.bank.account.dto.AccountRequest;
import com.nttdata.bank.account.dto.AccountResponse;
import com.nttdata.bank.account.dto.AccountUpdateRequest;
import com.nttdata.bank.account.dto.BalanceResponse;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** Operations over bank accounts. */
public interface AccountService {

  /** Opens a new account. */
  Single<AccountResponse> create(AccountRequest request);

  /** Lists accounts, optionally by customer. */
  Flowable<AccountResponse> findAll(String customerId);

  /** Finds an account by its identifier. */
  Single<AccountResponse> findById(String id);

  /** Updates the holders and signers of an account. */
  Single<AccountResponse> update(String id, AccountUpdateRequest request);

  /** Deletes an account. */
  Completable delete(String id);

  /** Returns the available balance of an account. */
  Single<BalanceResponse> getBalance(String id);
}
