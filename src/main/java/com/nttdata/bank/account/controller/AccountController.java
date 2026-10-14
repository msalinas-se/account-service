package com.nttdata.bank.account.controller;

import com.nttdata.bank.account.dto.AccountRequest;
import com.nttdata.bank.account.dto.AccountResponse;
import com.nttdata.bank.account.dto.AccountUpdateRequest;
import com.nttdata.bank.account.dto.BalanceResponse;
import com.nttdata.bank.account.service.AccountService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for bank account management. */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
  private final AccountService accountService;

  /** Opens an account. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Single<AccountResponse> create(@Valid @RequestBody AccountRequest request) {
    return accountService.create(request);
  }

  /** Lists accounts, optionally filtered by customer. */
  @GetMapping
  public Flowable<AccountResponse> findAll(@RequestParam(required = false) String customerId) {
    return accountService.findAll(customerId);
  }

  /** Gets an account by identifier. */
  @GetMapping("/{id}")
  public Single<AccountResponse> findById(@PathVariable String id) {
    return accountService.findById(id);
  }

  /** Updates the holders and authorized signers of an account. */
  @PutMapping("/{id}")
  public Single<AccountResponse> update(@PathVariable String id,
      @Valid @RequestBody AccountUpdateRequest request) {
    return accountService.update(id, request);
  }

  /** Deletes an account. */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Completable delete(@PathVariable String id) {
    return accountService.delete(id);
  }

  /** Gets the available balance of an account. */
  @GetMapping("/{id}/balance")
  public Single<BalanceResponse> getBalance(@PathVariable String id) {
    return accountService.getBalance(id);
  }
}
