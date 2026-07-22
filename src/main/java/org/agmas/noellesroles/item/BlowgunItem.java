package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.packet.item.BlowgunC2SPacket;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerConstants;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 制毒师吹矢。
 *
 * <p>真正命中结算放在服务端包里，这个物品类只负责播放使用反馈、写入冷却并把客户端瞄准目标发给服务端。</p>
 */
public class BlowgunItem extends Item {
    public BlowgunItem(@NotNull Settings settings) {
        super(settings);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity player, @NotNull Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
        if (!ignoresCooldown && player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        /*
         * 旁观/创造玩家常用于调试命中和回放，按需求不写冷却。
         * 正常存活玩家即使没有瞄到人，也和 kinssaba 原实现一样会进入使用冷却。
         */
        if (!ignoresCooldown) {
            player.getItemCooldownManager().set(ModItems.BLOWGUN, DrugmakerConstants.BLOWGUN_COOLDOWN_TICKS);
        }
        player.playSound(SoundEvents.ENTITY_PUFFER_FISH_BLOW_OUT, 0.5F, 1.5F);

        if (world.isClient) {
            HitResult hitResult = ProjectileUtil.getCollision(
                    player,
                    entity -> entity instanceof PlayerEntity target && GameFunctions.isPlayerAliveAndSurvival(target),
                    DrugmakerConstants.BLOWGUN_TARGET_RANGE
            );
            if (hitResult instanceof EntityHitResult entityHitResult) {
                sendPacket(new BlowgunC2SPacket(entityHitResult.getEntity().getId()));
            }
        }

        return TypedActionResult.success(stack, world.isClient);
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
