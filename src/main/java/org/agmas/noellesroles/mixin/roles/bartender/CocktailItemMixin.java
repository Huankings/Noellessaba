package org.agmas.noellesroles.mixin.roles.bartender;

import dev.doctor4t.wathe.item.CocktailItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
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

        ((BartenderPlayerComponent)BartenderPlayerComponent.KEY.get(user)).startGlow();
    }
}
