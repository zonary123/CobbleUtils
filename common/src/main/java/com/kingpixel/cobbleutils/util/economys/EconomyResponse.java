package com.kingpixel.cobbleutils.util.economys;

import java.math.BigDecimal;

/**
 * Represents the result of an economy operation.
 * Indicates whether the operation was successful, the amount modified,
 * the new balance, and an optional error message.
 */
public record EconomyResponse(
  BigDecimal amount,
  BigDecimal balance,
  ResponseType type,
  String errorMessage
) {

  public boolean isSuccess() {
    return success();
  }

  public Number getAmount() {
    return amount;
  }

  /**
   * Types of results for an economy operation.
   */
  public enum ResponseType {
    SUCCESS,
    FAILURE,
    NOT_IMPLEMENTED
  }

  /**
   * Returns true if the operation was successful.
   *
   * @return true if type == SUCCESS
   */
  public boolean success() {
    return type == ResponseType.SUCCESS;
  }

  /**
   * Creates a successful EconomyResponse.
   *
   * @param amount  The amount modified
   * @param balance The new balance of the player
   * @return EconomyResponse indicating success
   */
  public static EconomyResponse success(BigDecimal amount, BigDecimal balance) {
    return new EconomyResponse(amount, balance, ResponseType.SUCCESS, null);
  }

  /**
   * Creates a failed EconomyResponse.
   *
   * @param error The error message
   * @return EconomyResponse indicating failure
   */
  public static EconomyResponse failure(String error) {
    return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, ResponseType.FAILURE, error);
  }

  /**
   * Creates an EconomyResponse indicating the operation is not implemented.
   *
   * @return EconomyResponse indicating NOT_IMPLEMENTED
   */
  public static EconomyResponse notImplemented() {
    return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, ResponseType.NOT_IMPLEMENTED, "Operation not implemented");
  }
}