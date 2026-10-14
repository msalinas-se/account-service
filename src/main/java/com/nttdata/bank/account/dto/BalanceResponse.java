package com.nttdata.bank.account.dto;

import java.math.BigDecimal;

/** Available balance of an account. */
public record BalanceResponse(String accountId, String accountNumber, BigDecimal balance) {
}
