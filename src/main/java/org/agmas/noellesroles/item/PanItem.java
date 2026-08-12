package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.packet.item.PanC2SPacket;
import org.agmas.noellesroles.roles.cook.CookConstants;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 平底锅。
 *
 * <p>右键进入蓄力，松开时客户端只发送“我当前瞄准的目标 id”；
 * 服务器仍会重新校验手持、冷却、距离、存活状态，再真正眩晕目标。</p>
 */
public class PanItem extends Item {
    public PanItem(@NotNull Settings settings) {
        super(settings);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity player, @NotNull Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!GameFunctions.isPlayerSpectatingOrCreative(player) && player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        player.setCurrentHand(hand);
        player.playSound(WatheSounds.ITEM_KNIFE_PREPARE, 1.0F, 0.1F);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(@NotNull ItemStack stack, @NotNull World world, @NotNull LivingEntity livingEntity, int remainingUseTicks) {
        if (!(livingEntity instanceof PlayerEntity player)
                || player.isSpectator()
                || remainingUseTicks >= this.getMaxUseTime(stack, player) - CookConstants.PAN_MIN_USE_TICKS) {
            return;
        }

        if (world.isClient && remainingUseTicks > CookConstants.PAN_CLIENT_SEND_GRACE_TICKS) {
            EntityHitResult hitResult = WeaponTargetingApi.getAttackableAlivePlayerTarget(player, CookConstants.PAN_TARGET_RANGE);
            if (hitResult != null) {
                sendPacket(new PanC2SPacket(hitResult.getEntity().getId()));
            }
        }
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return CookConstants.PAN_MAX_USE_TICKS;
    }

    private static void sendPacket(@NotNull CustomPayload payload) {
        try {
            Class<?> networkingClass = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            Method sendMethod = networkingClass.getMethod("send", CustomPayload.class);
            sendMethod.invoke(null, payload);
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException | NoSuchMethodException ignored) {
            // main 源集不能硬依赖 client networking；客户端不可用时直接忽略。
        }
    }
}
