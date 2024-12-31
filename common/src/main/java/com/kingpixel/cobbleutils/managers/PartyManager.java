package com.kingpixel.cobbleutils.managers;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.PlayerInfo;
import com.kingpixel.cobbleutils.party.event.DeletePartyEvent;
import com.kingpixel.cobbleutils.party.models.PartyCreateResult;
import com.kingpixel.cobbleutils.party.models.PartyData;
import com.kingpixel.cobbleutils.party.models.UserParty;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.Data;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class PartyManager {
  private Map<UUID, PartyData> parties = new ConcurrentHashMap<>();
  private Map<UUID, UserParty> userParty = new ConcurrentHashMap<>();

  public PartyData getParty(ServerPlayerEntity player) {
    UserParty userPartyData = userParty.get(player.getUuid());
    return userPartyData != null ? parties.get(userPartyData.getPartyId()) : null;
  }

  public boolean isPlayerInParty(ServerPlayerEntity player) {
    return userParty.containsKey(player.getUuid());
  }

  public PartyCreateResult createParty(ServerPlayerEntity player, String partyName) {
    if (isPlayerInParty(player)) return PartyCreateResult.ALREADY_IN_PARTY;
    if (partyName.length() > CobbleUtils.partyConfig.getCharacterLimit()) return PartyCreateResult.NAME_TOO_LONG;
    PartyData newParty = new PartyData(partyName, PlayerInfo.fromPlayer(player));
    newParty.init();
    UUID partyId = newParty.getId();
    this.parties.put(partyId, newParty);
    this.userParty.put(player.getUuid(), new UserParty(partyId));
    return PartyCreateResult.SUCCESS;
  }

  public void sendInvite(ServerPlayerEntity player, ServerPlayerEntity invite) {
    PartyData party = getParty(player);
    if (party != null && isOwner(player)) {
      party.getInvites().add(invite.getUuid());
      PlayerUtils.sendMessage(player,
        CobbleUtils.partyLang.getPartyInviteSend()
          .replace("%player%", invite.getGameProfile().getName())
          .replace("%partyname%", party.getName()),
        CobbleUtils.partyLang.getPrefix()
      );
      PlayerUtils.sendMessage(invite,
        CobbleUtils.partyLang.getPartyInviteSend()
          .replace("%player%", player.getGameProfile().getName())
          .replace("%partyname%", party.getName()),
        CobbleUtils.partyLang.getPrefix()
      );
    }
  }

  public boolean leaveParty(ServerPlayerEntity player) {
    PartyData party = getParty(player);
    if (party == null) return false;

    party.getMembers().removeIf(member -> member.getPlayeruuid().equals(player.getUuid()));
    userParty.remove(player.getUuid());
    removeParty(party);
    player.sendMessage(
      AdventureTranslator.toNative(
        CobbleUtils.partyLang.getPartyLeave()
          .replace("%partyname%", party.getName()),
        CobbleUtils.partyLang.getPrefix()
      )
    );
    return true;
  }

  public boolean kickPlayer(ServerPlayerEntity player, ServerPlayerEntity target) {
    PartyData party = getParty(player);
    if (party == null || !isOwner(player) || player.getUuid().equals(target.getUuid())) return false;
    party.getMembers().removeIf(member -> member.getPlayeruuid().equals(target.getUuid()));
    userParty.remove(target.getUuid());
    removeParty(party);
    player.sendMessage(
      AdventureTranslator.toNative(
        CobbleUtils.partyLang.getPartyKick()
          .replace("%partyname%", party.getName())
          .replace("%player%", target.getGameProfile().getName()),
        CobbleUtils.partyLang.getPrefix()
      )
    );
    target.sendMessage(
      AdventureTranslator.toNative(
        CobbleUtils.partyLang.getPartyKickOther()
          .replace("%partyname%", party.getName())
          .replace("%player%", player.getGameProfile().getName()),
        CobbleUtils.partyLang.getPrefix()
      )
    );
    return true;
  }

  public void removeParty(PartyData partyData) {
    if (partyData.getMembers().isEmpty()) {
      DeletePartyEvent.DELETE_PARTY_EVENT.emit(partyData);
      parties.remove(partyData.getId());
    }
  }

  public ArrayList<PlayerInfo> getMembers(ServerPlayerEntity player) {
    PartyData party = getParty(player);
    return party != null ? new ArrayList<>(party.getMembers()) : new ArrayList<>();
  }

  public boolean acceptInvite(ServerPlayerEntity player, UUID partyId) {
    PartyData party = parties.get(partyId);
    if (party == null || !party.getInvites().remove(player.getUuid())) return false;

    party.getMembers().add(PlayerInfo.fromPlayer(player));
    userParty.put(player.getUuid(), new UserParty(partyId));

    // Limpiar otras invitaciones del jugador
    parties.values().forEach(p -> p.getInvites().remove(player.getUuid()));

    player.sendMessage(
      AdventureTranslator.toNative(
        CobbleUtils.partyLang.getPartyJoin()
          .replace("%partyname%", party.getName()),
        CobbleUtils.partyLang.getPrefix()
      )
    );

    return true;
  }

  public boolean playerCanInvite(ServerPlayerEntity player) {
    return isOwner(player);
  }

  public CompletableFuture<Suggestions> suggestParties(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
    UUID playerId = context.getSource().getPlayer().getUuid();
    parties.values().stream()
      .filter(party -> party.getInvites().contains(playerId))
      .forEach(party -> builder.suggest(party.getName()));
    return builder.buildFuture();
  }

  public boolean isOwner(ServerPlayerEntity player) {
    PartyData party = getParty(player);
    return party != null && party.getOwner().getPlayeruuid().equals(player.getUuid());
  }

  public ArrayList<PartyData> getInvites(ServerPlayerEntity player) {
    ArrayList<PartyData> invites = new ArrayList<>();
    UUID playerId = player.getUuid();
    parties.values().stream()
      .filter(party -> party.getInvites().contains(playerId))
      .forEach(invites::add);
    return invites;
  }
}
