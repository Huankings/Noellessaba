package org.agmas.noellesroles.mixin.roles.controller;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.controller.ControlledPlayerComponent;
import org.agmas.noellesroles.roles.controller.ControllerPlayerComponent;
import org.agmas.noellesroles.death.DeathProcessComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameFunctions.class)
public abstract class ControllerLogicMixin {

    @Inject(method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At("HEAD"))
    private static void onPlayerKilled(PlayerEntity victim, boolean spawnBody, PlayerEntity killer,
                                       Identifier identifier, CallbackInfo ci) {
        // 标记受害者正在处理
        DeathProcessComponent.KEY.get(victim).setProcessing(true);

        ControllerPlayerComponent controllerComp = ControllerPlayerComponent.KEY.get(victim);
        if (controllerComp.controlledTarget != null && controllerComp.possessTicks > 0) {
            PlayerEntity target = victim.getWorld().getPlayerByUuid(controllerComp.controlledTarget);
            if (target != null && GameFunctions.isPlayerAliveAndSurvival(target)) {
                // 记录附体师当前死亡位置（即要被传送给 target 的位置）
                Vec3d deathPos = victim.getPos();
                float deathYaw = victim.getYaw();
                float deathPitch = victim.getPitch();

                // 将被附体者传送到附体师的死亡位置
                target.refreshPositionAndAngles(deathPos.x, deathPos.y, deathPos.z, deathYaw, deathPitch);

                // 解除被附体者的控制状态（清除控制组件）
                ControlledPlayerComponent controlledComp = ControlledPlayerComponent.KEY.get(target);
                controlledComp.clearControlled();

                // 移除被附体者的状态效果
                target.removeStatusEffect(StatusEffects.INVISIBILITY);
                target.removeStatusEffect(StatusEffects.SLOW_FALLING);

                // 检查目标是否已在处理中，若否，则杀死目标（巫毒效果）
                if (!DeathProcessComponent.KEY.get(target).isProcessing()) {
                    /*
                     * gameplay killer 仍沿用原逻辑传入，
                     * 但回放里的“巫毒来源”应该显示为附体师本人，而不是外部击杀者。
                     */
                    NbtCompound replayDeathData = new NbtCompound();
                    replayDeathData.putUuid("replay_actor", victim.getUuid());
                    GameFunctions.killPlayer(target, true, killer, Noellesroles.VOODOO_MAGIC_DEATH_REASON, replayDeathData);
                }
            }

            // 附体师传送回原始位置（在死亡前）
            victim.refreshPositionAndAngles(controllerComp.originalX, controllerComp.originalY,
                    controllerComp.originalZ, controllerComp.originalYaw,
                    controllerComp.originalPitch);

            // 释放附体状态
            controllerComp.releasePossession(true);
        }
    }
}
