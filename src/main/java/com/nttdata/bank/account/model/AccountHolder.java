package com.nttdata.bank.account.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Holder or authorized signer of an account. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountHolder {
  private String documentNumber;
  private String fullName;
}
