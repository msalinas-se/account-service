package com.nttdata.bank.account.event;

/** Result of linking a wallet to a debit card. */
public record WalletLinkResultEvent(String walletId, String cardNumber, boolean success,
    String reason) {
}
