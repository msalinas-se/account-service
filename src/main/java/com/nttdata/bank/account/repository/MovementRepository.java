package com.nttdata.bank.account.repository;

import com.nttdata.bank.account.model.Movement;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.time.LocalDateTime;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;

/** Reactive repository for account movements. */
public interface MovementRepository extends RxJava3CrudRepository<Movement, String> {

  /** Lists the movements of an account. */
  Flowable<Movement> findByAccountIdOrderByDateDesc(String accountId);

  /** Counts the movements of an account between two dates. */
  Single<Long> countByAccountIdAndDateBetween(String accountId, LocalDateTime from,
      LocalDateTime to);

  /** Deletes the movements of an account. */
  Single<Long> deleteByAccountId(String accountId);
}
