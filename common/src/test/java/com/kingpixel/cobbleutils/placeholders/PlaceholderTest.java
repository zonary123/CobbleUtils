package com.kingpixel.cobbleutils.placeholders;

import com.kingpixel.cobbleutils.api.PlaceholderApi;
import com.kingpixel.cobbleutils.util.placeholders.CobblePlaceholderContext;
import com.kingpixel.cobbleutils.util.placeholders.PlaceholderValueConverter;
import com.kingpixel.cobbleutils.util.placeholders.PlaceholdersUtils;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PlaceholderTest {

  @BeforeEach
  public void setUp() {
    PlaceholdersUtils.unregisterNamespace("test");
    PlaceholdersUtils.unregisterNamespace("cobbleutils");
  }

  @Test
  @DisplayName("Test CobblePlaceholderContext Argument Parsing")
  public void testContextArgs() {
    CobblePlaceholderContext ctx = CobblePlaceholderContext.global("param1_123_true");

    Assertions.assertEquals("param1_123_true", ctx.getArgument());
    Assertions.assertEquals(3, ctx.numArgs());
    Assertions.assertEquals("param1", ctx.getArg(0, ""));
    Assertions.assertEquals(123, ctx.getArgInt(1, 0));
    Assertions.assertTrue(ctx.getArgBool(2, false));
    Assertions.assertEquals("fallback", ctx.getArg(5, "fallback"));
    Assertions.assertEquals(99, ctx.getArgInt(5, 99));
  }

  @Test
  @DisplayName("Test CobblePlaceholderContext Target Object Casting")
  public void testContextTarget() {
    String dummyTarget = "Pikachu";
    CobblePlaceholderContext ctx = CobblePlaceholderContext.of(null, dummyTarget, "info");

    Assertions.assertTrue(ctx.hasTarget());
    Assertions.assertTrue(ctx.targetAs(String.class).isPresent());
    Assertions.assertEquals("Pikachu", ctx.targetAs(String.class).get());
    Assertions.assertFalse(ctx.targetAs(Integer.class).isPresent());
  }

  @Test
  @DisplayName("Test Unified Registration and Internal Resolution")
  public void testRegistrationAndResolution() {
    PlaceholderApi.register("test", "server_name", ctx -> "MyAwesomeServer");
    PlaceholderApi.register("test", "count", ctx -> 42);
    PlaceholderApi.register("test", "colored", ctx -> Component.text("ColoredText"));

    String message = "Welcome to %test:server_name%! We have %test:count% players. %test:colored%";
    String parsed = PlaceholderApi.parseString(message, null);

    Assertions.assertTrue(parsed.contains("MyAwesomeServer"));
    Assertions.assertTrue(parsed.contains("42"));
    Assertions.assertTrue(parsed.contains("ColoredText"));
  }

  @Test
  @DisplayName("Test Exception Safety in Placeholder Callbacks")
  public void testExceptionSafety() {
    PlaceholderApi.register("test", "buggy", ctx -> {
      throw new RuntimeException("Simulated crash");
    });

    String message = "Result: %test:buggy%";
    // Should NOT throw exception and keep message intact or safely handle it
    String result = Assertions.assertDoesNotThrow(() -> PlaceholderApi.parseString(message, null));
    Assertions.assertNotNull(result);
  }

  @Test
  @DisplayName("Test Placeholder Unregistration")
  public void testUnregistration() {
    PlaceholderApi.register("test", "temp", ctx -> "Temporary");
    Assertions.assertEquals("Temporary", PlaceholderApi.parseString("%test:temp%", null));

    PlaceholderApi.unregister("test", "temp");
    Assertions.assertEquals("%test:temp%", PlaceholderApi.parseString("%test:temp%", null));
  }

  @Test
  @DisplayName("Test Value Converter Safety")
  public void testValueConverter() {
    Assertions.assertNull(PlaceholderValueConverter.toStringValue(null));
    Assertions.assertEquals("123", PlaceholderValueConverter.toStringValue(123));
    Assertions.assertEquals("Hello", PlaceholderValueConverter.toStringValue("Hello"));
    Assertions.assertEquals("Kyori", PlaceholderValueConverter.toStringValue(Component.text("Kyori")));
  }
}
