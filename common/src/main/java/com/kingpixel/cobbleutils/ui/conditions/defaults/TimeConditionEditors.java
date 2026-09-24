package com.kingpixel.cobbleutils.ui.conditions.defaults;

import com.kingpixel.cobbleutils.Model.conditions.*;
import com.kingpixel.cobbleutils.ui.conditions.ConditionEditor;
import com.kingpixel.cobbleutils.ui.conditions.ConditionEditorRegistry;
import com.kingpixel.cobbleutils.util.ChatPrompt;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ConditionEditor implementations for time and calendar conditions.
 */
public final class TimeConditionEditors {

  private TimeConditionEditors() {
  }

  public static void register() {
    registerTimeOfDay();
    registerRealTime();
    registerDateRange();
    registerDayOfWeek();
    registerDayOfMonth();
    registerMonth();
    registerYear();
    registerWeekOfYear();
  }

  private static void registerTimeOfDay() {
    ConditionEditorRegistry.register(TimeOfDayMinecraftCondition.class, new ConditionEditor<TimeOfDayMinecraftCondition>() {
      @Override
      public ItemStack getIcon(TimeOfDayMinecraftCondition condition) {
        return new ItemStack(Items.CLOCK);
      }

      @Override
      public List<String> getDetails(TimeOfDayMinecraftCondition condition) {
        return List.of("§7Tick Range: §f" + condition.getMinTime() + " - " + condition.getMaxTime() + " ticks");
      }

      @Override
      public void openEdit(ServerPlayerEntity player, TimeOfDayMinecraftCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter tick range 'min:max' (0-24000, e.g. 13000:23000):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            try {
              if (val.contains(":")) {
                String[] parts = val.split(":", 2);
                condition.setMinTime(Math.max(0, Long.parseLong(parts[0].trim())));
                condition.setMaxTime(Math.min(24000, Long.parseLong(parts[1].trim())));
              } else {
                long t = Long.parseLong(val.trim());
                condition.setMinTime(t);
                condition.setMaxTime(t);
              }
              player.sendMessage(Text.literal("§a[Editor] Minecraft time range set to: " + condition.getMinTime() + " - " + condition.getMaxTime()));
            } catch (NumberFormatException e) {
              player.sendMessage(Text.literal("§c[Editor] Invalid tick numbers."));
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerRealTime() {
    ConditionEditorRegistry.register(RealTimeCondition.class, new ConditionEditor<RealTimeCondition>() {
      @Override
      public ItemStack getIcon(RealTimeCondition condition) {
        return new ItemStack(Items.COMPASS);
      }

      @Override
      public List<String> getDetails(RealTimeCondition condition) {
        return List.of("§7Real Time Window: §f" + condition.getMinTime() + " - " + condition.getMaxTime());
      }

      @Override
      public void openEdit(ServerPlayerEntity player, RealTimeCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter real time window as 'HH:mm - HH:mm' (e.g. 18:00 - 22:00):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            String[] parts = val.contains("-") ? val.split("-", 2) : val.split(":", 2);
            if (parts.length == 2) {
              condition.setMinTime(parts[0].trim());
              condition.setMaxTime(parts[1].trim());
              player.sendMessage(Text.literal("§a[Editor] Real time set to: " + condition.getMinTime() + " - " + condition.getMaxTime()));
            } else {
              player.sendMessage(Text.literal("§c[Editor] Invalid format. Use 'HH:mm - HH:mm'."));
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerDateRange() {
    ConditionEditorRegistry.register(DateRangeCondition.class, new ConditionEditor<DateRangeCondition>() {
      @Override
      public ItemStack getIcon(DateRangeCondition condition) {
        return new ItemStack(Items.MAP);
      }

      @Override
      public List<String> getDetails(DateRangeCondition condition) {
        return List.of("§7Date Range: §f" + condition.getStartDate() + " to " + condition.getEndDate());
      }

      @Override
      public void openEdit(ServerPlayerEntity player, DateRangeCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter date range as 'YYYY-MM-DD to YYYY-MM-DD':", val -> {
          if (val != null && !val.trim().isEmpty()) {
            String str = val.trim();
            String[] parts = str.contains("to") ? str.split("to", 2) : str.split(" ", 2);
            if (parts.length == 2) {
              condition.setStartDate(parts[0].trim());
              condition.setEndDate(parts[1].trim());
              player.sendMessage(Text.literal("§a[Editor] Date range set to: " + condition.getStartDate() + " to " + condition.getEndDate()));
            } else {
              player.sendMessage(Text.literal("§c[Editor] Invalid format. Example: 2026-01-01 to 2026-12-31"));
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerDayOfWeek() {
    ConditionEditorRegistry.register(DayOfWeekCondition.class, new ConditionEditor<DayOfWeekCondition>() {
      @Override
      public ItemStack getIcon(DayOfWeekCondition condition) {
        return new ItemStack(Items.PAPER);
      }

      @Override
      public List<String> getDetails(DayOfWeekCondition condition) {
        String days = condition.getDays() != null && !condition.getDays().isEmpty()
          ? String.join(", ", condition.getDays())
          : "none";
        return List.of("§7Days: §f" + days);
      }

      @Override
      public void openEdit(ServerPlayerEntity player, DayOfWeekCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter day of week to toggle (MONDAY, TUESDAY, etc.):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            Set<String> days = new HashSet<>(condition.getDays() != null ? condition.getDays() : Set.of());
            String day = val.trim().toUpperCase();
            if (days.contains(day)) {
              days.remove(day);
              player.sendMessage(Text.literal("§c[Editor] Removed day: " + day));
            } else {
              days.add(day);
              player.sendMessage(Text.literal("§a[Editor] Added day: " + day));
            }
            condition.setDays(days);
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerDayOfMonth() {
    ConditionEditorRegistry.register(DayOfMonthCondition.class, new ConditionEditor<DayOfMonthCondition>() {
      @Override
      public ItemStack getIcon(DayOfMonthCondition condition) {
        return new ItemStack(Items.PAPER);
      }

      @Override
      public List<String> getDetails(DayOfMonthCondition condition) {
        return List.of("§7Days of Month: §f" + (condition.getDays() != null ? condition.getDays() : "none"));
      }

      @Override
      public void openEdit(ServerPlayerEntity player, DayOfMonthCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter day of month to toggle (1-31):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            try {
              int day = Integer.parseInt(val.trim());
              Set<Integer> days = new HashSet<>(condition.getDays() != null ? condition.getDays() : Set.of());
              if (days.contains(day)) {
                days.remove(day);
                player.sendMessage(Text.literal("§c[Editor] Removed day: " + day));
              } else {
                days.add(day);
                player.sendMessage(Text.literal("§a[Editor] Added day: " + day));
              }
              condition.setDays(days);
            } catch (NumberFormatException ignored) {
              player.sendMessage(Text.literal("§c[Editor] Invalid day number."));
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerMonth() {
    ConditionEditorRegistry.register(MonthCondition.class, new ConditionEditor<MonthCondition>() {
      @Override
      public ItemStack getIcon(MonthCondition condition) {
        return new ItemStack(Items.FLOWERING_AZALEA);
      }

      @Override
      public List<String> getDetails(MonthCondition condition) {
        return List.of("§7Months: §f" + (condition.getMonths() != null ? condition.getMonths() : "none"));
      }

      @Override
      public void openEdit(ServerPlayerEntity player, MonthCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter month number to toggle (1-12):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            try {
              int m = Integer.parseInt(val.trim());
              Set<Integer> months = new HashSet<>(condition.getMonths() != null ? condition.getMonths() : Set.of());
              if (months.contains(m)) {
                months.remove(m);
                player.sendMessage(Text.literal("§c[Editor] Removed month: " + m));
              } else {
                months.add(m);
                player.sendMessage(Text.literal("§a[Editor] Added month: " + m));
              }
              condition.setMonths(months);
            } catch (NumberFormatException ignored) {
              player.sendMessage(Text.literal("§c[Editor] Invalid month number."));
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerYear() {
    ConditionEditorRegistry.register(YearCondition.class, new ConditionEditor<YearCondition>() {
      @Override
      public ItemStack getIcon(YearCondition condition) {
        return new ItemStack(Items.BOOKSHELF);
      }

      @Override
      public List<String> getDetails(YearCondition condition) {
        return List.of("§7Years: §f" + (condition.getYears() != null ? condition.getYears() : "none"));
      }

      @Override
      public void openEdit(ServerPlayerEntity player, YearCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter year to toggle (e.g. 2026):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            try {
              int y = Integer.parseInt(val.trim());
              Set<Integer> years = new HashSet<>(condition.getYears() != null ? condition.getYears() : Set.of());
              if (years.contains(y)) {
                years.remove(y);
                player.sendMessage(Text.literal("§c[Editor] Removed year: " + y));
              } else {
                years.add(y);
                player.sendMessage(Text.literal("§a[Editor] Added year: " + y));
              }
              condition.setYears(years);
            } catch (NumberFormatException ignored) {
              player.sendMessage(Text.literal("§c[Editor] Invalid year."));
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }

  private static void registerWeekOfYear() {
    ConditionEditorRegistry.register(WeekOfYearCondition.class, new ConditionEditor<WeekOfYearCondition>() {
      @Override
      public ItemStack getIcon(WeekOfYearCondition condition) {
        return new ItemStack(Items.WRITABLE_BOOK);
      }

      @Override
      public List<String> getDetails(WeekOfYearCondition condition) {
        return List.of("§7Week Range: §f" + condition.getMinWeek() + " - " + condition.getMaxWeek());
      }

      @Override
      public void openEdit(ServerPlayerEntity player, WeekOfYearCondition condition, Runnable onUpdated) {
        ChatPrompt.prompt(player, "Enter week range 'min:max' (1-52):", val -> {
          if (val != null && !val.trim().isEmpty()) {
            try {
              if (val.contains(":")) {
                String[] parts = val.split(":", 2);
                condition.setMinWeek(Math.max(1, Integer.parseInt(parts[0].trim())));
                condition.setMaxWeek(Math.min(52, Integer.parseInt(parts[1].trim())));
              } else {
                int w = Integer.parseInt(val.trim());
                condition.setMinWeek(w);
                condition.setMaxWeek(w);
              }
              player.sendMessage(Text.literal("§a[Editor] Week range set to: " + condition.getMinWeek() + " - " + condition.getMaxWeek()));
            } catch (NumberFormatException e) {
              player.sendMessage(Text.literal("§c[Editor] Invalid week number."));
            }
          }
          if (onUpdated != null) onUpdated.run();
        });
      }
    });
  }
}
