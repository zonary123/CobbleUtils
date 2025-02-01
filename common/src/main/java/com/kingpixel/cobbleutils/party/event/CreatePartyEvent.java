package com.kingpixel.cobbleutils.party.event;

import com.kingpixel.cobbleutils.party.models.PartyData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 28/06/2024 8:43
 */
public class CreatePartyEvent {
  private final List<CreatePartyListener> partyCreatedListeners = new ArrayList<>();
  public static final CreatePartyEvent CREATE_PARTY_EVENT = new CreatePartyEvent();

  public void register(CreatePartyListener listener) {
    partyCreatedListeners.add(listener);
  }

  public void unregister(CreatePartyListener listener) {
    partyCreatedListeners.remove(listener);
  }

  public void emit(PartyData partyData) {
    notifyPartyCreated(partyData);
  }

  private void notifyPartyCreated(PartyData partyData) {
    for (CreatePartyListener listener : partyCreatedListeners) {
      listener.onCreateParty(partyData);
    }
  }

  public void clear() {
    partyCreatedListeners.clear();
  }
}
