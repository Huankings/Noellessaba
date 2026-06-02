package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualPossessionCamera;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualProjectionCamera;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 阻止本地投影相机实体本身被世界渲染出来。
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class SpiritualistRenderDispatcherMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void noellesroles$hideProjectionCamera(
            E entity,
            Frustum frustum,
            double x,
            double y,
            double z,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity instanceof SpiritualProjectionCamera || entity instanceof SpiritualPossessionCamera) {
            cir.setReturnValue(false);
            return;
        }

        /*
         * 附身时灵术师的镜头现在挂在本地假相机上，
         * 宿主玩家实体本身仍然会被世界正常渲染。
         * 如果这里不额外隐藏，灵术师自己就会在第一人称里看见宿主整个人，
         * 甚至像把视角塞进宿主头里一样贴脸穿模。
         */
        if (SpiritualistClientController.shouldHideEntityInPossessionView(entity)) {
            cir.setReturnValue(false);
            return;
        }

        /*
         * 附身中的灵术师本体即使服务器端已经被打成 invisible/noClip，
         * 这里仍然额外在渲染入口兜底一次，防止某些客户端把玩家自身的可见性缓存住，
         * 导致“只有灵术师自己看不见，其他人还看得到”的情况再次出现。
         */
        if (entity instanceof PlayerEntity player
                && SpiritualistPlayerComponent.KEY.get(player).isPossessing()) {
            cir.setReturnValue(false);
        }
    }
}
