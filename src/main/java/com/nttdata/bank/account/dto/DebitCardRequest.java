package com.nttdata.bank.account.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Payload used to create or update a debit card. */
public record DebitCardRequest(@NotBlank String customerId, @NotBlank String mainAccountId,
    List<String> accountIds) {
}
