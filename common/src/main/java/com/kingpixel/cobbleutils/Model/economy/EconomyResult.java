package com.kingpixel.cobbleutils.Model.economy;

import lombok.Getter;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Result for simple balance operations.
 */
@Getter
public final class EconomyResult extends AbstractEconomyResult {

  private final BigDecimal before;
  private final BigDecimal after;
  private final BigDecimal amount;

  private EconomyResult(
    @NonNull EconomyStatus status,
    @NonNull String reason,
    UUID transactionId,
    BigDecimal before,
    BigDecimal after,
    BigDecimal amount
  ) {
    super(status, reason, transactionId);
    this.before = before;
    this.after = after;
    this.amount = amount;
  }

  /* ========================= */
  /* ===== FACTORY METHODS ==== */
  /* ========================= */

  public static EconomyResult success(
    @NonNull BigDecimal before,
    @NonNull BigDecimal after,
    @NonNull BigDecimal amount,
    @NonNull String reason
  ) {
    return new EconomyResult(
      EconomyStatus.SUCCESS,
      reason,
      UUID.randomUUID(),
      before,
      after,
      amount
    );
  }

  public static EconomyResult failure(
    @NonNull EconomyStatus status,
    @NonNull String reason,
    BigDecimal currentBalance
  ) {
    if (status == EconomyStatus.SUCCESS) {
      throw new IllegalArgumentException("Failure cannot use SUCCESS status");
    }

    return new EconomyResult(
      status,
      reason,
      null,
      currentBalance,
      currentBalance,
      BigDecimal.ZERO
    );
  }

  /* ===================== */
  /* ====== HELPERS ====== */
  /* ===================== */

  public BigDecimal getDelta() {
    if (before == null || after == null) return BigDecimal.ZERO;
    return after.subtract(before);
  }

  public boolean balanceChanged() {
    if (before == null || after == null) return false;
    return before.compareTo(after) != 0;
  }
}