package com.kingpixel.cobbleutils.economys;

import com.kingpixel.cobbleutils.util.economys.EconomyResponse;
import com.kingpixel.cobbleutils.util.economys.providers.CobbleDollarsEconomy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class CobbleDollarsEconomyTest {

  @Test
  @DisplayName("Test Provider CobbleDollars Identity & Decimals")
  void testProviderProperties() {
    CobbleDollarsEconomy economy = new CobbleDollarsEconomy();
    Assertions.assertEquals("COBBLE_DOLLARS", economy.getIdentify());
    Assertions.assertEquals(0, economy.getDecimals(""));
  }

  @Test
  @DisplayName("Test Provider Offline/Null Player Handling")
  void testProviderNullSafety() {
    CobbleDollarsEconomy economy = new CobbleDollarsEconomy();
    UUID fakePlayer = UUID.randomUUID();

    // Negative deposit should fail immediately without throwing NPE
    CompletableFuture<EconomyResponse> depositNeg = economy.deposit(fakePlayer, "", new BigDecimal("-10"), "test");
    Assertions.assertFalse(depositNeg.join().success());

    // Null deposit should fail immediately
    CompletableFuture<EconomyResponse> depositNull = economy.deposit(fakePlayer, "", null, "test");
    Assertions.assertFalse(depositNull.join().success());

    // Negative withdraw should fail immediately
    CompletableFuture<EconomyResponse> withdrawNeg = economy.withdraw(fakePlayer, "", new BigDecimal("-10"), "test");
    Assertions.assertFalse(withdrawNeg.join().success());

    // Null withdraw should fail immediately
    CompletableFuture<EconomyResponse> withdrawNull = economy.withdraw(fakePlayer, "", null, "test");
    Assertions.assertFalse(withdrawNull.join().success());

    // Negative setBalance should fail immediately
    CompletableFuture<EconomyResponse> setBalanceNeg = economy.setBalance(fakePlayer, "", new BigDecimal("-10"), "test");
    Assertions.assertFalse(setBalanceNeg.join().success());
  }

  @Test
  @DisplayName("Test V1 CobbleDollars Identity & Validation")
  void testV1PropertiesAndValidation() {
    com.kingpixel.cobbleutils.util.economys.v1.CobbleDollarsEconomy v1 = new com.kingpixel.cobbleutils.util.economys.v1.CobbleDollarsEconomy();
    Assertions.assertEquals("COBBLE_DOLLARS", v1.getIdentify());
    Assertions.assertEquals(0, v1.getDecimals(""));

    UUID fakePlayer = UUID.randomUUID();
    // When player not found or offline (server is null in unit test), returns false/ZERO safely without throwing exception
    Assertions.assertFalse(v1.deposit(fakePlayer, new BigDecimal("100"), ""));
    Assertions.assertFalse(v1.deposit(fakePlayer, null, ""));
    Assertions.assertFalse(v1.deposit(fakePlayer, new BigDecimal("-50"), ""));

    Assertions.assertFalse(v1.withdraw(fakePlayer, new BigDecimal("100"), ""));
    Assertions.assertFalse(v1.withdraw(fakePlayer, null, ""));
    Assertions.assertFalse(v1.withdraw(fakePlayer, new BigDecimal("-50"), ""));

    Assertions.assertFalse(v1.setBalance(fakePlayer, new BigDecimal("100"), ""));
    Assertions.assertFalse(v1.setBalance(fakePlayer, null, ""));
    Assertions.assertFalse(v1.setBalance(fakePlayer, new BigDecimal("-50"), ""));

    Assertions.assertEquals(BigDecimal.ZERO, v1.getBalance(fakePlayer, ""));
  }
}
