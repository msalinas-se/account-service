package com.nttdata.bank.account.service;

import com.nttdata.bank.account.dto.MovementRequest;
import com.nttdata.bank.account.dto.MovementResponse;
import com.nttdata.bank.account.dto.TransferRequest;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** Deposits, withdrawals, transfers and movements of bank accounts. */
public interface MovementService {

  /** Deposits money into an account. */
  Single<MovementResponse> deposit(String accountId, MovementRequest request);

  /** Withdraws money from an account. */
  Single<MovementResponse> withdraw(String accountId, MovementRequest request);

  /** Transfers money between two accounts of the bank. */
  Single<MovementResponse> transfer(TransferRequest request);

  /** Lists the movements of an account. */
  Flowable<MovementResponse> findByAccount(String accountId);
}
