package org.agmas.noellesroles.roles.spring_trap;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 增速飞斧落地后的黄绿色光环。
 *
 * <p>这个状态必须做成世界组件，而不是静态 Map：
 * 时停者回溯会按组件 NBT 恢复运行态，只有把 aura 的位置、年龄和投掷者写进世界组件，
 * 回溯播放时粒子展开与药水影响才能严格回到目标快照。</p>
 */
public final class SpringTrapAuraWorldComponent implements Component, ServerTickingComponent {
    public static final ComponentKey<SpringTrapAuraWorldComponent> KEY = ComponentRegistry.getOrCreate(
            NoellesRolesCore.id("spring_trap_auras"),
            SpringTrapAuraWorldComponent.class
    );

    private static final DustParticleEffect AURA_PARTICLE = new DustParticleEffect(new Vector3f(0.72F, 0.95F, 0.08F), 1.15F);
    private static final double PARTICLE_HEIGHT = 0.18D;
    private static final int PARTICLES_PER_RING_TICK = 36;
    private static final int PARTICLE_TICK_INTERVAL = 2;

    private final World world;
    private final LinkedHashMap<UUID, AuraRecord> auras = new LinkedHashMap<>();

    public SpringTrapAuraWorldComponent(World world) {
        this.world = world;
    }

    public void addAura(@NotNull Vec3d center, @NotNull UUID ownerUuid) {
        this.auras.put(UUID.randomUUID(), new AuraRecord(center, ownerUuid, 0));
    }

    @Override
    public void serverTick() {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }
        if (this.auras.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, AuraRecord>> iterator = this.auras.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, AuraRecord> entry = iterator.next();
            AuraRecord aura = entry.getValue();
            aura.ageTicks++;

            if (aura.ageTicks > SpringTrapConstants.THROWING_SPEED_AURA_TOTAL_TICKS) {
                iterator.remove();
                continue;
            }

            double currentRadius = getCurrentRadius(aura.ageTicks);
            if (aura.ageTicks % PARTICLE_TICK_INTERVAL == 0) {
                spawnRingParticles(serverWorld, aura.center, currentRadius, aura.ageTicks);
            }
            applyEffects(serverWorld, aura, currentRadius);
        }
    }

    private static double getCurrentRadius(int ageTicks) {
        if (ageTicks >= SpringTrapConstants.THROWING_SPEED_AURA_EXPAND_TICKS) {
            return SpringTrapConstants.THROWING_SPEED_AURA_RADIUS_BLOCKS;
        }
        double progress = Math.max(0.0D, Math.min(1.0D, ageTicks / (double) SpringTrapConstants.THROWING_SPEED_AURA_EXPAND_TICKS));
        return Math.max(0.35D, SpringTrapConstants.THROWING_SPEED_AURA_RADIUS_BLOCKS * progress);
    }

    private static void spawnRingParticles(ServerWorld world, Vec3d center, double radius, int ageTicks) {
        /*
         * 粒子只按半径数学展开，不做方块射线检查。
         * 这保证“光环效果穿墙”；视觉上如果墙体遮挡，普通粒子仍会被玩家视角里的方块挡住。
         */
        double phase = ageTicks * 0.18D;
        for (int i = 0; i < PARTICLES_PER_RING_TICK; i++) {
            double angle = phase + (Math.PI * 2.0D * i / PARTICLES_PER_RING_TICK);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.spawnParticles(AURA_PARTICLE, x, center.y + PARTICLE_HEIGHT, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void applyEffects(ServerWorld world, AuraRecord aura, double radius) {
        if (aura.ageTicks % SpringTrapConstants.THROWING_SPEED_AURA_EFFECT_REFRESH_TICKS != 0) {
            return;
        }

        Box box = new Box(aura.center, aura.center).expand(radius);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!box.contains(player.getPos()) || player.squaredDistanceTo(aura.center) > radius * radius) {
                continue;
            }
            if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
                continue;
            }

            EffectSide side = resolveEffectSide(gameWorld, player, aura.ownerUuid);
            if (side == EffectSide.ALLY) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SPEED,
                        SpringTrapConstants.THROWING_SPEED_AURA_ALLY_SPEED_TICKS,
                        SpringTrapConstants.THROWING_SPEED_AURA_EFFECT_AMPLIFIER,
                        true,
                        true,
                        true
                ));
            } else if (side == EffectSide.ENEMY) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS,
                        SpringTrapConstants.THROWING_SPEED_AURA_ENEMY_SLOWNESS_TICKS,
                        SpringTrapConstants.THROWING_SPEED_AURA_EFFECT_AMPLIFIER,
                        true,
                        true,
                        true
                ));
            }
        }
    }

    private static EffectSide resolveEffectSide(GameWorldComponent gameWorld, ServerPlayerEntity player, UUID ownerUuid) {
        if (player.getUuid().equals(ownerUuid)) {
            return EffectSide.ALLY;
        }
        var role = gameWorld.getRole(player);
        if (role == null) {
            return EffectSide.NONE;
        }
        if (role.getFaction() == Faction.KILLER || NoellesRoleGroups.KILLER_SIDED_NEUTRALS.contains(role)) {
            return EffectSide.ALLY;
        }
        if (role.getFaction() == Faction.CIVILIAN
                || role.getFaction() == Faction.VIGILANTE
                || NoellesRoleGroups.INDEPENDENT_NEUTRALS.contains(role)) {
            return EffectSide.ENEMY;
        }
        return EffectSide.NONE;
    }

    public void reset() {
        this.auras.clear();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (this.auras.isEmpty()) {
            return;
        }

        NbtList list = new NbtList();
        for (Map.Entry<UUID, AuraRecord> entry : this.auras.entrySet()) {
            NbtCompound record = new NbtCompound();
            record.putUuid("Id", entry.getKey());
            record.putUuid("Owner", entry.getValue().ownerUuid);
            record.putDouble("X", entry.getValue().center.x);
            record.putDouble("Y", entry.getValue().center.y);
            record.putDouble("Z", entry.getValue().center.z);
            record.putInt("Age", entry.getValue().ageTicks);
            list.add(record);
        }
        tag.put("Auras", list);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.auras.clear();
        NbtList list = tag.getList("Auras", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound record = list.getCompound(i);
            if (!record.containsUuid("Id") || !record.containsUuid("Owner")) {
                continue;
            }
            this.auras.put(record.getUuid("Id"), new AuraRecord(
                    new Vec3d(record.getDouble("X"), record.getDouble("Y"), record.getDouble("Z")),
                    record.getUuid("Owner"),
                    record.getInt("Age")
            ));
        }
    }

    private enum EffectSide {
        ALLY,
        ENEMY,
        NONE
    }

    private static final class AuraRecord {
        private final Vec3d center;
        private final UUID ownerUuid;
        private int ageTicks;

        private AuraRecord(Vec3d center, UUID ownerUuid, int ageTicks) {
            this.center = center;
            this.ownerUuid = ownerUuid;
            this.ageTicks = ageTicks;
        }
    }
}
