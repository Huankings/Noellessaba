package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.block.DoorPartBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.TrapdoorBlock;
import org.jetbrains.annotations.NotNull;

/** 悦灵允许穿越的门类方块白名单。 */
public final class ThiefAllayDoorRules {
    private ThiefAllayDoorRules() {
    }

    /**
     * 同时覆盖原版门、活板门、栅栏门，以及 Wathe 的小门和列车门。
     * 这里只改变悦灵自己的寻路与碰撞，不会打开、破坏或修改门方块状态。
     */
    public static boolean canPassThrough(@NotNull BlockState state) {
        Block block = state.getBlock();
        return block instanceof DoorBlock
                || block instanceof TrapdoorBlock
                || block instanceof FenceGateBlock
                || block instanceof DoorPartBlock;
    }
}
