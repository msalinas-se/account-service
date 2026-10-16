package com.nttdata.bank.account.repository;

import com.nttdata.bank.account.model.Movement;
import com.nttdata.bank.account.model.MovementType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;

/** Reactive repository for account movements. */
public interface MovementRepository extends RxJava3CrudRepository<Movement, String> {

  /** Lists the movements of an account. */
  Flowable<Movement> findByAccountIdOrderByDateDesc(String accountId);

  /** Lists the last 10 movements of an account. */
  Flowable<Movement> findTop10ByAccountIdOrderByDateDesc(String accountId);

  /** Lists the movements registered between two dates. */
  Flowable<Movement> findByDateBetween(LocalDateTime from, LocalDateTime to);

  /** Counts the movements of some types of an account between two dates. */
  Single<Long> countByAccountIdAndTypeInAndDateBetween(String accountId,
      Collection<MovementType> types, LocalDateTime from, LocalDateTime to);

  /** Deletes the movements of an account. */
  Single<Long> deleteByAccountId(String accountId);
}
