package org.agmas.noellesroles.item;

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
import org.agmas.noellesroles.packet.item.BloodAxeStabC2SPacket;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapConstants;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 血斧。
 *
 * <p>右键蓄力刺杀参考 Wathe 匕首，但把所有权威判断留在服务端包里。
 * main 源集不能直接引用 client networking，因此客户端发包和猎刀一样通过反射调用。</p>
 */
public class BloodAxeItem extends Item {
    public BloodAxeItem(Settings settings) {
        super(settings);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity user, @NotNull Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!GameFunctions.isPlayerSpectatingOrCreative(user) && user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }
        user.setCurrentHand(hand);
        user.playSound(WatheSounds.ITEM_KNIFE_PREPARE, 1.0F, 1.0F);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(@NotNull ItemStack stack, @NotNull World world, @NotNull LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player) || player.isSpectator()) {
            return;
        }

        int usedTicks = this.getMaxUseTime(stack, player) - remainingUseTicks;
        if (usedTicks < SpringTrapConstants.BLOOD_AXE_MIN_USE_TICKS) {
            return;
        }

        if (world.isClient) {
            EntityHitResult target = SpringTrapTargeting.getPlayerTarget(player, SpringTrapConstants.BLOOD_AXE_TARGET_RANGE);
            if (target != null) {
                sendPacket(new BloodAxeStabC2SPacket(target.getEntity().getId()));
            }
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return SpringTrapConstants.BLOOD_AXE_MAX_USE_TICKS;
    }

    @Override
    public @NotNull UseAction getUseAction(@NotNull ItemStack stack) {
        return UseAction.SPEAR;
    }

    private static void sendPacket(@NotNull CustomPayload payload) {
        try {
            Class<?> networkingClass = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            Method sendMethod = networkingClass.getMethod("send", CustomPayload.class);
            sendMethod.invoke(null, payload);
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException | NoSuchMethodException ignored) {
            // 服务端或数据生成环境没有 client networking；这里静默跳过。
        }
    }
}
