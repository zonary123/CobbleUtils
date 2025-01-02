package com.kingpixel.cobbleutils.managers;

import com.kingpixel.cobbleutils.Model.RewardsData;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Carlos Varas Alonso - 28/06/2024 9:47
 */
@Getter

public class RewardsManager {
  private Map<UUID, RewardsData> rewardsData;

  public RewardsManager() {
    rewardsData = new ConcurrentHashMap<>();
  }

  public RewardsManager(Map<UUID, RewardsData> rewardsData) {
    this.rewardsData = rewardsData;
  }


}
