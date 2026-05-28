package org.agmas.noellesroles.roles.stalker;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.*;

public class StalkerPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<StalkerPlayerComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "stalker"), StalkerPlayerComponent.class);

    // ==================== 常量定义（可配置） ====================
    /** 一阶段进阶所需能量基数（每人×15） */
    public static final int PHASE1_ENERGY_PER_PLAYER = 15;
    /** 二阶段进阶所需能量基数（每人×2） */
    public static final int PHASE2_ENERGY_PER_PLAYER = 2;
    /** 二阶段进阶所需击杀基数（人数/6，向上取整，最小1） */
    public static final int PHASE2_KILLS_PER_PLAYER_DIVISOR = 6;
    /** 三阶段持续时间（秒） */
    public static final int PHASE3_DURATION_SECONDS = 300;
    /** 处决减少时间（秒） */
    public static final int EXECUTION_REDUCTION_SECONDS = 10;
    /** 二阶段攻击冷却（秒） */
    public static final int PHASE2_ATTACK_COOLDOWN_SECONDS = 1;
    /** 三阶段突进冷却（秒） */
    public static final int DASH_COOLDOWN_SECONDS = 0;
    /** 最小蓄力时间（秒） */
    public static final int MIN_CHARGE_SECONDS = 0;
    /** 最大蓄力时间（秒） */
    public static final int MAX_CHARGE_SECONDS = 4;
    /** 基础突进距离（格） */
    public static final double BASE_DASH_DISTANCE = 8.0;
    /** 每秒蓄力增加突进距离（格） */
    public static final double DASH_DISTANCE_PER_SECOND = 6.0;
    /** 窥视角度（度） */
    public static final double GAZE_ANGLE = 80.0;
    /** 窥视最大距离（格） */
    public static final double GAZE_DISTANCE = 48.0;
    /** 能量获取速率（每秒获得目标数） */
    public static final int ENERGY_PER_TARGET_PER_SECOND = 1;

    // ==================== 状态变量 ====================
    private final PlayerEntity player;
    /** 当前阶段 1/2/3 */
    public int phase = 0;
    /** 当前能量 */
    public int energy = 0;
    /** 二阶段击杀数 */
    public int phase2Kills = 0;
    /** 免疫是否已使用 */
    public boolean immunityUsed = false;
    /** 三阶段倒计时（tick） */
    public int phase3Timer = 0;
    /** 是否正在窥视 */
    public boolean isGazing = false;
    /** 当前窥视目标数（用于显示） */
    public int gazingTargetCount = 0;
    /** 三阶段突进模式是否激活 */
    public boolean dashModeActive = false;
    /** 是否正在蓄力 */
    public boolean isCharging = false;
    /** 蓄力时间（tick） */
    public int chargeTime = 0;
    /** 是否正在突进 */
    public boolean isDashing = false;
    /** 突进剩余距离 */
    public double dashDistanceRemaining = 0;
    /** 突进方向 */
    public Vec3d dashDirection = Vec3d.ZERO;
    /** 是否已标记为潜行者（用于角色转换后识别） */
    public boolean isStalkerMarked = false;
    /** 二阶段攻击冷却计时器（tick） */
    public int attackCooldown = 0;
    /** 三阶段突进冷却计时器（tick） */
    public int dashCooldown = 0;
    /** 能量获取计数器（每秒触发） */
    private int energyTickCounter = 0;

    /** 一阶段所需能量（动态计算） */
    private int phase1EnergyRequired = 500;
    /** 二阶段所需能量（动态计算） */
    private int phase2EnergyRequired = 30;
    /** 二阶段所需击杀数（动态计算） */
    private int phase2KillsRequired = 2;

    public int getPhase1EnergyRequired() { return phase1EnergyRequired; }
    public int getPhase2EnergyRequired() { return phase2EnergyRequired; }
    public int getPhase2KillsRequired() { return phase2KillsRequired; }

    public StalkerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        phase = tag.getInt("phase");
        energy = tag.getInt("energy");
        phase2Kills = tag.getInt("phase2Kills");
        immunityUsed = tag.getBoolean("immunityUsed");
        phase3Timer = tag.getInt("phase3Timer");
        isGazing = tag.getBoolean("isGazing");
        gazingTargetCount = tag.getInt("gazingTargetCount");
        dashModeActive = tag.getBoolean("dashModeActive");
        isCharging = tag.getBoolean("isCharging");
        chargeTime = tag.getInt("chargeTime");
        isDashing = tag.getBoolean("isDashing");
        dashDistanceRemaining = tag.getDouble("dashDistanceRemaining");
        double dx = tag.getDouble("dashDirX");
        double dy = tag.getDouble("dashDirY");
        double dz = tag.getDouble("dashDirZ");
        dashDirection = new Vec3d(dx, dy, dz);
        isStalkerMarked = tag.getBoolean("isStalkerMarked");
        attackCooldown = tag.getInt("attackCooldown");
        dashCooldown = tag.getInt("dashCooldown");
        phase1EnergyRequired = tag.getInt("phase1EnergyRequired");
        phase2EnergyRequired = tag.getInt("phase2EnergyRequired");
        phase2KillsRequired = tag.getInt("phase2KillsRequired");
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("phase", phase);
        tag.putInt("energy", energy);
        tag.putInt("phase2Kills", phase2Kills);
        tag.putBoolean("immunityUsed", immunityUsed);
        tag.putInt("phase3Timer", phase3Timer);
        tag.putBoolean("isGazing", isGazing);
        tag.putInt("gazingTargetCount", gazingTargetCount);
        tag.putBoolean("dashModeActive", dashModeActive);
        tag.putBoolean("isCharging", isCharging);
        tag.putInt("chargeTime", chargeTime);
        tag.putBoolean("isDashing", isDashing);
        tag.putDouble("dashDistanceRemaining", dashDistanceRemaining);
        tag.putDouble("dashDirX", dashDirection.x);
        tag.putDouble("dashDirY", dashDirection.y);
        tag.putDouble("dashDirZ", dashDirection.z);
        tag.putBoolean("isStalkerMarked", isStalkerMarked);
        tag.putInt("attackCooldown", attackCooldown);
        tag.putInt("dashCooldown", dashCooldown);
        tag.putInt("phase1EnergyRequired", phase1EnergyRequired);
        tag.putInt("phase2EnergyRequired", phase2EnergyRequired);
        tag.putInt("phase2KillsRequired", phase2KillsRequired);
    }

    @Override
    public void serverTick() {
        // 仅在潜行者且存活时更新
        if (!isActiveStalker() || !player.isAlive() || player.isSpectator()) return;

        // 二阶段及以上禁止冲刺
        if (phase >= 2 && player.isSprinting()) {
            player.setSprinting(false);
        }

        // 攻击冷却
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        // 突进冷却
        if (dashCooldown > 0) {
            dashCooldown--;
        }

        // 窥视能量获取（每秒）
        if (isGazing && phase <= 2) {
            energyTickCounter++;
            if (energyTickCounter >= 20) {
                energyTickCounter = 0;
                int visibleCount = getVisiblePlayers().size();
                gazingTargetCount = visibleCount;
                if (visibleCount > 0) {
                    addEnergy(visibleCount * ENERGY_PER_TARGET_PER_SECOND);
                }
            }
        }

        // 三阶段倒计时
        if (phase == 3 && dashModeActive) {
            if (phase3Timer > 0) {
                phase3Timer--;
                if (phase3Timer % 20 == 0) sync();
            }
            if (phase3Timer <= 0) {
                regressToPhase2();
            }
        }

        // 蓄力处理
        if (isCharging) {
            chargeTime++;
            if (chargeTime > MAX_CHARGE_SECONDS * 20) {
                chargeTime = MAX_CHARGE_SECONDS * 20;
            }
            // 蓄力时减速
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 2, 1, false, false));
        }

        // 突进处理
        if (isDashing) {
            performDash();
        }

        // 阶段进阶检查（在能量增加后调用）
        checkPhaseAdvance();

        // 周期性同步（可选）
        if (player.getWorld().getTime() % 20 == 0) sync();
    }

    @Override
    public void clientTick() {
        if (phase >= 2 && player.isSprinting()) {
            player.setSprinting(false);
        }
    }

    // ==================== 核心逻辑 ====================

    public void reset() {
        this.phase = 1;
        this.energy = 0;
        this.phase2Kills = 0;
        this.immunityUsed = false;
        this.phase3Timer = 0;
        this.isGazing = false;
        this.gazingTargetCount = 0;
        this.dashModeActive = false;
        this.isCharging = false;
        this.chargeTime = 0;
        this.isDashing = false;
        this.dashDistanceRemaining = 0;
        this.dashDirection = Vec3d.ZERO;
        this.isStalkerMarked = true;
        this.attackCooldown = 0;
        this.dashCooldown = 0;
        // 根据当前人数重新计算需求
        int playerCount = getPlayerCount();
        this.phase1EnergyRequired = playerCount * PHASE1_ENERGY_PER_PLAYER;
        this.phase2EnergyRequired = playerCount * PHASE2_ENERGY_PER_PLAYER;
        int kills = (int) Math.ceil(playerCount / (double) PHASE2_KILLS_PER_PLAYER_DIVISOR);
        this.phase2KillsRequired = Math.max(1, kills);
        this.sync();
    }

    public boolean isActiveStalker() {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRole(player, Noellesroles.STALKER) && isStalkerMarked && phase > 0;
    }

    private int getPlayerCount() {
        if (player.getWorld().isClient) return 8;
        return player.getWorld().getPlayers().size();
    }

    public void addEnergy(int amount) {
        this.energy += amount;
        checkPhaseAdvance();
        sync();
    }

    public void checkPhaseAdvance() {
        if (phase == 1 && energy >= phase1EnergyRequired) {
            advanceToPhase2();
        } else if (phase == 2 && energy >= phase2EnergyRequired && phase2Kills >= phase2KillsRequired) {
            advanceToPhase3();
        }
    }

    public void advanceToPhase2() {
        this.phase = 2;
        this.energy = 0;
        this.immunityUsed = true; // 进入二阶段盾牌消失
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.STALKER_PHASE_ADVANCE_1_TO_2_EVENT, serverPlayer, null);
            // 给予刀
            serverPlayer.getInventory().offerOrDrop(WatheItems.KNIFE.getDefaultStack());
            // 发送消息
            serverPlayer.sendMessage(Text.translatable("message.noellesroles.stalker.phase2_advance").formatted(Formatting.RED, Formatting.BOLD), false);
            // 播放音效
            player.getWorld().playSound(null, player.getBlockPos(), WatheSounds.ITEM_PSYCHO_ARMOUR, SoundCategory.PLAYERS, 1.0F, 1.5F);
        }
        sync();
    }

    public void advanceToPhase3() {
        this.phase = 3;
        this.phase3Timer = PHASE3_DURATION_SECONDS * 20;
        this.dashModeActive = true;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.STALKER_PHASE_ADVANCE_2_TO_3_EVENT, serverPlayer, null);
            serverPlayer.sendMessage(Text.translatable("message.noellesroles.stalker.phase3_advance").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
            player.getWorld().playSound(null, player.getBlockPos(), WatheSounds.ITEM_PSYCHO_ARMOUR, SoundCategory.PLAYERS, 1.0F, 0.5F); // 更换为合适音效
        }
        sync();
    }

    public void regressToPhase2() {
        this.phase = 2;
        this.dashModeActive = false;
        this.phase2Kills = 0; // 不保留击杀数
        this.phase3Timer = 0;
        this.isCharging = false;
        this.chargeTime = 0;
        this.isDashing = false;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.STALKER_PHASE_REGRESS_3_TO_2_EVENT, serverPlayer, null);
            serverPlayer.sendMessage(Text.translatable("message.noellesroles.stalker.phase_regress").formatted(Formatting.YELLOW), true);
        }
        sync();
    }

    public void addKill() {
        if (phase >= 2) {
            phase2Kills++;
            attackCooldown = PHASE2_ATTACK_COOLDOWN_SECONDS * 20;
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0F, 1.0F);
            checkPhaseAdvance();
            sync();
        }
    }

    public void startGazing() {
        this.isGazing = true;
        sync();
    }

    public void stopGazing() {
        this.isGazing = false;
        this.gazingTargetCount = 0;
        sync();
    }

    public void startCharging() {
        if (phase != 3 || !dashModeActive || isDashing || dashCooldown > 0) return;
        this.isCharging = true;
        this.chargeTime = 0;
        sync();
    }

    public void releaseCharge() {
        if (!isCharging) return;
        if (chargeTime < MIN_CHARGE_SECONDS * 20) {
            this.isCharging = false;
            this.chargeTime = 0;
            sync();
            return;
        }
        double chargeSeconds = Math.min(chargeTime, MAX_CHARGE_SECONDS * 20) / 20.0;
        double dashDistance = BASE_DASH_DISTANCE + (chargeSeconds - 1.0) * DASH_DISTANCE_PER_SECOND;

        this.isCharging = false;
        this.chargeTime = 0;
        this.isDashing = true;
        this.dashDistanceRemaining = dashDistance;
        this.dashCooldown = DASH_COOLDOWN_SECONDS * 20;

        // 获取水平方向
        Vec3d lookDir = player.getRotationVector();
        Vec3d horizontalDir = new Vec3d(lookDir.x, 0, lookDir.z).normalize();
        if (horizontalDir.lengthSquared() < 0.001) {
            float yaw = player.getYaw() * ((float) Math.PI / 180F);
            horizontalDir = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
        }
        this.dashDirection = horizontalDir;

        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BREEZE_CHARGE, SoundCategory.PLAYERS, 1.0F, 0.5F);
        sync();
    }

    private void performDash() {
        if (!isDashing || dashDistanceRemaining <= 0) {
            isDashing = false;
            dashDistanceRemaining = 0;
            sync();
            return;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        double movePerTick = 2.0;
        double actualMove = Math.min(movePerTick, dashDistanceRemaining);

        Vec3d currentPos = player.getPos();
        Vec3d newPos = currentPos.add(dashDirection.multiply(actualMove));

        // 简单碰撞检测（如果撞到方块则停止）
        if (player.getWorld().getBlockState(BlockPos.ofFloored(newPos.add(0, 0.5, 0))).isSolid()) {
            isDashing = false;
            dashDistanceRemaining = 0;
            sync();
            return;
        }

        // 收集本次突进中所有命中玩家（突进路径为从 currentPos 到 newPos 的线段）
        Set<PlayerEntity> hitTargets = new HashSet<>();
        for (PlayerEntity target : player.getWorld().getPlayers()) {
            if (target == player) continue;
            if (!GameFunctions.isPlayerAliveAndSurvival(target)) continue;

            // 计算目标到线段的距离
            double distance = distanceToSegment(target.getPos(), currentPos, newPos);
            if (distance <= 2.5) {
                hitTargets.add(target);
            }
        }

        // 处决所有命中目标（不减少三阶段时间）
        for (PlayerEntity target : hitTargets) {
            executePlayer(target, false);
        }

        // 如果本次突进至少处决了一个玩家，则减少一次三阶段时间
        if (!hitTargets.isEmpty()) {
            onExecution(); // 只减少一次时间
        }

        // 移动玩家
        serverPlayer.requestTeleport(newPos.x, newPos.y, newPos.z);
        dashDistanceRemaining -= actualMove;

        if (dashDistanceRemaining <= 0) {
            isDashing = false;
        }
        sync();
    }

    /**
     * 计算点到线段的最短距离
     */
    private double distanceToSegment(Vec3d point, Vec3d segStart, Vec3d segEnd) {
        Vec3d segVec = segEnd.subtract(segStart);
        Vec3d pointVec = point.subtract(segStart);

        double segLengthSq = segVec.lengthSquared();
        if (segLengthSq < 1e-8) {
            return point.distanceTo(segStart);
        }

        double t = pointVec.dotProduct(segVec) / segLengthSq;
        t = Math.max(0, Math.min(1, t));

        Vec3d projection = segStart.add(segVec.multiply(t));
        return point.distanceTo(projection);
    }

    /**
     * 处决目标玩家（默认减少三阶段时间）
     */
    private void executePlayer(PlayerEntity target) {
        executePlayer(target, true);
    }

    private void executePlayer(PlayerEntity target, boolean countExecution) {
        if (player instanceof ServerPlayerEntity) {
            GameFunctions.killPlayer(target, true, player, GameConstants.DeathReasons.KNIFE);
            if (countExecution) {
                onExecution(); // 减少三阶段时间
            }
            player.sendMessage(Text.translatable("message.noellesroles.stalker.execution_success", target.getName()).formatted(Formatting.RED), true);
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.0F, 1.5F);
        }
    }

    public void onExecution() {
        if (phase == 3) {
            phase3Timer -= EXECUTION_REDUCTION_SECONDS * 20;
            if (phase3Timer < 0) phase3Timer = 0;
            sync();
        }
    }

    private List<PlayerEntity> getVisiblePlayers() {
        List<PlayerEntity> visible = new ArrayList<>();
        Vec3d eyePos = player.getEyePos();
        Vec3d lookDir = player.getRotationVector();

        for (PlayerEntity target : player.getWorld().getPlayers()) {
            if (target == player) continue;
            if (!GameFunctions.isPlayerAliveAndSurvival(target)) continue;

            Vec3d targetPos = target.getEyePos();
            double distance = eyePos.distanceTo(targetPos);
            if (distance > GAZE_DISTANCE) continue;

            Vec3d toTarget = targetPos.subtract(eyePos).normalize();
            double dot = lookDir.dotProduct(toTarget);
            if (dot < Math.cos(Math.toRadians(GAZE_ANGLE))) continue;

            // 射线检测（简化，可使用 RaycastContext）
            // 这里略，假设无阻挡
            visible.add(target);
        }
        return visible;
    }
}
