package org.agmas.noellesroles.roles.thief;

import net.minecraft.entity.ai.pathing.BirdPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathContext;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;

/** 把门类方块视作可飞行节点的悦灵专属路径节点生成器。 */
public final class ThiefAllayPathNodeMaker extends BirdPathNodeMaker {
    @Override
    public PathNodeType getDefaultNodeType(PathContext context, int x, int y, int z) {
        if (ThiefAllayDoorRules.canPassThrough(context.getBlockState(new BlockPos(x, y, z)))) {
            return PathNodeType.OPEN;
        }
        return super.getDefaultNodeType(context, x, y, z);
    }
}
