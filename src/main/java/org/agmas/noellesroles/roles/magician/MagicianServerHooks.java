package org.agmas.noellesroles.roles.magician;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 魔术师服务端公共钩子。
 *
 * <p>这类逻辑会被很多 mixin / 数据包接收器反复调用：
 * 1. 录制期是否要记一次动作；
 * 2. 某个右键是否应该交给“专属语义动作”单独记录；
 * 3. 某个目标是否其实是魔术师播放体，如果是则要不要强制结束播放。
 *
 * <p>把这些判断统一收口到这里有两个好处：
 * 1. 后续你想继续补更多武器、更多可记录动作时，只需要改一处判断；
 * 2. 可以避免不同 mixin 各自抄一份 if-else，导致录制与命中逻辑慢慢跑偏。
 */
public final class MagicianServerHooks {

    private MagicianServerHooks() {
    }

    public static @Nullable MagicianPlayerComponent getRecordingComponent(@Nullable ServerPlayerEntity player) {
        if (player == null) {
            return null;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.MAGICIAN)
                || !gameWorld.isRunning()
                || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return null;
        }

        MagicianPlayerComponent component = MagicianPlayerComponent.KEY.get(player);
        return component.isRecording() ? component : null;
    }

    public static void recordAttack(@Nullable ServerPlayerEntity player) {
        MagicianPlayerComponent component = getRecordingComponent(player);
        if (component != null) {
            component.recordAttackAction();
        }
    }

    public static void recordUse(@Nullable ServerPlayerEntity player, @NotNull Hand hand) {
        recordUse(player, hand, player == null ? ItemStack.EMPTY : player.getStackInHand(hand));
    }

    public static void recordUse(@Nullable ServerPlayerEntity player, @NotNull Hand hand, @NotNull ItemStack stack) {
        MagicianPlayerComponent component = getRecordingComponent(player);
        if (component == null || shouldSkipGenericUseRecording(stack)) {
            return;
        }
        component.recordUseAction(hand);
    }

    public static void recordUseBlock(
            @Nullable ServerPlayerEntity player,
            @NotNull Hand hand,
            @NotNull ItemStack stack,
            @NotNull BlockHitResult hitResult
    ) {
        MagicianPlayerComponent component = getRecordingComponent(player);
        if (component == null || shouldSkipGenericUseRecording(stack)) {
            return;
        }
        component.recordUseBlockAction(hand, hitResult);
    }

    public static void recordReleaseUse(@Nullable ServerPlayerEntity player) {
        MagicianPlayerComponent component = getRecordingComponent(player);
        if (component != null) {
            component.recordReleaseUseAction();
        }
    }

    public static void recordSwing(@Nullable ServerPlayerEntity player, @NotNull Hand hand) {
        MagicianPlayerComponent component = getRecordingComponent(player);
        if (component == null || player == null) {
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (player.isUsingItem() || stack.getUseAction() != UseAction.NONE || shouldSkipVisualSwingRecording(stack)) {
            /*
             * 匕首、手雷、飞斧等长按物品已经由每帧的 usingItem/UseAction 复刻举起姿势。
             * 如果再把客户端起手挥手包录进去，播放时会先压一下手臂，
             * 看起来就像“停顿一下才举起来”。因此长按类物品不录纯挥手。
             *
             * 枪械开火也不能录纯挥手；Wathe 的左轮/短枪视觉重点是开火音效与命中，
             * 如果额外回放挥手，会表现成“开枪时抡了一下手”。
             */
            return;
        }

        component.recordSwingAction(hand);
    }

    public static void recordSelectSlot(@Nullable ServerPlayerEntity player, int slot) {
        MagicianPlayerComponent component = getRecordingComponent(player);
        if (component != null) {
            component.recordSelectSlotAction(slot);
        }
    }

    public static void recordGunShoot(@Nullable ServerPlayerEntity player) {
        MagicianPlayerComponent component = getRecordingComponent(player);
        if (component != null) {
            component.recordGunShootAction();
        }
    }

    public static void recordKnifeStab(@Nullable ServerPlayerEntity player) {
        MagicianPlayerComponent component = getRecordingComponent(player);
        if (component != null) {
            component.recordKnifeStabAction();
        }
    }

    public static void recordBayonetStab(@Nullable ServerPlayerEntity player) {
        MagicianPlayerComponent component = getRecordingComponent(player);
        if (component != null) {
            component.recordBayonetStabAction();
        }
    }

    public static void recordBayonetKnockback(@Nullable ServerPlayerEntity player) {
        MagicianPlayerComponent component = getRecordingComponent(player);
        if (component != null) {
            component.recordBayonetKnockbackAction();
        }
    }

    public static void recordSniperShoot(@Nullable ServerPlayerEntity player, double x, double y, double z) {
        MagicianPlayerComponent component = getRecordingComponent(player);
        if (component != null) {
            component.recordSniperShootAction(x, y, z);
        }
    }

    /**
     * 某些物品的“右键语义”会由专属动作单独记录。
     *
     * <p>如果这里不跳过，像左轮、刺刀、狙击枪这类武器就会同时留下：
     * 1. 一条通用 USE_MAIN_HAND；
     * 2. 一条专属的 GUN_SHOOT / BAYONET_STAB / SNIPER_SHOOT。
     *
     * <p>回放时两条动作叠在一起，就会变成重复开火或重复刺杀。
     * 因此这些“已经有明确独立动作语义”的物品，统一从通用右键记录里排除。
     */
    public static boolean shouldSkipGenericUseRecording(@NotNull ItemStack stack) {
        return stack.isIn(WatheItemTags.GUNS)
                || stack.isOf(ModItems.ROBBER_PISTOL)
                || stack.isOf(ModItems.SILENCED_REVOLVER)
                || stack.isOf(ModItems.BAYONET)
                || stack.isOf(ModItems.SNIPER_RIFLE);
    }

    private static boolean shouldSkipVisualSwingRecording(@NotNull ItemStack stack) {
        return stack.isIn(WatheItemTags.GUNS)
                || stack.isOf(ModItems.ROBBER_PISTOL)
                || stack.isOf(ModItems.SILENCED_REVOLVER)
                || stack.isOf(ModItems.SNIPER_RIFLE);
    }

    public static boolean stopPlaybackByWeaponTarget(
            @Nullable Entity target,
            @Nullable ServerPlayerEntity attacker,
            @NotNull Identifier deathReason,
            @NotNull String weaponName
    ) {
        MagicianPlaybackEntity playbackEntity = MagicianPlaybackManager.findPlaybackEntity(target);
        if (playbackEntity == null) {
            return false;
        }

        MagicianPlaybackManager.stopPlaybackByWeaponHit(playbackEntity, attacker, weaponName, deathReason);
        return true;
    }

    public static void stopPlaybackInExplosion(
            @NotNull ServerWorld world,
            @NotNull Box explosionBox,
            @Nullable ServerPlayerEntity attacker,
            @NotNull Identifier deathReason,
            @NotNull String weaponName
    ) {
        for (MagicianPlaybackEntity playbackEntity : world.getEntitiesByType(
                NoellesRolesEntities.MAGICIAN_PLAYBACK_ENTITY_TYPE,
                entity -> explosionBox.contains(entity.getPos())
        )) {
            MagicianPlaybackManager.stopPlaybackByWeaponHit(playbackEntity, attacker, weaponName, deathReason);
        }
    }

    public static @NotNull String getWeaponName(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return "replay.item.unknown";
        }

        /*
         * 普通手雷飞出去以后，实体手里持有的往往是“投掷中的手雷占位物品”，
         * 直接读它的翻译 key 会让回放事件里出现“已投掷手雷”。
         * 这里统一回退成玩家真正认识的“手雷”。
         */
        if (stack.isOf(WatheItems.THROWN_GRENADE)) {
            return WatheItems.GRENADE.getDefaultStack().getTranslationKey();
        }

        /*
         * 这里不能使用 stack.getName().getString()。
         *
         * 回放事件是在服务端记录的，而服务端没有客户端当前语言环境。
         * 如果在这里提前 getString()，物品名就会被固定成服务端默认语言，
         * 客户端之后即使选择中文，也只能看到已经固化好的英文普通字符串。
         *
         * 保存 item.xxx 这种翻译 key，让 replay formatter 返回 Text.translatable，
         * 才能在每个客户端按自己的语言文件显示“刺刀 / Bayonet”等本地化名称。
         */
        return stack.getTranslationKey();
    }
}
