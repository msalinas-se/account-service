package com.nttdata.bank.account.service;

import com.nttdata.bank.account.dto.MovementRequest;
import com.nttdata.bank.account.dto.MovementResponse;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** Deposits, withdrawals and movements of bank accounts. */
public interface MovementService {

  /** Deposits money into an account. */
  Single<MovementResponse> deposit(String accountId, MovementRequest request);

  /** Withdraws money from an account. */
  Single<MovementResponse> withdraw(String accountId, MovementRequest request);

  /** Lists the movements of an account. */
  Flowable<MovementResponse> findByAccount(String accountId);
}
