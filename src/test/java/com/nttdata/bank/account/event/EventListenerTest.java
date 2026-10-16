package com.nttdata.bank.account.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nttdata.bank.account.model.CustomerDebt;
import com.nttdata.bank.account.repository.CustomerDebtRepository;
import com.nttdata.bank.account.service.WalletService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventListenerTest {

  @Mock
  private CustomerDebtRepository customerDebtRepository;

  @Mock
  private WalletService walletService;

  @Mock
  private EventPublisher eventPublisher;

  private EventListener eventListener;

  @BeforeEach
  void setUp() {
    eventListener = new EventListener(customerDebtRepository, walletService, eventPublisher,
        new ObjectMapper());
    ReflectionTestUtils.setField(eventListener, "walletLinkResultTopic", "wallet-link-result");
    ReflectionTestUtils.setField(eventListener, "walletPaymentResultTopic",
        "wallet-payment-result");
  }

  @Test
  void savesDebtStatus() {
    CustomerDebt debt = new CustomerDebt("1", true);
    when(customerDebtRepository.save(debt)).thenReturn(Single.just(debt));

    eventListener.onDebtStatus("{\"customerId\":\"1\",\"overdue\":true}");

    verify(customerDebtRepository).save(debt);
  }

  @Test
  void answersWalletLink() {
    WalletLinkResultEvent result = new WalletLinkResultEvent("w1", "5000", true, null);
    when(walletService.link(new WalletLinkEvent("w1", "5000"))).thenReturn(Single.just(result));
    when(eventPublisher.publish("wallet-link-result", "w1", result))
        .thenReturn(Completable.complete());

    eventListener.onWalletLink("{\"walletId\":\"w1\",\"cardNumber\":\"5000\"}");

    verify(eventPublisher).publish("wallet-link-result", "w1", result);
  }

  @Test
  void answersWalletPayment() {
    WalletPaymentResultEvent result = new WalletPaymentResultEvent("p1", true, null);
    when(walletService.pay(any(WalletPaymentEvent.class))).thenReturn(Single.just(result));
    when(eventPublisher.publish(eq("wallet-payment-result"), eq("p1"), any()))
        .thenReturn(Completable.complete());

    eventListener.onWalletPayment(
        "{\"paymentId\":\"p1\",\"amount\":10,\"fromCardNumber\":\"5000\",\"toCardNumber\":null}");

    verify(eventPublisher).publish("wallet-payment-result", "p1", result);
  }
}
