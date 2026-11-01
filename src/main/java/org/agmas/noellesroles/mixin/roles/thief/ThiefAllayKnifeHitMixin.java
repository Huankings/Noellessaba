package org.agmas.noellesroles.mixin.roles.thief;

import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.roles.thief.ThiefAllayEntity;
import org.agmas.noellesroles.roles.thief.ThiefConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Wathe 匕首服务端包对小偷悦灵的窄兼容。 */
@Mixin(value = KnifeStabPayload.Receiver.class, priority = ThiefConstants.ALLAY_KNIFE_MIXIN_PRIORITY)
public abstract class ThiefAllayKnifeHitMixin {
    @Inject(
            method = "receive(Ldev/doctor4t/wathe/util/KnifeStabPayload;Lnet/fabricmc/fabric/api/networking/v1/ServerPlayNetworking$Context;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$killThiefAllay(
            KnifeStabPayload payload,
            ServerPlayNetworking.Context context,
            CallbackInfo ci
    ) {
        ServerPlayerEntity attacker = context.player();
        ItemStack weapon = attacker.getMainHandStack().copy();
        if (!weapon.isOf(WatheItems.KNIFE)
                || !(attacker.getServerWorld().getEntityById(payload.target()) instanceof ThiefAllayEntity allay)
                || attacker.distanceTo(allay) > ThiefConstants.ALLAY_KNIFE_HIT_RANGE
                || !TargetVisibilityApi.canAttackEntity(attacker, allay)) {
            return;
        }

        allay.killByWeapon(attacker, weapon, GameConstants.DeathReasons.KNIFE);
        allay.playSound(WatheSounds.ITEM_KNIFE_STAB, 1.0F, 1.0F);
        attacker.swingHand(Hand.MAIN_HAND, true);

        GameWorldComponent game = GameWorldComponent.KEY.get(attacker.getWorld());
        if (!attacker.isCreative() && game.getGameMode() != WatheGameModes.LOOSE_ENDS) {
            attacker.getItemCooldownManager().set(
                    WatheItems.KNIFE,
                    GameConstants.ITEM_COOLDOWNS.getOrDefault(WatheItems.KNIFE, 0)
            );
        }
        ci.cancel();
    }
}
