package com.nttdata.bank.account.dto;

import com.nttdata.bank.account.model.AccountHolder;
import com.nttdata.bank.account.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Payload used to open an account. */
public record AccountRequest(
    @NotBlank String customerId,
    @NotNull AccountType type,
    List<AccountHolder> holders,
    List<AccountHolder> authorizedSigners) {
}
