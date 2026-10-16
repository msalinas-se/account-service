package com.nttdata.bank.account.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Debit card associated to the accounts of a customer, the main account goes first. */
@Document("debit_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitCard {
  @Id
  private String id;
  private String cardNumber;
  private String customerId;
  private String mainAccountId;
  private List<String> accountIds;
}
