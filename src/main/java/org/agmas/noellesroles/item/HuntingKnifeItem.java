package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import dev.doctor4t.wathe.game.GameConstants;
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
import org.agmas.noellesroles.packet.item.HuntingKnifeC2SPacket;
import org.agmas.noellesroles.roles.hunter.HunterConstants;
import org.agmas.noellesroles.roles.hunter.HunterPlayerComponent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 猎刀。
 *
 * <p>主源码不能直接引用 Fabric client networking，所以和 NoellesRoles 刺刀/平底锅一样，
 * 客户端发包通过反射完成；服务端收到后还会重新校验命中是否合法。</p>
 */
public class HuntingKnifeItem extends Item {

    public HuntingKnifeItem(@NotNull Settings settings) {
        super(settings);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity player, @NotNull Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        /*
         * 调试时经常会用创造/旁观语义的玩家反复试刀。
         * Wathe 把这类玩家统一归到 isPlayerSpectatingOrCreative，所以这里让它们忽略猎刀冷却；
         * 正常存活玩家仍然完整遵守冷却，避免影响正式对局平衡。
         */
        if (!GameFunctions.isPlayerSpectatingOrCreative(player) && player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        HunterPlayerComponent.KEY.get(player).useHuntingKnife(player.isSprinting());
        player.setCurrentHand(hand);
        player.playSound(WatheSounds.ITEM_KNIFE_PREPARE, 1.0F, 1.0F);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(@NotNull ItemStack stack, @NotNull World world, @NotNull LivingEntity livingEntity, int remainingUseTicks) {
        if (!(livingEntity instanceof PlayerEntity player)
                || player.isSpectator()
                || remainingUseTicks >= this.getMaxUseTime(stack, player) - HunterConstants.HUNTING_KNIFE_MIN_USE_TICKS) {
            return;
        }

        setTemporaryCooldown(world, player);

        if (world.isClient && remainingUseTicks > HunterConstants.HUNTING_KNIFE_CLIENT_SEND_GRACE_TICKS) {
            EntityHitResult hitResult = WeaponTargetingApi.getAttackableAlivePlayerTarget(player, HunterConstants.HUNTING_KNIFE_TARGET_RANGE);
            if (hitResult != null) {
                sendPacket(new HuntingKnifeC2SPacket(hitResult.getEntity().getId()));
            }
        }
    }

    @Override
    public void usageTick(@NotNull World world, @NotNull LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (remainingUseTicks <= HunterConstants.HUNTING_KNIFE_CLIENT_SEND_GRACE_TICKS && entity instanceof PlayerEntity player) {
            player.stopUsingItem();
        }
    }

    @Override
    public UseAction getUseAction(@NotNull ItemStack stack) {
        return UseAction.SPEAR;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity livingEntity) {
        return HunterConstants.HUNTING_KNIFE_MAX_USE_TICKS;
    }

    public void setTemporaryCooldown(@NotNull World world, @NotNull PlayerEntity player) {
        if (world.isClient) {
            return;
        }

        HunterPlayerComponent hunter = HunterPlayerComponent.KEY.get(player);
        hunter.markReleasedForHit();
        if (GameFunctions.isPlayerAliveAndSurvival(player) && hunter.isSprinting) {
            /*
             * kinssaba 使用 GameConstants.getInTicks(0, knifeTicks / 10)：
             * 秒数是 tick/10，最终换算回 tick 后等于“举刀 tick * 2”，也就是文案里的举刀时间两倍冷却。
             */
            player.getItemCooldownManager().set(this, GameConstants.getInTicks(0, hunter.knifeTicks / 10));
        }
        hunter.stopHuntingKnife();
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
