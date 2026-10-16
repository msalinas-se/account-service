package com.nttdata.bank.account.event;

/** Result of a Yanki payment. */
public record WalletPaymentResultEvent(String paymentId, boolean success, String reason) {
}
