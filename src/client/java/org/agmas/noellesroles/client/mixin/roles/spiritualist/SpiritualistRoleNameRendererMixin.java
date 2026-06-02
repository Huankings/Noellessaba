package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.gui.RoleNameRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

/**
 * 让 wathe 的准心名字渲染在灵术师脱体视角下也从“当前相机”出发判定。
 *
 * <p>RoleNameRenderer 原本固定拿本地真实玩家实体做 ProjectileUtil.getCollision(...)。
 * 这在普通情况下没问题，但灵术师附身后相机已经切到宿主/附身相机上：
 * 如果还继续沿用本体玩家的朝向，就会出现“本体面前刚好有人，名字被错误渲染到灵术师屏幕上”的问题。</p>
 *
 * <p>这里不重写整段 HUD，只把射线检测的起点替换成当前 camera entity，
 * 这样玩家名字与纸条注视判定都会自然跟随附身视角。</p>
 */
@Mixin(RoleNameRenderer.class)
public abstract class SpiritualistRoleNameRendererMixin {

    @WrapOperation(
            method = "renderHud",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/ProjectileUtil;getCollision(Lnet/minecraft/entity/Entity;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/HitResult;"
            )
    )
    private static HitResult noellesroles$useDetachedCameraForRoleNameRaycast(
            Entity entity,
            Predicate<Entity> predicate,
            double range,
            Operation<HitResult> original
    ) {
        /*
         * 只有灵术师当前真的处于脱体视角时才改用相机实体。
         * 其余角色、普通第一人称与第三人称都继续走 wathe 原本逻辑，避免无关行为变化。
         */
        if (SpiritualistClientController.isProjectionActive() || SpiritualistClientController.isPossessionViewActive()) {
            Entity cameraEntity = MinecraftClient.getInstance().getCameraEntity();
            if (cameraEntity != null) {
                /*
                 * 附身视角下，相机实体会和宿主玩家重叠在一起。
                 * RoleNameRenderer 如果继续把宿主自己也当成可命中目标，
                 * 就会在后退或某些特殊朝向时，把“被附身玩家自己的名字”渲到灵术师屏幕上。
                 *
                 * 这里直接沿用前面已经用于本地隐藏宿主的判定，
                 * 把这名“只是镜头宿主、并非真正观察目标”的玩家从名字射线里剔除掉。
                 */
                Predicate<Entity> adjustedPredicate = target ->
                        predicate.test(target) && !SpiritualistClientController.shouldHideEntityInPossessionView(target);
                return original.call(cameraEntity, adjustedPredicate, range);
            }
        }

        return original.call(entity, predicate, range);
    }

    @WrapOperation(
            method = "renderHud",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/PlayerEntity;getDisplayName()Lnet/minecraft/text/Text;"
            )
    )
    private static Text noellesroles$replaceProjectionTargetNameWithSelf(
            PlayerEntity instance,
            Operation<Text> original
    ) {
        /*
         * 只有灵魂出窍时才把准心命中的玩家名字统一伪装成“自己”。
         *
         * 这样灵术师出窍穿行时，看到的所有玩家名都会是自己的名字，
         * 而附身状态仍然继续显示宿主视角下的正常目标名，不影响你前面刚修好的那部分逻辑。
         */
        if (SpiritualistClientController.isProjectionActive()) {
            ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
            if (localPlayer != null) {
                return localPlayer.getDisplayName();
            }
        }

        return original.call(instance);
    }
}
