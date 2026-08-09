package org.agmas.noellesroles.mixin.roles.spiritualist;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.EntityView;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistBodyRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 在移动碰撞列表生成层过滤灵术师脱体本体。
 *
 * <p>玩家主动移动时，Minecraft 会通过 {@code EntityView#getEntityCollisions}
 * 把附近可碰撞实体的包围盒转换成 {@link VoxelShape}，再用这些 shape 裁剪玩家位移。
 * 如果灵术师本体在这里已经被转成 shape，即使后续推挤被取消，主动走过去仍会像撞墙。</p>
 *
 * <p>因此这里只在“移动者是玩家，并且候选实体是灵术师脱体本体”时重建一次碰撞列表，
 * 把那一个空气壳排除掉；其它实体仍沿用原版筛选和 shape 生成逻辑，避免扩大影响面。</p>
 */
@Mixin(EntityView.class)
public interface SpiritualistEntityCollisionViewMixin {
    @Shadow
    List<Entity> getOtherEntities(Entity except, Box box, Predicate<? super Entity> predicate);

    @Inject(method = "getEntityCollisions", at = @At("HEAD"), cancellable = true)
    private void noellesroles$removeDetachedSpiritualistCollisionShapes(
            Entity entity,
            Box box,
            CallbackInfoReturnable<List<VoxelShape>> cir
    ) {
        if (!(entity instanceof PlayerEntity)) {
            return;
        }

        Predicate<Entity> vanillaPredicate = EntityPredicates.EXCEPT_SPECTATOR.and(candidate -> entity.collidesWith(candidate));
        List<Entity> candidates = this.getOtherEntities(entity, box.expand(1.0E-7D), vanillaPredicate);
        if (candidates.isEmpty()) {
            return;
        }

        boolean skippedDetachedBody = false;
        List<VoxelShape> shapes = new ArrayList<>(candidates.size());
        for (Entity candidate : candidates) {
            if (SpiritualistBodyRules.shouldIgnorePlayerBodyCollision(entity, candidate)) {
                skippedDetachedBody = true;
                continue;
            }
            shapes.add(VoxelShapes.cuboid(candidate.getBoundingBox()));
        }

        if (skippedDetachedBody) {
            cir.setReturnValue(List.copyOf(shapes));
        }
    }
}
