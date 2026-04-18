package com.kingpixel.cobbleutils.util.economys;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.function.Function;

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

  // =========================================================
  // Fluent API
  // =========================================================

  /**
   * If the operation was successful, apply the function to the response.
   */
  public <T> T map(Function<EconomyResponse, T> mapper) {
    return mapper.apply(this);
  }

  /**
   * Executes the consumer if the operation was successful.
   */
  public EconomyResponse ifSuccess(Consumer<EconomyResponse> consumer) {
    if (success()) consumer.accept(this);
    return this;
  }

  /**
   * Executes the consumer if the operation failed.
   */
  public EconomyResponse ifFailure(Consumer<String> consumer) {
    if (!success()) consumer.accept(errorMessage);
    return this;
  }
}