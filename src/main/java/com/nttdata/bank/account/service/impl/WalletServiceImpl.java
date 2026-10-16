package com.nttdata.bank.account.service.impl;

import com.nttdata.bank.account.event.WalletLinkEvent;
import com.nttdata.bank.account.event.WalletLinkResultEvent;
import com.nttdata.bank.account.event.WalletPaymentEvent;
import com.nttdata.bank.account.event.WalletPaymentResultEvent;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.Movement;
import com.nttdata.bank.account.model.MovementType;
import com.nttdata.bank.account.repository.AccountRepository;
import com.nttdata.bank.account.repository.DebitCardRepository;
import com.nttdata.bank.account.repository.MovementRepository;
import com.nttdata.bank.account.service.WalletService;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Implementation of WalletService. */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {
  private final DebitCardRepository debitCardRepository;
  private final AccountRepository accountRepository;
  private final MovementRepository movementRepository;

  /** Validates the debit card that a wallet wants to link. */
  @Override
  public Single<WalletLinkResultEvent> link(WalletLinkEvent event) {
    return debitCardRepository.findByCardNumber(event.cardNumber())
        .map(card -> new WalletLinkResultEvent(event.walletId(), event.cardNumber(), true, null))
        .defaultIfEmpty(new WalletLinkResultEvent(event.walletId(), event.cardNumber(), false,
            "Debit card not found"));
  }

  /** Applies a wallet payment on the main accounts of the linked debit cards. */
  @Override
  public Single<WalletPaymentResultEvent> pay(WalletPaymentEvent event) {
    Single<Optional<Account>> from = mainAccount(event.fromCardNumber());
    Single<Optional<Account>> to = mainAccount(event.toCardNumber());
    return Single.zip(from, to, (origin, destination) -> {
      if (event.fromCardNumber() != null && origin.isEmpty()
          || event.toCardNumber() != null && destination.isEmpty()) {
        return Single.just(result(event, false, "Debit card not found"));
      }
      if (origin.isPresent() && origin.get().getBalance().compareTo(event.amount()) < 0) {
        return Single.just(result(event, false, "Insufficient funds"));
      }
      return apply(origin, event.amount().negate(), MovementType.WALLET_OUT)
          .flatMap(done -> apply(destination, event.amount(), MovementType.WALLET_IN))
          .map(done -> result(event, true, null));
    }).flatMap(single -> single);
  }

  private Single<Optional<Account>> mainAccount(String cardNumber) {
    if (cardNumber == null) {
      return Single.just(Optional.empty());
    }
    return debitCardRepository.findByCardNumber(cardNumber)
        .flatMap(card -> accountRepository.findById(card.getMainAccountId()))
        .map(Optional::of)
        .defaultIfEmpty(Optional.empty());
  }

  private Single<Boolean> apply(Optional<Account> account, BigDecimal amount, MovementType type) {
    if (account.isEmpty()) {
      return Single.just(true);
    }
    Account target = account.get();
    target.setBalance(target.getBalance().add(amount));
    return accountRepository.save(target)
        .flatMap(saved -> movementRepository.save(Movement.builder()
            .accountId(saved.getId())
            .type(type)
            .amount(amount.abs())
            .fee(BigDecimal.ZERO)
            .balance(saved.getBalance())
            .date(LocalDateTime.now())
            .build()))
        .map(movement -> true);
  }

  private WalletPaymentResultEvent result(WalletPaymentEvent event, boolean success,
      String reason) {
    log.info("Wallet payment {} processed: {}", event.paymentId(), success);
    return new WalletPaymentResultEvent(event.paymentId(), success, reason);
  }
}
