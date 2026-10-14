package com.nttdata.bank.account.exception;

/** Error body returned by the API. */
public record ErrorResponse(String code, String message) {
}
