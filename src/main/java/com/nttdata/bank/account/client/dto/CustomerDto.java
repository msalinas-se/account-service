package com.nttdata.bank.account.client.dto;

import com.nttdata.bank.account.model.CustomerType;

/** Customer data returned by the customer microservice. */
public record CustomerDto(String id, CustomerType type, String documentNumber, String name) {
}
