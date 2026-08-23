package org.agmas.noellesroles.roles.waiter;

import dev.doctor4t.wathe.api.tray.TrayEffectHandler;
import dev.doctor4t.wathe.api.tray.TrayEffectRegistry;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.TrayEffectUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 服务员“递予物品”附带效果的共享执行器。
 *
 * <p>厨师投喂食物时要求完整触发服务员那套毒药/托盘试剂效果，因此这些效果不能继续藏在
 * {@link WaiterInteractionHandler} 的私有方法里。这里集中处理三件事：</p>
 * <p>1. Wathe 原生毒药组件 POISONER；</p>
 * <p>2. Noelles/Wathe 托盘扩展效果 TRAY_EFFECT；</p>
 * <p>3. 回放需要展示的“带有[试剂名]”文本数据。</p>
 */
public final class WaiterDeliveryEffects {
    private WaiterDeliveryEffects() {
    }

    /**
     * 对被递予/投喂的玩家应用物品快照上携带的毒药和托盘试剂。
     *
     * @param target 真正吃下/接收效果的玩家。
     * @param replaySnapshot 成功递出的那一份物品快照，必须在消耗手上物品前复制。
     * @param serviceType 服务类型，用于把 eat_food / drink_potion 等消费语义传给托盘效果。
     */
    public static void applyDeliveredStackEffects(
            ServerPlayerEntity target,
            ItemStack replaySnapshot,
            WaiterServiceItems.ServiceType serviceType
    ) {
        // Wathe 原毒药不是普通 trayEffect，而是 POISONER 组件，所以必须先单独处理。
        applyPoisonEffect(target, replaySnapshot);

        Identifier trayEffectId = TrayEffectUtils.getTrayEffectId(replaySnapshot);
        if (trayEffectId == null) {
            return;
        }

        TrayEffectHandler handler = TrayEffectRegistry.getByEffectId(trayEffectId);
        if (handler != null) {
            /*
             * defense_vial / delusion_vial / sedative 等托盘效果都会通过 Wathe 的统一接口应用到目标。
             * 这里不直接判断具体试剂类型，是为了让后续新增托盘效果时厨师和服务员自然同时兼容。
             */
            handler.onConsume(
                    target,
                    replaySnapshot,
                    serviceType.consumeType(),
                    TrayEffectUtils.getTrayEffectOwner(replaySnapshot)
            );
        }
    }

    /**
     * 从物品快照读取回放里的试剂/毒药名称。
     *
     * <p>服务员递予和厨师投喂都要在回放中显示“带有[某试剂]”，因此统一在这里解析。</p>
     */
    public static @Nullable EffectReplayInfo getEffectReplayInfo(ItemStack replaySnapshot) {
        if (replaySnapshot.contains(WatheDataComponentTypes.POISONER)) {
            return new EffectReplayInfo(WatheItems.POISON_VIAL.getTranslationKey(), "Poison");
        }

        Identifier trayEffectId = TrayEffectUtils.getTrayEffectId(replaySnapshot);
        if (trayEffectId == null) {
            return null;
        }
        if (trayEffectId.equals(NoellesEventIds.DEFENSE_TRAY_EFFECT)) {
            return new EffectReplayInfo("item.noellesroles.defense_vial", "Defense Vial");
        }
        if (trayEffectId.equals(NoellesEventIds.DELUSION_TRAY_EFFECT)) {
            return new EffectReplayInfo("item.noellesroles.delusion_vial", "Delusion Vial");
        }
        if (trayEffectId.equals(NoellesEventIds.SEDATIVE_TRAY_EFFECT)) {
            return new EffectReplayInfo("item.noellesroles.sedative", "Sedative");
        }
        if (trayEffectId.equals(NoellesEventIds.TIMED_BOMB_TRAY_EMBEDDED_EVENT)) {
            return new EffectReplayInfo("item.noellesroles.timed_bomb", "Timed Bomb");
        }
        return new EffectReplayInfo("effect." + trayEffectId.toString().replace(':', '.'), trayEffectId.getPath());
    }

    private static void applyPoisonEffect(ServerPlayerEntity target, ItemStack replaySnapshot) {
        // 没有 POISONER 组件代表这份物品没有毒，不做任何额外处理。
        String poisoner = replaySnapshot.getOrDefault(WatheDataComponentTypes.POISONER, null);
        if (poisoner == null) {
            return;
        }

        UUID poisonerUuid;
        try {
            poisonerUuid = UUID.fromString(poisoner);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        PlayerPoisonComponent poison = PlayerPoisonComponent.KEY.get(target);
        int currentPoisonTicks = poison.poisonTicks;
        /*
         * Wathe 毒药第一次中毒会随机生成结算时间；如果已经中毒，则再次递毒会提前结算。
         * 服务员和厨师共用这一段，保证“递予”和“投喂”的毒药节奏完全一致。
         */
        int poisonTicks = currentPoisonTicks == -1
                ? target.getWorld().getRandom().nextBetween(PlayerPoisonComponent.clampTime.getLeft(), PlayerPoisonComponent.clampTime.getRight())
                : MathHelper.clamp(
                        currentPoisonTicks - target.getWorld().getRandom().nextBetween(
                                WaiterConstants.POISON_STACK_ACCELERATION_MIN_TICKS,
                                WaiterConstants.POISON_STACK_ACCELERATION_MAX_TICKS
                        ),
                        0,
                        PlayerPoisonComponent.clampTime.getRight()
                );

        NbtCompound poisonData = new NbtCompound();
        poisonData.putString("item", Registries.ITEM.getId(replaySnapshot.getItem()).toString());
        poisonData.putString("item_name", Text.Serialization.toJsonString(replaySnapshot.getName(), target.getRegistryManager()));
        poison.setDetailedPoisonTicks(poisonTicks, poisonerUuid, GameConstants.DeathReasons.POISON, poisonData);
    }

    /**
     * 回放中展示试剂/毒药名所需的本地化 key 和英文兜底。
     */
    public record EffectReplayInfo(String translationKey, String fallback) {
    }
}
