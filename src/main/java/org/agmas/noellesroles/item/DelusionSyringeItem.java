package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import dev.doctor4t.wathe.game.GameFunctions;
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
import org.agmas.noellesroles.packet.item.DelusionSyringeC2SPacket;
import org.agmas.noellesroles.roles.dreamer.DreamerConstants;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 幻觉注剂。
 *
 * <p>它采用“蓄力举起 + 客户端发送准心目标 + 服务端重新校验”的结构，
 * 这样既能复用 Wathe 的目标选择语义，又不会把真实幻觉状态交给客户端决定。</p>
 */
public final class DelusionSyringeItem extends Item {
    public DelusionSyringeItem(@NotNull Settings settings) {
        super(settings);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(
            @NotNull World world,
            @NotNull PlayerEntity player,
            @NotNull Hand hand
    ) {
        ItemStack stack = player.getStackInHand(hand);
        boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);

        /*
         * 创造/旁观语义玩家主要用于调试，因此允许它们在冷却期间继续举起针剂；
         * 正常玩家仍由 ItemCooldownManager 在开始蓄力时阻止重复使用。
         */
        if (!ignoresCooldown && player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        player.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(
            @NotNull ItemStack stack,
            @NotNull World world,
            @NotNull LivingEntity livingEntity,
            int remainingUseTicks
    ) {
        if (!(livingEntity instanceof PlayerEntity player) || !world.isClient) {
            return;
        }

        /*
         * 没有最短蓄力时间：只要松手时准心对准合法目标，就发送注射请求。
         * 真正的距离、存活、目标可攻击性和冷却检查全部在服务端重做。
         */
        EntityHitResult hitResult = WeaponTargetingApi.getAttackableAlivePlayerTarget(
                player,
                DreamerConstants.DELUSION_SYRINGE_TARGET_RANGE
        );
        if (hitResult != null) {
            sendPacket(new DelusionSyringeC2SPacket(hitResult.getEntity().getId()));
        }
    }

    @Override
    public @NotNull UseAction getUseAction(@NotNull ItemStack stack) {
        return UseAction.SPEAR;
    }

    @Override
    public int getMaxUseTime(@NotNull ItemStack stack, @NotNull LivingEntity livingEntity) {
        return DreamerConstants.DELUSION_SYRINGE_MAX_USE_TICKS;
    }

    private static void sendPacket(@NotNull CustomPayload payload) {
        try {
            /*
             * 物品类位于 main source，不能直接硬依赖 Fabric client networking。
             * 这里沿用 NoellesRoles 现有蓄力物品的反射发包方式，客户端环境才真正调用该类。
             */
            Class<?> networkingClass = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            Method sendMethod = networkingClass.getMethod("send", CustomPayload.class);
            sendMethod.invoke(null, payload);
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException | NoSuchMethodException ignored) {
            // 服务端环境没有客户端网络类时不做任何事情。
        }
    }
}
