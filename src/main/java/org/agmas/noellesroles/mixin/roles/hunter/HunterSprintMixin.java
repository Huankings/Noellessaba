package org.agmas.noellesroles.mixin.roles.hunter;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.hunter.HunterConstants;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 追猎者举刀疾跑时的移动速度加成。
 */
@Mixin(value = PlayerEntity.class, priority = 2500)
public abstract class HunterSprintMixin extends LivingEntity {

    protected HunterSprintMixin(@NotNull EntityType<? extends LivingEntity> entityType, @NotNull World world) {
        super(entityType, world);
    }

    @ModifyReturnValue(method = "getMovementSpeed", at = @At("RETURN"))
    private float noellesroles$setHunterSprintSpeed(float original) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.HUNTER) || !player.isUsingItem() || !player.isSprinting()) {
            return original;
        }

        ItemStack stack = player.getActiveItem();
        /*
         * kinssaba 用 UseAction.SPEAR 判定“持刀蓄力”。
         * 迁入后额外限制为 Wathe 匕首或 Noelles 猎刀，避免追猎者拿到其它 SPEAR 动作物品时误吃加速。
         */
        if ((stack.isOf(WatheItems.KNIFE) || stack.isOf(ModItems.HUNTING_KNIFE))
                && stack.getItem().getUseAction(stack) == UseAction.SPEAR) {
            return HunterConstants.HUNTER_SPRINT_MOVEMENT_SPEED;
        }
        return original;
    }
}
