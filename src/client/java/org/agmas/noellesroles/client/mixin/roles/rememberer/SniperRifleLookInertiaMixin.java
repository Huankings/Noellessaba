package org.agmas.noellesroles.client.mixin.roles.rememberer;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import org.agmas.noellesroles.client.roles.rememberer.RemembererClientEffects;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 狙击枪手持时的“视角发沉 + 轻微惯性”。
 */
@Mixin(Entity.class)
public abstract class SniperRifleLookInertiaMixin {

    @WrapMethod(method = "changeLookDirection")
    private void noellesroles$slowSniperLook(double cursorDeltaX, double cursorDeltaY, Operation<Void> original) {
        double[] transformed = RemembererClientEffects.transformSniperLookInput(cursorDeltaX, cursorDeltaY);
        original.call(transformed[0], transformed[1]);
    }
}
