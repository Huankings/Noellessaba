package org.agmas.noellesroles.roles.spiritualist;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import dev.doctor4t.wathe.item.DerringerItem;
import dev.doctor4t.wathe.item.KnifeItem;
import dev.doctor4t.wathe.item.RevolverItem;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.AdventureUsable;
import dev.doctor4t.wathe.util.GunDropPayload;
import dev.doctor4t.wathe.util.ShootMuzzleS2CPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.projectile.ProjectileUtil;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.mixin.roles.spiritualist.PlayerEntityPoseInvoker;
import org.agmas.noellesroles.roles.assassin.BayonetKnockbackHandler;
import org.agmas.noellesroles.roles.coward.CowardConstants;
import org.agmas.noellesroles.roles.coward.SedativePlayerComponent;
import org.agmas.noellesroles.roles.executioner.ExecutionerPlayerComponent;
import org.agmas.noellesroles.packet.role.spiritualist.SpiritualistPossessionViewS2CPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * 灵术师服务端统一状态管理器。
 *
 * <p>灵术师涉及到的状态切换比较多：
 * 1. 出窍开始 / 结束；
 * 2. 附身开始 / 结束；
 * 3. 灵魂庇护挡死后的强制解除；
 * 4. 结算、转职、死亡等异常收束时的清理。
 *
 * <p>如果这些逻辑分别散落在能力类、死亡处理器、组件 tick 和 mixin 里，
 * 很容易出现“某条路径漏记回放”“冷却不一致”或“宿主状态没清掉”的问题。
 * 所以这里把真正的状态切换入口集中起来，其他地方只负责在合适的时机调用它。</p>
 */
public final class SpiritualistManager {

    /**
     * 以下常量统一收口“附身代理兼容”的关键数值与外部 ID。
     *
     * <p>后续如果你想继续调附身期间的武器/工具判定范围、概率或软兼容目标，
     * 只需要改这里，不用再去方法体里翻散落的 magic number。</p>
     */
    private static final String KINSWATHE_MOD_ID = "kinswathe";
    private static final Identifier KINSWATHE_BLOWGUN_ID = Identifier.of(KINSWATHE_MOD_ID, "blowgun");
    private static final Identifier KINSWATHE_HUNTING_KNIFE_ID = Identifier.of(KINSWATHE_MOD_ID, "hunting_knife");
    private static final Identifier KINSWATHE_PAN_ID = Identifier.of(KINSWATHE_MOD_ID, "pan");
    private static final Identifier KINSWATHE_ROBOT_ROLE_ID = Identifier.of(KINSWATHE_MOD_ID, "robot");
    private static final Identifier KINSWATHE_PAN_STUN_END_ID = Identifier.of(KINSWATHE_MOD_ID, "pan_stun_end");

    // 刺刀、猎刀、平底锅都沿用原物品的 3 格近战命中距离。
    private static final float POSSESSION_MELEE_TARGET_RANGE = 3.0f;
    // 吹矢沿用 kinswathe 原版 15 格的射程。
    private static final float POSSESSION_BLOWGUN_TARGET_RANGE = 15.0f;
    // 猎刀和平底锅都要求至少蓄力 10 tick 才允许松手生效。
    private static final int POSSESSION_CHARGED_WEAPON_MIN_USE_TICKS = 10;
    // 猎刀客户端只会在“剩余时间 > 5 tick”时发包，这里按等价的已使用时长做服务端代理。
    private static final int POSSESSION_HUNTING_KNIFE_MAX_USE_TICKS_FOR_HIT = 194;
    // 平底锅同理：超过 95 tick 基本等于已经拖到自然结束，不再补发眩晕判定。
    private static final int POSSESSION_PAN_MAX_USE_TICKS_FOR_HIT = 94;
    // 吹矢在“目标已经中毒”时，会额外削减 100~299 tick 的毒素时长。
    private static final int POSSESSION_BLOWGUN_POISON_REDUCTION_MIN = 100;
    private static final int POSSESSION_BLOWGUN_POISON_REDUCTION_RANDOM_BOUND = 200;
    // 平底锅命中后眩晕 100 tick，完全对齐 kinswathe 原逻辑。
    private static final int POSSESSION_PAN_STUN_TICKS = 100;
    // 强盗手枪击杀无辜者后的概率分支：20% 保留，30% 掉左轮，50% 消失。
    private static final int ROBBER_PISTOL_KEEP_CHANCE_PERCENT = 20;
    private static final int ROBBER_PISTOL_DROP_REVOLVER_CHANCE_PERCENT = 50;
    // kinswathe 失败提示文本用的红色值。
    private static final int KINSWATHE_FAILURE_TEXT_COLOR = 0xFF5555;

    private SpiritualistManager() {
    }

    public static boolean isPsychoTarget(@Nullable PlayerEntity target) {
        return target != null && PlayerPsychoComponent.KEY.get(target).getPsychoTicks() > 0;
    }

    public static @Nullable ServerPlayerEntity getCurrentPossessionTarget(@NotNull ServerPlayerEntity spiritualist) {
        SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(spiritualist);
        if (component.possessionTarget == null) {
            return null;
        }

        PlayerEntity target = spiritualist.getWorld().getPlayerByUuid(component.possessionTarget);
        return target instanceof ServerPlayerEntity serverTarget ? serverTarget : null;
    }

    public static void startProjection(@NotNull ServerPlayerEntity spiritualist) {
        SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(spiritualist);
        if (component.hasDetachedBodyState()) {
            return;
        }

        component.startProjection();
        GameRecordManager.recordGlobalEvent(
                spiritualist.getServerWorld(),
                org.agmas.noellesroles.Noellesroles.SPIRITUALIST_PROJECTION_STARTED_EVENT,
                spiritualist,
                null
        );
    }

    public static void endProjection(@NotNull ServerPlayerEntity spiritualist, boolean applyCooldown) {
        SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(spiritualist);
        if (!component.isProjecting()) {
            return;
        }

        component.finishProjection(applyCooldown);
        GameRecordManager.recordGlobalEvent(
                spiritualist.getServerWorld(),
                org.agmas.noellesroles.Noellesroles.SPIRITUALIST_PROJECTION_ENDED_EVENT,
                spiritualist,
                null
        );
    }

    public static void startPossession(@NotNull ServerPlayerEntity spiritualist, @NotNull ServerPlayerEntity target) {
        SpiritualistPlayerComponent spiritualistComponent = SpiritualistPlayerComponent.KEY.get(spiritualist);
        SpiritualistHostComponent hostComponent = SpiritualistHostComponent.KEY.get(target);

        spiritualistComponent.startPossession(target);
        hostComponent.startPossession(
                spiritualist.getUuid(),
                spiritualistComponent.bodyAnchorX,
                spiritualistComponent.bodyAnchorY,
                spiritualistComponent.bodyAnchorZ,
                spiritualistComponent.bodyAnchorYaw,
                spiritualistComponent.bodyAnchorPitch
        );

        sendRoleActionbar(
                spiritualist,
                "message.noellesroles.spiritualist.possessing_target",
                target.getDisplayName()
        );
        sendRoleActionbar(target, "message.noellesroles.spiritualist.possessed");

        net.minecraft.nbt.NbtCompound extra = new net.minecraft.nbt.NbtCompound();
        extra.putUuid("target_player", target.getUuid());
        GameRecordManager.recordGlobalEvent(
                spiritualist.getServerWorld(),
                org.agmas.noellesroles.Noellesroles.SPIRITUALIST_POSSESSION_STARTED_EVENT,
                spiritualist,
                extra
        );
    }

    /**
     * 统一结束附身。
     *
     * @param applyCooldown       是否在结束后进入 90 秒冷却
     * @param createLingering     是否为宿主留下 15 秒一次性余留庇护
     * @param teleportBodyToTarget 是否把灵术师本体显现到宿主当前位置
     */
    public static void endPossession(
            @NotNull ServerPlayerEntity spiritualist,
            boolean applyCooldown,
            boolean createLingering,
            boolean teleportBodyToTarget
    ) {
        SpiritualistPlayerComponent spiritualistComponent = SpiritualistPlayerComponent.KEY.get(spiritualist);
        if (!spiritualistComponent.isPossessing()) {
            spiritualistComponent.restoreSavedShadowState();
            spiritualistComponent.finishPossession(false);
            return;
        }

        ServerPlayerEntity host = getCurrentPossessionTarget(spiritualist);
        if (host != null) {
            SpiritualistHostComponent hostComponent = SpiritualistHostComponent.KEY.get(host);
            if (createLingering) {
                spiritualistComponent.setLingeringProtection(host.getUuid());
                hostComponent.applyLingeringProtection(spiritualist.getUuid());
            }
            hostComponent.stopPossession();
            sendRoleActionbar(host, "message.noellesroles.spiritualist.possession_ended");
        }

        if (teleportBodyToTarget && host != null) {
            spiritualist.refreshPositionAndAngles(host.getX(), host.getY(), host.getZ(), host.getYaw(), host.getPitch());
        }

        spiritualistComponent.finishPossession(applyCooldown);

        if (host != null) {
            net.minecraft.nbt.NbtCompound extra = new net.minecraft.nbt.NbtCompound();
            extra.putUuid("target_player", host.getUuid());
            GameRecordManager.recordGlobalEvent(
                    spiritualist.getServerWorld(),
                    org.agmas.noellesroles.Noellesroles.SPIRITUALIST_POSSESSION_ENDED_EVENT,
                    spiritualist,
                    extra
            );
        }
    }

    /**
     * 清理灵术师当前的一切脱体状态，但不施加额外冷却。
     *
     * <p>用于死亡、结算、强制重置等“外部异常收束”场景。</p>
     */
    public static void cleanupDetachedState(@NotNull ServerPlayerEntity spiritualist) {
        SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(spiritualist);
        if (component.isProjecting()) {
            endProjection(spiritualist, false);
            return;
        }

        if (component.isPossessing()) {
            endPossession(spiritualist, false, false, false);
        }
    }

    /**
     * 宿主被庇护后立即传回“灵术师开始附身时”的本体锚点。
     */
    public static void returnHostToSanctuary(@NotNull ServerPlayerEntity host) {
        SpiritualistHostComponent hostComponent = SpiritualistHostComponent.KEY.get(host);
        host.refreshPositionAndAngles(
                hostComponent.sanctuaryX,
                hostComponent.sanctuaryY,
                hostComponent.sanctuaryZ,
                hostComponent.sanctuaryYaw,
                hostComponent.sanctuaryPitch
        );
    }

    public static void sendRoleActionbar(@NotNull ServerPlayerEntity player, @NotNull String key, Object... args) {
        player.sendMessage(Text.translatable(key, args).withColor(SpiritualistConstants.ROLE_COLOR), true);
    }

    /**
     * 每 tick 驱动一次被附身宿主。
     *
     * <p>这里统一承接灵术师客户端送来的控制输入：
     * 1. 旋转视角；
     * 2. 行走 / 跳跃 / 潜行 / 冲刺；
     * 3. 左键攻击或挖掘；
     * 4. 右键交互与持续使用。</p>
     */
    public static void tickPossessedTarget(
            @NotNull ServerPlayerEntity spiritualist,
            @NotNull ServerPlayerEntity host
    ) {
        SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(spiritualist);
        double previousX = host.getX();
        double previousY = host.getY();
        double previousZ = host.getZ();
        float previousYaw = host.getYaw();
        float previousPitch = host.getPitch();

        /*
         * 先把灵术师发来的输入写回宿主。
         *
         * 注意：1.21.1 里 ServerPlayerEntity#updateInput(...) 只有“玩家正在骑乘载具”时才会真正写入
         * forwardSpeed / sidewaysSpeed / jumping 等字段；
         * 对普通站立玩家调用它，很多关键状态其实根本不会落地。
         *
         * 这正是附身手感发木、冲刺存在感很差的一大原因：
         * 服务端虽然在 travel(...)，但宿主身上的原版运动状态字段仍然像“没在动”。
         *
         * 所以下面保留 updateInput 的同时，再把常用运动字段手动补齐，
         * 让原版更多逻辑都能读到“宿主正在被驱动”的真实状态。
         */
        host.updateInput(
                component.possessionSidewaysInput,
                component.possessionForwardInput,
                component.possessionJumping,
                component.possessionSneaking
        );
        host.sidewaysSpeed = component.possessionSidewaysInput;
        host.forwardSpeed = component.possessionForwardInput;
        host.upwardSpeed = component.possessionJumping ? 1.0f : 0.0f;
        applyPossessionLook(host, component);
        applyPossessionStance(host, component);
        host.setJumping(component.possessionJumping);

        if (component.possessionJumping && host.isOnGround()) {
            host.jump();
        }

        Vec3d movementInput = new Vec3d(component.possessionSidewaysInput, 0.0d, component.possessionForwardInput);
        host.travel(movementInput);

        handlePossessionAttack(host, component);
        handlePossessionUse(host, component);

        /*
         * 旧实现这里会对宿主本人每 tick 调 requestTeleport(...)，
         * 它虽然能把“被别人远程控制”的本地视角硬拉回服务端真值，
         * 但副作用也非常明显：
         * 1. 被附身者自己的画面会像被一帧一帧瞬移；
         * 2. 灵术师若直接把相机挂在宿主远端实体上，也会一起看到卡顿转头。
         *
         * 现在改成两步：
         * 1. 服务端继续正常更新宿主真实位置与朝向；
         * 2. 再把这份“最终真值”用自定义 S2C 包发给灵术师和宿主客户端，
         *    让客户端各自做本地平滑视图层。
         *
         * 这样既保留服务端绝对裁定，又能避免 requestTeleport 的生硬观感。
         */
        syncPossessionView(spiritualist, host);
        host.networkHandler.syncWithPlayerPosition();
    }

    /**
     * 供“右键点击即发”的快速通道立即执行一次交互。
     *
     * <p>它不会替代原本 server tick 里的持续 use 逻辑，
     * 只负责把第一次按下 use 的那一刻提前落地，减轻门、按钮、拉杆、一次性右键物品的迟滞感。</p>
     */
    public static void handleImmediatePossessionUse(
            @NotNull ServerPlayerEntity spiritualist,
            @NotNull ServerPlayerEntity host
    ) {
        SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(spiritualist);
        if (!component.isPossessing() || component.lastPossessionUsing || host.isUsingItem()) {
            return;
        }

        applyPossessionLook(host, component);
        applyPossessionStance(host, component);

        if (tryUseHand(host, Hand.MAIN_HAND) == ActionResult.PASS) {
            tryUseHand(host, Hand.OFF_HAND);
        }

        component.lastPossessionUsing = true;
        syncPossessionView(spiritualist, host);
    }

    /**
     * 把宿主当前服务端真值同步给相关客户端。
     *
     * <p>目前只需要发给两个人：</p>
     * <p>1. 灵术师自己：驱动本地附身相机位置；</p>
     * <p>2. 被附身者本人：驱动本地平滑视图，避免 requestTeleport 式抖动。</p>
     */
    private static void syncPossessionView(
            @NotNull ServerPlayerEntity spiritualist,
            @NotNull ServerPlayerEntity host
    ) {
        SpiritualistPossessionViewS2CPacket payload = new SpiritualistPossessionViewS2CPacket(
                host.getUuid(),
                host.getX(),
                host.getY(),
                host.getZ(),
                host.getVelocity().x,
                host.getVelocity().y,
                host.getVelocity().z,
                host.getYaw(),
                host.getPitch(),
                host.getHeadYaw(),
                host.getBodyYaw(),
                host.getPose().name(),
                host.getEyeHeight(host.getPose()),
                host.isSprinting(),
                host.isSneaking(),
                host.isOnGround()
        );

        ServerPlayNetworking.send(spiritualist, payload);
        if (!host.getUuid().equals(spiritualist.getUuid())) {
            ServerPlayNetworking.send(host, payload);
        }
    }

    private static void handlePossessionAttack(
            @NotNull ServerPlayerEntity host,
            @NotNull SpiritualistPlayerComponent component
    ) {
        HitResult hitResult = getPossessionCrosshairHit(
                host,
                entity -> entity.canHit() && entity != host
        );
        boolean attacking = component.possessionAttacking;

        if (attacking && hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity().canHit()) {
            if (!component.lastPossessionAttacking) {
                if (tryHandleBayonetKnockbackAttack(host, entityHitResult)) {
                    abortMining(host, component);
                    component.lastPossessionAttacking = attacking;
                    return;
                }
                host.attack(entityHitResult.getEntity());
                host.swingHand(Hand.MAIN_HAND);
            }
            abortMining(host, component);
            component.lastPossessionAttacking = attacking;
            return;
        }

        if (attacking && hitResult instanceof BlockHitResult blockHitResult) {
            Direction side = blockHitResult.getSide();
            BlockPos blockPos = blockHitResult.getBlockPos();

            if (component.possessionMiningPos != null
                    && component.possessionMiningDirection != null
                    && (!component.possessionMiningPos.equals(blockPos)
                    || component.possessionMiningDirection != side)) {
                abortMining(host, component);
            }

            component.possessionMiningPos = blockPos.toImmutable();
            component.possessionMiningDirection = side;

            host.interactionManager.processBlockBreakingAction(
                    blockPos,
                    component.lastPossessionAttacking
                            ? PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK
                            : PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                    side,
                    host.getWorld().getHeight(),
                    0
            );

            /*
             * 第一人称左键挥手动画不能再依赖灵术师本地伪造，
             * 因此在真正开始敲击方块的那一刻，也让宿主像原版左键那样抬一次主手。
             * 这样灵术师附身视角和其他旁观客户端都能拿到一致的挥手状态。
             */
            if (!component.lastPossessionAttacking) {
                host.swingHand(Hand.MAIN_HAND);
            }

            if (component.lastPossessionAttacking) {
                host.interactionManager.processBlockBreakingAction(
                        blockPos,
                        PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                        side,
                        host.getWorld().getHeight(),
                        0
                );
            }
            component.lastPossessionAttacking = true;
            return;
        }

        if (!attacking) {
            abortMining(host, component);
        }

        component.lastPossessionAttacking = attacking;
    }

    private static void handlePossessionUse(
            @NotNull ServerPlayerEntity host,
            @NotNull SpiritualistPlayerComponent component
    ) {
        boolean using = component.possessionUsing;

        if (!using && component.lastPossessionUsing) {
            ItemStack releasedStack = host.getActiveItem().copy();
            int usedTicks = host.getItemUseTime();
            host.stopUsingItem();
            tryHandleWatheKnifeRelease(host, releasedStack, usedTicks);
            tryHandleKinsWatheChargedRelease(host, releasedStack, usedTicks);
            component.lastPossessionUsing = false;
            return;
        }

        if (!using || component.lastPossessionUsing || host.isUsingItem()) {
            component.lastPossessionUsing = using;
            return;
        }

        if (tryUseHand(host, Hand.MAIN_HAND) == ActionResult.PASS) {
            tryUseHand(host, Hand.OFF_HAND);
        }

        component.lastPossessionUsing = true;
    }

    /**
     * 把灵术师当前想看的朝向立即写到宿主身上。
     *
     * <p>这份逻辑既给正常的每 tick 驱动复用，
     * 也给“右键立即交互”复用，确保服务端 raycast 总是朝着灵术师刚刚看到的方向执行。</p>
     */
    private static void applyPossessionLook(
            @NotNull ServerPlayerEntity host,
            @NotNull SpiritualistPlayerComponent component
    ) {
        host.setYaw(component.possessionYaw);
        host.setPitch(component.possessionPitch);
        host.setHeadYaw(component.possessionYaw);
        host.setBodyYaw(component.possessionYaw);
        host.prevYaw = component.possessionYaw;
        host.prevPitch = component.possessionPitch;
        host.prevHeadYaw = component.possessionYaw;
        host.prevBodyYaw = component.possessionYaw;
    }

    /**
     * 右键交互也要吃到潜行 / 冲刺这类姿态信息，
     * 否则像“潜行右键方块避免触发物品使用”之类的原版分支就会和灵术师本地看到的不一致。
     */
    private static void applyPossessionStance(
            @NotNull ServerPlayerEntity host,
            @NotNull SpiritualistPlayerComponent component
    ) {
        boolean sneaking = component.possessionSneaking;

        /*
         * 原版里潜行与冲刺互斥。
         * 附身控制改成手工写姿态后，也必须把这层互斥补回来，
         * 否则就会出现“按着 Shift 但仍被冲刺状态顶掉，始终蹲不下去”的情况。
         */
        host.setSneaking(sneaking);
        host.setSprinting(!sneaking && component.possessionSprinting && component.possessionForwardInput > 0.0f);
        applyPossessionPose(host);
    }

    /**
     * 让服务端宿主直接走原版姿态决策。
     *
     * <p>相比我们手写 CROUCHING / STANDING / SWIMMING 的简单判断，
     * 原版这里还会一起处理：
     * 1. 一格高环境下能否保持低姿态；
     * 2. 游泳、滑翔、睡觉等更高优先级姿态；
     * 3. 当前空间不足时的安全回退。
     *
     * <p>这对修复“蹲下/起身过渡很突兀”“右键时蹲起抖动”“跳起来姿态很怪”
     * 这类问题更稳妥。</p>
     */
    private static void applyPossessionPose(@NotNull ServerPlayerEntity host) {
        ((PlayerEntityPoseInvoker) host).noellesroles$invokeUpdatePose();
    }

    private static @NotNull ActionResult tryUseHand(@NotNull ServerPlayerEntity host, @NotNull Hand hand) {
        ItemStack heldStack = host.getStackInHand(hand);
        if (hand == Hand.MAIN_HAND && tryHandleWatheGunUse(host, heldStack)) {
            return ActionResult.CONSUME;
        }

        HitResult hitResult = getPossessionCrosshairHit(
                host,
                entity -> entity != host && !entity.isSpectator()
        );

        if (hitResult instanceof EntityHitResult entityHitResult) {
            if (!host.canInteractWithEntity(entityHitResult.getEntity(), host.getEntityInteractionRange())) {
                return ActionResult.PASS;
            }

            ActionResult interactAtResult = entityHitResult.getEntity().interactAt(host, entityHitResult.getPos(), hand);
            if (interactAtResult != ActionResult.PASS) {
                if (interactAtResult.isAccepted()) {
                    host.swingHand(hand);
                }
                return interactAtResult;
            }

            ActionResult interactResult = entityHitResult.getEntity().interact(host, hand);
            if (interactResult.isAccepted()) {
                host.swingHand(hand);
                return interactResult;
            }

            if (entityHitResult.getEntity() instanceof LivingEntity livingEntity) {
                ActionResult useOnEntityResult = heldStack.useOnEntity(host, livingEntity, hand);
                if (useOnEntityResult.isAccepted()) {
                    host.swingHand(hand);
                    return useOnEntityResult;
                }
                if (useOnEntityResult != ActionResult.PASS) {
                    return useOnEntityResult;
                }
            }

            return tryInteractItemWithPossessionProxy(host, heldStack, hand, hitResult);
        }

        if (hitResult instanceof BlockHitResult blockHitResult) {
            if (!host.canInteractWithBlockAt(blockHitResult.getBlockPos(), host.getBlockInteractionRange())) {
                return ActionResult.PASS;
            }

            ActionResult preItemUseResult = tryUseAdventureItemBeforeBlockInteract(host, hand, blockHitResult);
            if (preItemUseResult != ActionResult.PASS) {
                return preItemUseResult;
            }

            ActionResult blockResult = host.interactionManager.interactBlock(
                    host,
                    host.getWorld(),
                    heldStack,
                    hand,
                    blockHitResult
            );
            if (blockResult.isAccepted()) {
                host.swingHand(hand);
            }
            if (blockResult == ActionResult.FAIL) {
                ActionResult fallbackAdventureUse = tryUseAdventureItemAfterBlockFail(host, hand, blockHitResult);
                if (fallbackAdventureUse != ActionResult.PASS) {
                    return fallbackAdventureUse;
                }
            }
            if (blockResult != ActionResult.PASS) {
                return blockResult;
            }
        }

        return tryInteractItemWithPossessionProxy(host, heldStack, hand, hitResult);
    }

    /**
     * 统一走一次“服务端代宿主使用物品”，并在成功后补发那些原本依赖客户端发包的特殊效果。
     *
     * <p>这一步是本次兼容的关键：
     * 普通物品仍走原版/原模组现成的 use 逻辑，
     * 只有 Bayonet / Blowgun 这类“真正效果在客户端发包后才进入服务端结算”的物品，
     * 才会在这里额外补一次代理。</p>
     */
    private static @NotNull ActionResult tryInteractItemWithPossessionProxy(
            @NotNull ServerPlayerEntity host,
            @NotNull ItemStack heldStack,
            @NotNull Hand hand,
            @Nullable HitResult hitResult
    ) {
        ActionResult itemResult = host.interactionManager.interactItem(
                host,
                host.getWorld(),
                heldStack,
                hand
        );
        if (!itemResult.isAccepted()) {
            return itemResult;
        }

        tryHandlePossessionClientOnlyItemEffects(host, heldStack, hand, hitResult);
        if (!shouldSkipAcceptedItemSwing(heldStack)) {
            host.swingHand(hand);
        }
        return itemResult;
    }

    /**
     * 某些物品自己的 use() 已经主动挥手，如果这里再补一次就会出现双挥手。
     */
    private static boolean shouldSkipAcceptedItemSwing(@NotNull ItemStack heldStack) {
        return heldStack.isOf(ModItems.BAYONET)
                || heldStack.isOf(WatheItems.REVOLVER)
                || heldStack.isOf(WatheItems.DERRINGER)
                || heldStack.isOf(ModItems.SILENCED_REVOLVER)
                || heldStack.isOf(ModItems.ROBBER_PISTOL);
    }

    /**
     * 给“原本必须由客户端发包才能生效”的物品补服务端代理。
     */
    private static void tryHandlePossessionClientOnlyItemEffects(
            @NotNull ServerPlayerEntity host,
            @NotNull ItemStack heldStack,
            @NotNull Hand hand,
            @Nullable HitResult hitResult
    ) {
        if (hand != Hand.MAIN_HAND) {
            return;
        }

        if (tryHandleBayonetStabAfterUse(host, heldStack, hitResult)) {
            return;
        }

        tryHandleKinsWatheBlowgunAfterUse(host, heldStack);
    }

    /**
     * Wathe 里有一批“冒险模式也能对方块生效”的工具，
     * 它们往往依赖 item.useOnBlock 先于普通方块交互运行。
     *
     * <p>附身控制走的是自定义服务端代操作链路，
     * 这里显式给 AdventureUsable 工具补一次前置 useOnBlock，
     * 用来恢复开锁器、撬棍等工具的原本行为。</p>
     */
    private static @NotNull ActionResult tryUseAdventureItemBeforeBlockInteract(
            @NotNull ServerPlayerEntity host,
            @NotNull Hand hand,
            @NotNull BlockHitResult blockHitResult
    ) {
        ItemStack heldStack = host.getStackInHand(hand);
        if (!(heldStack.getItem() instanceof AdventureUsable)) {
            return ActionResult.PASS;
        }

        /*
         * Lockpick / Crowbar 不能再一律抢在方块交互前执行：
         * 1. 开锁器只有潜行右键才应该 jam；
         * 2. 撬棍只有潜行右键时，才应该无视“本来能正常开门”的逻辑直接撬开。
         *
         * 非潜行时先让门方块自己处理，
         * 这样未上锁的门仍会正常开/关，上锁门也能保留原本的“锁住/需钥匙”判定分支。
         */
        if ((heldStack.isOf(WatheItems.LOCKPICK) || heldStack.isOf(WatheItems.CROWBAR)) && !host.isSneaking()) {
            return ActionResult.PASS;
        }

        return heldStack.useOnBlock(new ItemUsageContext(host, hand, blockHitResult));
    }

    /**
     * 某些门工具的正确行为是“先让门方块自己尝试交互，再根据失败结果决定是否由工具接管”。
     *
     * <p>Wathe 里 crowbar 就是典型例子：
     * 未上锁的门本来就能正常打开，只有门自己拒绝开启时，撬棍才应该顶上去执行爆破。</p>
     */
    private static @NotNull ActionResult tryUseAdventureItemAfterBlockFail(
            @NotNull ServerPlayerEntity host,
            @NotNull Hand hand,
            @NotNull BlockHitResult blockHitResult
    ) {
        ItemStack heldStack = host.getStackInHand(hand);
        if (!heldStack.isOf(WatheItems.CROWBAR)) {
            return ActionResult.PASS;
        }
        return heldStack.useOnBlock(new ItemUsageContext(host, hand, blockHitResult));
    }

    /**
     * Wathe 左轮 / 德林杰的真正开火逻辑并不在服务端 item.use() 里，
     * 而是客户端发送 GunShootPayload 后才进入结算。
     *
     * <p>灵术师附身时没有“宿主自己本地右键”这一步，
     * 因此需要在服务端把这段 client-only 逻辑补代理执行出来。</p>
     */
    private static boolean tryHandleWatheGunUse(@NotNull ServerPlayerEntity host, @NotNull ItemStack heldStack) {
        boolean watheRevolver = heldStack.isOf(WatheItems.REVOLVER);
        boolean watheDerringer = heldStack.isOf(WatheItems.DERRINGER);
        boolean silencedRevolver = heldStack.isOf(ModItems.SILENCED_REVOLVER);
        boolean robberPistol = heldStack.isOf(ModItems.ROBBER_PISTOL);

        if (!watheRevolver && !watheDerringer && !silencedRevolver && !robberPistol) {
            return false;
        }

        Item gunItem = heldStack.getItem();
        if (host.getItemCooldownManager().isCoolingDown(gunItem)) {
            return true;
        }

        if (!silencedRevolver) {
            host.getWorld().playSound(
                    null,
                    host.getX(),
                    host.getEyeY(),
                    host.getZ(),
                    WatheSounds.ITEM_REVOLVER_CLICK,
                    SoundCategory.PLAYERS,
                    0.5f,
                    1.0f + host.getRandom().nextFloat() * 0.1f - 0.05f
            );
        }

        boolean used = heldStack.getOrDefault(WatheDataComponentTypes.USED, false);
        if (watheDerringer) {
            if (used) {
                return true;
            }
            if (!host.isCreative()) {
                heldStack.set(WatheDataComponentTypes.USED, true);
            }
        }

        HitResult collision = watheDerringer
                ? DerringerItem.getGunTarget(host)
                : RevolverItem.getGunTarget(host);

        if (collision instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity target
                && target.distanceTo(host) < 65.0f) {
            GameWorldComponent game = GameWorldComponent.KEY.get(host.getWorld());
            boolean innocentTarget = isRevolverTargetConsideredInnocent(game, target);
            boolean backfire = false;

            if (target instanceof ServerPlayerEntity serverTarget) {
                GameRecordManager.recordItemHit(
                        host,
                        heldStack,
                        GameConstants.DeathReasons.GUN,
                        serverTarget,
                        null
                );
            }

            if (watheRevolver && innocentTarget && !host.isCreative()) {
                if (game.isInnocent(host) && host.getRandom().nextFloat() <= game.getBackfireChance()) {
                    backfire = true;
                    GameFunctions.killPlayer(host, true, host, GameConstants.DeathReasons.GUN);
                } else {
                    dropRevolverAfterInnocentShot(host);
                }
            }

            if (!backfire) {
                GameFunctions.killPlayer(target, true, host, GameConstants.DeathReasons.GUN);
                if (silencedRevolver && !GameFunctions.isPlayerAliveAndSurvival(target) && !host.isCreative()) {
                    dropSilencedRevolverAfterInnocentKill(host, target);
                } else if (robberPistol && !GameFunctions.isPlayerAliveAndSurvival(target) && !host.isCreative()) {
                    handleRobberPistolPostKillOutcome(host, target);
                }
            }
        }

        if (!silencedRevolver) {
            host.getWorld().playSound(
                    null,
                    host.getX(),
                    host.getEyeY(),
                    host.getZ(),
                    WatheSounds.ITEM_REVOLVER_SHOOT,
                    SoundCategory.PLAYERS,
                    5.0f,
                    1.0f + host.getRandom().nextFloat() * 0.1f - 0.05f
            );
        }

        notifyGunMuzzle(host);

        if (!host.isCreative()) {
            int cooldown = watheRevolver
                    ? getAdjustedRevolverCooldown(host)
                    : GameConstants.ITEM_COOLDOWNS.getOrDefault(gunItem, 0);
            host.getItemCooldownManager().set(gunItem, cooldown);
        }

        return true;
    }

    /**
     * Wathe 匕首的真正刺杀判定在客户端 onStoppedUsing 里发 KnifeStabPayload 触发，
     * 服务端原生 stopUsingItem() 并不会完成那一步。
     *
     * <p>因此附身模式在右键蓄满后松手时，需要额外补一次服务器代理刺杀。</p>
     */
    private static void tryHandleWatheKnifeRelease(
            @NotNull ServerPlayerEntity host,
            @NotNull ItemStack releasedStack,
            int usedTicks
    ) {
        if (!releasedStack.isOf(WatheItems.KNIFE) || usedTicks < 10 || host.isSpectator()) {
            return;
        }

        HitResult collision = KnifeItem.getKnifeTarget(host);
        if (!(collision instanceof EntityHitResult entityHitResult) || !(entityHitResult.getEntity() instanceof PlayerEntity target)) {
            return;
        }

        if (target.distanceTo(host) > 3.0f) {
            return;
        }

        if (target instanceof ServerPlayerEntity serverTarget) {
            GameRecordManager.recordItemHit(
                    host,
                    releasedStack,
                    GameConstants.DeathReasons.KNIFE,
                    serverTarget,
                    null
            );
        }

        GameFunctions.killPlayer(target, true, host, GameConstants.DeathReasons.KNIFE);
        target.playSound(WatheSounds.ITEM_KNIFE_STAB, 1.0f, 1.0f);
        host.swingHand(Hand.MAIN_HAND);

        if (!host.isCreative() && GameWorldComponent.KEY.get(host.getWorld()).getGameMode() != WatheGameModes.LOOSE_ENDS) {
            host.getItemCooldownManager().set(WatheItems.KNIFE, GameConstants.ITEM_COOLDOWNS.get(WatheItems.KNIFE));
        }
    }

    /**
     * 附身时右键刺刀并不会真的走到客户端发 BayonetStabC2SPacket，
     * 因此这里在服务端把那段即时刺杀逻辑补出来。
     */
    private static boolean tryHandleBayonetStabAfterUse(
            @NotNull ServerPlayerEntity host,
            @NotNull ItemStack heldStack,
            @Nullable HitResult hitResult
    ) {
        if (!heldStack.isOf(ModItems.BAYONET) || !(hitResult instanceof EntityHitResult entityHitResult)) {
            return false;
        }
        if (!(entityHitResult.getEntity() instanceof PlayerEntity target) || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            return false;
        }
        if (host.getItemCooldownManager().isCoolingDown(ModItems.BAYONET) || target.distanceTo(host) > POSSESSION_MELEE_TARGET_RANGE) {
            return false;
        }

        if (target instanceof ServerPlayerEntity serverTarget) {
            GameRecordManager.recordItemHit(
                    host,
                    heldStack,
                    GameConstants.DeathReasons.KNIFE,
                    serverTarget,
                    null
            );
        }

        GameFunctions.killPlayer(target, true, host, GameConstants.DeathReasons.KNIFE);
        if (!host.isCreative()) {
            host.getItemCooldownManager().set(
                    ModItems.BAYONET,
                    GameConstants.ITEM_COOLDOWNS.getOrDefault(ModItems.BAYONET, 0)
            );
        }
        return true;
    }

    /**
     * 附身左键刺刀时，优先走刺刀自己的“纯击退”判定，避免退回普通拳击/武器伤害。
     */
    private static boolean tryHandleBayonetKnockbackAttack(
            @NotNull ServerPlayerEntity host,
            @NotNull EntityHitResult entityHitResult
    ) {
        if (!(entityHitResult.getEntity() instanceof PlayerEntity target)) {
            return false;
        }
        if (!BayonetKnockbackHandler.canKnockback(host, target) || target.distanceTo(host) > POSSESSION_MELEE_TARGET_RANGE) {
            return false;
        }

        BayonetKnockbackHandler.applyKnockback(host, target);
        host.swingHand(Hand.MAIN_HAND, true);
        return true;
    }

    /**
     * 软兼容 kinswathe 的吹矢。
     *
     * <p>吹矢本体的 use() 会在服务端设置冷却和播放吹气声，
     * 真正的中毒结算却依赖客户端发 BlowgunC2SPacket。
     * 附身时没有宿主自己的客户端，所以这里补一次完全等价的服务器结算。</p>
     */
    private static void tryHandleKinsWatheBlowgunAfterUse(
            @NotNull ServerPlayerEntity host,
            @NotNull ItemStack heldStack
    ) {
        if (!isKinsWatheLoaded() || !isItemId(heldStack, KINSWATHE_BLOWGUN_ID)) {
            return;
        }

        HitResult collision = ProjectileUtil.getCollision(
                host,
                entity -> entity instanceof PlayerEntity player && GameFunctions.isPlayerAliveAndSurvival(player),
                POSSESSION_BLOWGUN_TARGET_RANGE
        );
        if (!(collision instanceof EntityHitResult entityHitResult) || !(entityHitResult.getEntity() instanceof PlayerEntity target)) {
            return;
        }
        if (target.distanceTo(host) > POSSESSION_BLOWGUN_TARGET_RANGE) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(host.getWorld());
        ItemStack replayStack = heldStack.copy();
        replayStack.setCount(1);

        if (isRoleId(gameWorld, target, KINSWATHE_ROBOT_ROLE_ID)) {
            if (target instanceof ServerPlayerEntity serverTarget) {
                NbtCompound extra = new NbtCompound();
                extra.putBoolean("robot_failed", true);
                GameRecordManager.recordItemHit(host, replayStack, serverTarget, extra);
            }
            host.sendMessage(
                    Text.translatable("tip.kinswathe.drugmaker.poison_failed").withColor(KINSWATHE_FAILURE_TEXT_COLOR),
                    true
            );
            return;
        }

        if (target instanceof ServerPlayerEntity serverTarget) {
            GameRecordManager.recordItemHit(host, replayStack, serverTarget, null);
        }

        PlayerPoisonComponent targetPoison = PlayerPoisonComponent.KEY.get(target);
        NbtCompound poisonData = GameFunctions.createReplayItemData(host.getServerWorld(), replayStack);
        if (targetPoison.poisonTicks > 0) {
            int reduction = POSSESSION_BLOWGUN_POISON_REDUCTION_MIN
                    + host.getRandom().nextInt(POSSESSION_BLOWGUN_POISON_REDUCTION_RANDOM_BOUND);
            int poisonTicks = Math.max(0, targetPoison.poisonTicks - reduction);
            targetPoison.setDetailedPoisonTicks(poisonTicks, host.getUuid(), GameConstants.DeathReasons.POISON, poisonData);
            return;
        }

        int poisonTicks = PlayerPoisonComponent.clampTime.getLeft()
                + host.getRandom().nextInt(PlayerPoisonComponent.clampTime.getRight() - PlayerPoisonComponent.clampTime.getLeft());
        targetPoison.setDetailedPoisonTicks(poisonTicks, host.getUuid(), GameConstants.DeathReasons.POISON, poisonData);
    }

    /**
     * 软兼容 kinswathe 那类“起手在 item.use，真正效果在 onStoppedUsing 客户端发包”的蓄力武器。
     */
    private static void tryHandleKinsWatheChargedRelease(
            @NotNull ServerPlayerEntity host,
            @NotNull ItemStack releasedStack,
            int usedTicks
    ) {
        if (!isKinsWatheLoaded() || host.isSpectator()) {
            return;
        }

        if (isItemId(releasedStack, KINSWATHE_HUNTING_KNIFE_ID)) {
            tryHandleKinsWatheHuntingKnifeRelease(host, releasedStack, usedTicks);
            return;
        }

        if (isItemId(releasedStack, KINSWATHE_PAN_ID)) {
            tryHandleKinsWathePanRelease(host, releasedStack, usedTicks);
        }
    }

    private static void tryHandleKinsWatheHuntingKnifeRelease(
            @NotNull ServerPlayerEntity host,
            @NotNull ItemStack releasedStack,
            int usedTicks
    ) {
        if (usedTicks < POSSESSION_CHARGED_WEAPON_MIN_USE_TICKS
                || usedTicks > POSSESSION_HUNTING_KNIFE_MAX_USE_TICKS_FOR_HIT) {
            return;
        }

        HitResult collision = ProjectileUtil.getCollision(
                host,
                entity -> entity instanceof PlayerEntity player && GameFunctions.isPlayerAliveAndSurvival(player),
                POSSESSION_MELEE_TARGET_RANGE
        );
        if (!(collision instanceof EntityHitResult entityHitResult) || !(entityHitResult.getEntity() instanceof PlayerEntity target)) {
            return;
        }
        if (target.distanceTo(host) > POSSESSION_MELEE_TARGET_RANGE) {
            return;
        }

        if (target instanceof ServerPlayerEntity serverTarget) {
            GameRecordManager.recordItemHit(
                    host,
                    releasedStack,
                    GameConstants.DeathReasons.KNIFE,
                    serverTarget,
                    null
            );
        }

        resetKinsHunterComponent(host);
        applyExternalItemAfterUsing(host, releasedStack.getItem(), null);
        GameFunctions.killPlayer(target, true, host, GameConstants.DeathReasons.KNIFE);
        target.playSound(WatheSounds.ITEM_KNIFE_STAB, 1.0f, 1.0f);
        host.swingHand(Hand.MAIN_HAND);
    }

    private static void tryHandleKinsWathePanRelease(
            @NotNull ServerPlayerEntity host,
            @NotNull ItemStack releasedStack,
            int usedTicks
    ) {
        if (usedTicks < POSSESSION_CHARGED_WEAPON_MIN_USE_TICKS
                || usedTicks > POSSESSION_PAN_MAX_USE_TICKS_FOR_HIT) {
            return;
        }

        HitResult collision = ProjectileUtil.getCollision(
                host,
                entity -> entity instanceof PlayerEntity player && GameFunctions.isPlayerAliveAndSurvival(player),
                POSSESSION_MELEE_TARGET_RANGE
        );
        if (!(collision instanceof EntityHitResult entityHitResult) || !(entityHitResult.getEntity() instanceof PlayerEntity target)) {
            return;
        }
        if (target.distanceTo(host) > POSSESSION_MELEE_TARGET_RANGE) {
            return;
        }

        setKinsPlayerStunTicks(target, POSSESSION_PAN_STUN_TICKS, KINSWATHE_PAN_STUN_END_ID);
        if (target instanceof ServerPlayerEntity serverTarget) {
            GameRecordManager.recordItemHit(host, releasedStack, serverTarget, null);
        }
        applyExternalItemAfterUsing(host, releasedStack.getItem(), null);
        target.playSound(SoundEvents.BLOCK_ANVIL_LAND, 0.8f, 0.8f);
        host.swingHand(Hand.MAIN_HAND);
    }

    private static void notifyGunMuzzle(@NotNull ServerPlayerEntity host) {
        ShootMuzzleS2CPayload payload = new ShootMuzzleS2CPayload(host.getUuidAsString());
        for (ServerPlayerEntity tracking : PlayerLookup.tracking(host)) {
            ServerPlayNetworking.send(tracking, payload);
        }
        ServerPlayNetworking.send(host, payload);

        SpiritualistHostComponent hostComponent = SpiritualistHostComponent.KEY.get(host);
        if (hostComponent.spiritualistController == null) {
            return;
        }

        PlayerEntity controller = host.getWorld().getPlayerByUuid(hostComponent.spiritualistController);
        if (controller instanceof ServerPlayerEntity serverController && !serverController.getUuid().equals(host.getUuid())) {
            ServerPlayNetworking.send(serverController, payload);
        }
    }

    private static void dropSilencedRevolverAfterInnocentKill(
            @NotNull ServerPlayerEntity host,
            @NotNull PlayerEntity target
    ) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(target.getWorld());
        if (!gameWorld.isInnocent(target)) {
            return;
        }

        host.getInventory().remove(stack -> stack.isOf(ModItems.SILENCED_REVOLVER), 1, host.getInventory());
        host.playerScreenHandler.sendContentUpdates();

        ItemEntity droppedGun = host.dropItem(ModItems.SILENCED_REVOLVER.getDefaultStack(), false, false);
        if (droppedGun != null) {
            droppedGun.setPickupDelay(10);
            droppedGun.setThrower(host);
        }
        ServerPlayNetworking.send(host, new GunDropPayload());
    }

    private static void handleRobberPistolPostKillOutcome(
            @NotNull ServerPlayerEntity host,
            @NotNull PlayerEntity target
    ) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(target.getWorld());
        if (!gameWorld.isInnocent(target)) {
            return;
        }

        int roll = host.getRandom().nextInt(100);
        if (roll < ROBBER_PISTOL_KEEP_CHANCE_PERCENT) {
            return;
        }

        removeOneRobberPistol(host);
        if (roll < ROBBER_PISTOL_DROP_REVOLVER_CHANCE_PERCENT) {
            ItemEntity droppedGun = host.dropItem(WatheItems.REVOLVER.getDefaultStack(), false, false);
            if (droppedGun != null) {
                droppedGun.setPickupDelay(10);
                droppedGun.setThrower(host);
            }
        }
    }

    private static void removeOneRobberPistol(@NotNull ServerPlayerEntity host) {
        host.getInventory().remove(stack -> stack.isOf(ModItems.ROBBER_PISTOL), 1, host.getInventory());
        host.playerScreenHandler.sendContentUpdates();
    }

    private static void dropRevolverAfterInnocentShot(@NotNull ServerPlayerEntity host) {
        host.getInventory().remove(stack -> stack.isOf(WatheItems.REVOLVER), 1, host.getInventory());
        ItemEntity droppedGun = host.dropItem(WatheItems.REVOLVER.getDefaultStack(), false, false);
        if (droppedGun != null) {
            droppedGun.setPickupDelay(10);
            droppedGun.setThrower(host);
        }
        PlayerMoodComponent.KEY.get(host).setMood(0);
        host.playerScreenHandler.sendContentUpdates();
    }

    private static int getAdjustedRevolverCooldown(@NotNull ServerPlayerEntity host) {
        int cooldown = GameConstants.getRevolverCooldown(host);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(host.getWorld());
        boolean coward = gameWorld.isRole(host, Noellesroles.COWARD);
        boolean sedative = SedativePlayerComponent.KEY.get(host).isActive();
        if (!coward && !sedative) {
            return cooldown;
        }

        float factor = 1.0f;
        if (coward) {
            factor *= CowardConstants.REVOLVER_COOLDOWN_FACTOR;
        }
        if (sedative) {
            factor *= CowardConstants.SEDATIVE_REVOLVER_COOLDOWN_FACTOR;
        }
        return Math.max(1, Math.round(cooldown * factor));
    }

    private static boolean isRevolverTargetConsideredInnocent(
            @NotNull GameWorldComponent gameWorld,
            @NotNull PlayerEntity target
    ) {
        for (UUID uuid : gameWorld.getAllWithRole(Noellesroles.EXECUTIONER)) {
            PlayerEntity executioner = target.getWorld().getPlayerByUuid(uuid);
            if (executioner == null) {
                continue;
            }

            ExecutionerPlayerComponent executionerComponent = ExecutionerPlayerComponent.KEY.get(executioner);
            if (executionerComponent.target != null && executionerComponent.target.equals(target.getUuid())) {
                return false;
            }
        }

        if (gameWorld.isRole(target, Noellesroles.VOODOO) && NoellesRolesConfig.HANDLER.instance().voodooShotLikeEvil) {
            return false;
        }

        return gameWorld.isInnocent(target);
    }

    /**
     * 复刻 kinswathe 的 KinsWatheItems#setItemAfterUsing，但只依赖 Wathe 本体与运行时物品实例。
     *
     * <p>这样我们就能在“kinswathe 已加载”时复用它的冷却语义，
     * 同时不需要给 noellesroles 添加任何编译时硬依赖。</p>
     */
    private static void applyExternalItemAfterUsing(
            @NotNull PlayerEntity player,
            @NotNull Item item,
            @Nullable Hand hand
    ) {
        Integer cooldown = GameConstants.ITEM_COOLDOWNS.get(item);
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }
        if (cooldown != null) {
            player.getItemCooldownManager().set(item, cooldown);
        }
        if (hand != null) {
            player.getStackInHand(hand).decrement(1);
        }
    }

    private static boolean isKinsWatheLoaded() {
        return FabricLoader.getInstance().isModLoaded(KINSWATHE_MOD_ID);
    }

    private static boolean isItemId(@NotNull ItemStack stack, @NotNull Identifier itemId) {
        return Registries.ITEM.getId(stack.getItem()).equals(itemId);
    }

    private static boolean isRoleId(
            @NotNull GameWorldComponent gameWorld,
            @NotNull PlayerEntity player,
            @NotNull Identifier roleId
    ) {
        Role role = gameWorld.getRole(player);
        return role != null && role.identifier().equals(roleId);
    }

    /**
     * 通过反射软调用 kinswathe 的玩家效果组件，给目标补上平底锅眩晕。
     */
    private static void setKinsPlayerStunTicks(
            @NotNull PlayerEntity target,
            int ticks,
            @NotNull Identifier source
    ) {
        try {
            Class<?> componentClass = Class.forName("org.BsXinQin.kinswathe.component.PlayerEffectComponent");
            ComponentKey<?> key = (ComponentKey<?>) componentClass.getField("KEY").get(null);
            Object component = key.get(target);
            componentClass.getMethod("setStunTicks", int.class, Identifier.class).invoke(component, ticks, source);
        } catch (ReflectiveOperationException ignored) {
            // kinswathe 未载入、类名变化或运行时签名不匹配时，直接忽略兼容效果，避免主模组崩溃。
        }
    }

    /**
     * 通过反射把猎刀组件 reset 到和原包接收器一致的状态。
     */
    private static void resetKinsHunterComponent(@NotNull PlayerEntity player) {
        try {
            Class<?> componentClass = Class.forName("org.BsXinQin.kinswathe.roles.hunter.HunterComponent");
            ComponentKey<?> key = (ComponentKey<?>) componentClass.getField("KEY").get(null);
            Object component = key.get(player);
            componentClass.getMethod("reset").invoke(component);
        } catch (ReflectiveOperationException ignored) {
            // 兼容只作为附加优化，反射失败时保留前面 stopUsingItem 已经做掉的基础收束即可。
        }
    }

    /**
     * 用“实体判定 + 方块判定”组合出更接近原版准心的命中结果。
     *
     * <p>之前这里只调用了 Entity#raycast(...)，
     * 它实际只会检测方块，不会稳定给出实体命中结果。
     * 这正是附身状态下对玩家左键没反应、实体右键也容易失效的根源之一。</p>
     */
    private static @NotNull HitResult getPossessionCrosshairHit(
            @NotNull ServerPlayerEntity host,
            @NotNull Predicate<Entity> entityPredicate
    ) {
        HitResult blockHit = host.raycast(host.getBlockInteractionRange(), 1.0f, false);
        HitResult entityHit = ProjectileUtil.getCollision(
                host,
                entity -> entityPredicate.test(entity),
                (float) Math.max(host.getEntityInteractionRange(), host.getBlockInteractionRange())
        );

        if (!(entityHit instanceof EntityHitResult entityHitResult)) {
            return blockHit;
        }

        if (blockHit.getType() == HitResult.Type.MISS) {
            return entityHitResult;
        }

        Vec3d eyePos = host.getEyePos();
        double entityDistanceSquared = eyePos.squaredDistanceTo(entityHitResult.getPos());
        double blockDistanceSquared = eyePos.squaredDistanceTo(blockHit.getPos());
        return entityDistanceSquared <= blockDistanceSquared ? entityHitResult : blockHit;
    }

    private static void abortMining(
            @NotNull ServerPlayerEntity host,
            @NotNull SpiritualistPlayerComponent component
    ) {
        if (component.possessionMiningPos == null || component.possessionMiningDirection == null) {
            component.possessionMiningPos = null;
            component.possessionMiningDirection = null;
            return;
        }

        host.interactionManager.processBlockBreakingAction(
                component.possessionMiningPos,
                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK,
                component.possessionMiningDirection,
                host.getWorld().getHeight(),
                0
        );
        component.possessionMiningPos = null;
        component.possessionMiningDirection = null;
    }
}
