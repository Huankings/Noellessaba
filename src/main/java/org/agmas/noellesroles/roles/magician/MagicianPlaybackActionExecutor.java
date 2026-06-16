package org.agmas.noellesroles.roles.magician;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerGrenadeComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
import org.agmas.noellesroles.item.BayonetItem;
import org.agmas.noellesroles.roles.assassin.BayonetKnockbackHandler;
import org.agmas.noellesroles.roles.rememberer.RemembererConstants;
import org.agmas.noellesroles.roles.rememberer.RemembererSniperManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 魔术师播放期间的“服务端代理操作执行器”。
 *
 * <p>可见皮套实体只负责外观与轨迹，而真正的服务端交互仍然需要一个
 * {@link MagicianPlaybackFakePlayer} 去走原版 / Wathe / 扩展模组原有逻辑。
 *
 * <p>这里的设计目标是：</p>
 * <p>1. 尽量复用已有服务端逻辑，不再额外造一套“第二玩法”；</p>
 * <p>2. 关键的击杀、奖励、回放归属统一算到魔术师本体；</p>
 * <p>3. 需要客户端发包才会生效的物品，在这里补出等价的服务端代理。</p>
 */
public final class MagicianPlaybackActionExecutor {

    private static final float MELEE_TARGET_RANGE = 3.0F;
    private static final float REVOLVER_TARGET_RANGE = 20.0F;
    private static final float DERRINGER_TARGET_RANGE = 7.0F;
    private static final double KNIFE_TARGET_BOX_EXPAND = 0.22D;
    private static final double KNIFE_FALLBACK_MIN_FORWARD = 0.25D;
    private static final double KNIFE_FALLBACK_MAX_SIDEWAYS = 0.65D;
    private static final double GUN_TARGET_BOX_EXPAND = 0.08D;
    private static final double GUN_CLOSE_FALLBACK_RANGE = 2.25D;
    private static final double GUN_CLOSE_FALLBACK_MIN_FORWARD = 0.05D;
    private static final double GUN_CLOSE_FALLBACK_BOX_EXPAND = 0.22D;

    private MagicianPlaybackActionExecutor() {
    }

    public static void performAction(
            @NotNull ServerPlayerEntity magician,
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull MagicianPlaybackEntity visibleEntity,
            @NotNull MagicianRecordedAction action,
            @NotNull String disguiseName
    ) {
        try (MagicianReplayActorContext.Scope ignored = MagicianReplayActorContext.push(
                magician.getUuid(),
                proxy.getReplayActorUuid(),
                proxy.getReplayActorName()
        )) {
            switch (action.type) {
                case ATTACK -> performAttack(magician, proxy, visibleEntity);
                case USE_MAIN_HAND -> performUse(magician, proxy, visibleEntity, Hand.MAIN_HAND, action.createRecordedBlockHitResult());
                case USE_OFF_HAND -> performUse(magician, proxy, visibleEntity, Hand.OFF_HAND, action.createRecordedBlockHitResult());
                case RELEASE_USE_ITEM -> performRelease(magician, proxy, visibleEntity);
                case SWING_MAIN_HAND -> performVisualSwing(proxy, visibleEntity, Hand.MAIN_HAND);
                case SWING_OFF_HAND -> performVisualSwing(proxy, visibleEntity, Hand.OFF_HAND);
                case SELECT_SLOT -> proxy.getInventory().selectedSlot = Math.max(0, Math.min(action.intValue, 8));
                case GUN_SHOOT -> performGunShoot(magician, proxy, visibleEntity);
                case KNIFE_STAB -> performKnifeStab(magician, proxy, visibleEntity);
                case BAYONET_STAB -> performBayonetStab(magician, proxy, visibleEntity);
                case BAYONET_KNOCKBACK -> performBayonetKnockback(proxy, visibleEntity);
                case SNIPER_SHOOT -> performSniperShoot(magician, proxy, new Vec3d(action.x, action.y, action.z));
            }
        }
    }

    /**
     * 播放期某些组件也需要临时同步到代理玩家。
     *
     * <p>目前最关键的是 Wathe 的手雷投掷模式组件：
     * 如果不把魔术师本体当前模式同步给代理，回放里的手雷可能会用错“直投 / 蓄力”模式。</p>
     */
    public static void syncTransientState(@NotNull ServerPlayerEntity magician, @NotNull MagicianPlaybackFakePlayer proxy) {
        PlayerGrenadeComponent.KEY.get(proxy).setThrowModeLocal(PlayerGrenadeComponent.KEY.get(magician).getThrowMode());
    }

    private static void performAttack(
            @NotNull ServerPlayerEntity magician,
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull MagicianPlaybackEntity visibleEntity
    ) {
        EntityHitResult entityHitResult = getAttackEntityTarget(proxy, MELEE_TARGET_RANGE);
        if (entityHitResult == null) {
            return;
        }
        Entity entity = entityHitResult.getEntity();
        if (!(entity instanceof PlayerEntity target) || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            return;
        }

        if (proxy.getMainHandStack().isOf(WatheItems.BAT)) {
            if (target instanceof ServerPlayerEntity serverTarget) {
                GameRecordManager.recordItemHit(
                        proxy,
                        proxy.getMainHandStack(),
                        GameConstants.DeathReasons.BAT,
                        serverTarget,
                        null
                );
            }
            GameFunctions.killPlayer(target, true, magician, GameConstants.DeathReasons.BAT, replayActorDeathData(proxy.getMainHandStack(), magician, proxy));
            proxy.getWorld().playSound(
                    null,
                    target.getX(),
                    target.getEyeY(),
                    target.getZ(),
                    WatheSounds.ITEM_BAT_HIT,
                    SoundCategory.PLAYERS,
                    3.0F,
                    1.0F
            );
            swingReplayHand(proxy, visibleEntity, Hand.MAIN_HAND);
            return;
        }

        if (proxy.getMainHandStack().isOf(ModItems.BAYONET)) {
            performBayonetKnockback(proxy, visibleEntity);
            return;
        }

        proxy.attack(target);
        swingReplayHand(proxy, visibleEntity, Hand.MAIN_HAND);
    }

    private static void performUse(
            @NotNull ServerPlayerEntity magician,
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull MagicianPlaybackEntity visibleEntity,
            @NotNull Hand hand,
            @Nullable BlockHitResult recordedBlockHitResult
    ) {
        ItemStack heldStack = proxy.getStackInHand(hand);

        if (recordedBlockHitResult != null && tryInteractRecordedBlock(magician, proxy, visibleEntity, hand, heldStack, recordedBlockHitResult)) {
            return;
        }

        if (!heldStack.isEmpty() && hand == Hand.MAIN_HAND && tryHandleWatheGunUse(magician, proxy, heldStack)) {
            return;
        }

        HitResult hitResult = getInteractionTarget(proxy, Math.max(proxy.getEntityInteractionRange(), proxy.getBlockInteractionRange()));
        if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity target = entityHitResult.getEntity();
            if (!(target instanceof MagicianPlaybackFakePlayer) && proxy.canInteractWithEntity(target, proxy.getEntityInteractionRange())) {
                ActionResult interactAtResult = target.interactAt(proxy, entityHitResult.getPos(), hand);
                if (interactAtResult.isAccepted()) {
                    swingReplayHand(proxy, visibleEntity, hand);
                    return;
                }

                ActionResult interactResult = target.interact(proxy, hand);
                if (interactResult.isAccepted()) {
                    swingReplayHand(proxy, visibleEntity, hand);
                    return;
                }

                if (!heldStack.isEmpty() && target instanceof LivingEntity livingEntity) {
                    ActionResult useOnEntityResult = heldStack.useOnEntity(proxy, livingEntity, hand);
                    if (useOnEntityResult.isAccepted()) {
                        swingReplayHand(proxy, visibleEntity, hand);
                        return;
                    }
                }
            }
        }

        if (hitResult instanceof BlockHitResult blockHitResult
                && proxy.canInteractWithBlockAt(blockHitResult.getBlockPos(), proxy.getBlockInteractionRange())) {
            ActionResult blockResult = proxy.interactionManager.interactBlock(
                    proxy,
                    proxy.getWorld(),
                    heldStack,
                    hand,
                    blockHitResult
            );
            if (blockResult.isAccepted()) {
                swingReplayHand(proxy, visibleEntity, hand);
                if (!heldStack.isEmpty() && hand == Hand.MAIN_HAND && tryHandleBayonetStabAfterUse(magician, proxy, visibleEntity, heldStack, hitResult)) {
                    return;
                }
                return;
            }
        }

        if (heldStack.isEmpty()) {
            return;
        }

        ActionResult itemResult = proxy.interactionManager.interactItem(proxy, proxy.getWorld(), heldStack, hand);
        if (!itemResult.isAccepted()) {
            return;
        }

        if (hand == Hand.MAIN_HAND && tryHandleBayonetStabAfterUse(magician, proxy, visibleEntity, heldStack, hitResult)) {
            return;
        }

        if (shouldSwingAfterAcceptedItemUse(proxy, heldStack)) {
            swingReplayHand(proxy, visibleEntity, hand);
        }
    }

    private static void performRelease(
            @NotNull ServerPlayerEntity magician,
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull MagicianPlaybackEntity visibleEntity
    ) {
        if (!proxy.isUsingItem()) {
            return;
        }

        ItemStack releasedStack = proxy.getActiveItem().copy();
        int usedTicks = releasedStack.getMaxUseTime(proxy) - proxy.getItemUseTimeLeft();
        proxy.stopUsingItem();
        /*
         * 可见皮套不再进入原版物品使用流程，这里只关掉客户端动画状态。
         * 如果对皮套调用 stopUsingItem，某些物品的 onStoppedUsing/finishUsing 链路会把它当真实实体处理。
         */
        visibleEntity.setReplayUseState(false, null);
        visibleEntity.setReplayItemUseTimeLeft(0);

        if (releasedStack.isOf(WatheItems.KNIFE) && usedTicks >= 10) {
            performKnifeStab(magician, proxy, visibleEntity);
        }
    }

    private static void performGunShoot(
            @NotNull ServerPlayerEntity magician,
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull MagicianPlaybackEntity visibleEntity
    ) {
        tryHandleWatheGunUse(magician, proxy, proxy.getMainHandStack());
    }

    private static void performKnifeStab(
            @NotNull ServerPlayerEntity magician,
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull MagicianPlaybackEntity visibleEntity
    ) {
        EntityHitResult entityHitResult = getKnifeEntityTarget(proxy);
        if (entityHitResult == null || !(entityHitResult.getEntity() instanceof PlayerEntity target)) {
            return;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(target) || target.distanceTo(proxy) > MELEE_TARGET_RANGE) {
            return;
        }

        if (target instanceof ServerPlayerEntity serverTarget) {
            GameRecordManager.recordItemHit(
                    proxy,
                    proxy.getMainHandStack(),
                    GameConstants.DeathReasons.KNIFE,
                    serverTarget,
                    null
            );
        }

        GameFunctions.killPlayer(target, true, magician, GameConstants.DeathReasons.KNIFE, replayActorDeathData(proxy.getMainHandStack(), magician, proxy));
        target.playSound(WatheSounds.ITEM_KNIFE_STAB, 1.0F, 1.0F);
        swingReplayHand(proxy, visibleEntity, Hand.MAIN_HAND);
        if (!proxy.isCreative()) {
            proxy.getItemCooldownManager().set(WatheItems.KNIFE, GameConstants.ITEM_COOLDOWNS.getOrDefault(WatheItems.KNIFE, 0));
        }
    }

    private static @Nullable EntityHitResult getKnifeEntityTarget(@NotNull PlayerEntity proxy) {
        Vec3d eyePos = proxy.getEyePos();
        Vec3d look = proxy.getRotationVec(1.0F).normalize();
        Vec3d endPos = eyePos.add(look.multiply(MELEE_TARGET_RANGE));
        Box searchBox = proxy.getBoundingBox().stretch(look.multiply(MELEE_TARGET_RANGE)).expand(0.75D);
        java.util.function.Predicate<Entity> predicate = entity -> entity instanceof PlayerEntity player
                && entity != proxy
                && !entity.isSpectator()
                && GameFunctions.isPlayerAliveAndSurvival(player);

        /*
         * Wathe 原版 KnifeItem.getKnifeTarget 使用 ProjectileUtil.getCollision。
         * 这对真实玩家手感很好，但播放代理是每 tick 按录制轨迹硬同步，且允许穿墙，
         * 宽松碰撞会把站在侧边的人也吸进命中结果。
         *
         * 这里先用更接近“准星线”的盒射线：只要玩家命中盒真的被这条线穿过才算命中。
         */
        EntityHitResult directHit = ProjectileUtil.raycast(
                proxy,
                eyePos,
                endPos,
                searchBox,
                entity -> {
                    if (!predicate.test(entity)) {
                        return false;
                    }
                    return entity.getBoundingBox()
                            .expand(entity.getTargetingMargin() + KNIFE_TARGET_BOX_EXPAND)
                            .raycast(eyePos, endPos)
                            .isPresent();
                },
                MELEE_TARGET_RANGE * MELEE_TARGET_RANGE
        );
        if (directHit != null) {
            return directHit;
        }

        /*
         * 再给一个很小的正前方容错：
         * 如果播放帧的头部角度/服务器实体位置和客户端看到的模型有轻微差异，
         * 站在正前方的人仍然可以被判定到；但横向距离过大的侧边玩家会被排除。
         */
        PlayerEntity bestTarget = null;
        double bestProjection = Double.MAX_VALUE;
        for (Entity entity : proxy.getWorld().getOtherEntities(proxy, searchBox, predicate)) {
            PlayerEntity playerTarget = (PlayerEntity) entity;
            Vec3d targetCenter = playerTarget.getBoundingBox().getCenter();
            Vec3d toTarget = targetCenter.subtract(eyePos);
            double projection = toTarget.dotProduct(look);
            if (projection < KNIFE_FALLBACK_MIN_FORWARD || projection > MELEE_TARGET_RANGE) {
                continue;
            }

            Vec3d closestPoint = eyePos.add(look.multiply(projection));
            double sidewaysDistanceSquared = targetCenter.squaredDistanceTo(closestPoint);
            if (sidewaysDistanceSquared > KNIFE_FALLBACK_MAX_SIDEWAYS * KNIFE_FALLBACK_MAX_SIDEWAYS) {
                continue;
            }

            if (projection < bestProjection) {
                bestProjection = projection;
                bestTarget = playerTarget;
            }
        }

        return bestTarget == null ? null : new EntityHitResult(bestTarget);
    }

    private static void performBayonetStab(
            @NotNull ServerPlayerEntity magician,
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull MagicianPlaybackEntity visibleEntity
    ) {
        if (!tryHandleBayonetStabAfterUse(magician, proxy, visibleEntity, proxy.getMainHandStack(), getInteractionTarget(proxy, MELEE_TARGET_RANGE))) {
            swingReplayHand(proxy, visibleEntity, Hand.MAIN_HAND);
        }
    }

    private static void performBayonetKnockback(
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull MagicianPlaybackEntity visibleEntity
    ) {
        HitResult hitResult = getInteractionTarget(proxy, MELEE_TARGET_RANGE);
        if (!(hitResult instanceof EntityHitResult entityHitResult) || !(entityHitResult.getEntity() instanceof PlayerEntity target)) {
            swingReplayHand(proxy, visibleEntity, Hand.MAIN_HAND);
            return;
        }
        if (!BayonetKnockbackHandler.canKnockback(proxy, target) || target.distanceTo(proxy) > MELEE_TARGET_RANGE) {
            swingReplayHand(proxy, visibleEntity, Hand.MAIN_HAND);
            return;
        }

        BayonetKnockbackHandler.applyKnockback(proxy, target);
        swingReplayHand(proxy, visibleEntity, Hand.MAIN_HAND);
    }

    private static void performSniperShoot(
            @NotNull ServerPlayerEntity magician,
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull Vec3d direction
    ) {
        ItemStack rifleStack = proxy.getMainHandStack();
        if (!rifleStack.isOf(ModItems.SNIPER_RIFLE) || proxy.getItemCooldownManager().isCoolingDown(ModItems.SNIPER_RIFLE)) {
            return;
        }

        int currentAmmo = rifleStack.getOrDefault(ModItems.SNIPER_AMMO, 0);
        if (currentAmmo <= 0) {
            return;
        }

        if (!proxy.isCreative()) {
            rifleStack.set(ModItems.SNIPER_AMMO, currentAmmo - 1);
        }

        proxy.getWorld().playSound(
                null,
                proxy.getX(),
                proxy.getEyeY(),
                proxy.getZ(),
                WatheSounds.ITEM_REVOLVER_SHOOT,
                SoundCategory.PLAYERS,
                5.0F,
                1.0F + proxy.getRandom().nextFloat() * 0.1F - 0.05F
        );
        GameRecordManager.recordItemUse(proxy, net.minecraft.registry.Registries.ITEM.getId(rifleStack.getItem()), null, null);
        proxy.getItemCooldownManager().set(ModItems.SNIPER_RIFLE, RemembererConstants.SNIPER_SHOT_COOLDOWN_TICKS);
        RemembererSniperManager.fireShot(
                proxy,
                direction.normalize(),
                rifleStack.copy(),
                magician,
                proxy.getReplayActorUuid(),
                proxy.getReplayActorName()
        );
    }

    private static boolean tryHandleWatheGunUse(
            @NotNull ServerPlayerEntity magician,
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull ItemStack heldStack
    ) {
        boolean watheRevolver = heldStack.isOf(WatheItems.REVOLVER);
        boolean watheDerringer = heldStack.isOf(WatheItems.DERRINGER);
        boolean silencedRevolver = heldStack.isOf(ModItems.SILENCED_REVOLVER);
        boolean robberPistol = heldStack.isOf(ModItems.ROBBER_PISTOL);
        if (!watheRevolver && !watheDerringer && !silencedRevolver && !robberPistol) {
            return false;
        }

        Item gunItem = heldStack.getItem();
        if (proxy.getItemCooldownManager().isCoolingDown(gunItem)) {
            return true;
        }

        if (!silencedRevolver) {
            proxy.getWorld().playSound(
                    null,
                    proxy.getX(),
                    proxy.getEyeY(),
                    proxy.getZ(),
                    WatheSounds.ITEM_REVOLVER_CLICK,
                    SoundCategory.PLAYERS,
                    0.5F,
                    1.0F + proxy.getRandom().nextFloat() * 0.1F - 0.05F
            );
        }

        if (watheDerringer) {
            Boolean used = heldStack.get(WatheDataComponentTypes.USED);
            if (used != null && used) {
                return true;
            }
            if (!proxy.isCreative()) {
                heldStack.set(WatheDataComponentTypes.USED, true);
            }
        }

        EntityHitResult entityHitResult = getGunEntityTarget(
                proxy,
                watheDerringer ? DERRINGER_TARGET_RANGE : REVOLVER_TARGET_RANGE
        );
        if (entityHitResult != null
                && entityHitResult.getEntity() instanceof PlayerEntity target) {
            if (target instanceof ServerPlayerEntity serverTarget) {
                GameRecordManager.recordItemHit(
                        proxy,
                        heldStack,
                        GameConstants.DeathReasons.GUN,
                        serverTarget,
                        null
                );
            }

            GameFunctions.killPlayer(target, true, magician, GameConstants.DeathReasons.GUN, replayActorDeathData(heldStack, magician, proxy));
            if (!GameFunctions.isPlayerAliveAndSurvival(target) && !proxy.isCreative()) {
                if (silencedRevolver) {
                    dropSilencedRevolverAfterInnocentKill(proxy, target);
                } else if (robberPistol) {
                    handleRobberPistolPostKillOutcome(proxy, target);
                }
            }
        }

        if (!silencedRevolver) {
            proxy.getWorld().playSound(
                    null,
                    proxy.getX(),
                    proxy.getEyeY(),
                    proxy.getZ(),
                    WatheSounds.ITEM_REVOLVER_SHOOT,
                    SoundCategory.PLAYERS,
                    5.0F,
                    1.0F + proxy.getRandom().nextFloat() * 0.1F - 0.05F
            );
        }

        if (!proxy.isCreative()) {
            int cooldown = watheRevolver ? GameConstants.getRevolverCooldown(proxy) : GameConstants.ITEM_COOLDOWNS.getOrDefault(gunItem, 0);
            proxy.getItemCooldownManager().set(gunItem, cooldown);
        }
        return true;
    }

    private static @Nullable EntityHitResult getGunEntityTarget(@NotNull PlayerEntity proxy, double range) {
        Vec3d eyePos = proxy.getEyePos();
        Vec3d look = proxy.getRotationVec(1.0F).normalize();
        Vec3d endPos = eyePos.add(look.multiply(range));
        Box searchBox = proxy.getBoundingBox().stretch(look.multiply(range)).expand(0.45D);
        java.util.function.Predicate<Entity> predicate = entity -> entity instanceof PlayerEntity player
                && entity != proxy
                && !(entity instanceof MagicianPlaybackFakePlayer)
                && !(entity instanceof MagicianPlaybackEntity)
                && !entity.isSpectator()
                && GameFunctions.isPlayerAliveAndSurvival(player);

        /*
         * Wathe 左轮的原逻辑是客户端用 ProjectileUtil.getCollision 算目标再发包。
         * 魔术师播放没有真实客户端发包，只能在服务端用代理玩家补算。
         *
         * 之前直接调用 RevolverItem.getGunTarget(proxy) 时，fake player 的碰撞搜索会比较宽松：
         * 有时准星前方的人没被命中，旁边的人反而被吸到。因此这里改成“枪械专用准星线”：
         * 只有从眼睛沿录制视角射出的线真正穿过玩家命中盒，才算开枪命中。
         */
        EntityHitResult directHit = ProjectileUtil.raycast(
                proxy,
                eyePos,
                endPos,
                searchBox,
                entity -> {
                    if (!predicate.test(entity)) {
                        return false;
                    }
                    return entity.getBoundingBox()
                            .expand(entity.getTargetingMargin() + GUN_TARGET_BOX_EXPAND)
                            .raycast(eyePos, endPos)
                            .isPresent();
                },
                range * range
        );
        if (directHit != null) {
            return directHit;
        }

        /*
         * 极近距离再给一点点“正前方容错”。
         *
         * 录制帧是服务端逐 tick 复刻，客户端看到的枪口/头部动画和服务端眼位可能有细微差别。
         * 这个兜底只在 2.25 格内生效，并且仍然要求目标碰撞盒靠近准星线，避免重新出现
         * “站在侧边却被左轮吸中”的问题。
         */
        PlayerEntity bestTarget = null;
        double bestProjection = Double.MAX_VALUE;
        double fallbackRange = Math.min(range, GUN_CLOSE_FALLBACK_RANGE);
        for (Entity entity : proxy.getWorld().getOtherEntities(proxy, searchBox, predicate)) {
            PlayerEntity playerTarget = (PlayerEntity) entity;
            Vec3d targetCenter = playerTarget.getBoundingBox().getCenter();
            Vec3d toTarget = targetCenter.subtract(eyePos);
            double projection = toTarget.dotProduct(look);
            if (projection < GUN_CLOSE_FALLBACK_MIN_FORWARD || projection > fallbackRange) {
                continue;
            }

            Vec3d closestPointOnShotLine = eyePos.add(look.multiply(projection));
            double boxDistanceSquared = playerTarget.getBoundingBox()
                    .expand(GUN_CLOSE_FALLBACK_BOX_EXPAND)
                    .squaredMagnitude(closestPointOnShotLine);
            if (boxDistanceSquared > GUN_TARGET_BOX_EXPAND * GUN_TARGET_BOX_EXPAND) {
                continue;
            }

            if (projection < bestProjection) {
                bestProjection = projection;
                bestTarget = playerTarget;
            }
        }

        return bestTarget == null ? null : new EntityHitResult(bestTarget);
    }

    private static boolean tryHandleBayonetStabAfterUse(
            @NotNull ServerPlayerEntity magician,
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull MagicianPlaybackEntity visibleEntity,
            @NotNull ItemStack heldStack,
            @Nullable HitResult hitResult
    ) {
        if (!heldStack.isOf(ModItems.BAYONET) || !(hitResult instanceof EntityHitResult entityHitResult)) {
            return false;
        }
        if (!(entityHitResult.getEntity() instanceof PlayerEntity target) || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            return false;
        }
        if (proxy.getItemCooldownManager().isCoolingDown(ModItems.BAYONET) || target.distanceTo(proxy) > MELEE_TARGET_RANGE) {
            return false;
        }

        if (target instanceof ServerPlayerEntity serverTarget) {
            GameRecordManager.recordItemHit(
                    proxy,
                    heldStack,
                    GameConstants.DeathReasons.KNIFE,
                    serverTarget,
                    null
            );
        }

        swingReplayHand(proxy, visibleEntity, Hand.MAIN_HAND);
        GameFunctions.killPlayer(target, true, magician, GameConstants.DeathReasons.KNIFE, replayActorDeathData(heldStack, magician, proxy));
        if (!proxy.isCreative()) {
            proxy.getItemCooldownManager().set(ModItems.BAYONET, GameConstants.ITEM_COOLDOWNS.getOrDefault(ModItems.BAYONET, 0));
        }
        return true;
    }

    private static boolean tryInteractRecordedBlock(
            @NotNull ServerPlayerEntity magician,
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull MagicianPlaybackEntity visibleEntity,
            @NotNull Hand hand,
            @NotNull ItemStack heldStack,
            @NotNull BlockHitResult blockHitResult
    ) {
        if (!proxy.canInteractWithBlockAt(blockHitResult.getBlockPos(), proxy.getBlockInteractionRange())) {
            return false;
        }

        ActionResult blockResult = proxy.interactionManager.interactBlock(
                proxy,
                proxy.getWorld(),
                heldStack,
                hand,
                blockHitResult
        );
        if (!blockResult.isAccepted()) {
            return false;
        }

        /*
         * 录制方块命中点成功时，直接认为这次交互已经复刻完成。
         * 对空手按钮、未上锁门、床这类交互来说，关键就是不能因为 heldStack 为空提前退出。
         */
        swingReplayHand(proxy, visibleEntity, hand);
        if (!heldStack.isEmpty() && hand == Hand.MAIN_HAND) {
            tryHandleBayonetStabAfterUse(magician, proxy, visibleEntity, heldStack, blockHitResult);
        }
        return true;
    }

    private static void swingReplayHand(
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull MagicianPlaybackEntity visibleEntity,
            @NotNull Hand hand
    ) {
        proxy.swingHand(hand, true);
        visibleEntity.playReplaySwing(hand);
    }

    private static void performVisualSwing(
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull MagicianPlaybackEntity visibleEntity,
            @NotNull Hand hand
    ) {
        if (proxy.isUsingItem() || visibleEntity.isReplayUsingItem()) {
            return;
        }
        if (shouldSkipVisualSwingForHeldStack(proxy.getStackInHand(hand))) {
            return;
        }
        swingReplayHand(proxy, visibleEntity, hand);
    }

    private static boolean shouldSkipAcceptedItemSwing(@NotNull ItemStack heldStack) {
        return heldStack.isOf(ModItems.BAYONET)
                || heldStack.isOf(WatheItems.REVOLVER)
                || heldStack.isOf(WatheItems.DERRINGER)
                || heldStack.isOf(ModItems.SILENCED_REVOLVER)
                || heldStack.isOf(ModItems.ROBBER_PISTOL);
    }

    private static boolean shouldSkipVisualSwingForHeldStack(@NotNull ItemStack heldStack) {
        return heldStack.isIn(WatheItemTags.GUNS)
                || heldStack.isOf(ModItems.ROBBER_PISTOL)
                || heldStack.isOf(ModItems.SILENCED_REVOLVER)
                || heldStack.isOf(ModItems.SNIPER_RIFLE);
    }

    private static boolean shouldSwingAfterAcceptedItemUse(
            @NotNull MagicianPlaybackFakePlayer proxy,
            @NotNull ItemStack heldStack
    ) {
        /*
         * 长按类物品（匕首 SPEAR、手雷/飞斧 BOW 等）在 interactItem 后会进入 usingItem。
         * 它们的视觉应该直接由 UseAction 举起姿势接管，不能再叠一次挥手，
         * 否则播放体会先顿一下/压一下手臂，然后才进入蓄力姿势。
         */
        return !proxy.isUsingItem()
                && heldStack.getUseAction() == UseAction.NONE
                && !shouldSkipAcceptedItemSwing(heldStack);
    }

    private static @NotNull HitResult getInteractionTarget(@NotNull PlayerEntity proxy, double range) {
        HitResult blockHit = proxy.raycast(proxy.getBlockInteractionRange(), 1.0F, false);
        HitResult entityHit = ProjectileUtil.getCollision(
                proxy,
                entity -> entity != proxy
                        && !(entity instanceof MagicianPlaybackFakePlayer)
                        && !(entity instanceof org.agmas.noellesroles.entities.MagicianPlaybackEntity)
                        && !entity.isSpectator(),
                (float) range
        );

        if (!(entityHit instanceof EntityHitResult entityHitResult)) {
            return blockHit;
        }
        if (blockHit.getType() == HitResult.Type.MISS) {
            return entityHitResult;
        }

        Vec3d eyePos = proxy.getEyePos();
        double entityDistanceSquared = eyePos.squaredDistanceTo(entityHitResult.getPos());
        double blockDistanceSquared = eyePos.squaredDistanceTo(blockHit.getPos());
        return entityDistanceSquared <= blockDistanceSquared ? entityHitResult : blockHit;
    }

    private static @Nullable EntityHitResult getAttackEntityTarget(@NotNull PlayerEntity proxy, double range) {
        Vec3d eyePos = proxy.getEyePos();
        Vec3d look = proxy.getRotationVec(1.0F);
        Vec3d endPos = eyePos.add(look.multiply(range));
        Box searchBox = proxy.getBoundingBox().stretch(look.multiply(range)).expand(0.75D);
        java.util.function.Predicate<Entity> predicate = entity -> entity != proxy
                && !(entity instanceof MagicianPlaybackFakePlayer)
                && !(entity instanceof org.agmas.noellesroles.entities.MagicianPlaybackEntity)
                && !entity.isSpectator();

        /*
         * 攻击动作和开门/按钮交互不同：录制的是“我挥向面前这个人”，
         * 播放皮套又允许穿墙/穿门复刻轨迹。如果继续复用 getInteractionTarget，
         * 方块命中点可能会比玩家更近，球棒就会被墙、门或装饰方块抢走目标。
         *
         * 这里改成攻击专用的实体射线，忽略方块阻挡；后面的近距离容错则处理
         * 帧同步/动作优化模组造成的一点点站位误差。
         */
        EntityHitResult directHit = ProjectileUtil.raycast(
                proxy,
                eyePos,
                endPos,
                searchBox,
                predicate,
                range * range
        );
        if (directHit != null) {
            return directHit;
        }

        PlayerEntity bestTarget = null;
        double bestProjection = Double.MAX_VALUE;
        for (Entity entity : proxy.getWorld().getOtherEntities(proxy, searchBox, predicate)) {
            if (!(entity instanceof PlayerEntity playerTarget) || !GameFunctions.isPlayerAliveAndSurvival(playerTarget)) {
                continue;
            }

            Vec3d toTarget = playerTarget.getBoundingBox().getCenter().subtract(eyePos);
            double projection = toTarget.dotProduct(look);
            if (projection < 0.0D || projection > range + 0.75D) {
                continue;
            }

            Vec3d closestPointOnSwingLine = eyePos.add(look.multiply(projection));
            double sidewaysDistanceSquared = playerTarget.getBoundingBox().expand(0.35D).squaredMagnitude(closestPointOnSwingLine);
            if (sidewaysDistanceSquared > 1.0D) {
                continue;
            }

            if (projection < bestProjection) {
                bestProjection = projection;
                bestTarget = playerTarget;
            }
        }

        return bestTarget == null ? null : new EntityHitResult(bestTarget);
    }

    private static @NotNull NbtCompound replayActorDeathData(
            @NotNull ItemStack stack,
            @NotNull ServerPlayerEntity magician,
            @NotNull MagicianPlaybackFakePlayer proxy
    ) {
        NbtCompound data = GameFunctions.createReplayItemData(magician.getServerWorld(), stack);
        data.putUuid("replay_actor", proxy.getReplayActorUuid());
        data.putString("replay_actor_name", proxy.getReplayActorName());
        data.putUuid("magician_owner", magician.getUuid());
        return data;
    }

    private static void dropSilencedRevolverAfterInnocentKill(@NotNull MagicianPlaybackFakePlayer proxy, @NotNull PlayerEntity target) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(target.getWorld());
        if (!gameWorld.isInnocent(target)) {
            return;
        }
        proxy.getInventory().remove(stack -> stack.isOf(ModItems.SILENCED_REVOLVER), 1, proxy.getInventory());
    }

    private static void handleRobberPistolPostKillOutcome(@NotNull MagicianPlaybackFakePlayer proxy, @NotNull PlayerEntity target) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(target.getWorld());
        if (!gameWorld.isInnocent(target)) {
            return;
        }
        int roll = proxy.getRandom().nextInt(100);
        if (roll < 20) {
            return;
        }

        proxy.getInventory().remove(stack -> stack.isOf(ModItems.ROBBER_PISTOL), 1, proxy.getInventory());
        if (roll < 50) {
            proxy.getInventory().offerOrDrop(WatheItems.REVOLVER.getDefaultStack());
        }
    }
}
