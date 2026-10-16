package com.nttdata.bank.account.mapper;

import com.nttdata.bank.account.dto.AccountResponse;
import com.nttdata.bank.account.dto.BalanceResponse;
import com.nttdata.bank.account.dto.MovementResponse;
import com.nttdata.bank.account.model.Account;
import com.nttdata.bank.account.model.Movement;
import org.springframework.stereotype.Component;

/** Converts account documents into API responses. */
@Component
public class AccountMapper {

  /** Builds the API representation of an account. */
  public AccountResponse toResponse(Account account) {
    return new AccountResponse(account.getId(), account.getAccountNumber(),
        account.getCustomerId(), account.getCustomerType(), account.getType(),
        account.getBalance(), account.getMaintenanceFee(), account.getMonthlyMovementLimit(),
        account.getMovementDay(), account.getFreeTransactions(), account.getTransactionFee(),
        account.getMinimumDailyAverage(), account.getHolders(), account.getAuthorizedSigners());
  }

  /** Builds the API representation of a movement. */
  public MovementResponse toResponse(Movement movement) {
    return new MovementResponse(movement.getId(), movement.getAccountId(), movement.getType(),
        movement.getAmount(), movement.getFee(), movement.getBalance(), movement.getDate());
  }

  /** Builds the balance of an account. */
  public BalanceResponse toBalance(Account account) {
    return new BalanceResponse(account.getId(), account.getAccountNumber(), account.getBalance());
  }
}
