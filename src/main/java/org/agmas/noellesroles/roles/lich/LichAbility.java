package org.agmas.noellesroles.roles.lich;

import dev.doctor4t.wathe.block.SmallDoorBlock;
import dev.doctor4t.wathe.block_entity.SmallDoorBlockEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 巫妖能力键：控门术。
 *
 * <p>扫描周围 Wathe 小门：
 * 1. 被撬棍永久破坏的门会被修复；
 * 2. 没有破坏的门会被锁定 30 秒，已经锁住的门也会刷新倒计时。</p>
 */
public final class LichAbility {
    private LichAbility() {
    }

    public static void handle(@NotNull ServerPlayerEntity player) {
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown > 0) {
            return;
        }

        Map<BlockPos, SmallDoorBlockEntity> doors = findNearbyDoors(player);
        if (doors.isEmpty()) {
            return;
        }

        int repaired = LichConstants.DOOR_CONTROL_EMPTY_COUNT;
        int locked = LichConstants.DOOR_CONTROL_EMPTY_COUNT;
        for (SmallDoorBlockEntity door : doors.values()) {
            if (door.isBlasted()) {
                repairDoor(door);
                repaired++;
            } else {
                lockDoor(door);
                locked++;
            }
        }

        if (locked == LichConstants.DOOR_CONTROL_EMPTY_COUNT && repaired == LichConstants.DOOR_CONTROL_EMPTY_COUNT) {
            return;
        }

        ability.setCooldown(LichConstants.DOOR_CONTROL_USE_COOLDOWN_TICKS);
        recordDoorControl(player, doors.size(), locked, repaired);
    }

    public static Map<BlockPos, SmallDoorBlockEntity> findNearbyDoors(@NotNull PlayerEntityLike playerLike) {
        World world = playerLike.world();
        BlockPos center = playerLike.blockPos();
        int radius = LichConstants.DOOR_CONTROL_RADIUS_BLOCKS;
        Map<BlockPos, SmallDoorBlockEntity> doors = new LinkedHashMap<>();

        /*
         * SmallDoorBlock 的方块实体只存在于下半部分。
         * 扫描时把上半部分统一归到 lowerPos，避免一扇门上下两格重复计数。
         */
        for (BlockPos pos : BlockPos.iterateOutwards(center, radius, radius, radius)) {
            if (center.getSquaredDistance(pos) > radius * radius) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof SmallDoorBlock)) {
                continue;
            }
            BlockPos lowerPos = state.get(SmallDoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.toImmutable() : pos.down().toImmutable();
            if (doors.containsKey(lowerPos)) {
                continue;
            }
            if (world.getBlockEntity(lowerPos) instanceof SmallDoorBlockEntity door) {
                doors.put(lowerPos, door);
            }
        }
        return doors;
    }

    public static Map<BlockPos, SmallDoorBlockEntity> findNearbyDoors(@NotNull ServerPlayerEntity player) {
        return findNearbyDoors(new PlayerEntityLike(player.getWorld(), player.getBlockPos()));
    }

    private static void repairDoor(@NotNull SmallDoorBlockEntity door) {
        door.setBlasted(false);
        door.setJammed(LichConstants.DOOR_CONTROL_REPAIRED_TIMER_TICKS);
        door.setCloseCountdown(LichConstants.DOOR_CONTROL_REPAIRED_TIMER_TICKS);
        door.sync();
        if (door.getWorld() != null) {
            door.getWorld().playSound(
                    null,
                    door.getPos().getX() + LichConstants.DOOR_CONTROL_SOUND_CENTER_OFFSET,
                    door.getPos().getY() + LichConstants.DOOR_CONTROL_SOUND_Y_OFFSET,
                    door.getPos().getZ() + LichConstants.DOOR_CONTROL_SOUND_CENTER_OFFSET,
                    WatheSounds.ITEM_CROWBAR_PRY,
                    SoundCategory.BLOCKS,
                    LichConstants.DOOR_CONTROL_SOUND_VOLUME,
                    LichConstants.DOOR_CONTROL_SOUND_PITCH
            );
        }
    }

    private static void lockDoor(@NotNull SmallDoorBlockEntity door) {
        door.setJammed(LichConstants.DOOR_CONTROL_JAM_TICKS);
        if (door.isOpen()) {
            /*
             * DoorBlockEntity#toggle 会同步方块状态并播放门声。
             * 这里传 false，让玩家听到控门术确实影响了门；随后再播锁门音作为技能提示。
             */
            door.toggle(false);
        }
        door.sync();
        if (door.getWorld() != null) {
            door.getWorld().playSound(
                    null,
                    door.getPos().getX() + LichConstants.DOOR_CONTROL_SOUND_CENTER_OFFSET,
                    door.getPos().getY() + LichConstants.DOOR_CONTROL_SOUND_Y_OFFSET,
                    door.getPos().getZ() + LichConstants.DOOR_CONTROL_SOUND_CENTER_OFFSET,
                    WatheSounds.ITEM_LOCKPICK_DOOR,
                    SoundCategory.BLOCKS,
                    LichConstants.DOOR_CONTROL_SOUND_VOLUME,
                    LichConstants.DOOR_CONTROL_SOUND_PITCH
            );
        }
    }

    private static void recordDoorControl(@NotNull ServerPlayerEntity player, int doorCount, int locked, int repaired) {
        NbtCompound extra = new NbtCompound();
        extra.putInt("radius", LichConstants.DOOR_CONTROL_RADIUS_BLOCKS);
        extra.putInt("door_count", doorCount);
        extra.putInt("locked_count", locked);
        extra.putInt("repaired_count", repaired);
        GameRecordManager.recordSkillUse(player, NoellesEventIds.LICH_DOOR_CONTROL_EVENT, null, extra);
    }

    /**
     * 只暴露 HUD/服务端扫描所需的世界和中心点，方便客户端 HUD 复用同一套门计数逻辑。
     */
    public record PlayerEntityLike(@NotNull World world, @NotNull BlockPos blockPos) {
    }
}
