package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 小偷悦灵全部回放事件的数据写入出口。 */
public final class ThiefAllayReplay {
    private ThiefAllayReplay() {
    }

    public static void recordSummon(ServerPlayerEntity thief, int count, ServerPlayerEntity target) {
        NbtCompound data = new NbtCompound();
        data.putInt("allay_count", count);
        data.putUuid("target_player", target.getUuid());
        GameRecordManager.recordGlobalEvent(thief.getServerWorld(), NoellesEventIds.THIEF_ALLAY_SUMMON_EVENT, thief, data);
    }

    public static void recordRetarget(ThiefAllayEntity allay, ServerPlayerEntity target) {
        NbtCompound data = base(allay);
        data.putUuid("target_player", target.getUuid());
        GameRecordManager.recordGlobalEvent(target.getServerWorld(), NoellesEventIds.THIEF_ALLAY_RETARGET_EVENT, null, data);
    }

    public static void recordStealSuccess(ThiefAllayEntity allay, ServerPlayerEntity target, ItemStack stack) {
        NbtCompound data = base(allay);
        data.putUuid("target_player", target.getUuid());
        putItem(data, stack, target.getServerWorld());
        GameRecordManager.recordGlobalEvent(target.getServerWorld(), NoellesEventIds.THIEF_ALLAY_STEAL_SUCCESS_EVENT, null, data);
    }

    public static void recordStealFail(ThiefAllayEntity allay, ServerPlayerEntity target) {
        NbtCompound data = base(allay);
        data.putUuid("target_player", target.getUuid());
        GameRecordManager.recordGlobalEvent(target.getServerWorld(), NoellesEventIds.THIEF_ALLAY_STEAL_FAIL_EVENT, null, data);
    }

    public static void recordDelivery(ThiefAllayEntity allay, ServerPlayerEntity thief, ItemStack stack) {
        NbtCompound data = base(allay);
        data.putUuid("thief_player", thief.getUuid());
        putItem(data, stack, thief.getServerWorld());
        GameRecordManager.recordGlobalEvent(thief.getServerWorld(), NoellesEventIds.THIEF_ALLAY_DELIVERY_EVENT, null, data);
    }

    public static void recordKilled(
            ThiefAllayEntity allay,
            @Nullable ServerPlayerEntity killer,
            @Nullable ItemStack weapon,
            @Nullable Identifier deathReason
    ) {
        if (!(allay.getWorld() instanceof ServerWorld world)) {
            return;
        }
        NbtCompound data = base(allay);
        if (weapon != null && !weapon.isEmpty()) {
            putItem(data, weapon, world);
        }
        if (deathReason != null) {
            data.putString("death_reason", deathReason.toString());
        }
        GameRecordManager.recordGlobalEvent(world, NoellesEventIds.THIEF_ALLAY_KILLED_EVENT, killer, data);
    }

    public static void recordOwnerPurge(ServerPlayerEntity thief) {
        GameRecordManager.recordGlobalEvent(
                thief.getServerWorld(),
                NoellesEventIds.THIEF_ALLAY_OWNER_PURGE_EVENT,
                thief,
                null
        );
    }

    private static NbtCompound base(ThiefAllayEntity allay) {
        NbtCompound data = new NbtCompound();
        data.putInt("allay_number", allay.getAllayNumber());
        data.putUuid("allay_owner", allay.getOwnerUuid());
        return data;
    }

    private static void putItem(NbtCompound data, @NotNull ItemStack stack, @NotNull ServerWorld world) {
        NbtCompound itemData = GameFunctions.createReplayItemData(world, stack);
        if (itemData != null) {
            for (String key : itemData.getKeys()) {
                data.put(key, itemData.get(key).copy());
            }
            return;
        }
        data.putString("item", Registries.ITEM.getId(stack.getItem()).toString());
        data.putString("item_name", Text.Serialization.toJsonString(stack.getName(), world.getRegistryManager()));
    }
}
