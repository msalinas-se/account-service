package com.nttdata.bank.account.rule;

import com.nttdata.bank.account.client.CreditClient;
import com.nttdata.bank.account.client.dto.CustomerDto;
import com.nttdata.bank.account.config.AccountProperties;
import com.nttdata.bank.account.dto.AccountRequest;
import com.nttdata.bank.account.exception.BusinessRuleException;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.AccountType;
import com.nttdata.bank.account.model.CustomerProfile;
import io.reactivex.rxjava3.core.Completable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** VIP savings accounts need a credit card and a minimum daily average. */
@Component
@RequiredArgsConstructor
public class VipProfileRule implements ProfileRule {
  private final CreditClient creditClient;
  private final AccountProperties accountProperties;

  /** Profile handled by this rule. */
  @Override
  public CustomerProfile profile() {
    return CustomerProfile.VIP;
  }

  /** Validates the credit card and the minimum amount for savings accounts. */
  @Override
  public Completable validate(CustomerDto customer, AccountRequest request) {
    if (request.type() != AccountType.SAVINGS) {
      return Completable.complete();
    }
    if (request.openingAmountOrZero().compareTo(accountProperties.vipMinimumDailyAverage()) < 0) {
      return Completable.error(new BusinessRuleException("MINIMUM_AVERAGE_NOT_MET",
          "VIP savings accounts need at least " + accountProperties.vipMinimumDailyAverage()));
    }
    return creditClient.hasCreditCard(customer.id())
        .flatMapCompletable(hasCard -> hasCard
            ? Completable.complete()
            : Completable.error(new BusinessRuleException("CREDIT_CARD_REQUIRED",
                "VIP customers need a credit card to open a savings account")));
  }

  /** Sets the minimum daily average on savings accounts. */
  @Override
  public void apply(Account account) {
    if (account.getType() == AccountType.SAVINGS) {
      account.setMinimumDailyAverage(accountProperties.vipMinimumDailyAverage());
    }
  }
}
