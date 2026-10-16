package com.nttdata.bank.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nttdata.bank.account.event.WalletLinkEvent;
import com.nttdata.bank.account.event.WalletPaymentEvent;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.DebitCard;
import com.nttdata.bank.account.model.Movement;
import com.nttdata.bank.account.repository.AccountRepository;
import com.nttdata.bank.account.repository.DebitCardRepository;
import com.nttdata.bank.account.repository.MovementRepository;
import com.nttdata.bank.account.service.impl.WalletServiceImpl;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

  @Mock
  private DebitCardRepository debitCardRepository;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private MovementRepository movementRepository;

  @InjectMocks
  private WalletServiceImpl walletService;

  private void card(String number, String accountId, Account account) {
    when(debitCardRepository.findByCardNumber(number)).thenReturn(Maybe.just(
        DebitCard.builder().cardNumber(number).mainAccountId(accountId).build()));
    when(accountRepository.findById(accountId)).thenReturn(Maybe.just(account));
  }

  private void saveAll() {
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
    when(movementRepository.save(any(Movement.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
  }

  @Test
  void linkExistingCard() {
    when(debitCardRepository.findByCardNumber("5000"))
        .thenReturn(Maybe.just(DebitCard.builder().cardNumber("5000").build()));

    walletService.link(new WalletLinkEvent("w1", "5000")).test()
        .assertValue(result -> result.success() && result.walletId().equals("w1"));
  }

  @Test
  void linkUnknownCard() {
    when(debitCardRepository.findByCardNumber("9999")).thenReturn(Maybe.empty());

    walletService.link(new WalletLinkEvent("w1", "9999")).test()
        .assertValue(result -> !result.success());
  }

  @Test
  void paymentBetweenLinkedCards() {
    Account from = Account.builder().id("a1").balance(new BigDecimal("100")).build();
    Account to = Account.builder().id("a2").balance(new BigDecimal("5")).build();
    card("5001", "a1", from);
    card("5002", "a2", to);
    saveAll();

    walletService.pay(new WalletPaymentEvent("p1", new BigDecimal("40"), "5001", "5002")).test()
        .assertValue(result -> result.success());
    assertEquals(60, from.getBalance().intValue());
    assertEquals(45, to.getBalance().intValue());
  }

  @Test
  void paymentToWalletWithoutCard() {
    Account from = Account.builder().id("a1").balance(new BigDecimal("100")).build();
    card("5001", "a1", from);
    saveAll();

    walletService.pay(new WalletPaymentEvent("p1", new BigDecimal("40"), "5001", null)).test()
        .assertValue(result -> result.success());
    assertEquals(60, from.getBalance().intValue());
  }

  @Test
  void paymentWithoutFunds() {
    card("5001", "a1", Account.builder().id("a1").balance(BigDecimal.ONE).build());

    walletService.pay(new WalletPaymentEvent("p1", BigDecimal.TEN, "5001", null)).test()
        .assertValue(result -> !result.success());
    verify(accountRepository, never()).save(any());
  }

  @Test
  void paymentWithUnknownCard() {
    when(debitCardRepository.findByCardNumber("9999")).thenReturn(Maybe.empty());

    walletService.pay(new WalletPaymentEvent("p1", BigDecimal.TEN, null, "9999")).test()
        .assertValue(result -> !result.success());
  }
}
