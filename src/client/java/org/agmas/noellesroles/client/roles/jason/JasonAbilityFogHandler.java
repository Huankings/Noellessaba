package org.agmas.noellesroles.client.roles.jason;

import dev.doctor4t.wathe.api.client.fog.FogOverrideApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.FogShape;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.jason.JasonAbilityPlayerComponent;
import org.agmas.noellesroles.roles.jason.JasonAbilityRules;
import org.agmas.noellesroles.roles.jason.JasonConstants;

/**
 * 杰森“无恶不在”的客户端雾效 provider。
 *
 * <p>这里不再直接注入 WorldRenderer。Wathe 会在原版和地图雾完成后统一询问 provider，
 * 再把最终值交给 RenderSystem；Iris 的 FogUniforms 会自然读取这份最终值，因此普通雾、
 * Wathe 地图雾和杰森雾不会因为渲染注入顺序或 shaderpack 重设而互相覆盖。</p>
 */
@Environment(EnvType.CLIENT)
public final class JasonAbilityFogHandler {
    private static final Identifier PROVIDER_ID = NoellesRolesCore.id("jason_ability_fog");
    private static boolean registered;

    private JasonAbilityFogHandler() {
    }

    /**
     * 在 NoellesRoles 客户端入口注册一次杰森雾效 provider。
     */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        FogOverrideApi.registerProvider(
                PROVIDER_ID,
                JasonConstants.ABILITY_FOG_PRIORITY,
                JasonAbilityFogHandler::resolve
        );
    }

    private static FogOverrideApi.FogOverride resolve(FogOverrideApi.FogContext context) {
        MinecraftClient client = context.client();
        if (client.world == null || client.player == null) {
            return FogOverrideApi.FogOverride.pass();
        }

        float progress = 0.0F;
        for (PlayerEntity candidate : client.world.getPlayers()) {
            JasonAbilityPlayerComponent component = JasonAbilityPlayerComponent.KEY.get(candidate);
            if (!component.isActiveLike()) {
                continue;
            }

            /*
             * 组件同步给所有客户端，但只有存活杰森的组件才是有效能力状态。
             * 这里额外使用规则层判断，避免上一局组件尚未到达清理包时把普通玩家误判成雾源。
             */
            if (!JasonAbilityRules.isAliveJason(candidate)) {
                continue;
            }
            progress = Math.max(progress, getTransitionProgress(component, context.tickDelta()));
        }

        if (progress <= 0.0F) {
            return FogOverrideApi.FogOverride.pass();
        }

        if (!isFogEnabledForViewer(client.player)) {
            return FogOverrideApi.FogOverride.pass();
        }

        FogRange target = getTargetRange(client.player);
        float start = lerp(context.baseStart(), target.start(), progress);
        float end = lerp(context.baseEnd(), target.end(), progress);
        return FogOverrideApi.FogOverride.override(start, end, FogShape.SPHERE);
    }

    private static boolean isFogEnabledForViewer(PlayerEntity viewer) {
        if (JasonAbilityRules.isAbilityActiveLike(viewer)) {
            return JasonConstants.ABILITY_FOG_FOR_JASON_SELF;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(viewer)) {
            return JasonConstants.ABILITY_FOG_FOR_NON_SURVIVAL;
        }
        return JasonConstants.ABILITY_FOG_FOR_OTHER_SURVIVORS;
    }

    private static FogRange getTargetRange(PlayerEntity viewer) {
        /*
         * 杰森本人使用专属视距；非存活/创造/旁观视角使用调试友好的较宽视距；
         * 其它正常存活玩家使用最窄的 2.0 -> 4.0 范围。
         */
        if (JasonAbilityRules.isAbilityActiveLike(viewer)) {
            return new FogRange(
                    JasonConstants.ABILITY_FOG_JASON_SELF_START,
                    JasonConstants.ABILITY_FOG_JASON_SELF_END
            );
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(viewer)) {
            return new FogRange(
                    JasonConstants.ABILITY_FOG_NON_SURVIVAL_START,
                    JasonConstants.ABILITY_FOG_NON_SURVIVAL_END
            );
        }
        return new FogRange(
                JasonConstants.ABILITY_FOG_SURVIVAL_START,
                JasonConstants.ABILITY_FOG_SURVIVAL_END
        );
    }

    private static float getTransitionProgress(JasonAbilityPlayerComponent component, float tickDelta) {
        float elapsed = component.getPhaseTicks() + tickDelta;
        return switch (component.getPhase()) {
            case ENTERING -> clamp01(elapsed / Math.max(1.0F, JasonConstants.ABILITY_ENTER_TICKS));
            case ACTIVE -> 1.0F;
            case EXITING -> 1.0F - clamp01(elapsed / Math.max(1.0F, JasonConstants.ABILITY_EXIT_TICKS));
            case IDLE -> 0.0F;
        };
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * clamp01(progress);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private record FogRange(float start, float end) {
    }
}
