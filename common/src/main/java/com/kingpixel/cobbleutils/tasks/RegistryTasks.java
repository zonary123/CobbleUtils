package com.kingpixel.cobbleutils.tasks;

import com.kingpixel.cobbleutils.CobbleUtils;

/**
 *
 * @author Carlos Varas Alonso - 30/12/2025 4:27
 */
public class RegistryTasks {
  public static void register() {
    TaskStorageNotification.register();

    // Cross-server tasks
    if (!CobbleUtils.config.isRedisMessaging()) return;
    TaskServers.register();
  }
}
