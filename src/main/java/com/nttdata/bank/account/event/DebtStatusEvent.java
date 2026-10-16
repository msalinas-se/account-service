package com.nttdata.bank.account.event;

/** Event with the overdue debt status of a customer. */
public record DebtStatusEvent(String customerId, boolean overdue) {
}
