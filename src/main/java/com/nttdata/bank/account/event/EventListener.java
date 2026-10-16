package com.nttdata.bank.account.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nttdata.bank.account.model.CustomerDebt;
import com.nttdata.bank.account.repository.CustomerDebtRepository;
import com.nttdata.bank.account.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes the events of the credit and Yanki microservices. */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventListener {
  private final CustomerDebtRepository customerDebtRepository;
  private final WalletService walletService;
  private final EventPublisher eventPublisher;
  private final ObjectMapper objectMapper;

  @Value("${topics.wallet-link-result}")
  private String walletLinkResultTopic;

  @Value("${topics.wallet-payment-result}")
  private String walletPaymentResultTopic;

  /** Saves the overdue debt status of a customer. */
  @SneakyThrows
  @KafkaListener(topics = "${topics.debt-status}", groupId = "account-service")
  public void onDebtStatus(String message) {
    DebtStatusEvent event = objectMapper.readValue(message, DebtStatusEvent.class);
    customerDebtRepository.save(new CustomerDebt(event.customerId(), event.overdue()))
        .blockingSubscribe(debt -> log.info("Debt status updated: {}", event));
  }

  /** Answers a request to link a wallet to a debit card. */
  @SneakyThrows
  @KafkaListener(topics = "${topics.wallet-link}", groupId = "account-service")
  public void onWalletLink(String message) {
    WalletLinkEvent event = objectMapper.readValue(message, WalletLinkEvent.class);
    walletService.link(event)
        .flatMapCompletable(result ->
            eventPublisher.publish(walletLinkResultTopic, result.walletId(), result))
        .blockingAwait();
  }

  /** Applies a wallet payment and sends the result. */
  @SneakyThrows
  @KafkaListener(topics = "${topics.wallet-payment}", groupId = "account-service")
  public void onWalletPayment(String message) {
    WalletPaymentEvent event = objectMapper.readValue(message, WalletPaymentEvent.class);
    walletService.pay(event)
        .flatMapCompletable(result ->
            eventPublisher.publish(walletPaymentResultTopic, result.paymentId(), result))
        .blockingAwait();
  }
}
