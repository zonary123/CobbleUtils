package com.kingpixel.cobbleutils.ui.editor;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.ScheduleValue;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.ChatPrompt;
import com.kingpixel.cobbleutils.util.UIUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reusable editor GUI for {@link ScheduleValue}.
 * Supports toggling between DURATION and CRON mode, editing expressions,
 * configuring timezones, and previewing next execution time.
 */
public final class ScheduleValueEditorMenu {

  private static final CronParser CRON_PARSER = new CronParser(
    CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
  );

  private ScheduleValueEditorMenu() {
  }

  /**
   * Opens the ScheduleValue editor GUI.
   *
   * @param player   the player opening the editor
   * @param initial  the current schedule value to edit
   * @param onSave   callback invoked when the schedule is saved
   * @param onBack   callback invoked when navigating back
   */
  public static void open(ServerPlayerEntity player, ScheduleValue initial, Consumer<ScheduleValue> onSave, Runnable onBack) {
    if (player == null) return;

    ScheduleValue working = initial != null ? initial : ScheduleValue.ofDuration(DurationValue.parse("1d"));

    ChestTemplate template = ChestTemplate.builder(4).build();

    // 1. Mode Toggle (Slot 10)
    ItemStack modeItem = new ItemStack(Items.CLOCK);
    modeItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lSchedule Mode"));
    List<String> modeLore = new ArrayList<>();
    modeLore.add("&7Current: &b" + working.getType().name());
    modeLore.add("");
    modeLore.add("&eClick to toggle (DURATION / CRON).");
    modeItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(modeLore)));

    template.set(10, GooeyButton.builder()
      .display(modeItem)
      .onClick(action -> {
        ScheduleValue nextValue;
        if (working.getType() == ScheduleValue.Mode.DURATION) {
          nextValue = ScheduleValue.ofCron("0 0 * * *", "UTC");
        } else {
          nextValue = ScheduleValue.ofDuration(DurationValue.parse("1d"));
        }
        open(player, nextValue, onSave, onBack);
      })
      .build());

    // 2. Value / Expression (Slot 12)
    ItemStack valueItem = new ItemStack(Items.REPEATER);
    valueItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lEdit Schedule Value"));
    String currentValueStr = working.getType() == ScheduleValue.Mode.DURATION
      ? (working.getDuration() != null ? working.getDuration().toString() : "1d")
      : (working.getExpression() != null ? working.getExpression() : "0 0 * * *");

    List<String> valueLore = new ArrayList<>();
    valueLore.add("&7Format: &f" + (working.getType() == ScheduleValue.Mode.DURATION ? "Duration (e.g. 1d, 12h, 30m)" : "UNIX Cron (e.g. 0 0 * * *)"));
    valueLore.add("&7Current: &e" + currentValueStr);
    valueLore.add("");
    valueLore.add("&eClick to edit via chat.");
    valueItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(valueLore)));

    template.set(12, GooeyButton.builder()
      .display(valueItem)
      .onClick(action -> {
        String promptMsg = working.getType() == ScheduleValue.Mode.DURATION
          ? "Enter duration (e.g. 1d, 12h, 30m, 60s):"
          : "Enter UNIX cron expression (e.g. 0 0 * * * for daily):";
        ChatPrompt.prompt(player, promptMsg, input -> {
          ScheduleValue updated = working;
          if (input != null && !input.trim().isEmpty()) {
            String trimmed = input.trim();
            try {
              if (working.getType() == ScheduleValue.Mode.DURATION) {
                DurationValue duration = DurationValue.parse(trimmed);
                updated = ScheduleValue.ofDuration(duration);
                player.sendMessage(Text.literal("§a[Editor] Duration set to: " + duration));
              } else {
                Cron cron = CRON_PARSER.parse(trimmed);
                cron.validate();
                updated = ScheduleValue.ofCron(trimmed, working.getZoneId());
                player.sendMessage(Text.literal("§a[Editor] Cron expression set to: " + trimmed));
              }
            } catch (Exception e) {
              player.sendMessage(Text.literal("§c[Editor] Invalid format: " + e.getMessage()));
            }
          }
          open(player, updated, onSave, onBack);
        });
      })
      .build());

    // 3. Timezone (Slot 14 - active only in CRON mode)
    ItemStack tzItem = new ItemStack(working.getType() == ScheduleValue.Mode.CRON ? Items.COMPASS : Items.GRAY_DYE);
    tzItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&e&lTimezone"));
    List<String> tzLore = new ArrayList<>();
    if (working.getType() == ScheduleValue.Mode.CRON) {
      String zoneStr = working.getZoneId() != null && !working.getZoneId().isBlank() ? working.getZoneId() : ZoneId.systemDefault().getId();
      tzLore.add("&7Current Zone: &b" + zoneStr);
      tzLore.add("");
      tzLore.add("&eClick to change timezone.");
    } else {
      tzLore.add("&7Only applicable for &bCRON &7mode.");
    }
    tzItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(tzLore)));

    template.set(14, GooeyButton.builder()
      .display(tzItem)
      .onClick(action -> {
        if (working.getType() != ScheduleValue.Mode.CRON) return;
        ChatPrompt.prompt(player, "Enter ZoneId (e.g. UTC, America/New_York, Europe/Madrid):", input -> {
          ScheduleValue updated = working;
          if (input != null && !input.trim().isEmpty()) {
            try {
              ZoneId zoneId = ZoneId.of(input.trim());
              updated = ScheduleValue.ofCron(working.getExpression(), zoneId.getId());
              player.sendMessage(Text.literal("§a[Editor] Timezone set to: " + zoneId.getId()));
            } catch (Exception e) {
              player.sendMessage(Text.literal("§c[Editor] Invalid timezone: " + e.getMessage()));
            }
          }
          open(player, updated, onSave, onBack);
        });
      })
      .build());

    // 4. Execution Preview (Slot 16)
    ItemStack previewItem = new ItemStack(Items.EMERALD);
    previewItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&a&lSchedule Preview"));
    List<String> previewLore = new ArrayList<>();
    try {
      long delayMillis = working.toDelayMillis();
      long nextEpoch = working.toNextEpochMillis();
      long delaySeconds = delayMillis / 1000L;
      long hours = delaySeconds / 3600L;
      long minutes = (delaySeconds % 3600L) / 60L;
      long seconds = delaySeconds % 60L;

      previewLore.add("&7Next run in: &e" + hours + "h " + minutes + "m " + seconds + "s");
      previewLore.add("&7Next run epoch: &b" + Instant.ofEpochMilli(nextEpoch).toString());
    } catch (Exception e) {
      previewLore.add("&cError evaluating schedule: " + e.getMessage());
    }
    previewItem.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(previewLore)));
    template.set(16, GooeyButton.builder().display(previewItem).build());

    // 5. Back / Cancel Button (Slot 27)
    template.set(27, UIUtils.getCancelButton(action -> {
      if (onBack != null) {
        onBack.run();
      } else {
        UIManager.closeUI(action.getPlayer());
      }
    }));

    // 6. Save & Confirm Button (Slot 35)
    template.set(35, UIUtils.getConfirmButton(action -> {
      if (onSave != null) {
        onSave.accept(working);
      }
      player.sendMessage(Text.literal("§a[Editor] Schedule saved successfully!"));
      if (onBack != null) {
        onBack.run();
      } else {
        UIManager.closeUI(action.getPlayer());
      }
    }));

    GooeyPage page = GooeyPage.builder()
      .template(template)
      .title(AdventureTranslator.toNative("&8Schedule Editor"))
      .build();

    CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
  }
}
