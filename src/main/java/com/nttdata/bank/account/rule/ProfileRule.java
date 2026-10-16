package com.nttdata.bank.account.rule;

import com.nttdata.bank.account.client.dto.CustomerDto;
import com.nttdata.bank.account.dto.AccountRequest;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.CustomerProfile;
import io.reactivex.rxjava3.core.Completable;

/** Strategy with the extra account rules of a customer profile. */
public interface ProfileRule {

  /** Profile handled by this rule. */
  CustomerProfile profile();

  /** Validates the profile requirements before opening the account. */
  Completable validate(CustomerDto customer, AccountRequest request);

  /** Applies the profile conditions to the new account. */
  void apply(Account account);
}
