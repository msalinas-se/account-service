package com.nttdata.bank.account.repository;

import com.nttdata.bank.account.model.DebitCard;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;

/** Reactive repository for debit cards. */
public interface DebitCardRepository extends RxJava3CrudRepository<DebitCard, String> {

  /** Lists the debit cards of a customer. */
  Flowable<DebitCard> findByCustomerId(String customerId);

  /** Finds a debit card by its number. */
  Maybe<DebitCard> findByCardNumber(String cardNumber);
}
