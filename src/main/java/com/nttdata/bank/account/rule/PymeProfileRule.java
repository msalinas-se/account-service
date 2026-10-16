package com.nttdata.bank.account.rule;

import com.nttdata.bank.account.client.CreditClient;
import com.nttdata.bank.account.client.dto.CustomerDto;
import com.nttdata.bank.account.dto.AccountRequest;
import com.nttdata.bank.account.exception.BusinessRuleException;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.AccountType;
import com.nttdata.bank.account.model.CustomerProfile;
import io.reactivex.rxjava3.core.Completable;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** PYME checking accounts need a credit card and have no maintenance fee. */
@Component
@RequiredArgsConstructor
public class PymeProfileRule implements ProfileRule {
  private final CreditClient creditClient;

  /** Profile handled by this rule. */
  @Override
  public CustomerProfile profile() {
    return CustomerProfile.PYME;
  }

  /** Validates that the customer has a credit card for checking accounts. */
  @Override
  public Completable validate(CustomerDto customer, AccountRequest request) {
    if (request.type() != AccountType.CHECKING) {
      return Completable.complete();
    }
    return creditClient.hasCreditCard(customer.id())
        .flatMapCompletable(hasCard -> hasCard
            ? Completable.complete()
            : Completable.error(new BusinessRuleException("CREDIT_CARD_REQUIRED",
                "PYME customers need a credit card to open a checking account")));
  }

  /** Removes the maintenance fee of checking accounts. */
  @Override
  public void apply(Account account) {
    if (account.getType() == AccountType.CHECKING) {
      account.setMaintenanceFee(BigDecimal.ZERO);
    }
  }
}
