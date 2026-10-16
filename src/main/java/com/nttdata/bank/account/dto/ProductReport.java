package com.nttdata.bank.account.dto;

import com.nttdata.bank.account.model.MovementType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Report of the movements of one account product in a period. */
public record ProductReport(long accounts, long totalMovements,
    Map<MovementType, BigDecimal> totals, BigDecimal totalFees,
    List<MovementResponse> movements) {
}
