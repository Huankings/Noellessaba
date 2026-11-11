package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.api.door.DoorApi;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.AdventureUsable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ToolboxItem extends Item implements AdventureUsable {
    public ToolboxItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return super.useOnBlock(context);
        }

        DoorBlockEntity door = DoorApi.findDoor(world, pos);
        if (door == null || !DoorApi.inspect(door).needsRepair()) {
            return super.useOnBlock(context);
        }

        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        /*
         * 工具箱与 kinssaba 扳手的设计不同：工具箱修复后把整个门组恢复为正常关闭状态。
         * 由 Wathe DoorApi 同时清除两侧 blasted/jammed 并写入同一个 OPEN，避免修完后双门一开一关。
         */
        DoorApi.OperationResult result = DoorApi.repair(door, DoorApi.RepairMode.CLOSE);
        if (!result.applied()) {
            return ActionResult.PASS;
        }

        if (result.wasBlasted()) {
            world.playSound(null, door.getPos().getX() + 0.5, door.getPos().getY() + 1, door.getPos().getZ() + 0.5,
                    WatheSounds.ITEM_CROWBAR_PRY, SoundCategory.BLOCKS, 2.5f, 1.0f);
        } else if (result.wasJammed()) {
            world.playSound(null, door.getPos().getX() + 0.5, door.getPos().getY() + 1, door.getPos().getZ() + 0.5,
                    WatheSounds.ITEM_LOCKPICK_DOOR, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }

        if (!player.isCreative()) {
            int cooldown = GameConstants.ITEM_COOLDOWNS.getOrDefault(this, 0);
            if (cooldown > 0) {
                player.getItemCooldownManager().set(this, cooldown);
            }
        }

        player.swingHand(Hand.MAIN_HAND, true);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.event(dev.doctor4t.wathe.record.GameRecordTypes.ITEM_USE)
                    .world(serverPlayer.getServerWorld())
                    .actor(serverPlayer)
                    .put("item", Registries.ITEM.getId(this).toString())
                    .putBool("repaired_blasted", result.wasBlasted())
                    .putBool("repaired_jammed", result.wasJammed())
                    .putInt("affected_doors", result.affectedDoors())
                    .record();
        }
        return ActionResult.CONSUME;
    }
}
