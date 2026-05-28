package org.agmas.noellesroles.roles.bartender;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 酒保“去毒试剂护盾”的死亡保护处理器。
 */
public final class BartenderDeathProtectionHandler {

    private BartenderDeathProtectionHandler() {
    }

    /**
     * 处理防御试剂护盾挡死。
     *
     * <p>这里保留旧实现的两个关键回放细节：</p>
     * <p>1. 统一走 wathe 的伤害来源解析，避免手雷 / 投掷类显示成未知物品；</p>
     * <p>2. 把 {@code death_reason} 一并写进 shield_blocked，方便无物品来源的伤害退回显示死因名。</p>
     */
    public static boolean allowDeath(PlayerEntity playerEntity, PlayerEntity killer, Identifier deathReason) {
        BartenderPlayerComponent bartenderPlayerComponent = BartenderPlayerComponent.KEY.get(playerEntity);
        if (bartenderPlayerComponent.armor <= 0) {
            return true;
        }

        if (playerEntity instanceof ServerPlayerEntity victimPlayer) {
            NbtCompound blockedReplayData = GameFunctions.createBlockedDamageReplayData(killer, deathReason);
            Identifier damageItem = GameFunctions.getReplayItemId(blockedReplayData);

            /*
             * 飞斧是 noellesroles 的扩展死因，旧实现里专门给了一个兜底，
             * 避免某些特殊情况下 replay item 仍为空时，回放掉成“未知物品”。
             */
            if (damageItem == null && Noellesroles.DEATH_REASON_THROWING_AXE.equals(deathReason)) {
                damageItem = Registries.ITEM.getId(ModItems.THROWING_AXE);
            }

            GameRecordManager.recordShieldBlocked(
                    victimPlayer,
                    killer instanceof ServerPlayerEntity killerPlayer ? killerPlayer : null,
                    Noellesroles.DEFENSE_TRAY_EFFECT,
                    damageItem,
                    blockedReplayData
            );
        }

        playerEntity.getWorld().playSound(
                playerEntity,
                playerEntity.getBlockPos(),
                WatheSounds.ITEM_PSYCHO_ARMOUR,
                SoundCategory.MASTER,
                5.0F,
                1.0F
        );
        bartenderPlayerComponent.armor--;
        return false;
    }
}
