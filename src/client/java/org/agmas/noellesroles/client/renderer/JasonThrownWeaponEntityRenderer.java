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
import org.agmas.noellesroles.roles.jason.JasonThrownWeaponEntity;

/**
 * 杰森投掷物实体渲染器。
 *
 * <p>实体同步保存了原始物品堆，因此同一个渲染器可以正确显示沾血飞斧、砍刀、战斧、
 * 玩具飞斧、飞镐与油桶，而不需要为每种投掷物分别注册实体类型。</p>
 */
@Environment(EnvType.CLIENT)
public final class JasonThrownWeaponEntityRenderer extends EntityRenderer<JasonThrownWeaponEntity> {
    private static final float ITEM_SCALE = 1.6F;

    private final ItemRenderer itemRenderer;

    public JasonThrownWeaponEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
            JasonThrownWeaponEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        ItemStack stack = entity.getItemStack();
        if (stack.isEmpty()) {
            // 网络同步尚未到达的首帧使用默认飞斧，避免实体短暂渲染成空白。
            stack = ModItems.THROWING_BLOOD_AXE.getDefaultStack();
        }

        matrices.push();
        if (!entity.isStuckInBlock()) {
            float rotation = (entity.getTicksAlive() + tickDelta) * 8.0F;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation * 0.7F));
        } else {
            orientStuckWeapon(matrices, entity.getHitDirection());
        }

        matrices.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        BakedModel model = this.itemRenderer.getModel(stack, entity.getWorld(), null, entity.getId());
        this.itemRenderer.renderItem(
                stack,
                ModelTransformationMode.GROUND,
                false,
                matrices,
                vertexConsumers,
                15728880,
                OverlayTexture.DEFAULT_UV,
                model
        );
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void orientStuckWeapon(MatrixStack matrices, Direction direction) {
        switch (direction) {
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
        }
    }

    @Override
    public Identifier getTexture(JasonThrownWeaponEntity entity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }
}
