package com.nttdata.bank.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point of the bank account microservice. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AccountServiceApplication {

  /** Starts the bank account microservice. */
  public static void main(String[] args) {
    SpringApplication.run(AccountServiceApplication.class, args);
  }
}
