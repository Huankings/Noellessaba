package org.agmas.noellesroles.roles.convener;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 召集者的巫毒免疫与反伤护盾。
 */
public final class ConvenerDeathProtectionHandler {
    private static final Set<UUID> SHIELD_PROCESSING = new HashSet<>();
    private static final Set<UUID> COUNTER_KILL_PROCESSING = new HashSet<>();

    private ConvenerDeathProtectionHandler() {
    }

    public static boolean allowDeath(PlayerEntity victim, PlayerEntity killer, Identifier deathReason) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(victim.getWorld());
        if (!gameWorld.isRole(victim, Noellesroles.CONVENER)) {
            return true;
        }

        if (Noellesroles.VOODOO_MAGIC_DEATH_REASON.equals(deathReason)) {
            recordVoodooImmunity(victim, killer);
            return false;
        }
        return handleCounterShield(victim, killer, deathReason);
    }

    private static void recordVoodooImmunity(PlayerEntity victim, PlayerEntity killer) {
        if (!(victim instanceof ServerPlayerEntity convener)) {
            return;
        }

        NbtCompound pendingDeathData = GameFunctions.getPendingExtraDeathData();
        UUID voodooCasterUuid = pendingDeathData != null && pendingDeathData.containsUuid("replay_actor")
                ? pendingDeathData.getUuid("replay_actor")
                : killer != null ? killer.getUuid() : null;
        if (voodooCasterUuid == null) {
            return;
        }

        NbtCompound extra = new NbtCompound();
        extra.putUuid("voodoo_player", voodooCasterUuid);
        GameRecordManager.recordGlobalEvent(convener.getServerWorld(), Noellesroles.CONVENER_VOODOO_IMMUNITY_EVENT, convener, extra);
    }

    private static boolean handleCounterShield(PlayerEntity victim, PlayerEntity killer, Identifier deathReason) {
        if (!ConvenerConstants.COUNTER_SHIELD_ENABLED
                || Noellesroles.CONVENER_COUNTER_KILL_DEATH_REASON.equals(deathReason)) {
            return true;
        }

        ConvenerPlayerComponent convener = ConvenerPlayerComponent.KEY.get(victim);
        if (!convener.hasCounterShield() || !SHIELD_PROCESSING.add(victim.getUuid())) {
            return true;
        }

        try {
            if (!convener.consumeCounterShield()) {
                return true;
            }
            convener.sync();

            if (victim instanceof ServerPlayerEntity serverConvener) {
                GameRecordManager.recordShieldBlocked(
                        serverConvener,
                        killer instanceof ServerPlayerEntity serverKiller ? serverKiller : null,
                        Noellesroles.CONVENER_COUNTER_SHIELD_SOURCE,
                        GameFunctions.resolveDamageItemForBlockedDeath(killer, deathReason),
                        buildBlockedExtra(deathReason)
                );
            }

            if (killer != null && killer != victim && GameFunctions.isPlayerAliveAndSurvival(killer)) {
                tryCounterKill(victim, killer);
            }
            return false;
        } finally {
            SHIELD_PROCESSING.remove(victim.getUuid());
        }
    }

    private static void tryCounterKill(PlayerEntity convener, PlayerEntity killer) {
        if (!COUNTER_KILL_PROCESSING.add(killer.getUuid())) {
            return;
        }

        try {
            GameFunctions.killPlayer(killer, true, convener, Noellesroles.CONVENER_COUNTER_KILL_DEATH_REASON);
            if (GameFunctions.isPlayerAliveAndSurvival(killer)) {
                PlayerMoodComponent.KEY.get(killer).setMood(0f);
            }
        } finally {
            COUNTER_KILL_PROCESSING.remove(killer.getUuid());
        }
    }

    private static NbtCompound buildBlockedExtra(Identifier deathReason) {
        NbtCompound extra = new NbtCompound();
        extra.putString("death_reason", deathReason.toString());
        return extra;
    }
}
