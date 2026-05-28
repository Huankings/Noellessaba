package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.block.SmallDoorBlock;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.AdventureUsable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
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

        // 获取点击位置的方块实体
        BlockEntity entity = world.getBlockEntity(pos);
        if (!(entity instanceof DoorBlockEntity)) {
            // 如果不是门实体，尝试下方一格（处理双门的上半部分）
            entity = world.getBlockEntity(pos.down());
        }

        if (entity instanceof DoorBlockEntity door && player != null) {
            // 记录当前需要修复的状态（用于决定播放哪种音效）
            boolean wasJammed = door.isJammed();
            boolean wasBlasted = door.isBlasted();

            // 只有存在至少一种需要修复的状态时才执行
            if ((wasJammed || wasBlasted) && !world.isClient) {
                // 获取下半部分的位置（用于邻居门判断）
                BlockPos lowerPos = pos;
                BlockState state = world.getBlockState(lowerPos);
                if (state.getBlock() instanceof SmallDoorBlock && state.get(SmallDoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                    lowerPos = lowerPos.down();
                    state = world.getBlockState(lowerPos);
                }

                // 同时清除当前门的卡住和永久撬开状态
                if (wasJammed) {
                    door.setJammed(0);
                }
                if (wasBlasted) {
                    door.setBlasted(false);
                }
                door.markDirty();

                // 清除邻居门的对应状态（如果有）
                if (state.getBlock() instanceof SmallDoorBlock) {
                    DoorBlockEntity neighbor = SmallDoorBlock.getNeighborDoorEntity(state, world, lowerPos);
                    if (neighbor != null) {
                        if (neighbor.isJammed()) {
                            neighbor.setJammed(0);
                        }
                        if (neighbor.isBlasted()) {
                            neighbor.setBlasted(false);
                        }
                        neighbor.markDirty();
                    }
                }

                // 播放音效：如果曾经被撬开，优先播放撬棍音效；否则播放开锁器音效（解除卡住）
                // 这样既符合解除卡住时的预期，又能明确反馈同时修复了撬开状态
                if (wasBlasted) {
                    world.playSound(null, lowerPos.getX() + 0.5, lowerPos.getY() + 1, lowerPos.getZ() + 0.5,
                            WatheSounds.ITEM_CROWBAR_PRY, SoundCategory.BLOCKS, 2.5f, 1.0f);
                } else if (wasJammed) {
                    world.playSound(null, lowerPos.getX() + 0.5, lowerPos.getY() + 1, lowerPos.getZ() + 0.5,
                            WatheSounds.ITEM_LOCKPICK_DOOR, SoundCategory.BLOCKS, 1.0f, 1.0f);
                }

                // 设置冷却（非创造模式）
                if (!player.isCreative()) {
                    int cooldown = GameConstants.ITEM_COOLDOWNS.getOrDefault(this, 0);
                    if (cooldown > 0) {
                        player.getItemCooldownManager().set(this, cooldown);
                    }
                }

                // 播放挥手动效（可选）
                player.swingHand(Hand.MAIN_HAND, true);
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    GameRecordManager.event(dev.doctor4t.wathe.record.GameRecordTypes.ITEM_USE)
                            .world(serverPlayer.getServerWorld())
                            .actor(serverPlayer)
                            .put("item", Registries.ITEM.getId(this).toString())
                            .putBool("repaired_blasted", wasBlasted)
                            .putBool("repaired_jammed", wasJammed)
                            .record();
                }

                return ActionResult.SUCCESS;
            }
        }

        return super.useOnBlock(context);
    }
}
