package com.nttdata.bank.account.model;

/** Kind of movement registered on an account. */
public enum MovementType {
  DEPOSIT,
  WITHDRAWAL,
  TRANSFER_IN,
  TRANSFER_OUT,
  DEBIT_CARD_PAYMENT,
  WALLET_IN,
  WALLET_OUT
}
