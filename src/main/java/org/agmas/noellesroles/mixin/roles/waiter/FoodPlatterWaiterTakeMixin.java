package org.agmas.noellesroles.mixin.roles.waiter;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.tray.TrayEffectHandler;
import dev.doctor4t.wathe.api.tray.TrayEffectRegistry;
import dev.doctor4t.wathe.block.FoodPlatterBlock;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.TrayEffectUtils;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.waiter.WaiterConstants;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 服务员专用的托盘取物逻辑。
 *
 * <p>Wathe 托盘原逻辑通常是一次取走一份随机物品；服务员需求要求“背包/主手中同类物品最多 2 份”，
 * 同时如果托盘上存在毒药或 NoellesRoles 试剂效果，取出来的那一份必须保留自己的组件属性。
 * 因此这里在 FoodPlatterBlock#onUse 的开头拦截服务员空手取物流程。</p>
 */
@Mixin(FoodPlatterBlock.class)
public abstract class FoodPlatterWaiterTakeMixin {
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void noellesroles$takeWaiterTrayItem(
            BlockState state,
            @NotNull World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        /*
         * 只改服务端、服务员、主手空手、目标方块确实是 Wathe 托盘的情况。
         * 非服务员和手上有物品时全部放回 Wathe 原逻辑，避免影响普通玩家摆盘或拿取。
         */
        if (world.isClient
                || !(player instanceof ServerPlayerEntity serverPlayer)
                || !serverPlayer.getStackInHand(Hand.MAIN_HAND).isEmpty()
                || !(world.getBlockEntity(pos) instanceof BeveragePlateBlockEntity plate)
                || !GameWorldComponent.KEY.get(world).isRole(serverPlayer, NoellesRoleRegistry.WAITER)) {
            return;
        }

        List<ItemStack> storedItems = plate.getStoredItems();
        if (storedItems.isEmpty()) {
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        List<ItemStack> eligibleItems = new ArrayList<>();
        for (ItemStack storedItem : storedItems) {
            // 同类判定只看 item 类型，不把毒药/试剂 NBT 算作不同类；这样“同类最多 2 份”符合需求。
            if (countInventoryItems(serverPlayer, storedItem) < WaiterConstants.MAX_TRAY_ITEM_COUNT) {
                eligibleItems.add(storedItem);
            }
        }

        if (eligibleItems.isEmpty()) {
            // 托盘里有物品但服务员同类都满 2 份时，吃掉交互但不给物品，防止继续走 Wathe 默认取物绕过限制。
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // 在仍可拿的物品中随机抽 1 份，并强制单个堆叠，方便后续每一份独立保留毒药/试剂组件。
        ItemStack randomItem = eligibleItems.get(world.random.nextInt(eligibleItems.size())).copy();
        randomItem.setCount(1);
        randomItem.set(DataComponentTypes.MAX_STACK_SIZE, 1);

        /*
         * Wathe 托盘的毒药/试剂效果存在托盘方块实体上。
         * 服务员取出物品时要把这些状态转移到 ItemStack 自己身上，这样之后递给玩家时能按“这一份物品”的属性生效。
         */
        String poisoner = plate.getPoisoner();
        String trayEffect = plate.getTrayEffect();
        String trayEffectOwner = plate.getTrayEffectOwner();
        if (poisoner != null) {
            randomItem.set(WatheDataComponentTypes.POISONER, poisoner);
            plate.setPoisoner(null);
        }
        if (trayEffect != null) {
            TrayEffectUtils.attachTrayEffect(randomItem, trayEffect, trayEffectOwner);
            plate.clearTrayEffect();
        }

        serverPlayer.playSoundToPlayer(SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 1.0F, 1.0F);
        serverPlayer.setStackInHand(Hand.MAIN_HAND, randomItem);

        NbtCompound extra = new NbtCompound();
        extra.putString("item_name", net.minecraft.text.Text.Serialization.toJsonString(randomItem.getName(), serverPlayer.getRegistryManager()));
        extra.putBoolean("is_drink_plate", plate.isDrink());
        if (trayEffect != null) {
            // 回放和部分 TrayEffectHandler 需要知道本次从托盘带走的效果及其拥有者。
            extra.putString("tray_effect", trayEffect);
            if (trayEffectOwner != null) {
                try {
                    extra.putUuid("tray_effect_owner", UUID.fromString(trayEffectOwner));
                } catch (IllegalArgumentException ignored) {
                }
            }

            TrayEffectHandler effectHandler = net.minecraft.util.Identifier.tryParse(trayEffect) == null
                    ? null
                    : TrayEffectRegistry.getByEffectId(net.minecraft.util.Identifier.tryParse(trayEffect));
            if (effectHandler != null) {
                UUID owner = null;
                if (trayEffectOwner != null) {
                    try {
                        owner = UUID.fromString(trayEffectOwner);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                // 通知效果 handler“这份带效果的物品被服务员拿走了”，用于需要在取物瞬间记录额外数据的试剂。
                effectHandler.onTakeFromTray(serverPlayer, randomItem, owner, extra);
            }
        }
        GameRecordManager.recordPlatterTake(serverPlayer, Registries.ITEM.getId(randomItem.getItem()), pos, poisoner, extra);
        cir.setReturnValue(ActionResult.SUCCESS);
    }

    private static int countInventoryItems(ServerPlayerEntity player, ItemStack targetStack) {
        // PlayerInventory 包含主背包、护甲、副手等槽位；主手中的物品也包含在这些槽位里。
        int count = 0;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.getItem() == targetStack.getItem()) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
