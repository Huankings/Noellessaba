package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.api.psycho.PsychoModeProfile;
import dev.doctor4t.wathe.api.psycho.PsychoVisualSettings;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 杰森模式的 Wathe 疯魔 profile。
 *
 * <p>这里显式从 Wathe 默认疯魔 profile 复制，因此默认护盾层数会直接继承
 * {@code GameConstants.PSYCHO_MODE_ARMOUR}。用户确认不需要杰森单独配置护盾层数，
 * 所以本类刻意不调用 {@link PsychoModeProfile.Builder#armour(int)}。</p>
 */
public final class JasonPsychoHandler {
    public static final net.minecraft.util.Identifier PROFILE_ID = NoellesRolesCore.id("jason_mode");

    private static boolean initialized;

    private JasonPsychoHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        PsychoModeProfile profile = PsychoModeProfile.copyOf(PsychoModeApi.createDefaultProfile(), PROFILE_ID)
                .nameTranslationKey("psycho_mode.noellesroles.jason")
                .shieldNameTranslationKey("psycho_shield.noellesroles.jason")
                .durationTicks(JasonConstants.PSYCHO_DURATION_TICKS)
                /*
                 * 杰森模式只授予飞镐。复制默认 profile 后必须覆盖球棒列表，
                 * 否则会同时出现默认球棒和飞镐，和“手持武器使用飞镐”的需求冲突。
                 */
                .grantedItems(List.of(ModItems.THROWING_PICKAXE.getDefaultStack()))
                /*
                 * 飞镐是蓄力投掷物，不作为疯魔近战一击杀武器。
                 * 关闭 profile 的近战击杀后，Wathe 不会把普通左键误判为疯魔球棒处决。
                 */
                .meleeKill(false, NoellesDeathReasons.JASON_THROWING_WEAPON_DEATH_REASON)
                .backgroundSound(NoellesRolesSounds.AMBIENT_JASON, true)
                .visualSettings(PsychoVisualSettings.skin(
                        NoellesRolesCore.id("textures/entity/jason.png"),
                        NoellesRolesCore.id("textures/entity/jason_thin.png"),
                        true
                ))
                .build();
        PsychoModeApi.registerProfile(profile);
    }

    /**
     * 从杰森模式商店图标启动疯魔。
     *
     * <p>物品冷却只在 Wathe 局内存活玩家上检查；死亡旁观或创造调试状态不应被
     * Jason 组件残留的动作锁错误限制。真正的模式启动仍由 Wathe API 完成，
     * 成功后只给商店图标写入 4 分 15 秒冷却。</p>
     */
    public static boolean startJasonMode(@NotNull PlayerEntity player) {
        if (!GameFunctions.isPlayerAliveAndSurvival(player)
                || JasonWoundManager.isWoundedActionLocked(player)
                || player.getItemCooldownManager().isCoolingDown(ModItems.PSYCHO_JASON)) {
            return false;
        }

        boolean started = PsychoModeApi.start(player, PROFILE_ID);
        if (started) {
            if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                /*
                 * 杰森模式和无恶不在都是强状态。进入杰森模式后必须强制清掉幽魂状态，
                 * 避免“飞镐无限投掷 + 隐身穿门 + 免伤”叠在一起。
                 */
                JasonAbilityManager.forceExitForJasonMode(serverPlayer);
            }
            player.getItemCooldownManager().set(ModItems.PSYCHO_JASON, JasonConstants.PSYCHO_COOLDOWN_TICKS);
        }
        return started;
    }

    public static boolean isJasonModeActive(@NotNull PlayerEntity player) {
        return PsychoModeApi.isActive(player, PROFILE_ID);
    }
}
