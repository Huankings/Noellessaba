package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityActionGuard;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * 小偷空手右键玩家偷取物品。
 */
public final class ThiefInteractionHandler {
    private static boolean initialized = false;

    private ThiefInteractionHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            if (!(player instanceof ServerPlayerEntity thief) || !(entity instanceof ServerPlayerEntity target)) {
                return ActionResult.PASS;
            }

            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(thief);
            if (!validateStealAttempt(thief, target, ability)) {
                return ActionResult.PASS;
            }

            handleThiefTakeItem(thief, target, ability);
            return ActionResult.CONSUME;
        });
    }

    private static void handleThiefTakeItem(ServerPlayerEntity thief, ServerPlayerEntity target, AbilityPlayerComponent ability) {
        /*
         * 只有在服务端验证通过、这次偷窃真正成立时，才记录“尝试偷取”事件。
         * 这样可以避免冷却中、距离不够等无效交互也污染回放。
         */
        NbtCompound attemptExtra = new NbtCompound();
        attemptExtra.putUuid("target_player", target.getUuid());
        GameRecordManager.recordGlobalEvent(thief.getServerWorld(), NoellesEventIds.THIEF_ATTEMPT_EVENT, thief, attemptExtra);

        List<Integer> stealableSlots = getStealableSlots(target);
        if (stealableSlots.isEmpty()) {
            ability.setCooldown(ThiefConstants.FAILED_STEAL_COOLDOWN_TICKS);
            thief.sendMessage(Text.translatable("message.noellesroles.thief.no_items", target.getName()).withColor(NoellesRoleRegistry.THIEF.color()), false);

            NbtCompound failExtra = new NbtCompound();
            failExtra.putUuid("target_player", target.getUuid());
            GameRecordManager.recordGlobalEvent(thief.getServerWorld(), NoellesEventIds.THIEF_FAIL_EVENT, thief, failExtra);

            thief.playSoundToPlayer(SoundEvents.ITEM_DYE_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
            return;
        }

        int slotIndex = stealableSlots.get(thief.getRandom().nextInt(stealableSlots.size()));
        ItemStack stolenItem = target.getInventory().main.get(slotIndex).copy();
        ItemStack replaySnapshot = stolenItem.copy();

        target.getInventory().main.set(slotIndex, ItemStack.EMPTY);
        target.getInventory().markDirty();

        /*
         * Inventory#insertStack 会修改传入的 ItemStack。
         * 因此回放数据必须使用 replaySnapshot，避免成功塞进背包后显示成“空气”。
         */
        if (!thief.getInventory().insertStack(stolenItem)) {
            thief.dropItem(stolenItem, true, false);
        }
        thief.getInventory().markDirty();

        ability.setCooldown(ThiefConstants.STEAL_COOLDOWN_TICKS);
        ThiefItemTracker.refresh(thief.getServerWorld());

        NbtCompound successExtra = new NbtCompound();
        successExtra.putUuid("target_player", target.getUuid());
        successExtra.putString("item", Registries.ITEM.getId(replaySnapshot.getItem()).toString());
        successExtra.putString("item_name", Text.Serialization.toJsonString(replaySnapshot.getName(), thief.getRegistryManager()));
        GameRecordManager.recordGlobalEvent(thief.getServerWorld(), NoellesEventIds.THIEF_SUCCESS_EVENT, thief, successExtra);
        thief.sendMessage(Text.translatable("message.noellesroles.thief.success", target.getName()).withColor(NoellesRoleRegistry.THIEF.color()), false);
        thief.playSoundToPlayer(SoundEvents.ITEM_ARMOR_EQUIP_CHAIN.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    private static List<Integer> getStealableSlots(ServerPlayerEntity target) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < target.getInventory().main.size(); i++) {
            ItemStack stack = target.getInventory().main.get(i);
            if (!stack.isEmpty() && ThiefItemRules.canTake(stack.getItem())) {
                slots.add(i);
            }
        }
        return slots;
    }

    public static boolean validateStealAttempt(PlayerEntity thief, PlayerEntity target, AbilityPlayerComponent ability) {
        if (thief == null || target == null || thief.isRemoved() || target.isRemoved()) {
            return false;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(thief.getWorld());
        if (!gameWorld.isRole(thief, NoellesRoleRegistry.THIEF)) {
            return false;
        }
        if (thief instanceof ServerPlayerEntity serverThief && DualPersonalityActionGuard.isDormant(serverThief)) {
            return false;
        }
        if (GameFunctions.isPlayerEliminated(target) || GameFunctions.isPlayerEliminated(thief)) {
            return false;
        }
        if (thief.getUuid().equals(target.getUuid())) {
            return false;
        }
        if (!thief.getWorld().getRegistryKey().equals(target.getWorld().getRegistryKey())) {
            return false;
        }
        if (ability.cooldown > 0 || !thief.getMainHandStack().isEmpty()) {
            return false;
        }
        if (!validateDistance(thief, target)) {
            return false;
        }
        return hasClearSight(thief, target);
    }

    public static boolean validateDistance(PlayerEntity thief, PlayerEntity target) {
        if (thief == null || target == null || thief.isRemoved() || target.isRemoved()) {
            return false;
        }
        if (!thief.getWorld().getRegistryKey().equals(target.getWorld().getRegistryKey())) {
            return false;
        }
        double maxDistance = thief.getWorld().isClient() ? ThiefConstants.CLIENT_STEAL_RANGE : ThiefConstants.SERVER_STEAL_RANGE;
        return thief.squaredDistanceTo(target) <= MathHelper.square(maxDistance);
    }

    private static boolean hasClearSight(PlayerEntity observer, Entity target) {
        Vec3d start = observer.getCameraPosVec(1.0F);
        Box targetBox = target.getBoundingBox();
        Vec3d lowerBody = new Vec3d(target.getX(), targetBox.minY + target.getHeight() * 0.25D, target.getZ());

        return hasUnblockedRay(observer, start, target.getCameraPosVec(1.0F))
                || hasUnblockedRay(observer, start, targetBox.getCenter())
                || hasUnblockedRay(observer, start, lowerBody);
    }

    private static boolean hasUnblockedRay(PlayerEntity observer, Vec3d start, Vec3d end) {
        HitResult hitResult = observer.getWorld().raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                observer
        ));
        return hitResult.getType() == HitResult.Type.MISS;
    }
}
