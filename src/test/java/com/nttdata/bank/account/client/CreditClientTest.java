package com.nttdata.bank.account.client;

import com.nttdata.bank.account.config.ApplicationConfig;
import com.nttdata.bank.account.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class CreditClientTest {

  private final ReactiveResilience4JCircuitBreakerFactory factory =
      new ReactiveResilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
          TimeLimiterRegistry.ofDefaults(), new Resilience4JConfigurationProperties());

  private CreditClient client(HttpStatus status, String body) {
    WebClient webClient = WebClient.builder()
        .exchangeFunction(request -> Mono.just(ClientResponse.create(status)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(body)
            .build()))
        .build();
    return new CreditClient(webClient, factory);
  }

  @Test
  void customerWithCreditCard() {
    client(HttpStatus.OK, "[{\"id\":\"cc1\"}]").hasCreditCard("1").test()
        .awaitDone(2, TimeUnit.SECONDS)
        .assertValue(true);
  }

  @Test
  void customerWithoutCreditCard() {
    client(HttpStatus.OK, "[]").hasCreditCard("1").test()
        .awaitDone(2, TimeUnit.SECONDS)
        .assertValue(false);
  }

  @Test
  void creditServiceDown() {
    client(HttpStatus.SERVICE_UNAVAILABLE, "{}").hasCreditCard("1").test()
        .awaitDone(2, TimeUnit.SECONDS)
        .assertError(ServiceUnavailableException.class);
  }

  @Test
  void creditServiceTooSlow() {
    ReactiveResilience4JCircuitBreakerFactory slowFactory =
        new ReactiveResilience4JCircuitBreakerFactory(CircuitBreakerRegistry.ofDefaults(),
            TimeLimiterRegistry.ofDefaults(), new Resilience4JConfigurationProperties());
    new ApplicationConfig().circuitBreakerCustomizer(Duration.ofMillis(200)).customize(slowFactory);
    WebClient webClient = WebClient.builder()
        .exchangeFunction(request -> Mono.delay(Duration.ofSeconds(1))
            .map(tick -> ClientResponse.create(HttpStatus.OK).body("[]").build()))
        .build();

    new CreditClient(webClient, slowFactory).hasCreditCard("1").test()
        .awaitDone(2, TimeUnit.SECONDS)
        .assertError(ServiceUnavailableException.class);
  }
}
