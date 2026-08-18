package org.agmas.noellesroles.client.items;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.TimekeeperWatchItem;
import org.agmas.noellesroles.roles.hacker.HackerPhoneComponent;
import org.jetbrains.annotations.NotNull;

public class NoellesRolesItemExtraModel {

    /**
     * 获取冷却模型谓词 ID（用于物品模型根据冷却状态变化）
     */
    public static Identifier getCooldownId() {
        return Identifier.of(NoellesRolesCore.MOD_ID, "item_cooldown");
    }

    public static Identifier getKillerGroupId() {
        return Identifier.of(NoellesRolesCore.MOD_ID, "killer_group");
    }

    public static Identifier getTimekeeperWatchStateId() {
        return Identifier.of(NoellesRolesCore.MOD_ID, "timekeeper_watch_state");
    }

    public static Identifier getTimekeeperWatchBrokenId() {
        return Identifier.of(NoellesRolesCore.MOD_ID, "timekeeper_watch_broken");
    }

    public static Identifier getTimekeeperWatchElegantId() {
        return Identifier.of(NoellesRolesCore.MOD_ID, "timekeeper_watch_elegant");
    }

    public static Identifier getJasonThrowingPoseId() {
        return Identifier.of(NoellesRolesCore.MOD_ID, "jason_throwing_pose");
    }

    /**
     * 注册物品的额外模型（当前仅注册冷却模型，方便后续扩展）
     */
    public static void registerExtraModel(@NotNull Item item) {
        ModelPredicateProviderRegistry.register(item, getCooldownId(), (itemStack, world, entity, seed) -> {
            if (MinecraftClient.getInstance().player == null) return 0.0F;
            if (isIgnoredForSpectatorOrCreative(item)
                    && GameFunctions.isPlayerSpectatingOrCreative(MinecraftClient.getInstance().player)) {
                return 0.0F;
            }
            return MinecraftClient.getInstance().player.getItemCooldownManager().isCoolingDown(item) ? 1.0F : 0.0F;
        });
        // 未来可以在此添加其他自定义模型谓词，例如：
        // ModelPredicateProviderRegistry.register(item, getSomeOtherId(), ...);
    }

    private static boolean isIgnoredForSpectatorOrCreative(@NotNull Item item) {
        /*
         * 这些迁移物品在旁观/创造/非存活视角下不受冷却限制。
         * 谓词也必须同步忽略冷却，否则客户端模型会误切到冷却贴图。
         */
        return item == ModItems.BLOWGUN
                || item == ModItems.POISON_INJECTOR
                || item == ModItems.DELUSION_SYRINGE
                || item == ModItems.KNOCKOUT_DRUG
                || item == ModItems.JERRY_CAN
                || item == ModItems.LIGHTER;
    }

    /**
     * 黑客手机需要额外的 killer_group 谓词来切换贴图。
     *
     * <p>不要把这个谓词注册给所有物品，否则任何带同名 override 的物品都会被手机状态误影响。</p>
     */
    public static void registerPhoneModel(@NotNull Item item) {
        registerExtraModel(item);
        ModelPredicateProviderRegistry.register(item, getKillerGroupId(), (itemStack, world, entity, seed) -> {
            if (MinecraftClient.getInstance().player == null) return 0.0F;
            return HackerPhoneComponent.KEY.get(MinecraftClient.getInstance().player).groupKiller ? 1.0F : 0.0F;
        });
    }

    /**
     * 投掷油桶的双姿势模型谓词。
     *
     * <p>平时油桶按 jerry_can 的手持姿势显示；玩家正在右键蓄力时，切到飞斧投掷姿势。
     * 这里只做客户端显示切换，真实投掷速度、重力和点火逻辑仍全部以服务端为准。</p>
     */
    public static void registerJasonJerryCanModel(@NotNull Item item) {
        registerExtraModel(item);
        ModelPredicateProviderRegistry.register(item, getJasonThrowingPoseId(), (itemStack, world, entity, seed) -> {
            if (entity == null) {
                return 0.0F;
            }
            return entity.isUsingItem() && entity.getActiveItem().isOf(item) ? 1.0F : 0.0F;
        });
    }

    /**
     * 注册怀表状态模型谓词。
     *
     * <p>濒毁怀表不使用通用冷却谓词，因为它有三套独立模式冷却；
     * 如果把整件物品放进原版 ItemCooldownManager，切换模式后也会被同一个冷却遮罩锁住。
     * 因此这里只根据物品数据组件切换普通 / 损坏 / 精致外观。</p>
     */
    public static void registerTimekeeperWatchModel(@NotNull Item item) {
        /*
         * 旧实现只暴露一个 ordinal 谓词：普通=0、损坏=1、精致=2。
         * Minecraft 物品模型 override 的谓词是“当前值 >= JSON 阈值”式匹配，
         * 所以精致状态 2 会同时满足损坏模型的 1.0 阈值；在当前加载顺序下会先命中损坏贴图。
         *
         * 这里保留 ordinal 谓词给调试/兼容使用，同时新增两个互斥布尔谓词：
         * - timekeeper_watch_broken 只有损坏状态返回 1；
         * - timekeeper_watch_elegant 只有精致状态返回 1。
         * 这样无论模型 override 顺序如何，精致怀表都不会再被损坏怀表的阈值误覆盖。
         */
        ModelPredicateProviderRegistry.register(item, getTimekeeperWatchStateId(), (itemStack, world, entity, seed) ->
                TimekeeperWatchItem.getState(itemStack).ordinal());
        ModelPredicateProviderRegistry.register(item, getTimekeeperWatchBrokenId(), (itemStack, world, entity, seed) ->
                TimekeeperWatchItem.getState(itemStack).isBroken() ? 1.0F : 0.0F);
        ModelPredicateProviderRegistry.register(item, getTimekeeperWatchElegantId(), (itemStack, world, entity, seed) ->
                TimekeeperWatchItem.getState(itemStack).isElegant() ? 1.0F : 0.0F);
    }
}
