package org.agmas.noellesroles.roles.prophet;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

/**
 * 先知“揭露后免疫巫毒魔法”的死亡处理器。
 */
public final class ProphetDeathProtectionHandler {

    private ProphetDeathProtectionHandler() {
    }

    /**
     * 只拦截巫毒魔法这一种死亡来源。
     *
     * <p>这里沿用旧逻辑，不去误伤其它正常击杀：</p>
     * <p>1. 如果不是 {@code voodoo} 死因，直接放行；</p>
     * <p>2. 如果玩家没有先知庇护，也直接放行；</p>
     * <p>3. 只有两者都满足时，才记一条全局回放并阻止死亡。</p>
     */
    public static boolean allowDeath(PlayerEntity playerEntity, PlayerEntity killer, Identifier deathReason) {
        if (!Noellesroles.VOODOO_MAGIC_DEATH_REASON.equals(deathReason)) {
            return true;
        }

        ProphetPlayerComponent prophetComponent = ProphetPlayerComponent.KEY.get(playerEntity);
        if (!prophetComponent.isImmuneToVoodooMagic()) {
            return true;
        }

        if (playerEntity instanceof ServerPlayerEntity protectedPlayer) {
            /*
             * 这里必须继续读取 wathe 暂存的 pending extra death data。
             * 因为某些巫毒击杀并没有传统 killer，
             * 真正应该在回放里展示的施法者，会通过 replay_actor 这条链路传进来。
             */
            NbtCompound pendingDeathData = GameFunctions.getPendingExtraDeathData();
            UUID prophetUuid = prophetComponent.getVoodooImmunityProvider();
            UUID voodooCasterUuid = pendingDeathData != null && pendingDeathData.containsUuid("replay_actor")
                    ? pendingDeathData.getUuid("replay_actor")
                    : killer != null ? killer.getUuid() : null;

            GameRecordManager.event(GameRecordTypes.GLOBAL_EVENT)
                    .world(protectedPlayer.getServerWorld())
                    .actor(protectedPlayer)
                    .put("event", Noellesroles.PROPHET_VOODOO_IMMUNITY_EVENT.toString())
                    .put("death_reason_id", deathReason.toString())
                    .putUuid("prophet_player", prophetUuid != null ? prophetUuid : protectedPlayer.getUuid())
                    .putUuid("voodoo_player", voodooCasterUuid != null ? voodooCasterUuid : protectedPlayer.getUuid())
                    .record();
        }

        return false;
    }
}
