package com.nttdata.bank.account.exception;

/** Raised when another microservice does not answer. */
public class ServiceUnavailableException extends RuntimeException {

  /** Creates the exception with a message. */
  public ServiceUnavailableException(String message) {
    super(message);
  }
}
