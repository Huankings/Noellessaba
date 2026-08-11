package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.api.event.AllowPlayerOpenLockedDoor;
import dev.doctor4t.wathe.block.SmallDoorBlock;
import dev.doctor4t.wathe.block.TrainDoorBlock;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import dev.doctor4t.wathe.cca.TrainWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.Nullable;

/**
 * 彩虹斧。
 *
 * <p>左键连杀由客户端攻击 mixin 发包，服务端包做权威结算。
 * 右键门时只在“这扇门正常打不开”或玩家蹲下时撬门，避免覆盖普通开门交互。</p>
 */
public class ColorfulAxeItem extends Item {
    public ColorfulAxeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return super.useOnBlock(context);
        }

        ActionResult result = tryPryWatheDoor(context.getWorld(), context.getBlockPos(), player);
        return result == ActionResult.PASS ? super.useOnBlock(context) : result;
    }

    /**
     * 彩虹斧撬开 Wathe 门的共享入口。
     *
     * <p>这个方法既会被物品的 useOnBlock 调用，也会被门方块 onUse 的 HEAD mixin 调用。
     * 后者是必要的：Wathe 锁门/行驶中车厢门会在方块交互阶段直接返回 FAIL，
     * 如果只依赖 Item#useOnBlock，彩虹斧会根本拿不到撬门机会。</p>
     */
    public static ActionResult tryPryWatheDoor(World world, BlockPos clickedPos, PlayerEntity player) {
        if (!player.getMainHandStack().isOf(ModItems.COLORFUL_AXE)) {
            return ActionResult.PASS;
        }

        DoorLookup lookup = findDoor(world, clickedPos);
        if (lookup == null || lookup.door().isBlasted()) {
            return ActionResult.PASS;
        }

        boolean forcePry = player.isSneaking();
        boolean normallyBlocked = isNormallyBlocked(world, lookup, player);
        if (!forcePry && !normallyBlocked) {
            return ActionResult.PASS;
        }

        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        /*
         * 与 Wathe 撬棍保持同一套结算：播放撬门声、挥手、blast 门。
         * 门的普通开关声会在 SpringTrapDoorSoundContext 记录的调用栈内被静音，
         * 因此玩家只会听到“彩虹斧撬门”的动作音。
         */
        if (!world.isClient) {
            world.playSound(null, clickedPos, WatheSounds.ITEM_CROWBAR_PRY, SoundCategory.BLOCKS, 2.5F, 1.0F);
            player.swingHand(Hand.MAIN_HAND, true);
            lookup.door().blast();
            if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                NbtCompound extra = new NbtCompound();
                extra.putString("mode", "pry");
                GameRecordManager.putBlockPos(extra, "pos", lookup.lowerPos());
                GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(ModItems.COLORFUL_AXE), null, extra);
            }
        }
        return ActionResult.CONSUME;
    }

    private static @Nullable DoorLookup findDoor(World world, BlockPos pos) {
        BlockEntity entity = world.getBlockEntity(pos);
        BlockPos lowerPos = pos;
        BlockState state = world.getBlockState(pos);
        if (!(entity instanceof DoorBlockEntity)) {
            lowerPos = pos.down();
            entity = world.getBlockEntity(lowerPos);
            state = world.getBlockState(lowerPos);
        } else if (state.contains(SmallDoorBlock.HALF) && state.get(SmallDoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            lowerPos = pos.down();
            entity = world.getBlockEntity(lowerPos);
            state = world.getBlockState(lowerPos);
        }
        return entity instanceof DoorBlockEntity door ? new DoorLookup(lowerPos, state, door) : null;
    }

    private static boolean isNormallyBlocked(World world, DoorLookup lookup, PlayerEntity player) {
        DoorBlockEntity door = lookup.door();
        if (door.isOpen()) {
            return false;
        }
        if (player.isCreative() || AllowPlayerOpenLockedDoor.EVENT.invoker().allowOpen(player)) {
            return false;
        }
        if (door.isJammed()) {
            return true;
        }
        if (lookup.state().getBlock() instanceof TrainDoorBlock && TrainWorldComponent.KEY.get(world).getSpeed() != 0) {
            return !player.getMainHandStack().isOf(WatheItems.LOCKPICK);
        }
        if (!door.getKeyName().isEmpty()) {
            if (player.getMainHandStack().isOf(WatheItems.LOCKPICK)) {
                return false;
            }
            if (!player.getMainHandStack().isOf(WatheItems.KEY)) {
                return true;
            }
            LoreComponent lore = player.getMainHandStack().get(DataComponentTypes.LORE);
            return lore == null || lore.lines().isEmpty() || !lore.lines().getFirst().getString().equals(door.getKeyName());
        }
        return false;
    }

    private record DoorLookup(BlockPos lowerPos, BlockState state, DoorBlockEntity door) {
    }
}
