package com.nttdata.bank.account.controller;

import com.nttdata.bank.account.dto.MovementRequest;
import com.nttdata.bank.account.dto.MovementResponse;
import com.nttdata.bank.account.service.MovementService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for deposits, withdrawals and movement history of an account. */
@RestController
@RequestMapping("/accounts/{accountId}")
@RequiredArgsConstructor
public class MovementController {
  private final MovementService movementService;

  /** Deposits money into an account. */
  @PostMapping("/deposits")
  @ResponseStatus(HttpStatus.CREATED)
  public Single<MovementResponse> deposit(@PathVariable String accountId,
      @Valid @RequestBody MovementRequest request) {
    return movementService.deposit(accountId, request);
  }

  /** Withdraws money from an account. */
  @PostMapping("/withdrawals")
  @ResponseStatus(HttpStatus.CREATED)
  public Single<MovementResponse> withdraw(@PathVariable String accountId,
      @Valid @RequestBody MovementRequest request) {
    return movementService.withdraw(accountId, request);
  }

  /** Lists the movements of an account, newest first. */
  @GetMapping("/movements")
  public Flowable<MovementResponse> findMovements(@PathVariable String accountId) {
    return movementService.findByAccount(accountId);
  }
}
