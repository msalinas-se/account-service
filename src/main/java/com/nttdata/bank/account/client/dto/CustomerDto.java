package com.nttdata.bank.account.client.dto;

import com.nttdata.bank.account.model.CustomerProfile;
import com.nttdata.bank.account.model.CustomerType;

/** Customer data returned by the customer microservice. */
public record CustomerDto(String id, CustomerType type, CustomerProfile profile,
    String documentNumber, String name) {
}
