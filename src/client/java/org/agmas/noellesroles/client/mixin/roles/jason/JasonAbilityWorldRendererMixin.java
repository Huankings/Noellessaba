package org.agmas.noellesroles.client.mixin.roles.jason;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.jason.JasonAbilityPlayerComponent;
import org.agmas.noellesroles.roles.jason.JasonConstants;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 无恶不在期间的客户端迷雾表现。
 *
 * <p>这里注入在原版 BackgroundRenderer.applyFog 调用之后，先让原版和 Wathe 自己的雾效跑完，
 * 再按杰森无恶不在状态覆盖最终雾距。覆盖判断只读 jason_ability 组件本身，
 * 不再依赖客户端能否知道“谁是杰森”，否则普通存活玩家客户端在隐藏职业信息时会算不出雾效。</p>
 */
@Mixin(WorldRenderer.class)
public abstract class JasonAbilityWorldRendererMixin {
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/BackgroundRenderer;applyFog(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/BackgroundRenderer$FogType;FZF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void noellesroles$applyJasonAbilityFog(
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            return;
        }

        float progress = noellesroles$getFogProgress(client, tickCounter.getTickDelta(false));
        if (progress <= 0.0F) {
            return;
        }

        FogRange range = noellesroles$getFogRange(client.player);
        /*
         * Wathe 地图雾和原版雾会在 applyFog 内写入 RenderSystem。
         * 无恶不在是一个全局压迫效果，需求指定了明确的 start/end，因此这里在最后直接覆盖，
         * 避免因为 Wathe 当前 fogEnd 比目标更近/更远而表现成“仍然是地图默认雾”。
         */
        float targetStart = noellesroles$lerp(RenderSystem.getShaderFogStart(), range.start(), progress);
        float targetEnd = noellesroles$lerp(RenderSystem.getShaderFogEnd(), range.end(), progress);
        RenderSystem.setShaderFogStart(Math.min(targetStart, targetEnd - 0.01F));
        RenderSystem.setShaderFogEnd(targetEnd);
        RenderSystem.setShaderFogShape(FogShape.SPHERE);
    }

    @Unique
    private static float noellesroles$getFogProgress(MinecraftClient client, float tickDelta) {
        float progress = 0.0F;
        for (PlayerEntity candidate : client.world.getPlayers()) {
            JasonAbilityPlayerComponent component = JasonAbilityPlayerComponent.KEY.get(candidate);
            if (!component.isActiveLike()) {
                continue;
            }
            progress = Math.max(progress, noellesroles$getFogProgress(component, tickDelta));
        }
        return progress;
    }

    @Unique
    private static float noellesroles$getFogProgress(JasonAbilityPlayerComponent component, float tickDelta) {
        if (component.isEntering()) {
            return noellesroles$clamp01((component.getPhaseTicks() + tickDelta) / (float) JasonConstants.ABILITY_ENTER_TICKS);
        }
        if (component.isFullyActive()) {
            return 1.0F;
        }
        if (component.isExiting()) {
            return 1.0F - noellesroles$clamp01((component.getPhaseTicks() + tickDelta) / (float) JasonConstants.ABILITY_EXIT_TICKS);
        }
        return 0.0F;
    }

    @Unique
    private static FogRange noellesroles$getFogRange(PlayerEntity viewer) {
        /*
         * 杰森本人视角同样不依赖职业可见性判断。
         * 只要本地玩家自己的无恶不在组件处于 active-like 阶段，就使用杰森专属雾距。
         */
        if (GameFunctions.isPlayerAliveAndSurvival(viewer) && JasonAbilityPlayerComponent.KEY.get(viewer).isActiveLike()) {
            return new FogRange(JasonConstants.ABILITY_FOG_JASON_SELF_START, JasonConstants.ABILITY_FOG_JASON_SELF_END);
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(viewer)) {
            return new FogRange(JasonConstants.ABILITY_FOG_NON_SURVIVAL_START, JasonConstants.ABILITY_FOG_NON_SURVIVAL_END);
        }
        return new FogRange(JasonConstants.ABILITY_FOG_SURVIVAL_START, JasonConstants.ABILITY_FOG_SURVIVAL_END);
    }

    @Unique
    private static float noellesroles$lerp(float from, float to, float progress) {
        return from + (to - from) * noellesroles$clamp01(progress);
    }

    @Unique
    private static float noellesroles$clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    @Unique
    private record FogRange(float start, float end) {
    }
}
