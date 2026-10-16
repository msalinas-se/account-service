package com.nttdata.bank.account.service;

import com.nttdata.bank.account.event.WalletLinkEvent;
import com.nttdata.bank.account.event.WalletLinkResultEvent;
import com.nttdata.bank.account.event.WalletPaymentEvent;
import com.nttdata.bank.account.event.WalletPaymentResultEvent;
import io.reactivex.rxjava3.core.Single;

/** Handles the requests of the Yanki wallet received by Kafka. */
public interface WalletService {

  /** Validates the debit card that a wallet wants to link. */
  Single<WalletLinkResultEvent> link(WalletLinkEvent event);

  /** Applies a wallet payment on the main accounts of the linked debit cards. */
  Single<WalletPaymentResultEvent> pay(WalletPaymentEvent event);
}
