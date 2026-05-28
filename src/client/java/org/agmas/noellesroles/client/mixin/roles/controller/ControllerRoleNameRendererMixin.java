package org.agmas.noellesroles.client.mixin.roles.controller;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.roles.controller.ControllerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(dev.doctor4t.wathe.client.gui.RoleNameRenderer.class)
public abstract class ControllerRoleNameRendererMixin {

    @WrapOperation(method = "renderHud",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;getDisplayName()Lnet/minecraft/text/Text;"))
    private static Text renderRoleHud(PlayerEntity instance, Operation<Text> original) {
        ControllerPlayerComponent controllerComp = ControllerPlayerComponent.KEY.get(instance);
        UUID targetUuid = controllerComp.getDisguiseTarget();

        // 附体师名牌显示也统一读取附体师自己的伪装目标，
        // 不再依赖变形怪组件中的 disguise 字段。
        if (targetUuid != null) {

            // 如果附体的是自己
            if (targetUuid.equals(instance.getUuid())) {
                return instance.getDisplayName();
            }

            // 如果附体的是本地玩家
            ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
            if (localPlayer != null && targetUuid.equals(localPlayer.getUuid())) {
                return localPlayer.getDisplayName();
            }

            // 首先尝试获取玩家实体
            PlayerEntity targetPlayer = instance.getWorld().getPlayerByUuid(targetUuid);
            if (targetPlayer != null) {
                return targetPlayer.getDisplayName();
            }

            // 如果玩家死亡/旁观，从本地玩家的networkHandler获取
            if (localPlayer != null && localPlayer.networkHandler != null) {
                PlayerListEntry playerListEntry = localPlayer.networkHandler.getPlayerListEntry(targetUuid);
                if (playerListEntry != null) {
                    return Text.literal(playerListEntry.getProfile().getName());
                }
            }

            // 如果都找不到，返回"Unknown Player"
            return Text.literal("Unknown Player");
        }

        return original.call(instance);
    }
}
