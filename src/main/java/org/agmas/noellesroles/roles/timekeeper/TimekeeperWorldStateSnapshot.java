package org.agmas.noellesroles.roles.timekeeper;

import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 时停者的“可控世界状态”快照。
 *
 * <p>这里故意只处理门、普通火和营火点燃状态，不做整张地图方块级回滚。
 * 原因是整图方块快照会在每 4 tick 采样时制造很大的 CPU/内存压力；
 * 而时停者真正需要回滚的地图交互，当前主要集中在门的运行态和火源这类可控小范围状态。</p>
 */
public final class TimekeeperWorldStateSnapshot {
    private final Map<Long, DoorState> doorStates;
    private final Set<Long> fireBlocks;
    private final Map<Long, BlockState> litCampfireStates;

    private TimekeeperWorldStateSnapshot(
            @NotNull Map<Long, DoorState> doorStates,
            @NotNull Set<Long> fireBlocks,
            @NotNull Map<Long, BlockState> litCampfireStates
    ) {
        this.doorStates = doorStates;
        this.fireBlocks = fireBlocks;
        this.litCampfireStates = litCampfireStates;
    }

    public TimekeeperWorldStateSnapshot(@NotNull TimekeeperWorldStateSnapshot other) {
        this.doorStates = new HashMap<>();
        for (Map.Entry<Long, DoorState> entry : other.doorStates.entrySet()) {
            this.doorStates.put(entry.getKey(), new DoorState(entry.getValue()));
        }
        this.fireBlocks = new HashSet<>(other.fireBlocks);
        this.litCampfireStates = new HashMap<>(other.litCampfireStates);
    }

    public static @NotNull TimekeeperWorldStateSnapshot capture(@NotNull ServerWorld world) {
        RegistryWrapper.WrapperLookup registryLookup = world.getRegistryManager();
        Map<Long, DoorState> doors = new HashMap<>();
        Set<Long> fires = new HashSet<>();
        Map<Long, BlockState> litCampfires = new HashMap<>();

        forEachPlayAreaBlockEntity(world, blockEntity -> {
            if (blockEntity instanceof DoorBlockEntity door) {
                /*
                 * 门的 open / blasted / closeCountdown / jammedTime / keyName 都已经由 Wathe
                 * 自己写进 block entity NBT。这里保存 NBT 而不是拆字段，是为了以后 Wathe
                 * 给门追加运行态时，时停者回溯能自然跟上。
                 */
                doors.put(door.getPos().asLong(), DoorState.capture(door, registryLookup));
            }
        });

        forEachPlayAreaBlock(world, pos -> {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(Blocks.FIRE)) {
                fires.add(pos.asLong());
                return;
            }
            if (state.isOf(Blocks.CAMPFIRE)
                    && state.contains(CampfireBlock.LIT)
                    && state.get(CampfireBlock.LIT)) {
                litCampfires.put(pos.asLong(), state);
            }
        });

        return new TimekeeperWorldStateSnapshot(doors, fires, litCampfires);
    }

    public void restore(@NotNull ServerWorld world) {
        restoreDoorStates(world);
        restoreFires(world);
    }

    private void restoreDoorStates(@NotNull ServerWorld world) {
        RegistryWrapper.WrapperLookup registryLookup = world.getRegistryManager();
        for (Map.Entry<Long, DoorState> entry : this.doorStates.entrySet()) {
            BlockPos pos = BlockPos.fromLong(entry.getKey());
            DoorState state = entry.getValue();

            /*
             * 只回写已经存在的门方块实体，不尝试凭空重造门。
             * 这样可以避免误把地图重置、爆炸或外部插件改方块的结果整片覆盖回去。
             */
            if (!(world.getBlockEntity(pos) instanceof DoorBlockEntity door)) {
                continue;
            }

            if (world.getBlockState(pos).isOf(state.blockState.getBlock())) {
                world.setBlockState(pos, state.blockState, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            }
            door.readComponentlessNbt(state.nbt.copy(), registryLookup);
            door.setComponents(state.components);
            door.markDirty();
            door.sync();
        }
    }

    private void restoreFires(@NotNull ServerWorld world) {
        Set<Long> targetFires = this.fireBlocks;
        Map<Long, BlockState> targetCampfires = this.litCampfireStates;

        /*
         * 普通火是可增可删的临时方块：当前多出来的火要清掉，快照里有但现在没了的火要补回。
         * 只在 playArea 扫描，避免对大厅、备用结构或其它维度造成大范围方块写入。
         */
        forEachPlayAreaBlock(world, pos -> {
            long key = pos.asLong();
            BlockState current = world.getBlockState(pos);

            if (current.isOf(Blocks.FIRE) && !targetFires.contains(key)) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                return;
            }

            BlockState targetCampfireState = targetCampfires.get(key);
            if (current.isOf(Blocks.CAMPFIRE) && current.contains(CampfireBlock.LIT)) {
                boolean shouldBeLit = targetCampfireState != null && targetCampfireState.get(CampfireBlock.LIT);
                if (current.get(CampfireBlock.LIT) != shouldBeLit) {
                    world.setBlockState(pos, current.with(CampfireBlock.LIT, shouldBeLit), Block.NOTIFY_ALL);
                }
            }
        });

        for (long firePos : targetFires) {
            BlockPos pos = BlockPos.fromLong(firePos);
            if (world.getBlockState(pos).isAir()) {
                world.setBlockState(pos, Blocks.FIRE.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
    }

    private static void forEachPlayAreaBlockEntity(@NotNull ServerWorld world, @NotNull Consumer<BlockEntity> consumer) {
        Box playArea = MapVariablesWorldComponent.KEY.get(world).getPlayArea();
        if (playArea == null) {
            return;
        }

        int minX = (int) Math.floor(playArea.minX);
        int minY = (int) Math.floor(playArea.minY);
        int minZ = (int) Math.floor(playArea.minZ);
        int maxX = (int) Math.ceil(playArea.maxX);
        int maxY = (int) Math.ceil(playArea.maxY);
        int maxZ = (int) Math.ceil(playArea.maxZ);

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getPos();
                    if (pos.getX() < minX || pos.getX() > maxX
                            || pos.getY() < minY || pos.getY() > maxY
                            || pos.getZ() < minZ || pos.getZ() > maxZ) {
                        continue;
                    }
                    consumer.accept(blockEntity);
                }
            }
        }
    }

    private static void forEachPlayAreaBlock(@NotNull ServerWorld world, @NotNull Consumer<BlockPos> consumer) {
        Box playArea = MapVariablesWorldComponent.KEY.get(world).getPlayArea();
        if (playArea == null) {
            return;
        }

        int minX = (int) Math.floor(playArea.minX);
        int minY = (int) Math.floor(playArea.minY);
        int minZ = (int) Math.floor(playArea.minZ);
        int maxX = (int) Math.ceil(playArea.maxX);
        int maxY = (int) Math.ceil(playArea.maxY);
        int maxZ = (int) Math.ceil(playArea.maxZ);

        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    consumer.accept(mutable.set(x, y, z).toImmutable());
                }
            }
        }
    }

    private record DoorState(BlockState blockState, NbtCompound nbt, ComponentMap components) {
        private DoorState(@NotNull DoorState other) {
            this(other.blockState, other.nbt.copy(), other.components);
        }

        private static @NotNull DoorState capture(@NotNull DoorBlockEntity door, RegistryWrapper.WrapperLookup registryLookup) {
            return new DoorState(
                    door.getCachedState(),
                    door.createComponentlessNbt(registryLookup),
                    door.getComponents()
            );
        }
    }
}
