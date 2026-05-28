package org.agmas.noellesroles.client.mixin.roles.coroner;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(dev.doctor4t.wathe.client.gui.RoleNameRenderer.class)
public abstract class CoronerRoleNameRendererMixin {

    @WrapOperation(method = "renderHud",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;getDisplayName()Lnet/minecraft/text/Text;"))
    private static Text renderRoleHud(PlayerEntity instance, Operation<Text> original) {
        CoronerPlayerComponent coronerComp = CoronerPlayerComponent.KEY.get(instance);

        if (coronerComp.getMorphTicks() > 0) {
            UUID disguiseUuid = coronerComp.disguise;

            if (disguiseUuid == null) {
                return original.call(instance);
            }

            // 如果伪装的是自己
            if (disguiseUuid.equals(instance.getUuid())) {
                return instance.getDisplayName();
            }

            // 如果伪装的是本地玩家
            ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
            if (disguiseUuid.equals(localPlayer.getUuid())) {
                return localPlayer.getDisplayName();
            }

            // 首先尝试获取玩家实体
            PlayerEntity disguisePlayer = instance.getWorld().getPlayerByUuid(disguiseUuid);
            if (disguisePlayer != null) {
                return disguisePlayer.getDisplayName();
            }

            // 如果玩家死亡/旁观，从本地玩家的networkHandler获取
            if (localPlayer != null && localPlayer.networkHandler != null) {
                PlayerListEntry playerListEntry = localPlayer.networkHandler.getPlayerListEntry(disguiseUuid);
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