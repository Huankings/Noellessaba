package org.agmas.noellesroles.roles.lich;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * 巫妖调试友好的物品使用规则。
 *
 * <p>用户明确要求新物品允许旁观/创造调试玩家继续使用，并且不受冷却影响。
 * 因此这里把判断集中起来，避免三件物品分别写出略有差异的校验。</p>
 */
final class LichItemUseRules {
    /** 巫妖蓄力物品写入 ItemStack CUSTOM_DATA 的根字段，用来标记这次右键蓄力从什么状态开始。 */
    private static final String CHARGED_USE_ROOT_KEY = "NoellesLichChargedUse";
    /** 蓄力开始玩家 UUID 字段，避免物品转手或异常同步时误把别人的蓄力视为自己的。 */
    private static final String CHARGED_USE_PLAYER_KEY = "Player";
    /** 蓄力开始时是否为旁观/创造调试玩家，保证调试玩家仍可按需求释放蓄力物品。 */
    private static final String CHARGED_USE_DEBUG_KEY = "StartedAsDebug";

    private LichItemUseRules() {
    }

    static boolean canUseLichDebugAwareItem(@NotNull PlayerEntity player, @NotNull Item item) {
        boolean debugPlayer = GameFunctions.isPlayerSpectatingOrCreative(player);
        if (!debugPlayer && !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return false;
        }
        return debugPlayer || !player.getItemCooldownManager().isCoolingDown(item);
    }

    static void beginChargedUse(@NotNull PlayerEntity player, @NotNull ItemStack stack) {
        /*
         * 简易法杖和魔法屏障都有“按住蓄力 -> 松手结算”的窗口。
         * 如果玩家在这个窗口中被 Wathe 击杀，松手时会变成 isPlayerSpectatingOrCreative，
         * 而巫妖物品又允许调试旁观者使用。这里记录“蓄力起手状态”，用于结算时区分：
         * 1. 本来就是旁观/创造的调试释放，可以继续放；
         * 2. 原本是正常存活玩家，中途死亡后松手，不再结算技能。
         */
        NbtCompound customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        NbtCompound root = new NbtCompound();
        root.putUuid(CHARGED_USE_PLAYER_KEY, player.getUuid());
        root.putBoolean(CHARGED_USE_DEBUG_KEY, GameFunctions.isPlayerSpectatingOrCreative(player));
        customData.put(CHARGED_USE_ROOT_KEY, root);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
    }

    static boolean finishChargedUse(@NotNull PlayerEntity player, @NotNull Item item, @NotNull ItemStack stack) {
        boolean startedAsDebug = startedChargedUseAsDebug(player, stack);
        clearChargedUse(stack);

        if (startedAsDebug && GameFunctions.isPlayerSpectatingOrCreative(player)) {
            return true;
        }
        /*
         * Wathe 的 isPlayerAliveAndSurvival 主要描述玩法视角，原版 isAlive 仍可能覆盖某些边界。
         * 两者一起检查，可以同时挡住 Wathe 击杀后的 spectator 状态，以及极少数原版死亡态。
         */
        if (!player.isAlive() || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return false;
        }
        return !player.getItemCooldownManager().isCoolingDown(item);
    }

    static void playUseSoundFromPlayer(@NotNull World world, @NotNull PlayerEntity player, @NotNull SoundEvent sound, float volume, float pitch) {
        if (world.isClient) {
            return;
        }
        /*
         * 使用实体音源包，而不是在释放瞬间的坐标播放声音。
         * 玩家边移动边释放时，客户端会把声音绑定到玩家实体位置，听感不会滞留在旧坐标。
         */
        world.playSoundFromEntity(null, player, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    private static boolean startedChargedUseAsDebug(@NotNull PlayerEntity player, @NotNull ItemStack stack) {
        NbtCompound root = chargedUseRoot(stack);
        return root.containsUuid(CHARGED_USE_PLAYER_KEY)
                && player.getUuid().equals(root.getUuid(CHARGED_USE_PLAYER_KEY))
                && root.getBoolean(CHARGED_USE_DEBUG_KEY);
    }

    private static @NotNull NbtCompound chargedUseRoot(@NotNull ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound data = component.copyNbt();
        return data.contains(CHARGED_USE_ROOT_KEY, NbtElement.COMPOUND_TYPE)
                ? data.getCompound(CHARGED_USE_ROOT_KEY)
                : new NbtCompound();
    }

    private static void clearChargedUse(@NotNull ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (component == null) {
            return;
        }

        NbtCompound customData = component.copyNbt();
        customData.remove(CHARGED_USE_ROOT_KEY);
        if (customData.isEmpty()) {
            stack.remove(DataComponentTypes.CUSTOM_DATA);
            return;
        }
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
    }
}
