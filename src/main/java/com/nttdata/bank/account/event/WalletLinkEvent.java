package com.nttdata.bank.account.event;

/** Request from Yanki to link a wallet to a debit card. */
public record WalletLinkEvent(String walletId, String cardNumber) {
}
