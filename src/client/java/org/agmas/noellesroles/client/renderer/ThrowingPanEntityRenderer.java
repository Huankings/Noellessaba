package org.agmas.noellesroles.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.entities.ThrowingPanEntity;
import org.agmas.noellesroles.roles.cook.CookConstants;

/**
 * 飞锅实体渲染器。
 *
 * <p>普通飞锅和疯魔飞锅共用同一实体，客户端通过实体同步的 ItemStack 自动选择贴图。</p>
 */
@Environment(EnvType.CLIENT)
public final class ThrowingPanEntityRenderer extends EntityRenderer<ThrowingPanEntity> {
    private final ItemRenderer itemRenderer;

    public ThrowingPanEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
            ThrowingPanEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        ItemStack itemStack = entity.getItemStack();
        if (itemStack.isEmpty()) {
            // 网络同步尚未到达的首帧使用普通飞锅兜底，避免实体短暂渲染为空白。
            itemStack = new ItemStack(ModItems.THROWING_PAN);
        }

        matrices.push();
        BakedModel bakedModel = this.itemRenderer.getModel(itemStack, entity.getWorld(), null, entity.getId());

        if (!entity.isStuckInBlock()) {
            float rotation = (entity.getTicksAlive() + tickDelta) * CookConstants.THROWING_PAN_RENDER_Y_ROTATION_PER_TICK;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation * CookConstants.THROWING_PAN_RENDER_Z_ROTATION_MULTIPLIER));
        } else {
            orientStuckPan(matrices, entity.getHitDirection());
        }

        matrices.scale(
                CookConstants.THROWING_PAN_RENDER_SCALE,
                CookConstants.THROWING_PAN_RENDER_SCALE,
                CookConstants.THROWING_PAN_RENDER_SCALE
        );
        this.itemRenderer.renderItem(
                itemStack,
                ModelTransformationMode.GROUND,
                false,
                matrices,
                vertexConsumers,
                15728880,
                OverlayTexture.DEFAULT_UV,
                bakedModel
        );

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void orientStuckPan(MatrixStack matrices, Direction direction) {
        switch (direction) {
            case UP -> {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
                matrices.translate(0.0F, -CookConstants.THROWING_PAN_STUCK_RENDER_OFFSET, 0.0F);
            }
            case DOWN -> matrices.translate(0.0F, -CookConstants.THROWING_PAN_STUCK_RENDER_OFFSET, 0.0F);
            case NORTH -> {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
                matrices.translate(0.0F, 0.0F, CookConstants.THROWING_PAN_STUCK_RENDER_OFFSET);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(CookConstants.THROWING_PAN_STUCK_RENDER_SIDE_TILT_DEGREES));
            }
            case SOUTH -> {
                matrices.translate(0.0F, 0.0F, CookConstants.THROWING_PAN_STUCK_RENDER_OFFSET);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(CookConstants.THROWING_PAN_STUCK_RENDER_SIDE_TILT_DEGREES));
            }
            case WEST -> {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270.0F));
                matrices.translate(0.0F, 0.0F, CookConstants.THROWING_PAN_STUCK_RENDER_OFFSET);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(CookConstants.THROWING_PAN_STUCK_RENDER_SIDE_TILT_DEGREES));
            }
            case EAST -> {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                matrices.translate(0.0F, 0.0F, CookConstants.THROWING_PAN_STUCK_RENDER_OFFSET);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(CookConstants.THROWING_PAN_STUCK_RENDER_SIDE_TILT_DEGREES));
            }
        }
    }

    @Override
    public Identifier getTexture(ThrowingPanEntity entity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }
}
