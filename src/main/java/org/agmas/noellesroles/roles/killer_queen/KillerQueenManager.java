package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.config.HarpyModLoaderConfig;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.convener.ConvenerSummonLockdownHelper;
import org.agmas.noellesroles.roles.outlaw.OutlawWorldComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 杀手皇后的服务端 tick、标记收束、疯魔压制和招募转职管理器。 */
public final class KillerQueenManager {
    private KillerQueenManager() {
    }

    public static void tickWorld(ServerWorld world) {
        KillerQueenWorldComponent state = KillerQueenWorldComponent.KEY.get(world);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (!OutlawWorldComponent.KEY.get(world).hasActiveOutlaw()) {
            /* 暂停期间掉线的杀手皇后重新上线后，也要在亡命时刻结束时恢复同一次疯魔。 */
            KillerQueenPsychoHandler.resumeAfterOutlaw(world);
        }

        /* 标记只对当前仍存活且仍属于好人/独立中立的玩家有效。 */
        for (UUID targetUuid : state.getAllMarkedTargets()) {
            ServerPlayerEntity target = world.getServer().getPlayerManager().getPlayer(targetUuid);
            if (target == null || !KillerQueenRules.isLivingGoodOrIndependent(target)) {
                state.removeAllMarkersForTarget(targetUuid);
            }
        }

        boolean active = hasActivePsycho(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!state.hasPsychoSession(player.getUuid())) continue;
            if (PsychoModeApi.isActive(player, KillerQueenPsychoHandler.PROFILE_ID)) continue;
            /* 亡命时刻暂停保存了快照，不属于疯魔真正结束。 */
            if (state.getSuspendedPsycho(player.getUuid()) != null) {
                if (!GameFunctions.isPlayerAliveAndSurvival(player) || !KillerQueenRules.isKillerQueen(player)) {
                    finishPsychoSession(player, true);
                }
                continue;
            }
            finishPsychoSession(player, true);
        }

        if (active) {
            suppressOtherPsychoModes(world, state, gameWorld);
            for (ServerPlayerEntity convener : world.getPlayers(player ->
                    GameFunctions.isPlayerAliveAndSurvival(player) && gameWorld.isRole(player, NoellesRoleRegistry.CONVENER))) {
                AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(convener);
                if (ability.cooldown < KillerQueenConstants.CONVENER_MIN_ABILITY_COOLDOWN_TICKS) {
                    ability.setCooldown(KillerQueenConstants.CONVENER_MIN_ABILITY_COOLDOWN_TICKS);
                }
            }
        } else {
            state.clearSuppressedPsychoProfiles();
        }

        retargetInvalidRecruitments(world, state);
    }

    public static boolean hasActivePsycho(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (PsychoModeApi.isActive(player, KillerQueenPsychoHandler.PROFILE_ID)) return true;
        }
        return false;
    }

    private static void suppressOtherPsychoModes(ServerWorld world, KillerQueenWorldComponent state,
                                                  GameWorldComponent gameWorld) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            Role role = gameWorld.getRole(player);
            if (!KillerQueenRules.isKillerOrKillerSidedNeutral(role)
                    || PsychoModeApi.isActive(player, KillerQueenPsychoHandler.PROFILE_ID)) continue;
            var profile = PsychoModeApi.getActiveProfile(player);
            if (profile != null) {
                state.setSuppressedPsychoProfile(player.getUuid(), profile.id());
                PsychoModeApi.stop(player, true);
            }
        }
        for (Map.Entry<UUID, net.minecraft.util.Identifier> entry : state.getSuppressedPsychoProfiles().entrySet()) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                PsychoModeApi.ensureMinimumCooldown(player, entry.getValue(),
                        KillerQueenConstants.SUPPRESSED_PSYCHO_MIN_COOLDOWN_TICKS);
            }
        }
    }

    public static void finishPsychoSession(ServerPlayerEntity queen, boolean rollbackIfNeeded) {
        KillerQueenWorldComponent state = KillerQueenWorldComponent.KEY.get(queen.getServerWorld());
        Set<UUID> addedDuringPsycho = state.finishPsychoSession(queen.getUuid());
        if (!rollbackIfNeeded || !hasUnmarkedEligibleTarget(queen)) return;
        for (UUID targetUuid : addedDuringPsycho) {
            if (!state.removeMarker(queen.getUuid(), targetUuid)) continue;
            KillerQueenReplayHelper.record(queen, NoellesEventIds.KILLER_QUEEN_MARK_ROLLBACK_EVENT,
                    queen, targetUuid, null);
        }
    }

    private static boolean hasUnmarkedEligibleTarget(ServerPlayerEntity queen) {
        KillerQueenWorldComponent state = KillerQueenWorldComponent.KEY.get(queen.getServerWorld());
        return queen.getServerWorld().getPlayers(KillerQueenRules::isLivingGoodOrIndependent).stream()
                .anyMatch(target -> !state.isMarkedBy(queen.getUuid(), target.getUuid()));
    }

    private static void retargetInvalidRecruitments(ServerWorld world, KillerQueenWorldComponent state) {
        for (UUID recruitUuid : state.getRecruitedPlayers()) {
            ServerPlayerEntity recruit = world.getServer().getPlayerManager().getPlayer(recruitUuid);
            KillerQueenWorldComponent.Recruitment recruitment = state.getRecruitment(recruitUuid);
            if (recruit == null || recruitment == null || !GameFunctions.isPlayerAliveAndSurvival(recruit)) continue;
            ServerPlayerEntity current = world.getServer().getPlayerManager().getPlayer(recruitment.target());
            if (current != null && KillerQueenRules.isLivingGoodOrIndependent(current)) continue;
            ServerPlayerEntity replacement = selectRecruitmentTarget(world, recruit);
            if (replacement == null) continue;
            UUID oldTarget = recruitment.target();
            state.updateRecruitmentTarget(recruitUuid, replacement.getUuid());
            KillerQueenReplayHelper.record(recruit, NoellesEventIds.KILLER_QUEEN_RECRUIT_TARGET_CHANGED_EVENT,
                    recruit, oldTarget, replacement.getUuid());
        }
    }

    public static ServerPlayerEntity selectRecruitmentTarget(ServerWorld world, ServerPlayerEntity recruit) {
        List<ServerPlayerEntity> candidates = new ArrayList<>(world.getPlayers(player ->
                !player.getUuid().equals(recruit.getUuid()) && KillerQueenRules.isLivingGoodOrIndependent(player)));
        Collections.shuffle(candidates);
        return candidates.isEmpty() ? null : candidates.getFirst();
    }

    public static void completeRecruitment(ServerPlayerEntity recruit, KillerQueenWorldComponent.Recruitment recruitment) {
        KillerQueenWorldComponent state = KillerQueenWorldComponent.KEY.get(recruit.getServerWorld());
        state.clearRecruitment(recruit.getUuid());
        state.removeMarker(recruitment.queen(), recruit.getUuid());
        transformToKiller(recruit, selectRandomEnabledKiller());
        PlayerShopComponent.KEY.get(recruit).setBalance(KillerQueenConstants.RECRUITED_KILLER_STARTING_COINS);
        PlayerShopComponent.KEY.get(recruit).sync();
    }

    public static Role selectRandomEnabledKiller() {
        List<Role> roles = new ArrayList<>(WatheRoles.ROLES);
        roles.removeIf(role -> role.getFaction() != Faction.KILLER || isDisabled(role));
        if (roles.isEmpty()) roles.add(WatheRoles.KILLER);
        Collections.shuffle(roles);
        return roles.getFirst();
    }

    public static void transformToKiller(ServerPlayerEntity player, Role role) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getServerWorld());
        gameWorld.addRole(player, role);
        ModdedRoleAssigned.EVENT.invoker().assignModdedRole(player, role);
        sendRoleAnnouncement(player, gameWorld, role);
    }

    public static boolean isDisabled(Role role) {
        return HarpyModLoaderConfig.HANDLER.instance().disabled.contains(role.identifier().toString())
                || HarpyModLoaderConfig.HANDLER.instance().disabled.contains(role.identifier().getPath());
    }

    public static void dropAllRevolvers(ServerPlayerEntity player) {
        dropRevolvers(player, player.getInventory().main);
        dropRevolvers(player, player.getInventory().offHand);
        player.getInventory().markDirty();
        player.playerScreenHandler.sendContentUpdates();
    }

    private static void dropRevolvers(ServerPlayerEntity player, DefaultedList<ItemStack> stacks) {
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (!stack.isOf(WatheItems.REVOLVER)) continue;
            player.dropItem(stack.copy(), true, false);
            stacks.set(slot, ItemStack.EMPTY);
        }
    }

    private static void sendRoleAnnouncement(ServerPlayerEntity player, GameWorldComponent gameWorld, Role role) {
        RoleAnnouncementTexts.RoleAnnouncementText announcement = Harpymodloader.VANNILA_ROLES.contains(role)
                ? RoleAnnouncementTexts.KILLER : Harpymodloader.autogeneratedAnnouncements.get(role);
        if (announcement == null) announcement = RoleAnnouncementTexts.KILLER;
        ServerPlayNetworking.send(player, new AnnounceWelcomePayload(
                RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(announcement),
                gameWorld.getAllKillerTeamPlayers().size(), 0));
    }
}
