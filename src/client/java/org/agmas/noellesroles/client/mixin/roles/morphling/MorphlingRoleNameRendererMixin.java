package org.agmas.noellesroles.client.mixin.roles.morphling;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;


@Mixin(RoleNameRenderer.class)
public abstract class MorphlingRoleNameRendererMixin {

    @WrapOperation(method = "renderHud", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getDisplayName()Lnet/minecraft/text/Text;"))
    private static Text renderRoleHud(PlayerEntity instance, Operation<Text> original) {

        if (WatheClient.moodComponent != null) {
            if ((ConfigWorldComponent.KEY.get(instance.getWorld())).insaneSeesMorphs && WatheClient.moodComponent.isLowerThanDepressed() && NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.get(instance.getUuid()) != null) {
                return Text.literal("??!?!").formatted(Formatting.OBFUSCATED);
            }
        }
        if (instance.isInvisible()) {
            return Text.literal("");
        }


        MorphlingPlayerComponent morphComp = MorphlingPlayerComponent.KEY.get(instance);

        if (morphComp.getMorphTicks() > 0) {
            UUID disguiseUuid = morphComp.disguise;

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
                } else {
                Log.info(LogCategory.GENERAL, "Morphling disguise not found in player list: " + disguiseUuid);
                }
            }
            // 如果都找不到，返回"Unknown Player"
            return Text.literal("Unknown Player");
        }

        return original.call(instance);
    }

}
