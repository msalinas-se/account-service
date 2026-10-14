package com.nttdata.bank.account.dto;

import com.nttdata.bank.account.model.AccountHolder;
import java.util.List;

/** Payload used to update the holders and signers of an account. */
public record AccountUpdateRequest(List<AccountHolder> holders,
    List<AccountHolder> authorizedSigners) {
}
