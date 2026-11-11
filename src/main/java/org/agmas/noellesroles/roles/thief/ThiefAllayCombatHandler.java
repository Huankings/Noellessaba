package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.GunShotContext;
import dev.doctor4t.wathe.api.combat.GunShotResult;
import dev.doctor4t.wathe.api.combat.GunTargetContext;
import dev.doctor4t.wathe.api.combat.GunTargetResult;
import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterConstants;
import org.jetbrains.annotations.NotNull;

/**
 * 悦灵对 Wathe/Noelles 枪械的公共目标与服务端命中接入。
 *
 * <p>Wathe 枪击包会先进入 GunShotApi，因此这里不需要 mixin 枪击接收器。
 * 悦灵命中优先于各职业的“击杀玩家后奖励/掉枪”逻辑，避免把悦灵误算成无辜玩家、悬赏目标或职业击杀。</p>
 */
public final class ThiefAllayCombatHandler {
    private static boolean initialized;

    private ThiefAllayCombatHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GunShotApi.registerTargetRule(
                NoellesRolesCore.id("thief_allay/gun_target"),
                ThiefConstants.ALLAY_GUN_TARGET_PRIORITY,
                ThiefAllayCombatHandler::resolveGunTarget
        );
        GunShotApi.registerShotHandler(
                NoellesRolesCore.id("thief_allay/gun_hit"),
                ThiefConstants.ALLAY_GUN_HANDLER_PRIORITY,
                ThiefAllayCombatHandler::handleGunShot
        );
    }

    private static GunTargetResult resolveGunTarget(@NotNull GunTargetContext context) {
        if (!isSupportedGun(context.stack())) {
            return GunTargetResult.pass();
        }
        HitResult allayHit = ProjectileUtil.getCollision(
                context.user(),
                entity -> entity instanceof ThiefAllayEntity allay
                        && allay.isOperationallyAlive()
                        && TargetVisibilityApi.canAttackEntity(context.user(), allay),
                context.range()
        );
        if (!(allayHit instanceof EntityHitResult allayEntityHit)) {
            return GunTargetResult.pass();
        }

        /* 玩家或其它默认目标更靠近时不抢目标，保证枪械原本的射线优先级不变。 */
        HitResult defaultTarget = context.defaultTarget();
        if (defaultTarget != null && defaultTarget.getType() != HitResult.Type.MISS) {
            double allayDistance = context.user().getEyePos().squaredDistanceTo(allayEntityHit.getPos());
            double defaultDistance = context.user().getEyePos().squaredDistanceTo(defaultTarget.getPos());
            if (defaultDistance + ThiefConstants.ALLAY_TARGET_DISTANCE_EPSILON < allayDistance) {
                return GunTargetResult.pass();
            }
        }
        return GunTargetResult.target(allayEntityHit);
    }

    private static GunShotResult handleGunShot(@NotNull GunShotContext context) {
        if (!isSupportedGun(context.stack()) || !(context.targetEntity() instanceof ThiefAllayEntity allay)) {
            return GunShotResult.PASS;
        }
        if (context.isCoolingDown()
                || !allay.isOperationallyAlive()
                /*
                 * 枪械准心通过射线命中悦灵碰撞箱，因此服务端也使用 Wathe 的碰撞箱距离。
                 * 这同时覆盖普通左轮和两把赏金枪，避免它们在各自极限射程产生假锁定。
                 */
                || !WeaponTargetingApi.isWithinTargetingRange(
                        context.shooter(),
                        allay,
                        resolveGunRange(context.stack())
                )) {
            return GunShotResult.HANDLED;
        }

        /* Wathe 德林杰仍然只能发射一次；命中悦灵不能绕过 USED 数据组件。 */
        if (context.stack().isOf(WatheItems.DERRINGER)) {
            if (context.stack().getOrDefault(WatheDataComponentTypes.USED, false)) {
                return GunShotResult.HANDLED;
            }
            if (!context.isCreative()) {
                context.stack().set(WatheDataComponentTypes.USED, true);
            }
        }

        boolean silenced = context.stack().isOf(ModItems.SILENCED_REVOLVER);
        if (!silenced) {
            context.playDefaultClickSound();
        }
        allay.killByWeapon(context.shooter(), context.stack().copy(), GameConstants.DeathReasons.GUN);
        if (!silenced) {
            context.playDefaultShootSound();
        }
        context.sendMuzzle();

        if (!context.isCreative()) {
            context.applyCooldown(resolveGunCooldown(context));
        }
        return GunShotResult.HANDLED;
    }

    private static int resolveGunCooldown(GunShotContext context) {
        ItemStack stack = context.stack();
        if (stack.isOf(WatheItems.REVOLVER)) {
            return GameConstants.getRevolverCooldown(context.shooter());
        }
        if (stack.isOf(ModItems.BOUNTY_PISTOL)) {
            return BountyHunterConstants.BOUNTY_PISTOL_FAILED_COOLDOWN_TICKS;
        }
        if (stack.isOf(ModItems.BOUNTY_DERRINGER)) {
            return BountyHunterConstants.BOUNTY_DERRINGER_COOLDOWN_TICKS;
        }
        return GameConstants.ITEM_COOLDOWNS.getOrDefault(stack.getItem(), 0);
    }

    public static boolean isSupportedGun(@NotNull ItemStack stack) {
        return !stack.isOf(ModItems.FAKE_REVOLVER)
                && (stack.isIn(WatheItemTags.GUNS)
                || stack.isOf(ModItems.ROBBER_PISTOL)
                || stack.isOf(ModItems.SILENCED_REVOLVER)
                || stack.isOf(ModItems.BOUNTY_PISTOL)
                || stack.isOf(ModItems.BOUNTY_DERRINGER));
    }

    private static float resolveGunRange(ItemStack stack) {
        if (stack.isOf(ModItems.BOUNTY_PISTOL)) {
            return BountyHunterConstants.BOUNTY_PISTOL_RANGE_BLOCKS;
        }
        if (stack.isOf(ModItems.BOUNTY_DERRINGER)) {
            return BountyHunterConstants.BOUNTY_DERRINGER_RANGE_BLOCKS;
        }
        return ThiefConstants.ALLAY_DEFAULT_GUN_SERVER_RANGE;
    }

    public static boolean isSupportedWeapon(@NotNull ItemStack stack) {
        return isSupportedGun(stack)
                || stack.isOf(WatheItems.KNIFE)
                || stack.isOf(WatheItems.GRENADE)
                || stack.isOf(ModItems.HUNTING_KNIFE)
                || stack.isOf(ModItems.BAYONET)
                || stack.isOf(ModItems.THROWING_AXE)
                || stack.isOf(ModItems.THROWING_SPEED_AXE)
                || stack.isOf(ModItems.THROWING_BOMB_AXE)
                || stack.isOf(ModItems.THROWING_BLOOD_AXE)
                || stack.isOf(ModItems.THROWING_MACHETE)
                || stack.isOf(ModItems.TOMAHAWK)
                || stack.isOf(ModItems.THROWING_TOYS_AXE)
                || stack.isOf(ModItems.SILENT_GRENADE)
                || stack.isOf(ModItems.SNIPER_RIFLE);
    }

    /**
     * 只允许真正的近战武器通过原版 DamageSource 直接击杀悦灵。
     * 枪、手雷和投射物必须走各自的开火/爆炸/飞行命中链，避免玩家拿着左轮左键敲一下也能杀死悦灵。
     */
    public static boolean isSupportedDirectDamageWeapon(@NotNull ItemStack stack) {
        return stack.isOf(WatheItems.KNIFE)
                || stack.isOf(ModItems.HUNTING_KNIFE)
                || stack.isOf(ModItems.BAYONET)
                || stack.isOf(ModItems.BLOOD_AXE)
                || stack.isOf(ModItems.COLORFUL_AXE);
    }

    public static Identifier resolveDeathReason(@NotNull ItemStack stack) {
        if (stack.isOf(WatheItems.KNIFE) || stack.isOf(ModItems.HUNTING_KNIFE) || stack.isOf(ModItems.BAYONET)) {
            return GameConstants.DeathReasons.KNIFE;
        }
        if (stack.isOf(WatheItems.GRENADE) || stack.isOf(ModItems.SILENT_GRENADE) || stack.isOf(ModItems.THROWING_BOMB_AXE)) {
            return GameConstants.DeathReasons.GRENADE;
        }
        if (stack.isOf(ModItems.THROWING_AXE) || stack.isOf(ModItems.THROWING_SPEED_AXE)) {
            return NoellesDeathReasons.DEATH_REASON_THROWING_AXE;
        }
        if (stack.isOf(ModItems.THROWING_BLOOD_AXE)
                || stack.isOf(ModItems.THROWING_MACHETE)
                || stack.isOf(ModItems.TOMAHAWK)
                || stack.isOf(ModItems.THROWING_TOYS_AXE)) {
            return NoellesDeathReasons.JASON_THROWING_WEAPON_DEATH_REASON;
        }
        if (stack.isOf(ModItems.SNIPER_RIFLE)) {
            return NoellesDeathReasons.DEATH_REASON_SNIPER_RIFLE;
        }
        return GameConstants.DeathReasons.GUN;
    }
}
