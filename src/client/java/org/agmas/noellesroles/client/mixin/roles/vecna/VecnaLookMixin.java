package org.agmas.noellesroles.client.mixin.roles.vecna;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.agmas.noellesroles.roles.vecna.VecnaPlayerComponent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

/** 颠倒状态下反转鼠标水平/垂直视角输入。 */
@Mixin(Entity.class)
public abstract class VecnaLookMixin {
    @WrapMethod(method = "changeLookDirection")
    private void noellesroles$invertMouse(double cursorDeltaX, double cursorDeltaY, Operation<Void> original) {
        if (shouldInvert()) {
            original.call(-cursorDeltaX, -cursorDeltaY);
        } else {
            original.call(cursorDeltaX, cursorDeltaY);
        }
    }

    private static boolean shouldInvert() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && VecnaPlayerComponent.KEY.get(client.player).isPsychoInverted();
    }
}
