package org.agmas.noellesroles.client.renderer;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.entities.CaptureDeviceEntity;

public class CaptureDeviceEntityRenderer extends EntityRenderer<CaptureDeviceEntity> {
    private final ItemRenderer itemRenderer;
    private final float scale;

    public CaptureDeviceEntityRenderer(EntityRendererFactory.Context ctx, float scale) {
        super(ctx);
        this.itemRenderer = ctx.getItemRenderer();
        this.scale = scale;
    }

    public CaptureDeviceEntityRenderer(EntityRendererFactory.Context context) {
        this(context, 1.0F);
    }

    @Override
    public Identifier getTexture(CaptureDeviceEntity entity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public void render(CaptureDeviceEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // 仅当观察者是工程师或观察者/创造模式时渲染
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;

        var gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (gameWorld.isRole(client.player, Noellesroles.ENGINEER) || WatheClient.isPlayerSpectatingOrCreative()) {
            // 渲染捕捉装置物品
            matrices.push();
            // 旋转朝向
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getYaw()));
            // 添加微小偏移以防止与其他实体渲染重叠（类似地雷）
            matrices.translate(0.0F, (float) entity.hashCode() % 24.0F * 1.0E-4F, 0.0F);
            // 旋转使其平放
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            // 缩放（根据 scale 调整，此处使用 0.4F 与地雷一致）
            matrices.scale(this.scale * 0.4F, this.scale * 0.4F, this.scale * 0.4F);

            // 渲染物品
            this.itemRenderer.renderItem(
                    ModItems.CAPTURE_DEVICE.getDefaultStack(),
                    ModelTransformationMode.FIXED,
                    light,
                    OverlayTexture.DEFAULT_UV,
                    matrices,
                    vertexConsumers,
                    entity.getWorld(),
                    entity.getId()
            );

            matrices.pop();
            super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        }
    }
}