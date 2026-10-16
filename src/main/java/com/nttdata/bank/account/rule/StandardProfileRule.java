package com.nttdata.bank.account.rule;

import com.nttdata.bank.account.client.dto.CustomerDto;
import com.nttdata.bank.account.dto.AccountRequest;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.CustomerProfile;
import io.reactivex.rxjava3.core.Completable;
import org.springframework.stereotype.Component;

/** Standard customers have no extra rules. */
@Component
public class StandardProfileRule implements ProfileRule {

  /** Profile handled by this rule. */
  @Override
  public CustomerProfile profile() {
    return CustomerProfile.STANDARD;
  }

  /** Nothing to validate for standard customers. */
  @Override
  public Completable validate(CustomerDto customer, AccountRequest request) {
    return Completable.complete();
  }

  /** Nothing to change for standard customers. */
  @Override
  public void apply(Account account) {
  }
}
