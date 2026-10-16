package com.nttdata.bank.account.event;

import java.math.BigDecimal;

/** Yanki payment that must be applied on the main accounts of the linked debit cards. */
public record WalletPaymentEvent(String paymentId, BigDecimal amount, String fromCardNumber,
    String toCardNumber) {
}
