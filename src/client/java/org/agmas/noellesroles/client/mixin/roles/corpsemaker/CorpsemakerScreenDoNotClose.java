package org.agmas.noellesroles.client.mixin.roles.corpsemaker;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.ui.roles.corpsemaker.CorpsemakerPhase;
import org.agmas.noellesroles.client.ui.roles.corpsemaker.CorpsemakerState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LimitedHandledScreen.class)
public class CorpsemakerScreenDoNotClose {

    @WrapOperation(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;matchesKey(II)Z", ordinal = 0))
    boolean doNotCloseInventory(KeyBinding instance, int keyCode, int scanCode, Operation<Boolean> original) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
            boolean isCorpsemaker = gameWorld.isRole(client.player, Noellesroles.CORPSEMAKER);
            // 只有当玩家是造尸怪且处于角色输入阶段时才阻止E键
            if (isCorpsemaker && CorpsemakerState.phase == CorpsemakerPhase.ROLE_INPUT) {
                return false;
            }
        }
        return original.call(instance, keyCode, scanCode);
    }
}