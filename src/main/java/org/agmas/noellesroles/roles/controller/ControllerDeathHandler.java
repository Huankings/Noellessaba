package org.agmas.noellesroles.roles.controller;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.death.DeathProcessComponent;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 附体师死亡时解除附体，并把被附体者拉到死亡位置承受巫毒连锁。
 */
public final class ControllerDeathHandler {
    private static boolean initialized = false;

    private ControllerDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerBeforeAttempt(
                NoellesRolesCore.id("controller_death_logic"),
                DeathApi.PRIORITY_DEATH_PROCESS_STATE - 100,
                context -> {
                    /*
                     * 附体师死亡时，被控制者需要先从控制状态中拉回真实世界。
                     * 这里放在 beforeAttempt：死亡是否最终成立还未确定，但旧逻辑就是在死亡请求入口
                     * 立即释放附体，避免受害者之后被护盾/免死拦下时仍卡在被控制状态。
                     */
                    ControllerPlayerComponent controllerComp = ControllerPlayerComponent.KEY.get(context.victim());
                    if (controllerComp.controlledTarget == null || controllerComp.possessTicks <= 0) {
                        return;
                    }

                    PlayerEntity target = context.victim().getWorld().getPlayerByUuid(controllerComp.controlledTarget);
                    if (target != null && GameFunctions.isPlayerAliveAndSurvival(target)) {
                        Vec3d deathPos = context.victim().getPos();
                        float deathYaw = context.victim().getYaw();
                        float deathPitch = context.victim().getPitch();

                        target.refreshPositionAndAngles(deathPos.x, deathPos.y, deathPos.z, deathYaw, deathPitch);

                        ControlledPlayerComponent controlledComp = ControlledPlayerComponent.KEY.get(target);
                        controlledComp.clearControlled();
                        target.removeStatusEffect(StatusEffects.INVISIBILITY);
                        target.removeStatusEffect(StatusEffects.SLOW_FALLING);

                        if (!DeathProcessComponent.KEY.get(target).isProcessing()) {
                            /*
                             * gameplay killer 沿用原逻辑传入；回放额外写 replay_actor，
                             * 让展示文本显示为“附体师的巫毒魔法”造成死亡。
                             * DeathProcessComponent 防止附体目标在同一条死亡链里被重复递归击杀。
                             */
                            NbtCompound replayDeathData = new NbtCompound();
                            replayDeathData.putUuid("replay_actor", context.victim().getUuid());
                            GameFunctions.killPlayer(target, true, context.killer(), NoellesDeathReasons.VOODOO_MAGIC_DEATH_REASON, replayDeathData);
                        }
                    }

                    // 最后把附体师本体放回原位，再释放组件状态，保留旧 mixin 的结算顺序。
                    context.victim().refreshPositionAndAngles(
                            controllerComp.originalX,
                            controllerComp.originalY,
                            controllerComp.originalZ,
                            controllerComp.originalYaw,
                            controllerComp.originalPitch
                    );
                    controllerComp.releasePossession(true);
                }
        );
    }
}
