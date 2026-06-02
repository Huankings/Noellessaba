package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualProjectionCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让灵魂相机穿过方块碰撞。
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class SpiritualistBlockStateMixin {

    @Inject(
            method = "getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$removeProjectionCollision(
            BlockView world,
            BlockPos pos,
            ShapeContext context,
            CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (context instanceof EntityShapeContext entityContext && entityContext.getEntity() instanceof SpiritualProjectionCamera) {
            cir.setReturnValue(VoxelShapes.empty());
        }
    }
}
