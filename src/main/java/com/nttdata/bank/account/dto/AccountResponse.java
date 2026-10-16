package com.nttdata.bank.account.dto;

import com.nttdata.bank.account.model.AccountHolder;
import com.nttdata.bank.account.model.AccountType;
import com.nttdata.bank.account.model.CustomerType;
import java.math.BigDecimal;
import java.util.List;

/** Account representation returned by the API. */
public record AccountResponse(
    String id,
    String accountNumber,
    String customerId,
    CustomerType customerType,
    AccountType type,
    BigDecimal balance,
    BigDecimal maintenanceFee,
    Integer monthlyMovementLimit,
    Integer movementDay,
    Integer freeTransactions,
    BigDecimal transactionFee,
    BigDecimal minimumDailyAverage,
    List<AccountHolder> holders,
    List<AccountHolder> authorizedSigners) {
}
