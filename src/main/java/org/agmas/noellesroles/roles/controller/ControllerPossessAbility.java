package org.agmas.noellesroles.roles.controller;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.role.controller.ControllerPossessC2SPacket;

import java.util.UUID;

public final class ControllerPossessAbility {

    private ControllerPossessAbility() {
        // 工具类，禁止实例化
    }

    /**
     * 处理附体师的附体请求
     * @param payload 客户端发送的数据包
     * @param player  技能使用玩家（附体师）
     */
    public static void handle(ControllerPossessC2SPacket payload, ServerPlayerEntity player) {
        var world = player.getWorld();
        var gameWorld = GameWorldComponent.KEY.get(world);

        // 检查角色
        if (!gameWorld.isRole(player, Noellesroles.CONTROLLER)) return;

        UUID targetUuid = payload.target();
        PlayerEntity target = world.getPlayerByUuid(targetUuid);

        if (target == null || target.equals(player)) return;

        // 获取目标位置和朝向
        double targetX = target.getX();
        double targetY = target.getY();
        double targetZ = target.getZ();
        float targetYaw = target.getYaw();
        float targetPitch = target.getPitch();

        // 获取附体师组件
        ControllerPlayerComponent controllerComp = ControllerPlayerComponent.KEY.get(player);
        controllerComp.startPossession(targetUuid,
                new Vec3d(targetX, targetY, targetZ), targetYaw, targetPitch);

        // 保存被附体者状态
        ControlledPlayerComponent controlledComp = ControlledPlayerComponent.KEY.get(target);
        controlledComp.setControlled(player.getUuid(),
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch());

        // 交换位置
        player.refreshPositionAndAngles(targetX, targetY, targetZ, targetYaw, targetPitch);
        target.refreshPositionAndAngles(controllerComp.originalX, controllerComp.originalY,
                controllerComp.originalZ, controllerComp.originalYaw, controllerComp.originalPitch);

        // 给被附体者添加效果（持续整个附体时间）
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY,
                ControllerPlayerComponent.POSSESS_DURATION_TICKS, 0, false, false, false));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING,
                ControllerPlayerComponent.POSSESS_DURATION_TICKS, 0, false, false, false));
        // 附体师的伪装状态现在完全由 ControllerPlayerComponent 自己维护。
        // 只要附体生效，客户端就会根据 controlledTarget 和 possessTicks 直接渲染成目标外观。
    }
}
