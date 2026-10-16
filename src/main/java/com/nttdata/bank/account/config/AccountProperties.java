package com.nttdata.bank.account.config;

import com.nttdata.bank.account.model.AccountType;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Account product settings loaded from the config server. */
@ConfigurationProperties("account")
public record AccountProperties(BigDecimal vipMinimumDailyAverage,
    Map<AccountType, ProductRule> products) {

  /** Conditions of an account product. */
  public record ProductRule(BigDecimal maintenanceFee, Integer monthlyMovementLimit,
      Integer movementDay, BigDecimal minimumOpeningAmount, Integer freeTransactions,
      BigDecimal transactionFee) {
  }
}
