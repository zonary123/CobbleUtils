package com.kingpixel.cobbleutils.Model;

import lombok.*;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents parsed components of a money reward or economy transaction.
 */
@Data
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class ParsedMoney {
  private String economyId;
  private String currency;
  private String amountString;
  private BigDecimal amount;
  @Builder.Default
  private UUID playerUuid = null;

  public ParsedMoney(String economyId, String currency, String amountString, BigDecimal amount) {
    this(economyId, currency, amountString, amount, null);
  }

  public ParsedMoney(String economyId, String currency, String amountString, BigDecimal amount, UUID playerUuid) {
    this.economyId = economyId != null ? economyId : "";
    this.currency = currency != null && !currency.isBlank() ? currency : "money";
    this.amountString = amountString != null ? amountString : "";
    this.amount = amount != null ? amount : BigDecimal.ZERO;
    this.playerUuid = playerUuid;
  }

  /**
   * Associates a player UUID with this parsed money.
   *
   * @param uuid The player UUID.
   * @return This instance for chaining.
   */
  public ParsedMoney withPlayer(@Nullable UUID uuid) {
    this.playerUuid = uuid;
    return this;
  }

  /**
   * Associates a server player with this parsed money.
   *
   * @param player The server player entity.
   * @return This instance for chaining.
   */
  public ParsedMoney withPlayer(@Nullable ServerPlayerEntity player) {
    this.playerUuid = player != null ? player.getUuid() : null;
    return this;
  }

  public String getEconomyId() {
    return economyId != null ? economyId : "";
  }

  public String getCurrency() {
    return currency != null && !currency.isBlank() ? currency : "money";
  }

  public String getAmountString() {
    return amountString != null ? amountString : "";
  }

  public BigDecimal getAmount() {
    return amount != null ? amount : BigDecimal.ZERO;
  }
}
