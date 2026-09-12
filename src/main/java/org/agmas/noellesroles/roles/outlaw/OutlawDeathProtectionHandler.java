package org.agmas.noellesroles.roles.outlaw;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import dev.doctor4t.wathe.cca.GameWorldComponent;

/** 亡命徒的通用致死护盾。跌出列车、倒计时结束和掉线死亡不消耗护盾。 */
public final class OutlawDeathProtectionHandler {
    private OutlawDeathProtectionHandler() {
    }

    public static boolean allowDeath(PlayerEntity victim, PlayerEntity killer, Identifier deathReason) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(victim.getWorld());
        if (!gameWorld.isRole(victim, NoellesRoleRegistry.OUTLAW)
                || NoellesDeathReasons.OUTLAW_TIMEOUT_DEATH_REASON.equals(deathReason)
                || NoellesDeathReasons.OUTLAW_OFFLINE_DEATH_REASON.equals(deathReason)
                || GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(deathReason)) {
            return true;
        }

        OutlawPlayerComponent state = OutlawPlayerComponent.KEY.get(victim);
        if (!state.consumeShield()) {
            return true;
        }
        if (victim instanceof ServerPlayerEntity serverVictim) {
            NbtCompound replayData = GameFunctions.createBlockedDamageReplayData(killer, deathReason);
            GameRecordManager.recordShieldBlocked(
                    serverVictim,
                    killer instanceof ServerPlayerEntity serverKiller ? serverKiller : null,
                    NoellesEventIds.OUTLAW_SHIELD_SOURCE,
                    GameFunctions.getReplayItemId(replayData),
                    replayData
            );
        }
        return false;
    }
}
