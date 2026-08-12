package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import dev.doctor4t.wathe.item.DerringerItem;
import dev.doctor4t.wathe.item.RevolverItem;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * 赏金德林加。
 *
 * <p>Wathe 原版德林加会把 USED 数据写在物品上，导致开一枪后需要装填。
 * 赏金德林加明确要求像左轮一样“冷却结束即可继续开枪”，所以这里只复用德林加 7 格射线和粒子手感，
 * 不写 WatheDataComponentTypes.USED。</p>
 */
public class BountyDerringerItem extends RevolverItem {
    public BountyDerringerItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            EntityHitResult collision = getGunAttackTarget(user, stack);
            if (collision != null) {
                Entity target = collision.getEntity();
                sendShootPacket(new GunShootPayload(target.getId()));
            } else {
                sendShootPacket(new GunShootPayload(-1));
            }
            user.setPitch(user.getPitch() - 4);
            DerringerItem.spawnHandParticle();
        }
        return TypedActionResult.consume(stack);
    }

    public static HitResult getGunTarget(PlayerEntity user) {
        return getGunTarget(user, user.getMainHandStack());
    }

    public static HitResult getGunTarget(PlayerEntity user, ItemStack stack) {
        return WeaponTargetingApi.resolveVisibleGunTarget(user, stack, BountyHunterConstants.BOUNTY_DERRINGER_RANGE_BLOCKS);
    }

    /**
     * 只用于开火发包的真实命中目标。
     *
     * <p>德林加的准心显示仍然复用 {@link #getGunTarget(PlayerEntity)}，
     * 这样尸体伪装不会被高亮成可锁定目标；真正开枪时则允许命中伪装尸体。</p>
     */
    public static @Nullable EntityHitResult getGunAttackTarget(PlayerEntity user) {
        return getGunAttackTarget(user, user.getMainHandStack());
    }

    public static @Nullable EntityHitResult getGunAttackTarget(PlayerEntity user, ItemStack stack) {
        HitResult hitResult = WeaponTargetingApi.resolveAttackableGunTarget(user, stack, BountyHunterConstants.BOUNTY_DERRINGER_RANGE_BLOCKS);
        return hitResult instanceof EntityHitResult entityHitResult ? entityHitResult : null;
    }

    private static void sendShootPacket(@NotNull GunShootPayload payload) {
        try {
            Class<?> networkingClass = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            Method sendMethod = networkingClass.getMethod("send", net.minecraft.network.packet.CustomPayload.class);
            sendMethod.invoke(null, payload);
        } catch (ReflectiveOperationException ignored) {
            // 保持 main 源集不硬依赖 client networking。
        }
    }
}
