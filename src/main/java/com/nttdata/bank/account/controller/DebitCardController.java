package com.nttdata.bank.account.controller;

import com.nttdata.bank.account.dto.BalanceResponse;
import com.nttdata.bank.account.dto.DebitCardRequest;
import com.nttdata.bank.account.dto.MovementRequest;
import com.nttdata.bank.account.dto.MovementResponse;
import com.nttdata.bank.account.model.DebitCard;
import com.nttdata.bank.account.service.DebitCardService;
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

/** REST endpoints for debit cards. */
@RestController
@RequestMapping("/debit-cards")
@RequiredArgsConstructor
public class DebitCardController {
  private final DebitCardService debitCardService;

  /** Creates a debit card. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Single<DebitCard> create(@Valid @RequestBody DebitCardRequest request) {
    return debitCardService.create(request);
  }

  /** Lists debit cards, optionally by customer. */
  @GetMapping
  public Flowable<DebitCard> findAll(@RequestParam(required = false) String customerId) {
    return debitCardService.findAll(customerId);
  }

  /** Gets a debit card. */
  @GetMapping("/{id}")
  public Single<DebitCard> findById(@PathVariable String id) {
    return debitCardService.findById(id);
  }

  /** Updates the accounts of a debit card. */
  @PutMapping("/{id}")
  public Single<DebitCard> update(@PathVariable String id,
      @Valid @RequestBody DebitCardRequest request) {
    return debitCardService.update(id, request);
  }

  /** Deletes a debit card. */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Completable delete(@PathVariable String id) {
    return debitCardService.delete(id);
  }

  /** Pays with a debit card. */
  @PostMapping("/{id}/payments")
  @ResponseStatus(HttpStatus.CREATED)
  public Single<MovementResponse> pay(@PathVariable String id,
      @Valid @RequestBody MovementRequest request) {
    return debitCardService.pay(id, request);
  }

  /** Balance of the main account of a debit card. */
  @GetMapping("/{id}/balance")
  public Single<BalanceResponse> getBalance(@PathVariable String id) {
    return debitCardService.getBalance(id);
  }

  /** Last 10 movements of a debit card. */
  @GetMapping("/{id}/movements/last")
  public Flowable<MovementResponse> lastMovements(@PathVariable String id) {
    return debitCardService.lastMovements(id);
  }
}
