package org.agmas.noellesroles.client.roles.lich;

import dev.doctor4t.wathe.block.SmallDoorBlock;
import dev.doctor4t.wathe.block_entity.SmallDoorBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.lich.LichConstants;

import java.util.HashSet;
import java.util.Set;

/**
 * 巫妖右下角能力 HUD。
 *
 * <p>这里仅做客户端提示：扫描本地已同步的 Wathe 小门数量并显示可用状态。
 * 真正能否释放、释放后修复/锁门哪些门，仍由服务端 {@code LichAbility} 在收到能力键包后权威判定。</p>
 */
public final class LichStatusHud {
    private LichStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/lich/status", NoellesRoleRegistry.LICH, context -> {
            if (NoellesrolesClient.abilityBind == null) {
                return;
            }

            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            Text line;
            if (ability.cooldown > 0) {
                line = Text.translatable("tip.noellesroles.lich.ability.cooldown", seconds(ability.cooldown));
            } else {
                int nearbyDoors = countNearbyDoors(context.player().getWorld(), context.player().getBlockPos());
                line = nearbyDoors <= LichConstants.DOOR_CONTROL_EMPTY_COUNT
                        ? Text.translatable("tip.noellesroles.lich.ability.no_doors", LichConstants.DOOR_CONTROL_RADIUS_BLOCKS)
                        : Text.translatable(
                        "tip.noellesroles.lich.ability.ready",
                        NoellesrolesClient.abilityBind.getBoundKeyLocalizedText(),
                        LichConstants.DOOR_CONTROL_RADIUS_BLOCKS,
                        nearbyDoors
                );
            }

            NoellesHudSupport.drawBottomRightLine(context, line, LichConstants.ROLE_COLOR);
        });
    }

    private static int seconds(int ticks) {
        return Math.max(
                LichConstants.DOOR_CONTROL_EMPTY_COUNT,
                (ticks + LichConstants.HUD_SECOND_ROUNDING_TICKS) / LichConstants.HUD_TICKS_PER_SECOND
        );
    }

    private static int countNearbyDoors(World world, BlockPos center) {
        int radius = LichConstants.DOOR_CONTROL_RADIUS_BLOCKS;
        Set<BlockPos> lowerDoorPositions = new HashSet<>();

        /*
         * Wathe 小门的方块实体只挂在下半部分。
         * HUD 扫描也统一折算到下半格，避免一扇门上下两格被计算两次。
         */
        for (BlockPos pos : BlockPos.iterateOutwards(center, radius, radius, radius)) {
            if (center.getSquaredDistance(pos) > radius * radius) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof SmallDoorBlock)) {
                continue;
            }
            BlockPos lowerPos = state.get(SmallDoorBlock.HALF) == DoubleBlockHalf.LOWER
                    ? pos.toImmutable()
                    : pos.down().toImmutable();
            if (world.getBlockEntity(lowerPos) instanceof SmallDoorBlockEntity) {
                lowerDoorPositions.add(lowerPos);
            }
        }
        return lowerDoorPositions.size();
    }
}
