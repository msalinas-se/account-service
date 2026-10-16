package com.nttdata.bank.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.nttdata.bank.account.dto.AccountRequest;
import com.nttdata.bank.account.dto.AccountResponse;
import com.nttdata.bank.account.dto.AccountUpdateRequest;
import com.nttdata.bank.account.dto.BalanceResponse;
import com.nttdata.bank.account.dto.MovementRequest;
import com.nttdata.bank.account.dto.MovementResponse;
import com.nttdata.bank.account.dto.ProductReport;
import com.nttdata.bank.account.dto.TransferRequest;
import com.nttdata.bank.account.exception.BusinessRuleException;
import com.nttdata.bank.account.exception.ResourceNotFoundException;
import com.nttdata.bank.account.exception.ServiceUnavailableException;
import com.nttdata.bank.account.model.AccountType;
import com.nttdata.bank.account.service.AccountService;
import com.nttdata.bank.account.service.MovementService;
import com.nttdata.bank.account.service.ReportService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest({AccountController.class, MovementController.class, TransferController.class,
    ReportController.class})
class AccountControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockBean
  private AccountService accountService;

  @MockBean
  private MovementService movementService;

  @MockBean
  private ReportService reportService;

  private final AccountResponse account = new AccountResponse("a1", "123", "1", null,
      AccountType.SAVINGS, BigDecimal.TEN, BigDecimal.ZERO, 20, null, 10, BigDecimal.ONE, null,
      List.of(), List.of());
  private final MovementResponse movement =
      new MovementResponse("m1", "a1", null, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, null);

  @Test
  void accountEndpoints() {
    when(accountService.create(any(AccountRequest.class))).thenReturn(Single.just(account));
    when(accountService.findAll(null)).thenReturn(Flowable.just(account));
    when(accountService.findById("a1")).thenReturn(Single.just(account));
    when(accountService.update(eq("a1"), any(AccountUpdateRequest.class)))
        .thenReturn(Single.just(account));
    when(accountService.delete("a1")).thenReturn(Completable.complete());
    when(accountService.getBalance("a1"))
        .thenReturn(Single.just(new BalanceResponse("a1", "123", BigDecimal.TEN)));

    webTestClient.post().uri("/accounts").bodyValue(Map.of("customerId", "1", "type", "SAVINGS"))
        .exchange().expectStatus().isCreated();
    webTestClient.get().uri("/accounts").exchange().expectStatus().isOk();
    webTestClient.get().uri("/accounts/a1").exchange().expectStatus().isOk();
    webTestClient.put().uri("/accounts/a1").bodyValue(Map.of()).exchange().expectStatus().isOk();
    webTestClient.delete().uri("/accounts/a1").exchange().expectStatus().isNoContent();
    webTestClient.get().uri("/accounts/a1/balance").exchange().expectStatus().isOk();
  }

  @Test
  void movementEndpoints() {
    when(movementService.deposit(eq("a1"), any(MovementRequest.class)))
        .thenReturn(Single.just(movement));
    when(movementService.withdraw(eq("a1"), any(MovementRequest.class)))
        .thenReturn(Single.just(movement));
    when(movementService.transfer(any(TransferRequest.class))).thenReturn(Single.just(movement));
    when(movementService.findByAccount("a1")).thenReturn(Flowable.just(movement));

    webTestClient.post().uri("/accounts/a1/deposits").bodyValue(Map.of("amount", 10))
        .exchange().expectStatus().isCreated();
    webTestClient.post().uri("/accounts/a1/withdrawals").bodyValue(Map.of("amount", 10))
        .exchange().expectStatus().isCreated();
    webTestClient.post().uri("/accounts/transfers")
        .bodyValue(Map.of("fromAccountId", "a1", "toAccountId", "a2", "amount", 10))
        .exchange().expectStatus().isCreated();
    webTestClient.get().uri("/accounts/a1/movements").exchange().expectStatus().isOk();
  }

  @Test
  void reportEndpoints() {
    when(reportService.productReport(any(), any())).thenReturn(Single.just(Map.of(
        AccountType.SAVINGS, new ProductReport(1, 1, Map.of(), BigDecimal.ZERO, List.of()))));
    when(reportService.lastMovements("a1")).thenReturn(Flowable.just(movement));

    webTestClient.get().uri("/accounts/report?from=2026-10-01&to=2026-10-31").exchange()
        .expectStatus().isOk()
        .expectBody().jsonPath("$.SAVINGS.totalMovements").isEqualTo(1);
    webTestClient.get().uri("/accounts/a1/movements/last").exchange().expectStatus().isOk();
  }

  @Test
  void errors() {
    when(accountService.findById("1")).thenReturn(Single.error(new ResourceNotFoundException("x")));
    when(accountService.findById("2"))
        .thenReturn(Single.error(new BusinessRuleException("CODE", "x")));
    when(accountService.findById("3"))
        .thenReturn(Single.error(new ServiceUnavailableException("x")));
    when(accountService.findById("4")).thenReturn(Single.error(new IllegalStateException("x")));

    webTestClient.get().uri("/accounts/1").exchange().expectStatus().isNotFound();
    webTestClient.get().uri("/accounts/2").exchange().expectStatus().isBadRequest();
    webTestClient.get().uri("/accounts/3").exchange().expectStatus().isEqualTo(503);
    webTestClient.get().uri("/accounts/4").exchange().expectStatus().is5xxServerError();
    webTestClient.post().uri("/accounts").bodyValue(Map.of()).exchange()
        .expectStatus().isBadRequest();
  }
}
