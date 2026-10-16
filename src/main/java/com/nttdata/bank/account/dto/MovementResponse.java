package com.nttdata.bank.account.dto;

import com.nttdata.bank.account.model.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Movement representation returned by the API. */
public record MovementResponse(String id, String accountId, MovementType type, BigDecimal amount,
    BigDecimal fee, BigDecimal balance, LocalDateTime date) {
}
