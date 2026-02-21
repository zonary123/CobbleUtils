package com.kingpixel.cobbleutils.Model.economy;

import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Result for transfer operations between two players.
 */
@Getter
public final class EconomyTransferResult extends AbstractEconomyResult {

  private final UUID fromPlayer;
  private final UUID toPlayer;

  private final BigDecimal fromBefore;
  private final BigDecimal fromAfter;

  private final BigDecimal toBefore;
  private final BigDecimal toAfter;

  private final BigDecimal amount;

  private EconomyTransferResult(
    @NonNull EconomyStatus status,
    @NonNull String reason,
    UUID transactionId,
    @NonNull UUID fromPlayer,
    @NonNull UUID toPlayer,
    BigDecimal fromBefore,
    BigDecimal fromAfter,
    BigDecimal toBefore,
    BigDecimal toAfter,
    BigDecimal amount
  ) {
    super(status, reason, transactionId);
    this.fromPlayer = fromPlayer;
    this.toPlayer = toPlayer;
    this.fromBefore = fromBefore;
    this.fromAfter = fromAfter;
    this.toBefore = toBefore;
    this.toAfter = toAfter;
    this.amount = amount;
  }

  /* ========================= */
  /* ===== FACTORY METHODS ==== */
  /* ========================= */

  public static EconomyTransferResult success(
    @NonNull UUID fromPlayer,
    @NonNull UUID toPlayer,
    @NonNull BigDecimal fromBefore,
    @NonNull BigDecimal fromAfter,
    @NonNull BigDecimal toBefore,
    @NonNull BigDecimal toAfter,
    @NonNull BigDecimal amount,
    @NonNull String reason
  ) {
    return new EconomyTransferResult(
      EconomyStatus.SUCCESS,
      reason,
      UUID.randomUUID(),
      fromPlayer,
      toPlayer,
      fromBefore,
      fromAfter,
      toBefore,
      toAfter,
      amount
    );
  }

  public static EconomyTransferResult failure(
    @NonNull EconomyStatus status,
    @NonNull UUID fromPlayer,
    @NonNull UUID toPlayer,
    @NonNull String reason,
    @Nullable BigDecimal fromCurrent,
    @Nullable BigDecimal toCurrent
  ) {
    if (status == EconomyStatus.SUCCESS) {
      throw new IllegalArgumentException("Failure cannot use SUCCESS status");
    }

    return new EconomyTransferResult(
      status,
      reason,
      null,
      fromPlayer,
      toPlayer,
      fromCurrent,
      fromCurrent,
      toCurrent,
      toCurrent,
      BigDecimal.ZERO
    );
  }

  /* ===================== */
  /* ====== HELPERS ====== */
  /* ===================== */

  public BigDecimal getFromDelta() {
    if (fromBefore == null || fromAfter == null) return BigDecimal.ZERO;
    return fromAfter.subtract(fromBefore);
  }

  public BigDecimal getToDelta() {
    if (toBefore == null || toAfter == null) return BigDecimal.ZERO;
    return toAfter.subtract(toBefore);
  }

  public boolean balancesChanged() {
    return getFromDelta().compareTo(BigDecimal.ZERO) != 0 ||
      getToDelta().compareTo(BigDecimal.ZERO) != 0;
  }
}