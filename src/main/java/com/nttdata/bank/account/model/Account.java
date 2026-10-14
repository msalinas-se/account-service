package com.nttdata.bank.account.model;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Bank account stored in the accounts collection. */
@Document("accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
  @Id
  private String id;
  private String accountNumber;
  private String customerId;
  private CustomerType customerType;
  private AccountType type;
  private BigDecimal balance;
  private BigDecimal maintenanceFee;
  private Integer monthlyMovementLimit;
  private Integer movementDay;
  private List<AccountHolder> holders;
  private List<AccountHolder> authorizedSigners;
}
