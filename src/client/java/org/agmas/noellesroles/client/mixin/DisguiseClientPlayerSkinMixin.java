package org.agmas.noellesroles.client.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 像 StupidExpress 里的 Convener 一样，从“玩家自己对外暴露的皮肤数据”这一层统一替换伪装皮肤。
 *
 * <p>这样做的好处是：
 * 1. EntityRenderDispatcher 选 slim/classic 渲染器时，天然会拿到正确模型类型；
 * 2. PlayerEntityRenderer / Cape / 手臂 / 其它直接调用 getSkinTextures() 的地方，
 *    都会统一读到同一份伪装结果；
 * 3. 不需要再赌某个具体渲染调用点是不是在 1.21.1 当前版本里刚好会先执行。
 *
 * <p>为了避免把之前修掉的“互相变形套娃崩溃”带回来，
 * DisguiseRenderHelper 内部解析伪装皮肤时不会再去读取目标实体的 getSkinTextures()，
 * 而是只走 PlayerListEntry / Wathe 缓存 / 默认皮肤这些原始来源。</p>
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class DisguiseClientPlayerSkinMixin extends PlayerEntity {

    protected DisguiseClientPlayerSkinMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "getSkinTextures", at = @At("HEAD"), cancellable = true)
    private void noellesroles$replaceDisplayedSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        SkinTextures overrideSkin = DisguiseRenderHelper.resolveAppearanceOverrideSkinTextures(
                (AbstractClientPlayerEntity) (Object) this
        );
        if (overrideSkin != null) {
            cir.setReturnValue(overrideSkin);
        }
    }
}
