package com.kingpixel.cobbleutils.util.economys;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;

/**
 * Represents the result of an economy operation (deposit, withdraw, setBalance, etc.)
 */
@Data
@Builder
public class EconomyResult {
  private final boolean success;
  @NonNull
  private final BigDecimal before;
  @NonNull
  private final BigDecimal after;
  @NonNull
  private final String reason;

  public static EconomyResult success(@NonNull BigDecimal before, @NonNull BigDecimal after, @NonNull String reason) {
    return EconomyResult.builder()
      .success(true)
      .before(before)
      .after(after)
      .reason(reason)
      .build();
  }

  public static EconomyResult fail(@NonNull String reason) {
    return EconomyResult.builder()
      .success(false)
      .before(BigDecimal.ZERO)
      .after(BigDecimal.ZERO)
      .reason(reason)
      .build();
  }

  public static EconomyResult fail(@NonNull String reason, @NonNull BigDecimal before) {
    return EconomyResult.builder()
      .success(false)
      .before(before)
      .after(before)
      .reason(reason)
      .build();
  }

}

