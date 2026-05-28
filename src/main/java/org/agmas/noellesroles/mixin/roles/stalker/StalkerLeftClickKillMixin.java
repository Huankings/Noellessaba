package org.agmas.noellesroles.mixin.roles.stalker;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.stalker.StalkerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class StalkerLeftClickKillMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onStalkerKnifeAttack(Entity target, CallbackInfo ci) {
        ServerPlayerEntity attacker = (ServerPlayerEntity) (Object) this;
        if (!(target instanceof PlayerEntity victim)) return;
        if (!GameFunctions.isPlayerAliveAndSurvival(attacker) || !GameFunctions.isPlayerAliveAndSurvival(victim)) return;

        var gameWorld = dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(attacker.getWorld());
        if (!gameWorld.isRole(attacker, Noellesroles.STALKER)) return;

        StalkerPlayerComponent comp = StalkerPlayerComponent.KEY.get(attacker);
        if (!comp.isActiveStalker() || comp.phase < 2) return;

        // 检查手持是否为刀
        if (!attacker.getMainHandStack().isOf(WatheItems.KNIFE)) return;

        // 三阶段只能用突进，不能用左键
        if (comp.phase == 3 && comp.dashModeActive) {
            ci.cancel();
            return;
        }

        // 攻击冷却
        if (comp.attackCooldown > 0) {
            ci.cancel();
            return;
        }

        // 执行击杀
        GameFunctions.killPlayer(victim, true, attacker, GameConstants.DeathReasons.KNIFE);
        comp.addKill(); // 记录击杀并触发冷却

        // 取消原攻击逻辑
        ci.cancel();
    }
}