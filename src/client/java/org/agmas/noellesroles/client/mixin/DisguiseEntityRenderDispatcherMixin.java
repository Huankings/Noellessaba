package org.agmas.noellesroles.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.SkinTextures;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 修正玩家渲染器选型阶段的 slim/classic 判断。
 *
 * <p>1.21.1 里客户端在真正执行 PlayerEntityRenderer.render(...) 之前，
 * 会先在 EntityRenderDispatcher.getRenderer(...) 里读取
 * AbstractClientPlayerEntity#getSkinTextures().model()，
 * 决定这个玩家本次到底使用 slim 还是 classic 的玩家渲染器实例。
 *
 * <p>之前魔改版只在 PlayerEntityRenderer 内部替换了纹理和 model 字段，
 * 但这里更上游的“渲染器选型”仍然看到的是玩家原始皮肤模型类型，
 * 就会出现“贴图变了，整体身形还是原来的 Alex/Steve”的情况。
 *
 * <p>这里在不改动玩家组件、不改动服务器逻辑的前提下，
 * 只把这一步读取到的 SkinTextures 替换成“当前实际应该显示出来的伪装皮肤”，
 * 这样渲染器实例从一开始就会按目标皮肤模型类型被选对。
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class DisguiseEntityRenderDispatcherMixin {

    @WrapOperation(
            method = "getRenderer(Lnet/minecraft/entity/Entity;)Lnet/minecraft/client/render/entity/EntityRenderer;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"
            )
    )
    private SkinTextures noellesroles$useDisplayedSkinTexturesForRendererSelection(
            AbstractClientPlayerEntity player,
            Operation<SkinTextures> original
    ) {
        SkinTextures displayedSkin = DisguiseRenderHelper.resolveDisplayedSkinTextures(player);
        return displayedSkin != null ? displayedSkin : original.call(player);
    }
}
