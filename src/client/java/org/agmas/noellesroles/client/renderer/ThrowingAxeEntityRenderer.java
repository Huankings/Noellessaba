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
import org.agmas.noellesroles.entities.ThrowingAxeEntity;

/**
 * 飞斧实体渲染器。
 * 飞行时做旋转动画，插入方块后根据命中面调整朝向。
 */
@Environment(EnvType.CLIENT)
public class ThrowingAxeEntityRenderer extends EntityRenderer<ThrowingAxeEntity> {

    private final ItemRenderer itemRenderer;
    private final float scale = 1.6F;

    public ThrowingAxeEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
            ThrowingAxeEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        ItemStack itemStack = entity.getItemStack();
        if (itemStack.isEmpty()) {
            itemStack = new ItemStack(ModItems.THROWING_AXE);
        }

        matrices.push();

        BakedModel bakedModel = this.itemRenderer.getModel(itemStack, entity.getWorld(), null, entity.getId());
        boolean inGround = entity.isStuckInBlock();

        if (!inGround) {
            // 飞行时持续旋转，强调“高速投掷武器”的视觉反馈。
            float rotation = (entity.getTicksAlive() + tickDelta) * 8.0F;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation * 0.7F));
        } else {
            // 命中方块后按碰撞面的方向把飞斧“插”进去。
            Direction hitDirection = entity.getHitDirection();
            switch (hitDirection) {
                case UP -> {
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
                    matrices.translate(0.0F, -0.35F, 0.0F);
                }
                case DOWN -> matrices.translate(0.0F, -0.35F, 0.0F);
                case NORTH -> {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
                    matrices.translate(0.0F, 0.0F, 0.35F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F));
                }
                case SOUTH -> {
                    matrices.translate(0.0F, 0.0F, 0.35F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F));
                }
                case WEST -> {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270.0F));
                    matrices.translate(0.0F, 0.0F, 0.35F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F));
                }
                case EAST -> {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                    matrices.translate(0.0F, 0.0F, 0.35F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F));
                }
                default -> {
                    matrices.translate(0.0F, 0.0F, 0.35F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F));
                }
            }
        }

        matrices.scale(this.scale, this.scale, this.scale);

        // 固定用较亮光照，避免飞斧在黑暗里几乎看不见。
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

    @Override
    public Identifier getTexture(ThrowingAxeEntity entity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }
}
