package com.nttdata.bank.account.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Payload used for deposits and withdrawals. */
public record MovementRequest(@NotNull @Positive BigDecimal amount) {
}
