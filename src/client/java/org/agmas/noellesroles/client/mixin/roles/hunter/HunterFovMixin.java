package org.agmas.noellesroles.client.mixin.roles.hunter;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.hunter.HunterConstants;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 追猎者举刀疾跑时同步放大 FOV，给客户端速度反馈。
 */
@Mixin(GameRenderer.class)
public class HunterFovMixin {

    @Shadow @Final MinecraftClient client;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void noellesroles$modifyHunterFov(Camera camera, float tickDelta, boolean changingFov, @NotNull CallbackInfoReturnable<Double> cir) {
        if (this.client.player == null) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.client.player.getWorld());
        if (!gameWorld.isRole(this.client.player, Noellesroles.HUNTER)
                || !this.client.player.isUsingItem()
                || !this.client.player.isSprinting()) {
            return;
        }

        ItemStack stack = this.client.player.getActiveItem();
        if ((stack.isOf(WatheItems.KNIFE) || stack.isOf(ModItems.HUNTING_KNIFE))
                && stack.getItem().getUseAction(stack) == UseAction.SPEAR) {
            cir.setReturnValue(cir.getReturnValue() * HunterConstants.HUNTER_SPRINT_FOV_MULTIPLIER);
        }
    }
}
