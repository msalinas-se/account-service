package com.nttdata.bank.account.controller;

import com.nttdata.bank.account.dto.MovementResponse;
import com.nttdata.bank.account.dto.ProductReport;
import com.nttdata.bank.account.model.AccountType;
import com.nttdata.bank.account.service.ReportService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for the account reports. */
@RestController
@RequiredArgsConstructor
public class ReportController {
  private final ReportService reportService;

  /** General report by product between two dates. */
  @GetMapping("/accounts/report")
  public Single<Map<AccountType, ProductReport>> productReport(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return reportService.productReport(from, to);
  }

  /** Last 10 movements of an account. */
  @GetMapping("/accounts/{id}/movements/last")
  public Flowable<MovementResponse> lastMovements(@PathVariable String id) {
    return reportService.lastMovements(id);
  }
}
