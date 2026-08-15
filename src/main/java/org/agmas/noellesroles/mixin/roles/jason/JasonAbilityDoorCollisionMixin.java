package org.agmas.noellesroles.mixin.roles.jason;

import dev.doctor4t.wathe.block.DoorPartBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.agmas.noellesroles.roles.jason.JasonAbilityRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 无恶不在期间让杰森穿过门类方块。
 *
 * <p>玩家碰撞已经通过 Wathe PlayerCollisionApi 处理；门属于方块碰撞 shape，
 * 需要在 AbstractBlockState 的碰撞查询入口做窄特判。这里仅对门、栅栏门、活板门和 Wathe 门返回空 shape，
 * 不影响墙、玻璃、地板等普通方块。</p>
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class JasonAbilityDoorCollisionMixin {
    @Shadow
    public abstract Block getBlock();

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void noellesroles$phaseJasonThroughDoors(BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (!(context instanceof EntityShapeContext entityShapeContext)
                || !(entityShapeContext.getEntity() instanceof PlayerEntity player)
                || !JasonAbilityRules.isAbilityActiveLike(player)
                || !isDoorLikeBlock(this.getBlock())) {
            return;
        }

        cir.setReturnValue(VoxelShapes.empty());
    }

    private static boolean isDoorLikeBlock(Block block) {
        return block instanceof DoorBlock
                || block instanceof FenceGateBlock
                || block instanceof TrapdoorBlock
                || block instanceof DoorPartBlock;
    }
}
