package org.agmas.noellesroles.client.mixin.roles.robber;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让 Wathe 左轮的“准星锁定高亮”也能识别强盗手枪。
 * 这里只扩展第一段左轮判断，不去碰后面的匕首/短枪逻辑。
 */
@Mixin(CrosshairRenderer.class)
public class RobberGunCrosshairMixin {

    @WrapOperation(
            method = "renderCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z",
                    ordinal = 0
            )
    )
    private static boolean noellesroles$allowRobberPistolTarget(ItemStack instance, Item item, Operation<Boolean> original) {
        return original.call(instance, item) || instance.isOf(ModItems.ROBBER_PISTOL);
    }
}
