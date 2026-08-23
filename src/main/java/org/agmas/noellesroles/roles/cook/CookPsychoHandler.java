package org.agmas.noellesroles.roles.cook;

import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.api.psycho.PsychoModeProfile;
import dev.doctor4t.wathe.api.psycho.PsychoVisualSettings;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 厨师专属疯魔模式。
 *
 * <p>它复用 Wathe 疯魔 API 的持续时间、护盾、锁栏、结束清理和回放流程，
 * 但把武器替换成“疯魔飞锅”，并关闭默认球棒近战击杀和背景音乐。</p>
 */
public final class CookPsychoHandler {
    public static final Identifier PROFILE_ID = NoellesRolesCore.id("cook_psycho");

    private static boolean initialized = false;

    private CookPsychoHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        PsychoModeProfile profile = PsychoModeProfile.copyOf(PsychoModeApi.createDefaultProfile(), PROFILE_ID)
                .nameTranslationKey("psycho_mode.noellesroles.cook")
                .shieldNameTranslationKey("psycho_shield.noellesroles.cook")
                .durationTicks(CookConstants.PSYCHO_COOK_DURATION_TICKS)
                /*
                 * 厨师疯魔只授予疯魔飞锅。
                 * 复制默认 profile 后必须覆盖 grantedItems，否则玩家会同时获得默认球棒。
                 */
                .grantedItems(List.of(ModItems.PSYCHO_THROWING_PAN.getDefaultStack()))
                /*
                 * 疯魔飞锅是投掷眩晕武器，不应该让左键近战走 Wathe 默认球棒一击杀。
                 */
                .meleeKill(false, GameConstants.DeathReasons.BAT)
                /*
                 * 用户要求厨师疯魔不播放疯魔音乐。
                 * backgroundSound(null, false) 会让客户端疯魔音乐系统完全跳过该 profile。
                 */
                .backgroundSound(null, false)
                /*
                 * 厨师疯魔使用专属疯魔皮肤。
                 * Wathe 会根据玩家当前模型自动在普通手臂 cook.png 与细手臂 cook_thin.png 之间切换。
                 */
                .visualSettings(PsychoVisualSettings.skin(
                        NoellesRolesCore.id("textures/entity/cook.png"),
                        NoellesRolesCore.id("textures/entity/cook_thin.png"),
                        true
                ))
                .build();
        PsychoModeApi.registerProfile(profile);
    }

    public static boolean startCookPsycho(@NotNull PlayerEntity player) {
        boolean debugPlayer = GameFunctions.isPlayerSpectatingOrCreative(player);
        if (!debugPlayer) {
            if (!GameFunctions.isPlayerAliveAndSurvival(player)
                    || !dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.COOK)
                    || player.getItemCooldownManager().isCoolingDown(ModItems.PSYCHO_COOK)) {
                return false;
            }
        }

        boolean started = PsychoModeApi.start(player, PROFILE_ID);
        if (started) {
            /*
             * 厨师疯魔购买成功后按常量开关决定是否额外给予夜视。
             * 这样后续测试不同地图亮度或平衡性时，只需要改 CookConstants 里的布尔值。
             * 持续时间直接复用厨师疯魔 profile 的持续 tick，确保玩家看到的夜视窗口和疯魔倒计时一致。
             */
            if (CookConstants.PSYCHO_COOK_GRANTS_NIGHT_VISION) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NIGHT_VISION,
                        CookConstants.PSYCHO_COOK_DURATION_TICKS,
                        CookConstants.PSYCHO_COOK_NIGHT_VISION_AMPLIFIER,
                        false,
                        false,
                        true
                ));
            }

            if (!debugPlayer) {
                player.getItemCooldownManager().set(ModItems.PSYCHO_COOK, CookConstants.PSYCHO_COOK_COOLDOWN_TICKS);
            }
        }
        return started;
    }

    public static boolean isCookPsychoActive(@NotNull PlayerEntity player) {
        return PsychoModeApi.isActive(player, PROFILE_ID);
    }

    public static void tickPsychoState(@NotNull ServerPlayerEntity player) {
        CookPlayerComponent cookState = CookPlayerComponent.KEY.get(player);
        if (isCookPsychoActive(player)) {
            /*
             * “无限体力”用 Wathe 公开体力 API 表达：
             * 进入状态时加一个足够大的上限，每 tick 回满；退出时恢复原上限。
             */
            cookState.applyPsychoStaminaBonus();
        } else {
            cookState.restorePsychoStaminaBonusIfNeeded();
        }
    }
}
