package org.agmas.noellesroles.roles.lich;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 魔法屏障运行实体。
 *
 * <p>屏障粒子在服务端生成并同步给周围玩家，因此它可以穿墙显示；
 * 影响判定也只按距离，不做视线检测，符合“粒子可穿墙”和范围压制的需求。</p>
 */
public class LichMagicBarrierEntity extends Entity {
    private static final DustParticleEffect PARTICLE = new DustParticleEffect(
            new Vector3f(
                    LichConstants.MAGIC_BARRIER_PARTICLE_RED,
                    LichConstants.MAGIC_BARRIER_PARTICLE_GREEN,
                    LichConstants.MAGIC_BARRIER_PARTICLE_BLUE
            ),
            LichConstants.MAGIC_BARRIER_PARTICLE_SCALE
    );

    private static final Set<Item> DAMAGE_WEAPONS = Set.of(
            WatheItems.REVOLVER,
            WatheItems.DERRINGER,
            WatheItems.KNIFE,
            WatheItems.GRENADE,
            WatheItems.BAT,
            WatheItems.POISON_VIAL,
            WatheItems.SCORPION,
            ModItems.THROWING_AXE,
            ModItems.BLOOD_AXE,
            ModItems.COLORFUL_AXE,
            ModItems.THROWING_SPEED_AXE,
            ModItems.THROWING_BOMB_AXE,
            ModItems.THROWING_BLOOD_AXE,
            ModItems.THROWING_MACHETE,
            ModItems.TOMAHAWK,
            ModItems.THROWING_TOYS_AXE,
            ModItems.THROWING_PICKAXE,
            ModItems.THROWING_JERRY_CAN,
            ModItems.ONCE_LIGHTER,
            ModItems.ROBBER_PISTOL,
            ModItems.BOUNTY_PISTOL,
            ModItems.BOUNTY_DERRINGER,
            ModItems.BAYONET,
            ModItems.SILENCED_REVOLVER,
            ModItems.SILENT_GRENADE,
            ModItems.SNIPER_RIFLE,
            ModItems.PAN,
            ModItems.THROWING_PAN,
            ModItems.PSYCHO_THROWING_PAN,
            ModItems.HUNTING_KNIFE,
            ModItems.SULFURIC_ACID_BARREL,
            ModItems.BLOWGUN,
            ModItems.POISON_INJECTOR,
            ModItems.KNOCKOUT_DRUG,
            ModItems.JERRY_CAN,
            ModItems.LIGHTER,
            ModItems.ONCE_STAFF,
            ModItems.PSYCHO_STAFF
    );

    private Vec3d spawnPos = Vec3d.ZERO;
    private int ticksAlive;
    private UUID ownerUuid;
    private final Set<UUID> playersInside = new HashSet<>();
    private boolean disappearRecorded;

    public LichMagicBarrierEntity(EntityType<? extends LichMagicBarrierEntity> entityType, World world) {
        super(entityType, world);
        this.noClip = true;
    }

    public void setOwner(ServerPlayerEntity owner) {
        /*
         * 屏障当前不需要给施法者结算伤害，但仍保存 owner。
         * 后续如果要加回放、免疫施法者或统计来源，可以直接复用这份稳定 UUID。
         */
        this.ownerUuid = owner.getUuid();
    }

    public ServerPlayerEntity getOwnerPlayer() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld) || this.ownerUuid == null) {
            return null;
        }
        MinecraftServer server = serverWorld.getServer();
        return server.getPlayerManager().getPlayer(this.ownerUuid);
    }

    @Override
    protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        this.noClip = true;
        this.ticksAlive++;

        if (this.spawnPos.equals(Vec3d.ZERO)) {
            this.spawnPos = this.getPos();
        }

        if (!this.getWorld().isClient) {
            /*
             * Entity 基类不会像 ProjectileEntity 那样自动按 velocity 前进。
             * 屏障是纯机制/粒子实体，所以这里在服务端权威移动，再由实体同步位置给客户端。
             */
            this.setPosition(this.getPos().add(this.getVelocity()));

            double traveledSquared = this.spawnPos.squaredDistanceTo(this.getPos());
            if (traveledSquared >= LichConstants.MAGIC_BARRIER_RANGE_BLOCKS * LichConstants.MAGIC_BARRIER_RANGE_BLOCKS) {
                ServerWorld serverWorld = (ServerWorld) this.getWorld();
                recordTrackedPlayerExits(serverWorld);
                recordDisappear(serverWorld);
                this.discard();
                return;
            }

            double radius = this.currentRadius();
            spawnSphereParticles((ServerWorld) this.getWorld(), radius);
            applyCooldownAura(radius);
        }
    }

    private double currentRadius() {
        float progress = MathHelper.clamp(this.ticksAlive / (float) LichConstants.MAGIC_BARRIER_EXPAND_TICKS, 0.0F, 1.0F);
        return LichConstants.MAGIC_BARRIER_RADIUS_BLOCKS * progress;
    }

    private void spawnSphereParticles(ServerWorld world, double radius) {
        if (radius <= LichConstants.MAGIC_BARRIER_MIN_VISIBLE_RADIUS_BLOCKS) {
            return;
        }

        /*
         * 用经纬线采样球壳，而不是填满整个球体。
         * 这样屏障可读性更强，粒子量也稳定受常量控制。
         */
        for (int lat = 0; lat <= LichConstants.MAGIC_BARRIER_PARTICLE_LATITUDE_STEPS; lat++) {
            double theta = Math.PI * lat / LichConstants.MAGIC_BARRIER_PARTICLE_LATITUDE_STEPS;
            double y = Math.cos(theta) * radius;
            double ringRadius = Math.sin(theta) * radius;
            for (int lon = 0; lon < LichConstants.MAGIC_BARRIER_PARTICLE_LONGITUDE_STEPS; lon++) {
                double phi = Math.PI * LichConstants.MAGIC_BARRIER_FULL_CIRCLE_RADIANS_MULTIPLIER * lon / LichConstants.MAGIC_BARRIER_PARTICLE_LONGITUDE_STEPS;
                double x = Math.cos(phi) * ringRadius;
                double z = Math.sin(phi) * ringRadius;
                world.spawnParticles(
                        PARTICLE,
                        this.getX() + x,
                        this.getY() + y,
                        this.getZ() + z,
                        LichConstants.MAGIC_BARRIER_PARTICLES_PER_POINT,
                        LichConstants.MAGIC_BARRIER_PARTICLE_SPREAD,
                        LichConstants.MAGIC_BARRIER_PARTICLE_SPREAD,
                        LichConstants.MAGIC_BARRIER_PARTICLE_SPREAD,
                        LichConstants.MAGIC_BARRIER_PARTICLE_SPEED
                );
            }
        }
    }

    private void applyCooldownAura(double radius) {
        if (radius <= 0.0D) {
            return;
        }
        ServerWorld serverWorld = (ServerWorld) this.getWorld();
        Set<UUID> currentInside = new HashSet<>();
        Box box = this.getBoundingBox().expand(radius);
        for (PlayerEntity player : this.getWorld().getEntitiesByClass(PlayerEntity.class, box, target ->
                target instanceof ServerPlayerEntity
                        && !isOwner(target)
                        && GameFunctions.isPlayerAliveAndSurvival(target)
                        && target.squaredDistanceTo(this) <= radius * radius
                        && isAffectedFaction(target))) {
            UUID playerUuid = player.getUuid();
            currentInside.add(playerUuid);
            if (!this.playersInside.contains(playerUuid)) {
                recordBarrierPlayerEvent(serverWorld, NoellesEventIds.LICH_MAGIC_BARRIER_ENTER_EVENT, playerUuid);
            }
            refreshTargetCooldowns(player);
        }
        /*
         * 进入/脱离回放不能跟冷却刷新一样每 tick 写一条。
         * 所以这里用上一 tick 的 UUID 集合做差集，只在玩家第一次进入或真正离开影响范围时记录。
         */
        for (UUID previousUuid : this.playersInside) {
            if (!currentInside.contains(previousUuid)) {
                recordBarrierPlayerEvent(serverWorld, NoellesEventIds.LICH_MAGIC_BARRIER_EXIT_EVENT, previousUuid);
            }
        }
        this.playersInside.clear();
        this.playersInside.addAll(currentInside);
    }

    private void recordDisappear(ServerWorld world) {
        if (this.disappearRecorded) {
            return;
        }
        this.disappearRecorded = true;

        NbtCompound extra = new NbtCompound();
        if (this.ownerUuid != null) {
            extra.putUuid("owner", this.ownerUuid);
        }
        /*
         * 屏障到达最大飞行距离后会被直接清理。
         * 这里单独记录自然消失事件，避免和回合结束/实体强制清理一类非玩法原因混在一起。
         */
        GameRecordManager.recordGlobalEvent(world, NoellesEventIds.LICH_MAGIC_BARRIER_DISAPPEAR_EVENT, null, extra);
    }

    private void recordTrackedPlayerExits(ServerWorld world) {
        /*
         * 如果目标一直待到屏障自然消失，下一次范围差集已经不会再执行。
         * 这里补齐“脱离魔法屏障”的回放，保证进入/脱离事件在时间线上成对闭合。
         */
        for (UUID playerUuid : this.playersInside) {
            recordBarrierPlayerEvent(world, NoellesEventIds.LICH_MAGIC_BARRIER_EXIT_EVENT, playerUuid);
        }
        this.playersInside.clear();
    }

    private void recordBarrierPlayerEvent(ServerWorld world, Identifier eventId, UUID playerUuid) {
        NbtCompound extra = new NbtCompound();
        extra.putUuid("player", playerUuid);
        if (this.ownerUuid != null) {
            extra.putUuid("owner", this.ownerUuid);
        }
        /*
         * 进入/脱离事件的主角是被屏障影响的玩家，而不是施法者。
         * 因此 UUID 明确写入 player 字段，formatter 即使在玩家离线后也能从对局缓存还原名字。
         */
        GameRecordManager.recordGlobalEvent(world, eventId, null, extra);
    }

    private boolean isOwner(PlayerEntity player) {
        /*
         * 屏障的施法者无论自己是什么阵营，都不应该被自己的粒子球反向锁武器/锁技能。
         * 这里用保存的 UUID 判断，而不是按角色判断，确保巫妖通过调试/转职/特殊状态释放后也能稳定免疫。
         */
        return this.ownerUuid != null && this.ownerUuid.equals(player.getUuid());
    }

    private boolean isAffectedFaction(PlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        var role = gameWorld.getRole(player);
        if (role == null) {
            return false;
        }
        Faction faction = role.getFaction();
        return faction == Faction.CIVILIAN
                || faction == Faction.VIGILANTE
                || NoellesRoleGroups.INDEPENDENT_NEUTRALS.contains(role);
    }

    private void refreshTargetCooldowns(PlayerEntity player) {
        /*
         * 用户强调“偷到杀手武器也一样吃冷却”。
         * 因此这里扫描玩家整个背包，只要持有可造成伤害的武器，就刷新该物品类型的冷却。
         */
        for (Item item : DAMAGE_WEAPONS) {
            if (hasItem(player, item)) {
                player.getItemCooldownManager().set(item, LichConstants.MAGIC_BARRIER_ITEM_COOLDOWN_TICKS);
            }
        }

        /*
         * 第三条确认里说明：这里的“技能冷却 15s”指 NoellesRoles 通用能力键组件。
         * 在屏障内每 tick 刷新，离开范围后组件自然按自己的 tick 递减。
         */
        AbilityPlayerComponent.KEY.get(player).setCooldown(LichConstants.MAGIC_BARRIER_ABILITY_COOLDOWN_TICKS);
    }

    private boolean hasItem(PlayerEntity player, Item item) {
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(item)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offHand) {
            if (stack.isOf(item)) {
                return true;
            }
        }
        return player.getMainHandStack().isOf(item) || player.getOffHandStack().isOf(item);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.ticksAlive = nbt.getInt("LichBarrierAge");
        this.spawnPos = new Vec3d(nbt.getDouble("LichBarrierSpawnX"), nbt.getDouble("LichBarrierSpawnY"), nbt.getDouble("LichBarrierSpawnZ"));
        this.ownerUuid = nbt.containsUuid("LichBarrierOwner") ? nbt.getUuid("LichBarrierOwner") : null;
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("LichBarrierAge", this.ticksAlive);
        nbt.putDouble("LichBarrierSpawnX", this.spawnPos.x);
        nbt.putDouble("LichBarrierSpawnY", this.spawnPos.y);
        nbt.putDouble("LichBarrierSpawnZ", this.spawnPos.z);
        if (this.ownerUuid != null) {
            nbt.putUuid("LichBarrierOwner", this.ownerUuid);
        }
    }
}
