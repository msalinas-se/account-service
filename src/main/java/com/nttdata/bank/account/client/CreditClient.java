package com.nttdata.bank.account.client;

import com.nttdata.bank.account.exception.ServiceUnavailableException;
import io.reactivex.rxjava3.core.Single;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Client of the credit microservice protected by a circuit breaker. */
@Component
@RequiredArgsConstructor
public class CreditClient {
  private final WebClient creditWebClient;
  private final ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;

  /** Checks if the customer has at least one credit card. */
  public Single<Boolean> hasCreditCard(String customerId) {
    Mono<Boolean> call = creditWebClient.get()
        .uri(uri -> uri.path("/credit-cards").queryParam("customerId", customerId).build())
        .retrieve()
        .bodyToFlux(Map.class)
        .hasElements();
    return Single.fromPublisher(circuitBreakerFactory.create("credit-service")
        .run(call, error -> Mono.error(
            new ServiceUnavailableException("Credit service is not available"))));
  }
}
