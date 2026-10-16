package com.nttdata.bank.account.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Deposit or withdrawal stored in the movements collection. */
@Document("movements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movement {
  @Id
  private String id;
  private String accountId;
  private MovementType type;
  private BigDecimal amount;
  private BigDecimal fee;
  private BigDecimal balance;
  private LocalDateTime date;
}
