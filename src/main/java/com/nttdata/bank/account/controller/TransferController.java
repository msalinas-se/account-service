package com.nttdata.bank.account.controller;

import com.nttdata.bank.account.dto.MovementResponse;
import com.nttdata.bank.account.dto.TransferRequest;
import com.nttdata.bank.account.service.MovementService;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoint for transfers between accounts. */
@RestController
@RequiredArgsConstructor
public class TransferController {
  private final MovementService movementService;

  /** Transfers money to an own account or to a third party account of the bank. */
  @PostMapping("/accounts/transfers")
  @ResponseStatus(HttpStatus.CREATED)
  public Single<MovementResponse> transfer(@Valid @RequestBody TransferRequest request) {
    return movementService.transfer(request);
  }
}
