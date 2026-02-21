package com.kingpixel.cobbleutils.Model.economy;

import lombok.Getter;
import lombok.NonNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all economy operation results.
 * Immutable and thread-safe.
 */
@Getter
public abstract class AbstractEconomyResult {

  private final EconomyStatus status;
  private final String reason;
  private final UUID transactionId;
  private final Instant timestamp;

  protected AbstractEconomyResult(
    @NonNull EconomyStatus status,
    @NonNull String reason,
    UUID transactionId
  ) {
    this.status = Objects.requireNonNull(status);
    this.reason = Objects.requireNonNull(reason);
    this.transactionId = transactionId;
    this.timestamp = Instant.now();
  }

  /* ===================== */
  /* ====== HELPERS ====== */
  /* ===================== */

  public boolean isSuccess() {
    return status == EconomyStatus.SUCCESS;
  }

  public boolean isFailure() {
    return !isSuccess();
  }

  public boolean is(EconomyStatus status) {
    return this.status == status;
  }

  public boolean hasTransaction() {
    return transactionId != null;
  }

  public String debugString() {
    return getClass().getSimpleName() +
      "{status=" + status +
      ", reason='" + reason + '\'' +
      ", transactionId=" + transactionId +
      ", timestamp=" + timestamp +
      '}';
  }

  @Override
  public String toString() {
    return debugString();
  }
}