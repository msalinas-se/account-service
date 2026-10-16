package com.nttdata.bank.account.repository;

import com.nttdata.bank.account.model.CustomerDebt;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;

/** Reactive repository for the debt status of the customers. */
public interface CustomerDebtRepository extends RxJava3CrudRepository<CustomerDebt, String> {
}
