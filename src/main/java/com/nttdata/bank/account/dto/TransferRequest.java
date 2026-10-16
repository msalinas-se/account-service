package com.nttdata.bank.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Payload used to transfer money between two accounts of the bank. */
public record TransferRequest(@NotBlank String fromAccountId, @NotBlank String toAccountId,
    @NotNull @Positive BigDecimal amount) {
}
