package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.item.RevolverItem;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterConstants;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * 赏金手枪。
 *
 * <p>不能只继承 Wathe 左轮的 use 逻辑，因为 RevolverItem#use 内部会固定调用 20 格射程。
 * 这里保留 Wathe 的客户端开火包、后坐力和手部粒子，只把客户端射线范围改成赏金手枪自己的 15 格。</p>
 */
public class BountyPistolItem extends RevolverItem {
    public BountyPistolItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity user, Hand hand) {
        if (world.isClient) {
            HitResult collision = getGunTarget(user);
            if (collision instanceof EntityHitResult entityHitResult) {
                Entity target = entityHitResult.getEntity();
                sendShootPacket(new GunShootPayload(target.getId()));
            } else {
                sendShootPacket(new GunShootPayload(-1));
            }
            user.setPitch(user.getPitch() - 4);
            RevolverItem.spawnHandParticle();
        }
        return TypedActionResult.consume(user.getStackInHand(hand));
    }

    public static HitResult getGunTarget(PlayerEntity user) {
        return ProjectileUtil.getCollision(
                user,
                entity -> entity instanceof PlayerEntity player && GameFunctions.isPlayerAliveAndSurvival(player),
                BountyHunterConstants.BOUNTY_PISTOL_RANGE_BLOCKS
        );
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
