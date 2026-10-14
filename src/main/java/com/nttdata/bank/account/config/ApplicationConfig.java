package com.nttdata.bank.account.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** Beans of the account microservice. */
@Configuration
public class ApplicationConfig {

  /** Web client for the customer microservice. */
  @Bean
  public WebClient customerWebClient(@Value("${services.customer.url}") String customerUrl) {
    return WebClient.builder().baseUrl(customerUrl).build();
  }
}
