package org.agmas.noellesroles.roles.spiritualist;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 灵术师本人组件。
 *
 * <p>这里统一维护灵术师的全部“主状态”：
 * 1. 当前是正常 / 出窍 / 附身中的哪一种；
 * 2. 出窍或附身开始时，本体原地停留的锚点坐标与朝向；
 * 3. 当前附身的目标 UUID；
 * 4. 正常解除附身后，为目标保留的一次性余留庇护。
 *
 * <p>这样服务端判定、客户端 HUD、渲染隐藏和语音聊天规则都只需要看这一份状态。</p>
 */
public class SpiritualistPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<SpiritualistPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "spiritualist_player"),
            SpiritualistPlayerComponent.class
    );

    private final PlayerEntity player;

    public SpiritualistState state = SpiritualistState.NORMAL;
    @Nullable
    public UUID possessionTarget;

    /**
     * 本体停留点。
     *
     * <p>出窍时用于判定“本体是否被传送 / 异常位移”；</p>
     * <p>附身时用于：</p>
     * <p>1. 灵术师隐藏本体的锚点；</p>
     * <p>2. 被附身者受保护时传回这里；</p>
     * <p>3. 正常解除附身后的余留庇护回传点。</p>
     */
    public double bodyAnchorX;
    public double bodyAnchorY;
    public double bodyAnchorZ;
    public float bodyAnchorYaw;
    public float bodyAnchorPitch;

    /**
     * 余留庇护目标。
     *
     * <p>正常主动结束附身后，只给刚才那名被附身者保留一次 15 秒免伤。</p>
     */
    @Nullable
    public UUID lingeringProtectedTarget;
    public int lingeringProtectionTicks = 0;
    public boolean lingeringProtectionAvailable = false;
    public double lingeringReturnX;
    public double lingeringReturnY;
    public double lingeringReturnZ;
    public float lingeringReturnYaw;
    public float lingeringReturnPitch;

    /**
     * 下面这一组字段只在服务端内存里用来保存“灵术师原本自己的状态”。
     *
     * <p>附身期间，灵术师客户端需要看到并操作被附身者的血量、经验和物品栏，
     * 因此服务端会把这些“可视状态”实时镜像到灵术师自己身上。
     * 但真正结束附身时，又必须无损恢复灵术师原本的数据，
     * 所以这里专门保存一份进入附身前的快照。</p>
     *
     * <p>这些数据不写入 NBT，也不需要同步给客户端：
     * 1. 它们只是服务器运行期的兜底缓存；
     * 2. 真正需要同步给客户端的是当前主状态，而不是这份内部备份。</p>
     */
    @Nullable
    private NbtList savedInventoryNbt;
    private int savedSelectedSlot = 0;
    private float savedHealth = 20.0f;
    private float savedAbsorption = 0.0f;
    private int savedFoodLevel = 20;
    private float savedSaturation = 5.0f;
    private float savedExhaustion = 0.0f;
    private int savedExperienceLevel = 0;
    private int savedTotalExperience = 0;
    private float savedExperienceProgress = 0.0f;
    private boolean shadowStateSaved = false;

    /**
     * 附身控制输入缓存。
     *
     * <p>灵术师客户端会把当前“想让宿主做什么”这一帧输入同步到服务端，
     * 服务端再由这里暂存，并在 serverTick 内统一驱动宿主行动。
     * 这样可以把“收包”和“真正执行动作”的时机统一到主线程 tick 内，
     * 避免直接在收包回调里改玩家运动时序导致的奇怪抖动。</p>
     */
    public float possessionForwardInput = 0.0f;
    public float possessionSidewaysInput = 0.0f;
    public float possessionYaw = 0.0f;
    public float possessionPitch = 0.0f;
    public boolean possessionJumping = false;
    public boolean possessionSneaking = false;
    public boolean possessionSprinting = false;
    public boolean possessionUsing = false;
    public boolean possessionAttacking = false;
    public boolean lastPossessionUsing = false;
    public boolean lastPossessionAttacking = false;

    /**
     * 挖掘状态缓存。
     *
     * <p>左键按住时，原版挖掘并不是“一次点击直接破坏”，
     * 而是 start / continue / abort 三段式。
     * 因此这里额外记录灵术师当前正在代替宿主挖哪一格，方便服务端继续推进挖掘进度。</p>
     */
    @Nullable
    public BlockPos possessionMiningPos;
    @Nullable
    public Direction possessionMiningDirection;

    /**
     * 宿主上一次镜像到灵术师界面的背包快照。
     *
     * <p>附身期间如果灵术师打开背包并调整物品，
     * 那些点击其实会先落在“灵术师自己的 player inventory”上。
     * 为了把这类改动再同步回宿主，我们需要知道：
     * “当前灵术师背包是不是已经偏离上一次从宿主拉下来的快照了”。</p>
     */
    @Nullable
    private NbtList lastMirroredTargetInventoryNbt;
    private int lastMirroredTargetSelectedSlot = 0;

    /**
     * 附身期间“隐藏本体空气壳”所需的原始状态快照。
     *
     * <p>你要求灵术师附身后，真实本体对其他玩家来说必须像空气一样：
     * 看不见、撞不到、不能交互、也不会自己滑走。
     * 为了避免直接硬改完却忘记在结束时恢复，这里额外把进入附身前的相关物理标记暂存下来。</p>
     *
     * <p>这些字段只在服务器运行时存在，不写入 NBT。</p>
     */
    private boolean detachedBodyShellApplied = false;
    private boolean savedDetachedBodyInvisible = false;
    private boolean savedDetachedBodyNoGravity = false;
    private boolean savedDetachedBodyNoClip = false;

    public SpiritualistPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void reset() {
        restoreDetachedBodyShellState();
        this.state = SpiritualistState.NORMAL;
        this.possessionTarget = null;
        this.bodyAnchorX = 0;
        this.bodyAnchorY = 0;
        this.bodyAnchorZ = 0;
        this.bodyAnchorYaw = 0;
        this.bodyAnchorPitch = 0;
        clearLingeringProtection();
        clearSavedShadowState();
        clearPossessionControlState();
        clearMirroredTargetInventory();
        this.sync();
    }

    public boolean isProjecting() {
        return this.state == SpiritualistState.PROJECTING;
    }

    public boolean isPossessing() {
        return this.state == SpiritualistState.POSSESSING && this.possessionTarget != null;
    }

    /**
     * 当灵术师进入附身时，真实本体需要被隐藏并完全视作“不可交互的空壳”；
     * 当灵术师进入出窍时，本体仍然可见，但要失去实体推挤体积。
     *
     * <p>因此这里把“本体目前是否处于特殊状态”抽出来，供碰撞 / 渲染 / 交互 mixin 统一读取。</p>
     */
    public boolean hasDetachedBodyState() {
        return this.state != SpiritualistState.NORMAL;
    }

    public void startProjection() {
        rememberCurrentBodyAnchor();
        this.state = SpiritualistState.PROJECTING;
        this.possessionTarget = null;
        clearPossessionControlState();
        this.sync();
    }

    public void finishProjection(boolean applyCooldown) {
        if (!this.isProjecting()) {
            return;
        }

        this.state = SpiritualistState.NORMAL;
        this.possessionTarget = null;
        clearPossessionControlState();
        if (applyCooldown) {
            AbilityPlayerComponent.KEY.get(this.player).setCooldown(SpiritualistConstants.PROJECTION_END_COOLDOWN_TICKS);
        }
        this.sync();
    }

    public void startPossession(@NotNull PlayerEntity target) {
        rememberCurrentBodyAnchor();
        clearLingeringProtection();
        saveCurrentShadowState();
        applyDetachedBodyShellState();
        this.state = SpiritualistState.POSSESSING;
        this.possessionTarget = target.getUuid();
        this.possessionYaw = target.getYaw();
        this.possessionPitch = target.getPitch();
        copyTargetShadowState(target);
        rememberMirroredTargetInventory(target);
        this.sync();
    }

    public void finishPossession(boolean applyCooldown) {
        restoreSavedShadowState();
        restoreDetachedBodyShellState();
        this.state = SpiritualistState.NORMAL;
        this.possessionTarget = null;
        clearPossessionControlState();
        clearMirroredTargetInventory();
        if (applyCooldown) {
            AbilityPlayerComponent.KEY.get(this.player).setCooldown(SpiritualistConstants.POSSESSION_END_COOLDOWN_TICKS);
        }
        this.sync();
    }

    public void setLingeringProtection(@NotNull UUID targetUuid) {
        this.lingeringProtectedTarget = targetUuid;
        this.lingeringProtectionTicks = SpiritualistConstants.LINGERING_PROTECTION_TICKS;
        this.lingeringProtectionAvailable = true;
        this.lingeringReturnX = this.bodyAnchorX;
        this.lingeringReturnY = this.bodyAnchorY;
        this.lingeringReturnZ = this.bodyAnchorZ;
        this.lingeringReturnYaw = this.bodyAnchorYaw;
        this.lingeringReturnPitch = this.bodyAnchorPitch;
        this.sync();
    }

    public boolean hasLingeringProtectionFor(@Nullable PlayerEntity target) {
        return target != null
                && this.lingeringProtectionAvailable
                && this.lingeringProtectedTarget != null
                && this.lingeringProtectedTarget.equals(target.getUuid())
                && this.lingeringProtectionTicks > 0;
    }

    public void consumeLingeringProtection() {
        clearLingeringProtection();
        this.sync();
    }

    public void clearLingeringProtection() {
        this.lingeringProtectedTarget = null;
        this.lingeringProtectionTicks = 0;
        this.lingeringProtectionAvailable = false;
        this.lingeringReturnX = 0;
        this.lingeringReturnY = 0;
        this.lingeringReturnZ = 0;
        this.lingeringReturnYaw = 0;
        this.lingeringReturnPitch = 0;
    }

    public boolean isBodyAnchorMoved() {
        double dx = this.player.getX() - this.bodyAnchorX;
        double dy = this.player.getY() - this.bodyAnchorY;
        double dz = this.player.getZ() - this.bodyAnchorZ;
        return dx * dx + dy * dy + dz * dz > SpiritualistConstants.BODY_MOVE_CANCEL_DISTANCE_SQUARED;
    }

    /**
     * 更新“灵术师当前想让宿主执行的输入”。
     */
    public void updatePossessionControl(
            float forward,
            float sideways,
            float yaw,
            float pitch,
            boolean jumping,
            boolean sneaking,
            boolean sprinting,
            boolean using,
            boolean attacking
    ) {
        this.possessionForwardInput = forward;
        this.possessionSidewaysInput = sideways;
        this.possessionYaw = yaw;
        this.possessionPitch = pitch;
        this.possessionJumping = jumping;
        this.possessionSneaking = sneaking;
        this.possessionSprinting = sprinting;
        this.possessionUsing = using;
        this.possessionAttacking = attacking;
    }

    /**
     * 记录灵术师本人进入附身前的可视状态。
     */
    private void saveCurrentShadowState() {
        PlayerInventory inventory = this.player.getInventory();
        this.savedInventoryNbt = inventory.writeNbt(new NbtList());
        this.savedSelectedSlot = inventory.selectedSlot;
        this.savedHealth = this.player.getHealth();
        this.savedAbsorption = this.player.getAbsorptionAmount();

        HungerManager hungerManager = this.player.getHungerManager();
        this.savedFoodLevel = hungerManager.getFoodLevel();
        this.savedSaturation = hungerManager.getSaturationLevel();
        this.savedExhaustion = hungerManager.getExhaustion();

        this.savedExperienceLevel = this.player.experienceLevel;
        this.savedTotalExperience = this.player.totalExperience;
        this.savedExperienceProgress = this.player.experienceProgress;
        this.shadowStateSaved = true;
    }

    /**
     * 把当前被附身者的“客户端应该看到的数据”镜像到灵术师本人身上。
     *
     * <p>这样灵术师自己的客户端就能直接复用原版 HUD / 物品栏同步，
     * 而不用为血量、经验和背包再额外重写一整套同步协议。</p>
     */
    public void copyTargetShadowState(@NotNull PlayerEntity target) {
        this.player.getInventory().clone(target.getInventory());
        this.player.getInventory().selectedSlot = target.getInventory().selectedSlot;

        this.player.setHealth(target.getHealth());
        this.player.setAbsorptionAmount(target.getAbsorptionAmount());

        HungerManager playerHunger = this.player.getHungerManager();
        HungerManager targetHunger = target.getHungerManager();
        playerHunger.setFoodLevel(targetHunger.getFoodLevel());
        playerHunger.setSaturationLevel(targetHunger.getSaturationLevel());
        playerHunger.setExhaustion(targetHunger.getExhaustion());

        this.player.experienceLevel = target.experienceLevel;
        this.player.totalExperience = target.totalExperience;
        this.player.experienceProgress = target.experienceProgress;

        this.player.playerScreenHandler.sendContentUpdates();
        this.player.currentScreenHandler.sendContentUpdates();
    }

    /**
     * 把灵术师本地背包界面的改动回写到宿主。
     *
     * <p>只有当灵术师背包相较于“上一份从宿主镜像下来的快照”发生变化时才写回，
     * 这样就不会每 tick 都无脑覆写宿主，也能兼容宿主因使用物品、拾取物品而产生的正常变化。</p>
     */
    public void applyInventoryChangesToTarget(@NotNull PlayerEntity target) {
        if (this.lastMirroredTargetInventoryNbt == null) {
            rememberMirroredTargetInventory(target);
            return;
        }

        NbtList currentSpiritualistInventory = this.player.getInventory().writeNbt(new NbtList());
        boolean selectedSlotChanged = this.player.getInventory().selectedSlot != this.lastMirroredTargetSelectedSlot;
        if (currentSpiritualistInventory.equals(this.lastMirroredTargetInventoryNbt) && !selectedSlotChanged) {
            return;
        }

        target.getInventory().clone(this.player.getInventory());
        target.getInventory().selectedSlot = this.player.getInventory().selectedSlot;

        if (target instanceof net.minecraft.server.network.ServerPlayerEntity serverTarget) {
            serverTarget.playerScreenHandler.sendContentUpdates();
            serverTarget.currentScreenHandler.sendContentUpdates();
        }
    }

    /**
     * 记住“宿主刚刚同步给灵术师时”的背包样貌，供下一次检测 GUI 改动使用。
     */
    public void rememberMirroredTargetInventory(@NotNull PlayerEntity target) {
        this.lastMirroredTargetInventoryNbt = target.getInventory().writeNbt(new NbtList());
        this.lastMirroredTargetSelectedSlot = target.getInventory().selectedSlot;
    }

    /**
     * 结束附身后恢复灵术师原本自己的状态。
     */
    public void restoreSavedShadowState() {
        if (!this.shadowStateSaved || this.savedInventoryNbt == null) {
            clearSavedShadowState();
            return;
        }

        this.player.getInventory().readNbt(this.savedInventoryNbt);
        this.player.getInventory().selectedSlot = this.savedSelectedSlot;
        this.player.setHealth(this.savedHealth);
        this.player.setAbsorptionAmount(this.savedAbsorption);

        HungerManager hungerManager = this.player.getHungerManager();
        hungerManager.setFoodLevel(this.savedFoodLevel);
        hungerManager.setSaturationLevel(this.savedSaturation);
        hungerManager.setExhaustion(this.savedExhaustion);

        this.player.experienceLevel = this.savedExperienceLevel;
        this.player.totalExperience = this.savedTotalExperience;
        this.player.experienceProgress = this.savedExperienceProgress;

        this.player.playerScreenHandler.sendContentUpdates();
        this.player.currentScreenHandler.sendContentUpdates();
        clearSavedShadowState();
    }

    private void clearSavedShadowState() {
        this.savedInventoryNbt = null;
        this.savedSelectedSlot = 0;
        this.savedHealth = 20.0f;
        this.savedAbsorption = 0.0f;
        this.savedFoodLevel = 20;
        this.savedSaturation = 5.0f;
        this.savedExhaustion = 0.0f;
        this.savedExperienceLevel = 0;
        this.savedTotalExperience = 0;
        this.savedExperienceProgress = 0.0f;
        this.shadowStateSaved = false;
    }

    private void clearPossessionControlState() {
        this.possessionForwardInput = 0.0f;
        this.possessionSidewaysInput = 0.0f;
        this.possessionYaw = 0.0f;
        this.possessionPitch = 0.0f;
        this.possessionJumping = false;
        this.possessionSneaking = false;
        this.possessionSprinting = false;
        this.possessionUsing = false;
        this.possessionAttacking = false;
        this.lastPossessionUsing = false;
        this.lastPossessionAttacking = false;
        this.possessionMiningPos = null;
        this.possessionMiningDirection = null;
    }

    private void clearMirroredTargetInventory() {
        this.lastMirroredTargetInventoryNbt = null;
        this.lastMirroredTargetSelectedSlot = 0;
    }

    /**
     * 把附身中的灵术师本体改造成“隐藏空气壳”。
     *
     * <p>这里只处理真正需要广播给服务器和其他客户端的底层物理状态：
     * 1. `invisible` 让所有客户端都不再正常渲染本体；
     * 2. `noClip + noGravity` 让它既不会被实体挤走，也不会自己下坠漂移；
     * 3. 每 tick 继续清零速度，避免残余速度把本体偷偷滑离锚点。</p>
     */
    private void applyDetachedBodyShellState() {
        if (!this.detachedBodyShellApplied) {
            this.savedDetachedBodyInvisible = this.player.isInvisible();
            this.savedDetachedBodyNoGravity = this.player.hasNoGravity();
            this.savedDetachedBodyNoClip = this.player.noClip;
            this.detachedBodyShellApplied = true;
        }

        this.player.setInvisible(true);
        this.player.setNoGravity(true);
        this.player.noClip = true;
        this.player.setVelocity(Vec3d.ZERO);
        this.player.fallDistance = 0.0f;
    }

    /**
     * 结束附身或异常重置后，恢复灵术师本体原本的物理标记。
     */
    private void restoreDetachedBodyShellState() {
        if (!this.detachedBodyShellApplied) {
            return;
        }

        this.player.setInvisible(this.savedDetachedBodyInvisible);
        this.player.setNoGravity(this.savedDetachedBodyNoGravity);
        this.player.noClip = this.savedDetachedBodyNoClip;
        this.player.setVelocity(Vec3d.ZERO);
        this.player.fallDistance = 0.0f;

        this.detachedBodyShellApplied = false;
        this.savedDetachedBodyInvisible = false;
        this.savedDetachedBodyNoGravity = false;
        this.savedDetachedBodyNoClip = false;
    }

    private void rememberCurrentBodyAnchor() {
        this.bodyAnchorX = this.player.getX();
        this.bodyAnchorY = this.player.getY();
        this.bodyAnchorZ = this.player.getZ();
        this.bodyAnchorYaw = this.player.getYaw();
        this.bodyAnchorPitch = this.player.getPitch();
    }

    @Override
    public void serverTick() {
        if (this.lingeringProtectionTicks > 0) {
            this.lingeringProtectionTicks--;
            if (this.lingeringProtectionTicks <= 0 || !this.lingeringProtectionAvailable) {
                clearLingeringProtection();
            }
            if (this.lingeringProtectionTicks % 20 == 0 || this.lingeringProtectionTicks == 0) {
                this.sync();
            }
        }

        if (this.state == SpiritualistState.NORMAL) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (!gameWorld.isRunning()
                || !gameWorld.isRole(this.player, Noellesroles.SPIRITUALIST)
                || !this.player.isAlive()
                || !GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            this.reset();
            return;
        }

        if (this.isProjecting()) {
            if (this.isBodyAnchorMoved()) {
                SpiritualistManager.endProjection((net.minecraft.server.network.ServerPlayerEntity) this.player, true);
            }
            return;
        }

        if (!this.isPossessing()) {
            this.reset();
            return;
        }

        /*
         * 每 tick 都继续维持一次“空气壳”状态。
         *
         * 这样即便外部逻辑临时改写了实体标记，
         * 也不会让灵术师本体在附身期间突然重新显形或开始掉落。
         */
        applyDetachedBodyShellState();

        PlayerEntity possessionTargetEntity = this.player.getWorld().getPlayerByUuid(this.possessionTarget);
        if (possessionTargetEntity == null || !possessionTargetEntity.isAlive()
                || !GameFunctions.isPlayerAliveAndSurvival(possessionTargetEntity)) {
            SpiritualistManager.endPossession((net.minecraft.server.network.ServerPlayerEntity) this.player, true, false, false);
            return;
        }

        applyInventoryChangesToTarget(possessionTargetEntity);
        if (possessionTargetEntity instanceof net.minecraft.server.network.ServerPlayerEntity serverTarget) {
            SpiritualistManager.tickPossessedTarget(
                    (net.minecraft.server.network.ServerPlayerEntity) this.player,
                    serverTarget
            );
        }
        copyTargetShadowState(possessionTargetEntity);
        rememberMirroredTargetInventory(possessionTargetEntity);

        if (this.isBodyAnchorMoved()) {
            SpiritualistManager.endPossession((net.minecraft.server.network.ServerPlayerEntity) this.player, true, false, false);
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putString("state", this.state.serializedName());
        if (this.possessionTarget != null) {
            tag.putUuid("possessionTarget", this.possessionTarget);
        }

        tag.putDouble("bodyAnchorX", this.bodyAnchorX);
        tag.putDouble("bodyAnchorY", this.bodyAnchorY);
        tag.putDouble("bodyAnchorZ", this.bodyAnchorZ);
        tag.putFloat("bodyAnchorYaw", this.bodyAnchorYaw);
        tag.putFloat("bodyAnchorPitch", this.bodyAnchorPitch);

        if (this.lingeringProtectedTarget != null) {
            tag.putUuid("lingeringProtectedTarget", this.lingeringProtectedTarget);
        }
        tag.putInt("lingeringProtectionTicks", this.lingeringProtectionTicks);
        tag.putBoolean("lingeringProtectionAvailable", this.lingeringProtectionAvailable);
        tag.putDouble("lingeringReturnX", this.lingeringReturnX);
        tag.putDouble("lingeringReturnY", this.lingeringReturnY);
        tag.putDouble("lingeringReturnZ", this.lingeringReturnZ);
        tag.putFloat("lingeringReturnYaw", this.lingeringReturnYaw);
        tag.putFloat("lingeringReturnPitch", this.lingeringReturnPitch);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.state = SpiritualistState.fromSerializedName(tag.getString("state"));
        this.possessionTarget = tag.containsUuid("possessionTarget") ? tag.getUuid("possessionTarget") : null;

        this.bodyAnchorX = tag.getDouble("bodyAnchorX");
        this.bodyAnchorY = tag.getDouble("bodyAnchorY");
        this.bodyAnchorZ = tag.getDouble("bodyAnchorZ");
        this.bodyAnchorYaw = tag.getFloat("bodyAnchorYaw");
        this.bodyAnchorPitch = tag.getFloat("bodyAnchorPitch");

        this.lingeringProtectedTarget = tag.containsUuid("lingeringProtectedTarget")
                ? tag.getUuid("lingeringProtectedTarget")
                : null;
        this.lingeringProtectionTicks = tag.getInt("lingeringProtectionTicks");
        this.lingeringProtectionAvailable = tag.getBoolean("lingeringProtectionAvailable");
        this.lingeringReturnX = tag.getDouble("lingeringReturnX");
        this.lingeringReturnY = tag.getDouble("lingeringReturnY");
        this.lingeringReturnZ = tag.getDouble("lingeringReturnZ");
        this.lingeringReturnYaw = tag.getFloat("lingeringReturnYaw");
        this.lingeringReturnPitch = tag.getFloat("lingeringReturnPitch");
    }
}
