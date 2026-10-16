package com.nttdata.bank.account.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Overdue debt status of a customer received from the credit microservice. */
@Document("customer_debts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDebt {
  @Id
  private String customerId;
  private boolean overdue;
}
