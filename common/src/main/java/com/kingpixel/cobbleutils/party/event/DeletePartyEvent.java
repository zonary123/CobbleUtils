package com.kingpixel.cobbleutils.party.event;

import com.kingpixel.cobbleutils.party.models.PartyData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 28/06/2024 8:44
 */
public class DeletePartyEvent {
  private final List<DeletePartyListener> partyDeleteListeners = new ArrayList<>();
  public static final DeletePartyEvent DELETE_PARTY_EVENT = new DeletePartyEvent();

  public void register(DeletePartyListener listener) {
    partyDeleteListeners.add(listener);
  }

  public void unregister(DeletePartyListener listener) {
    partyDeleteListeners.remove(listener);
  }

  public void emit(PartyData partyData) {
    notifyPartyCreated(partyData);
  }

  private void notifyPartyCreated(PartyData partyData) {
    for (DeletePartyListener listener : partyDeleteListeners) {
      listener.onDeleteParty(partyData);
    }
  }

  public void clear() {
    partyDeleteListeners.clear();
  }
}
