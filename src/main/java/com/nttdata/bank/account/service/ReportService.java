package com.nttdata.bank.account.service;

import com.nttdata.bank.account.dto.MovementResponse;
import com.nttdata.bank.account.dto.ProductReport;
import com.nttdata.bank.account.model.AccountType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.time.LocalDate;
import java.util.Map;

/** Reports of the bank accounts. */
public interface ReportService {

  /** General report by product between two dates. */
  Single<Map<AccountType, ProductReport>> productReport(LocalDate from, LocalDate to);

  /** Last 10 movements of an account. */
  Flowable<MovementResponse> lastMovements(String accountId);
}
