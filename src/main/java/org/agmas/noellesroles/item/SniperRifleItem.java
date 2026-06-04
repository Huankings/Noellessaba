package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.item.RevolverItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.packet.item.SniperRifleShootC2SPacket;
import org.agmas.noellesroles.roles.rememberer.RemembererConstants;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 狙击枪。
 *
 * <p>它保留左轮的枪口火花与手感，但命中结算完全走自定义弹道。</p>
 */
public class SniperRifleItem extends Item {

    public SniperRifleItem(Settings settings) {
        super(settings);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity user, @NotNull Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }
        if (stack.getOrDefault(ModItems.SNIPER_AMMO, 0) <= 0) {
            return TypedActionResult.fail(stack);
        }

        if (world.isClient) {
            sendShootPacket(SniperRifleShootC2SPacket.fromLook(user.getRotationVector()));
            user.setPitch(user.getPitch() - RemembererConstants.SNIPER_RECOIL_PITCH);
            RevolverItem.spawnHandParticle();
        }
        return TypedActionResult.consume(stack);
    }

    @Override
    public boolean onClicked(
            ItemStack stack,
            ItemStack otherStack,
            net.minecraft.screen.slot.Slot slot,
            ClickType clickType,
            PlayerEntity player,
            net.minecraft.inventory.StackReference cursorStackReference
    ) {
        if (clickType != ClickType.RIGHT || !otherStack.isOf(ModItems.SNIPER_RIFLE_BULLET)) {
            return false;
        }

        int currentAmmo = stack.getOrDefault(ModItems.SNIPER_AMMO, 0);
        if (currentAmmo >= RemembererConstants.SNIPER_MAX_AMMO) {
            return false;
        }

        stack.set(ModItems.SNIPER_AMMO, currentAmmo + 1);
        if (!player.isCreative()) {
            otherStack.decrement(1);
            cursorStackReference.set(otherStack);
        }

        if (!player.getWorld().isClient() && player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            net.minecraft.nbt.NbtCompound extra = new net.minecraft.nbt.NbtCompound();
            extra.putInt("current_ammo", currentAmmo + 1);
            extra.putInt("max_ammo", RemembererConstants.SNIPER_MAX_AMMO);
            dev.doctor4t.wathe.record.GameRecordManager.recordGlobalEvent(
                    serverPlayer.getServerWorld(),
                    org.agmas.noellesroles.Noellesroles.REMEMBERER_SNIPER_RELOADED_EVENT,
                    serverPlayer,
                    extra
            );
            serverPlayer.playerScreenHandler.sendContentUpdates();
        }
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.empty());
        tooltip.add(Text.translatable(
                "item.noellesroles.sniper_rifle.ammo",
                stack.getOrDefault(ModItems.SNIPER_AMMO, 0),
                RemembererConstants.SNIPER_MAX_AMMO
        ));
    }

    /**
     * 仅用于准心高亮的“可见目标”检测。
     *
     * <p>这里刻意不做穿墙，因为用户只要求“没有穿墙时对准玩家才高亮”。</p>
     */
    public static HitResult getVisibleTarget(@NotNull PlayerEntity user) {
        return ProjectileUtil.getCollision(
                user,
                entity -> entity instanceof PlayerEntity player && GameFunctions.isPlayerAliveAndSurvival(player),
                (float) RemembererConstants.SNIPER_RANGE_BLOCKS
        );
    }

    private static void sendShootPacket(@NotNull SniperRifleShootC2SPacket payload) {
        try {
            Class<?> networkingClass = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            Method sendMethod = networkingClass.getMethod("send", net.minecraft.network.packet.CustomPayload.class);
            sendMethod.invoke(null, payload);
        } catch (ReflectiveOperationException ignored) {
            // 保持 main 源集不硬依赖 client networking。
        }
    }
}
