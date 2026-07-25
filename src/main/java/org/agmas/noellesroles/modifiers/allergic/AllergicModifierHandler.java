package org.agmas.noellesroles.modifiers.allergic;

import dev.doctor4t.wathe.api.event.AllowPlayerDeath;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.agmas.harpymodloader.events.ModifierAssigned;
import org.agmas.harpymodloader.events.ResetPlayerEvent;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;

/**
 * 过敏患者词条的生命周期和免死护盾接入。
 */
public final class AllergicModifierHandler {
    private static boolean initialized = false;

    private AllergicModifierHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ModifierAssigned.EVENT.register((player, modifier) -> {
            if (!modifier.equals(NoellesModifierRegistry.ALLERGIC)) {
                return;
            }
            /*
             * 词条分配由 Harpy 在服务端完成；这里只在服务端玩家身上初始化组件，
             * 避免客户端本地世界误写随机过敏类型后与服务端状态打架。
             */
            if (player instanceof ServerPlayerEntity) {
                AllergicPlayerComponent.KEY.get(player).assignRandomType();
            }
        });

        ResetPlayerEvent.EVENT.register(player -> AllergicPlayerComponent.KEY.get(player).reset());

        AllowPlayerDeath.EVENT.register(AllergicModifierHandler::allowDeath);
    }

    private static boolean allowDeath(PlayerEntity victim, PlayerEntity killer, Identifier deathReason) {
        AllergicPlayerComponent allergic = AllergicPlayerComponent.KEY.get(victim);
        if (!allergic.isAllergic() || !allergic.hasShield()) {
            return true;
        }

        PlayerPoisonComponent poison = PlayerPoisonComponent.KEY.get(victim);
        /*
         * 保留 Starry 旧语义：如果这次死亡来自过敏患者自己触发的毒，不允许护盾抵挡。
         * 其它来源的致命伤害才会消耗过敏护盾并清除身上的毒计时。
         */
        if (victim.getUuid().equals(poison.getPoisoner())) {
            return true;
        }

        if (victim instanceof ServerPlayerEntity victimPlayer) {
            NbtCompound damageReplayData = GameFunctions.createBlockedDamageReplayData(killer, deathReason);
            GameRecordManager.recordShieldBlocked(
                    victimPlayer,
                    killer instanceof ServerPlayerEntity attacker ? attacker : null,
                    NoellesEventIds.ALLERGIC_SHIELD_SOURCE,
                    null,
                    damageReplayData
            );
        }

        allergic.consumeShield();
        poison.reset();
        victim.playSoundToPlayer(WatheSounds.ITEM_PSYCHO_ARMOUR, SoundCategory.MASTER, 5.0F, 1.0F);
        return false;
    }
}
