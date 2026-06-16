package org.agmas.noellesroles.mixin.roles.bartender;

import dev.doctor4t.wathe.item.CocktailItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.bartender.BartenderPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CocktailItem.class)
public class CocktailItemMixin {

    @Inject(method = "finishUsing", at = @At("HEAD"))
    public void bartenderVision(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        /*
         * CocktailItem.finishUsing 的 user 类型是 LivingEntity，不保证一定是玩家。
         * 魔术师播放皮套、其他模组实体甚至原版非玩家实体都可能触发这里；
         * 只有真实服务端玩家才拥有 noellesroles:bartender 组件并需要调酒师视觉效果。
         */
        if (!(user instanceof ServerPlayerEntity player)) {
            return;
        }

        BartenderPlayerComponent.KEY.get(player).startGlow();
    }
}
