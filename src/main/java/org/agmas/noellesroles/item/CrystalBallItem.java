package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.item.CrystalBallMarkC2SPacket;
import org.agmas.noellesroles.roles.prophet.ProphetConstants;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 水晶球。
 *
 * <p>实现要点：
 * 1. 使用手感按“手雷/飞斧”的长按蓄力方式来做；
 * 2. 标记判定范围按 wathe 原版 Knife 的 3 格距离；
 * 3. 真正的标记逻辑只在服务端执行，客户端这里只负责在蓄力结束后发包。</p>
 */
public class CrystalBallItem extends Item {

    public CrystalBallItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player) || player.isSpectator()) {
            return;
        }

        int usedTicks = this.getMaxUseTime(stack, user) - remainingUseTicks;
        if (usedTicks < ProphetConstants.CRYSTAL_BALL_CHARGE_TICKS) {
            return;
        }

        if (!world.isClient) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.PROPHET)) {
            return;
        }

        HitResult hitResult = getCrystalBallTarget(player);
        if (hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity) {
            boolean offHand = player.getActiveHand() == Hand.OFF_HAND;
            sendMarkPacket(entityHitResult.getEntity().getId(), offHand);
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    /**
     * 复用 Knife 的近距离对准检测方式，只是把筛选条件改成：
     * 必须是“当前这把游戏里的存活玩家”。
     */
    public static HitResult getCrystalBallTarget(@NotNull PlayerEntity user) {
        return ProjectileUtil.getCollision(user, entity -> isValidCrystalBallTarget(user, entity), ProphetConstants.CRYSTAL_BALL_RANGE);
    }

    /**
     * 返回当前蓄力进度，供客户端准星渲染复用。
     */
    public static float getChargeProgress(@NotNull PlayerEntity player) {
        if (!player.isUsingItem() || !player.getActiveItem().isOf(org.agmas.noellesroles.ModItems.CRYSTAL_BALL)) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) player.getItemUseTime() / ProphetConstants.CRYSTAL_BALL_CHARGE_TICKS);
    }

    private static boolean isValidCrystalBallTarget(@NotNull PlayerEntity user, Entity entity) {
        if (!(entity instanceof PlayerEntity target)) {
            return false;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(user.getWorld());
        return gameWorld.isRunning()
                && gameWorld.getRole(target) != null
                && GameFunctions.isPlayerAliveAndSurvival(target);
    }

    /**
     * 通过反射调用 ClientPlayNetworking.send。
     *
     * <p>这是为了兼容扩展职业模组把 client/main 源集拆开的结构，
     * 避免像 wathe 原版那样直接在主源码里硬引用 client 包导致服务端加载炸掉。</p>
     */
    private static void sendMarkPacket(int targetId, boolean offHand) {
        try {
            Class<?> networkingClass = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            Method sendMethod = networkingClass.getMethod("send", net.minecraft.network.packet.CustomPayload.class);
            sendMethod.invoke(null, new CrystalBallMarkC2SPacket(targetId, offHand));
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException | NoSuchMethodException ignored) {
        }
    }
}
