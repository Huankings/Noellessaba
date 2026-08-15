package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 杰森投掷油桶与火焰范围的世界级运行态。
 *
 * <p>落地油桶、自动点燃倒计时和火焰范围都不是某个玩家自己的状态，
 * 所以必须放在世界组件里保存。这样时停者回溯时可以把油桶是否已落地、火焰展开进度、
 * 剩余持续时间一起倒回，而不是只回滚玩家身上的汽油标记。</p>
 */
public final class JasonFireWorldComponent implements Component, ServerTickingComponent {
    public static final ComponentKey<JasonFireWorldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "jason_fire"),
            JasonFireWorldComponent.class
    );

    private static final String CANS_KEY = "cans";
    private static final String FIRES_KEY = "fires";
    private static final String ID_KEY = "id";
    private static final String OWNER_KEY = "owner";
    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";
    private static final String Z_KEY = "z";
    private static final String AGE_KEY = "age";

    private final World world;
    private final LinkedHashMap<UUID, LandedCanRecord> landedCans = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, FireRecord> fires = new LinkedHashMap<>();

    public JasonFireWorldComponent(World world) {
        this.world = world;
    }

    public void addLandedCan(@NotNull Vec3d center, @Nullable UUID ownerUuid) {
        addLandedCan(UUID.randomUUID(), center, ownerUuid);
    }

    public void addLandedCan(@NotNull UUID id, @NotNull Vec3d center, @Nullable UUID ownerUuid) {
        /*
         * 真实投掷油桶会把实体 UUID 作为 id 传进来。
         * 这样点燃时可以用同一个 id 清掉落地实体，不会出现火焰开始后油桶还留在地上的不同步表现。
         */
        this.landedCans.put(id, new LandedCanRecord(center, ownerUuid, 0));
        applyGasoline(id, center, ownerUuid);
    }

    public boolean tryIgniteCan(@NotNull ServerPlayerEntity owner, @NotNull UUID sourceId) {
        /*
         * 一次性打火机现在和投掷油桶一一绑定。
         * 因此点燃时只查指定 UUID 的落地油桶，并再次校验投掷者，防止别人捡到打火机后代替杰森点燃。
         * 如果油桶还在飞行中，世界组件里还没有 landedCan 记录，使用会失败且不消耗。
         */
        LandedCanRecord can = this.landedCans.get(sourceId);
        if (can == null || !owner.getUuid().equals(can.ownerUuid)) {
            return false;
        }
        recordManualIgnition(owner);
        igniteCan(sourceId, can);
        return true;
    }

    public boolean hasOwnedLandedCan(@NotNull UUID ownerUuid) {
        for (LandedCanRecord can : this.landedCans.values()) {
            if (ownerUuid.equals(can.ownerUuid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void serverTick() {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }

        Iterator<Map.Entry<UUID, LandedCanRecord>> canIterator = this.landedCans.entrySet().iterator();
        while (canIterator.hasNext()) {
            Map.Entry<UUID, LandedCanRecord> entry = canIterator.next();
            LandedCanRecord can = entry.getValue();
            can.ageTicks++;
            if (can.ageTicks >= JasonConstants.JERRY_CAN_AUTO_IGNITE_TICKS) {
                /*
                 * 自动燃烧和手动点燃走同一条 igniteCan 逻辑。
                 * 区别只在于自动燃烧不会经过打火机 use()，所以要在这里主动清掉
                 * “绑定到当前油桶 UUID”的那枚一次性打火机。
                 * 这里处于 landedCans 的 iterator 循环内，因此由 iterator.remove()
                 * 负责删除当前油桶，避免 igniteCan 再直接修改同一个 Map 导致运行时迭代异常。
                 */
                UUID sourceId = entry.getKey();
                recordAutomaticIgnition(serverWorld, can.ownerUuid);
                igniteCan(sourceId, can, false);
                canIterator.remove();
                removeOnceLightersForCan(serverWorld.getServer(), sourceId);
            }
        }

        Iterator<Map.Entry<UUID, FireRecord>> fireIterator = this.fires.entrySet().iterator();
        while (fireIterator.hasNext()) {
            Map.Entry<UUID, FireRecord> entry = fireIterator.next();
            FireRecord fire = entry.getValue();
            fire.ageTicks++;
            if (fire.ageTicks > JasonConstants.FIRE_DURATION_TICKS) {
                fireIterator.remove();
                continue;
            }
            double radius = currentFireRadius(fire.ageTicks);
            spawnFireParticles(serverWorld, fire.center, radius);
            burnPlayersInside(serverWorld, fire, radius);
        }
    }

    private void applyGasoline(@NotNull UUID sourceId, @NotNull Vec3d center, @Nullable UUID ownerUuid) {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }

        Box box = new Box(center, center).expand(JasonConstants.JERRY_CAN_GASOLINE_RADIUS, JasonConstants.FIRE_VERTICAL_RANGE, JasonConstants.JERRY_CAN_GASOLINE_RADIUS);
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
                continue;
            }
            if (!box.contains(player.getPos()) || player.squaredDistanceTo(center) > JasonConstants.JERRY_CAN_GASOLINE_RADIUS * JasonConstants.JERRY_CAN_GASOLINE_RADIUS) {
                continue;
            }
            JasonWoundedPlayerComponent.KEY.get(player).markGasoline(ownerUuid, sourceId);
            recordGasolineDoused(player, ownerUuid);
        }
    }

    private void igniteCan(@NotNull UUID sourceId, @NotNull LandedCanRecord can) {
        igniteCan(sourceId, can, true);
    }

    private void igniteCan(@NotNull UUID sourceId, @NotNull LandedCanRecord can, boolean removeLandedCan) {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }

        /*
         * 点燃瞬间先结算“被这个油桶沾染汽油”的玩家，再生成持续火焰范围。
         * 这样被沾油者即使正好站在火圈边缘，也会按需求立即被烧死。
         */
        ServerPlayerEntity owner = can.ownerUuid == null ? null : serverWorld.getServer().getPlayerManager().getPlayer(can.ownerUuid);
        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            JasonWoundedPlayerComponent component = JasonWoundedPlayerComponent.KEY.get(player);
            if (!GameFunctions.isPlayerAliveAndSurvival(player) || !component.isGasoline()) {
                continue;
            }
            if (sourceId.equals(component.getGasolineSourceUuid())) {
                GameFunctions.killPlayer(player, true, owner, NoellesDeathReasons.JASON_BURN_DEATH_REASON);
                component.clearGasoline();
            }
        }

        if (removeLandedCan) {
            this.landedCans.remove(sourceId);
        }
        discardLandedCanEntity(serverWorld, sourceId);
        this.fires.put(UUID.randomUUID(), new FireRecord(can.center, can.ownerUuid, 0));
    }

    private static void recordManualIgnition(@NotNull ServerPlayerEntity owner) {
        /*
         * 主动点燃由使用一次性打火机的玩家作为 actor 记录。
         * 回放 formatter 只需要 actor，就能显示“某人点燃了油桶”。
         */
        GameRecordManager.recordGlobalEvent(owner.getServerWorld(), NoellesEventIds.JASON_JERRY_CAN_IGNITED_EVENT, owner, null);
    }

    private static void recordAutomaticIgnition(@NotNull ServerWorld world, @Nullable UUID ownerUuid) {
        /*
         * 自动燃烧发生时投掷者可能已经离线，所以除了在线时传 actor，
         * 还额外写入 owner UUID 作为回放兜底，避免“某人的油桶”丢失名字。
         */
        ServerPlayerEntity owner = ownerUuid == null ? null : world.getServer().getPlayerManager().getPlayer(ownerUuid);
        NbtCompound extra = new NbtCompound();
        if (ownerUuid != null) {
            extra.putUuid(OWNER_KEY, ownerUuid);
        }
        GameRecordManager.recordGlobalEvent(world, NoellesEventIds.JASON_JERRY_CAN_AUTO_IGNITED_EVENT, owner, extra);
    }

    private static void recordGasolineDoused(@NotNull ServerPlayerEntity victim, @Nullable UUID ownerUuid) {
        /*
         * 汽油沾染是油桶落地瞬间的独立信息点，和后续手动/自动燃烧分开记录。
         * owner 同样写入额外数据，保证投掷者离线时回放还能显示“谁的油桶”。
         */
        ServerPlayerEntity owner = ownerUuid == null ? null : victim.getServer().getPlayerManager().getPlayer(ownerUuid);
        NbtCompound extra = new NbtCompound();
        extra.putUuid("victim", victim.getUuid());
        if (ownerUuid != null) {
            extra.putUuid(OWNER_KEY, ownerUuid);
        }
        GameRecordManager.recordGlobalEvent(victim.getServerWorld(), NoellesEventIds.JASON_GASOLINE_DOUSED_EVENT, owner, extra);
    }

    public void removeLandedCan(@NotNull UUID sourceId) {
        /*
         * 油桶落地后若因为实体超时、方块消失或回合清理而被移除，
         * 绑定到它的一次性打火机也不能继续留在任何玩家或掉落物里。
         */
        boolean removed = this.landedCans.remove(sourceId) != null;
        if (removed && this.world instanceof ServerWorld serverWorld) {
            removeOnceLightersForCan(serverWorld.getServer(), sourceId);
        }
    }

    private static void discardLandedCanEntity(@NotNull ServerWorld world, @NotNull UUID sourceId) {
        Entity entity = world.getEntity(sourceId);
        if (entity instanceof JasonThrownWeaponEntity) {
            entity.discard();
        }
    }

    private void burnPlayersInside(@NotNull ServerWorld world, @NotNull FireRecord fire, double radius) {
        ServerPlayerEntity owner = fire.ownerUuid == null ? null : world.getServer().getPlayerManager().getPlayer(fire.ownerUuid);
        Box box = new Box(fire.center, fire.center).expand(radius, JasonConstants.FIRE_VERTICAL_RANGE, radius);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
                continue;
            }
            if (!box.contains(player.getPos())) {
                continue;
            }

            Vec3d delta = player.getPos().subtract(fire.center);
            double horizontalDistanceSquared = delta.x * delta.x + delta.z * delta.z;
            if (horizontalDistanceSquared <= radius * radius && Math.abs(delta.y) <= JasonConstants.FIRE_VERTICAL_RANGE) {
                // 用户确认火焰范围是“进入范围即以 burn 致死”，不做持续扣血或额外倒计时。
                GameFunctions.killPlayer(player, true, owner, NoellesDeathReasons.JASON_BURN_DEATH_REASON);
                JasonWoundedPlayerComponent.KEY.get(player).clearGasoline();
            }
        }
    }

    private static double currentFireRadius(int ageTicks) {
        if (ageTicks >= JasonConstants.FIRE_EXPAND_TICKS) {
            return JasonConstants.FIRE_RADIUS_BLOCKS;
        }
        double progress = Math.max(0.0D, Math.min(1.0D, ageTicks / (double) JasonConstants.FIRE_EXPAND_TICKS));
        return Math.max(0.4D, JasonConstants.FIRE_RADIUS_BLOCKS * progress);
    }

    private static void spawnFireParticles(@NotNull ServerWorld world, @NotNull Vec3d center, double radius) {
        /*
         * 粒子表现采用“扩张圆盘 + 向下速度”的近似。
         * Minecraft 粒子本身不适合做完整流体模拟，这里重点保证玩家能看到火焰从油桶位置向外扩散，
         * 并在有向下空间时用负 y 速度表现“向下蔓延”的视觉趋势。
         */
        for (int i = 0; i < JasonConstants.FIRE_PARTICLES_PER_TICK; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2.0D;
            double distance = Math.sqrt(world.random.nextDouble()) * radius;
            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;
            double y = center.y + 0.15D + world.random.nextDouble() * 0.35D;
            world.spawnParticles(ParticleTypes.FLAME, x, y, z, 1, 0.02D, -0.08D, 0.02D, 0.02D);
            if (world.random.nextInt(3) == 0) {
                world.spawnParticles(ParticleTypes.SMOKE, x, y + 0.1D, z, 1, 0.03D, -0.04D, 0.03D, 0.01D);
            }
        }
    }

    public static void removeOnceLightersForCan(@NotNull MinecraftServer server, @NotNull UUID sourceId) {
        /*
         * 自动燃烧可能发生在打火机被丢出、被其他玩家捡走或正被鼠标拖拽时。
         * 所以这里按“绑定油桶 UUID”扫描所有在线玩家和所有维度的 ItemEntity，
         * 只清理对应油桶的那枚打火机，不影响同一名杰森后续投出的其它油桶。
         */
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            removeBoundOnceLighters(player, sourceId);
        }
        removeDroppedOnceLighters(server, sourceId);
    }

    public static void removeOnceLighters(@NotNull ServerPlayerEntity player) {
        /*
         * 玩家重置/死亡清理使用“移除全部一次性打火机”。
         * 这和油桶自动燃烧的精准清理不同，保留独立入口可以避免异常流程留下旧局物品。
         */
        PlayerInventory inventory = player.getInventory();
        removeOnceLightersFromList(inventory.main);
        removeOnceLightersFromList(inventory.armor);
        removeOnceLightersFromList(inventory.offHand);
        if (player.currentScreenHandler.getCursorStack().isOf(ModItems.ONCE_LIGHTER)) {
            player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
        }
        player.getInventory().markDirty();
        player.currentScreenHandler.sendContentUpdates();
    }

    private static void removeBoundOnceLighters(@NotNull ServerPlayerEntity player, @NotNull UUID sourceId) {
        PlayerInventory inventory = player.getInventory();
        boolean changed = false;
        changed |= removeBoundOnceLightersFromList(inventory.main, sourceId);
        changed |= removeBoundOnceLightersFromList(inventory.armor, sourceId);
        changed |= removeBoundOnceLightersFromList(inventory.offHand, sourceId);
        if (JasonOnceLighterItem.isBoundToCan(player.currentScreenHandler.getCursorStack(), sourceId)) {
            player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
            changed = true;
        }
        if (changed) {
            player.getInventory().markDirty();
            player.currentScreenHandler.sendContentUpdates();
        }
    }

    private static void removeDroppedOnceLighters(@NotNull MinecraftServer server, @Nullable UUID sourceId) {
        for (ServerWorld world : server.getWorlds()) {
            for (ItemEntity itemEntity : world.getEntitiesByType(EntityType.ITEM, itemEntity -> shouldRemoveOnceLighter(itemEntity.getStack(), sourceId))) {
                itemEntity.discard();
            }
        }
    }

    private static void removeOnceLightersFromList(@NotNull List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i).isOf(ModItems.ONCE_LIGHTER)) {
                stacks.set(i, ItemStack.EMPTY);
            }
        }
    }

    private static boolean removeBoundOnceLightersFromList(@NotNull List<ItemStack> stacks, @NotNull UUID sourceId) {
        boolean changed = false;
        for (int i = 0; i < stacks.size(); i++) {
            if (JasonOnceLighterItem.isBoundToCan(stacks.get(i), sourceId)) {
                stacks.set(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean shouldRemoveOnceLighter(@NotNull ItemStack stack, @Nullable UUID sourceId) {
        if (!stack.isOf(ModItems.ONCE_LIGHTER)) {
            return false;
        }
        return sourceId == null || JasonOnceLighterItem.isBoundToCan(stack, sourceId);
    }

    public void reset() {
        if (this.world instanceof ServerWorld serverWorld) {
            /*
             * 回合结束时世界组件会清空所有落地油桶/火焰。
             * 同步清掉所有维度中的一次性打火机，防止打火机被丢到地上后绕过玩家 ResetPlayerEvent 残留到下一局。
             */
            for (ServerPlayerEntity player : serverWorld.getServer().getPlayerManager().getPlayerList()) {
                removeOnceLighters(player);
            }
            removeDroppedOnceLighters(serverWorld.getServer(), null);
        }
        this.landedCans.clear();
        this.fires.clear();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList cans = new NbtList();
        for (Map.Entry<UUID, LandedCanRecord> entry : this.landedCans.entrySet()) {
            NbtCompound record = writeRecord(entry.getKey(), entry.getValue().center, entry.getValue().ownerUuid, entry.getValue().ageTicks);
            cans.add(record);
        }
        tag.put(CANS_KEY, cans);

        NbtList fires = new NbtList();
        for (Map.Entry<UUID, FireRecord> entry : this.fires.entrySet()) {
            NbtCompound record = writeRecord(entry.getKey(), entry.getValue().center, entry.getValue().ownerUuid, entry.getValue().ageTicks);
            fires.add(record);
        }
        tag.put(FIRES_KEY, fires);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.landedCans.clear();
        this.fires.clear();

        NbtList cans = tag.getList(CANS_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < cans.size(); i++) {
            NbtCompound record = cans.getCompound(i);
            if (!record.containsUuid(ID_KEY)) {
                continue;
            }
            this.landedCans.put(record.getUuid(ID_KEY), new LandedCanRecord(
                    readCenter(record),
                    record.containsUuid(OWNER_KEY) ? record.getUuid(OWNER_KEY) : null,
                    Math.max(0, record.getInt(AGE_KEY))
            ));
        }

        NbtList fires = tag.getList(FIRES_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < fires.size(); i++) {
            NbtCompound record = fires.getCompound(i);
            if (!record.containsUuid(ID_KEY)) {
                continue;
            }
            this.fires.put(record.getUuid(ID_KEY), new FireRecord(
                    readCenter(record),
                    record.containsUuid(OWNER_KEY) ? record.getUuid(OWNER_KEY) : null,
                    Math.max(0, record.getInt(AGE_KEY))
            ));
        }
    }

    private static NbtCompound writeRecord(UUID id, Vec3d center, @Nullable UUID ownerUuid, int ageTicks) {
        NbtCompound record = new NbtCompound();
        record.putUuid(ID_KEY, id);
        if (ownerUuid != null) {
            record.putUuid(OWNER_KEY, ownerUuid);
        }
        record.putDouble(X_KEY, center.x);
        record.putDouble(Y_KEY, center.y);
        record.putDouble(Z_KEY, center.z);
        record.putInt(AGE_KEY, ageTicks);
        return record;
    }

    private static Vec3d readCenter(NbtCompound record) {
        return new Vec3d(record.getDouble(X_KEY), record.getDouble(Y_KEY), record.getDouble(Z_KEY));
    }

    private static final class LandedCanRecord {
        private final Vec3d center;
        private final @Nullable UUID ownerUuid;
        private int ageTicks;

        private LandedCanRecord(Vec3d center, @Nullable UUID ownerUuid, int ageTicks) {
            this.center = center;
            this.ownerUuid = ownerUuid;
            this.ageTicks = ageTicks;
        }
    }

    private static final class FireRecord {
        private final Vec3d center;
        private final @Nullable UUID ownerUuid;
        private int ageTicks;

        private FireRecord(Vec3d center, @Nullable UUID ownerUuid, int ageTicks) {
            this.center = center;
            this.ownerUuid = ownerUuid;
            this.ageTicks = ageTicks;
        }
    }
}
