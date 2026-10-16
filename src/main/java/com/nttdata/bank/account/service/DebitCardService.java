package com.nttdata.bank.account.service;

import com.nttdata.bank.account.dto.BalanceResponse;
import com.nttdata.bank.account.dto.DebitCardRequest;
import com.nttdata.bank.account.dto.MovementRequest;
import com.nttdata.bank.account.dto.MovementResponse;
import com.nttdata.bank.account.model.DebitCard;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** Operations over debit cards. */
public interface DebitCardService {

  /** Creates a debit card associated to accounts of the customer. */
  Single<DebitCard> create(DebitCardRequest request);

  /** Lists debit cards, optionally by customer. */
  Flowable<DebitCard> findAll(String customerId);

  /** Finds a debit card by its identifier. */
  Single<DebitCard> findById(String id);

  /** Updates the accounts associated to a debit card. */
  Single<DebitCard> update(String id, DebitCardRequest request);

  /** Deletes a debit card. */
  Completable delete(String id);

  /** Pays with the debit card, from the main account or the next one with enough balance. */
  Single<MovementResponse> pay(String id, MovementRequest request);

  /** Balance of the main account of the debit card. */
  Single<BalanceResponse> getBalance(String id);

  /** Last 10 movements of the main account of the debit card. */
  Flowable<MovementResponse> lastMovements(String id);
}
