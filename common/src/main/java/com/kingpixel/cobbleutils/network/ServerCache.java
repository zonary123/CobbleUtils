package com.kingpixel.cobbleutils.network;

/**
 *
 * @author Carlos Varas Alonso - 30/12/2025 4:30
 */

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServerCache {
  private static final Set<String> SERVERS = Collections.newSetFromMap(new ConcurrentHashMap<>());

  public static void updateServers(String[] servers) {
    SERVERS.clear();
    Collections.addAll(SERVERS, servers);
  }

  public static Set<String> getServers() {
    return Collections.unmodifiableSet(SERVERS);
  }
}
