package org.agmas.noellesroles.roles.magician;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔术师录制时，每 tick 保存下来的一帧轨迹快照。
 *
 * <p>这份数据负责“轨迹、姿态、临时背包与使用状态复刻”：
 * 1. 位置；
 * 2. 身体/头部朝向；
 * 3. 潜行、冲刺、在地面状态；
 * 4. 当前物品栏选槽；
 * 5. 当前姿态；
 * 6. 当前这一 tick 的临时背包状态；
 * 7. 正在使用的手和蓄力剩余时间；
 * 8. 睡觉床位；
 * 9. 是否正在乘坐座位/载具。
 *
 * <p>真正的交互，例如开门、挥刀、开枪、右键使用物品，
 * 仍然额外记录成 {@link MagicianRecordedAction}。
 * 这里保存背包和使用状态，是为了让播放代理在执行那些动作时，手里确实拿着录制当刻的临时道具，
 * 而不是只拿着录制开始瞬间的旧快照。</p>
 */
public record MagicianReplayFrame(
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        float headYaw,
        float bodyYaw,
        String poseName,
        boolean sneaking,
        boolean sprinting,
        boolean onGround,
        boolean sitting,
        GameMode gameMode,
        boolean swimming,
        boolean touchingWater,
        boolean fallFlying,
        int fallFlyingTicks,
        float leaningPitch,
        double velocityX,
        double velocityY,
        double velocityZ,
        int selectedSlot,
        List<ItemStack> mainStacks,
        List<ItemStack> armorStacks,
        List<ItemStack> offHandStacks,
        boolean usingItem,
        @Nullable Hand activeHand,
        int itemUseTimeLeft,
        int itemUseMaxTime,
        @Nullable BlockPos sleepingPosition
) {
    public MagicianReplayFrame {
        selectedSlot = Math.max(0, Math.min(selectedSlot, 8));
        mainStacks = copyStacks(mainStacks);
        armorStacks = copyStacks(armorStacks);
        offHandStacks = copyStacks(offHandStacks);
        itemUseTimeLeft = Math.max(0, itemUseTimeLeft);
        itemUseMaxTime = Math.max(0, itemUseMaxTime);
        fallFlyingTicks = Math.max(0, fallFlyingTicks);
    }

    public static @NotNull MagicianReplayFrame capture(@NotNull PlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        Vec3d velocity = player.getVelocity();
        /*
         * 录制只会在服务端真实玩家上执行，但保留 DEFAULT 兜底可以避免测试实体或未来调用者传入
         * 普通 PlayerEntity 时产生空值。游戏模式必须逐帧保存，播放代理才能严格遵守当时的
         * 冒险/生存/创造交互权限，而不是使用 FakePlayer 默认的生存模式。
         */
        GameMode gameMode = player instanceof ServerPlayerEntity serverPlayer
                ? serverPlayer.interactionManager.getGameMode()
                : GameMode.DEFAULT;
        return new MagicianReplayFrame(
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYaw(),
                player.getPitch(),
                player.getHeadYaw(),
                player.bodyYaw,
                player.getPose().name(),
                player.isSneaking(),
                player.isSprinting(),
                player.isOnGround(),
                player.hasVehicle(),
                gameMode,
                player.isSwimming(),
                player.isTouchingWater(),
                player.isFallFlying(),
                player.getFallFlyingTicks(),
                player.getLeaningPitch(1.0F),
                velocity.x,
                velocity.y,
                velocity.z,
                inventory.selectedSlot,
                inventory.main,
                inventory.armor,
                inventory.offHand,
                player.isUsingItem(),
                player.isUsingItem() ? player.getActiveHand() : null,
                player.isUsingItem() ? player.getItemUseTimeLeft() : 0,
                player.isUsingItem() ? player.getActiveItem().getMaxUseTime(player) : 0,
                player.getSleepingPosition().orElse(null)
        );
    }

    /**
     * 把这一帧录到的临时背包完整写回代理玩家。
     *
     * <p>这里每帧都覆盖一次，是为了支持“录制中买了新道具、随后拿出来使用”的情况。
     * 播放代理使用的是录制帧里的临时副本，不会消耗魔术师当前真实背包。</p>
     */
    public void applyInventoryTo(@NotNull PlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        applyStacks(this.mainStacks, inventory.main);
        applyStacks(this.armorStacks, inventory.armor);
        applyStacks(this.offHandStacks, inventory.offHand);
        inventory.selectedSlot = this.selectedSlot;
    }

    /**
     * 只把客户端可见的装备槽同步给皮套实体。
     */
    public void syncVisibleEquipmentTo(@NotNull MagicianPlaybackEntity playbackEntity) {
        playbackEntity.clearEquipment();
        playbackEntity.equipStack(EquipmentSlot.MAINHAND, getStackAt(this.mainStacks, this.selectedSlot));
        playbackEntity.equipStack(EquipmentSlot.OFFHAND, getStackAt(this.offHandStacks, 0));
        playbackEntity.equipStack(EquipmentSlot.FEET, getStackAt(this.armorStacks, 0));
        playbackEntity.equipStack(EquipmentSlot.LEGS, getStackAt(this.armorStacks, 1));
        playbackEntity.equipStack(EquipmentSlot.CHEST, getStackAt(this.armorStacks, 2));
        playbackEntity.equipStack(EquipmentSlot.HEAD, getStackAt(this.armorStacks, 3));
    }

    public @NotNull ItemStack getStackInHand(@NotNull Hand hand) {
        return hand == Hand.MAIN_HAND
                ? getStackAt(this.mainStacks, this.selectedSlot)
                : getStackAt(this.offHandStacks, 0);
    }

    private static @NotNull List<ItemStack> copyStacks(@NotNull List<ItemStack> source) {
        List<ItemStack> copy = new ArrayList<>(source.size());
        for (ItemStack stack : source) {
            copy.add(stack.copy());
        }
        return List.copyOf(copy);
    }

    private static void applyStacks(@NotNull List<ItemStack> source, @NotNull List<ItemStack> target) {
        for (int index = 0; index < target.size(); index++) {
            target.set(index, getStackAt(source, index));
        }
    }

    private static @NotNull ItemStack getStackAt(@NotNull List<ItemStack> stacks, int index) {
        if (index < 0 || index >= stacks.size()) {
            return ItemStack.EMPTY;
        }
        return stacks.get(index).copy();
    }
}
