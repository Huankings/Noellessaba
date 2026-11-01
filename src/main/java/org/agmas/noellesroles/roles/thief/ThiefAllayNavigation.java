package org.agmas.noellesroles.roles.thief;

import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Set;

/** 使用门穿越节点生成器的悦灵飞行导航。 */
public final class ThiefAllayNavigation extends BirdNavigation {
    public ThiefAllayNavigation(MobEntity entity, World world) {
        super(entity, world);
        setCanPathThroughDoors(true);
        setCanEnterOpenDoors(true);
        setCanSwim(true);
    }

    @Override
    protected PathNodeNavigator createPathNodeNavigator(int range) {
        this.nodeMaker = new ThiefAllayPathNodeMaker();
        this.nodeMaker.setCanOpenDoors(true);
        this.nodeMaker.setCanEnterOpenDoors(true);
        return new PathNodeNavigator(this.nodeMaker, range);
    }

    /** 使用比原版方块目标更大的缓存范围规划移动实体路径。 */
    public Path findExtendedPathTo(Entity destination, int accuracy) {
        return this.findPathTo(
                Set.of(destination.getBlockPos()),
                ThiefConstants.ALLAY_EXTENDED_PATH_SEARCH_RANGE,
                true,
                accuracy
        );
    }

    /** 使用扩大范围一次性寻找多个目标附近落点。 */
    public Path findExtendedPathTo(Set<BlockPos> destinations, int accuracy) {
        return this.findPathTo(
                destinations,
                ThiefConstants.ALLAY_EXTENDED_PATH_SEARCH_RANGE,
                false,
                accuracy
        );
    }

    /** 使用扩大范围规划单个绕行锚点。 */
    public Path findExtendedPathTo(BlockPos destination, int accuracy) {
        return this.findExtendedPathTo(Set.of(destination), accuracy);
    }
}
