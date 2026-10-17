package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.api.psycho.PsychoModeProfile;
import dev.doctor4t.wathe.api.psycho.PsychoModeState;
import dev.doctor4t.wathe.api.psycho.PsychoVisualSettings;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.outlaw.OutlawWorldComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** 杀手皇后疯魔 profile、启动以及亡命时刻暂停/恢复。 */
public final class KillerQueenPsychoHandler {
    public static final Identifier PROFILE_ID = NoellesRolesCore.id("psycho_killer_queen");
    private static boolean initialized;

    private KillerQueenPsychoHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        PsychoModeProfile profile = PsychoModeProfile.copyOf(PsychoModeApi.createDefaultProfile(), PROFILE_ID)
                .nameTranslationKey("psycho_mode.noellesroles.killer_queen")
                .shieldNameTranslationKey("psycho_shield.noellesroles.killer_queen")
                .endEventId(Wathe.id("psycho_mode_end"))
                .durationTicks(KillerQueenConstants.PSYCHO_DURATION_TICKS)
                .armour(KillerQueenConstants.PSYCHO_SHIELD_LAYERS)
                .cooldownItem(ModItems.PSYCHO_KILLER_QUEEN)
                .grantedItems(List.of(ModItems.MARKER_STAFF.getDefaultStack()))
                /* 标记法杖先进入 Wathe 的满力近战链，再由 DeathApi 早期规则取消真实死亡并改成定身。 */
                .meleeKill(true, GameConstants.DeathReasons.BAT)
                .meleeWeaponPredicate((player, stack) -> stack.isOf(ModItems.MARKER_STAFF))
                .hitSound(WatheSounds.ITEM_BAT_HIT)
                /* 音乐由客户端优先级控制器播放，才能只让位于影子小丑、执照恶棍和亡命时刻而不暂停机制。 */
                .backgroundSound(null, false)
                .visualSettings(PsychoVisualSettings.skin(
                        NoellesRolesCore.id("textures/entity/killer_queen.png"),
                        NoellesRolesCore.id("textures/entity/killer_queen_thin.png"), true))
                .build();
        PsychoModeApi.registerProfile(profile);
    }

    public static boolean start(@NotNull PlayerEntity player) {
        boolean debug = GameFunctions.isPlayerSpectatingOrCreative(player);
        if (!debug && (!GameFunctions.isPlayerAliveAndSurvival(player)
                || !GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.KILLER_QUEEN)
                || player.getItemCooldownManager().isCoolingDown(ModItems.PSYCHO_KILLER_QUEEN))) {
            return false;
        }
        if (player.getWorld() instanceof ServerWorld world && OutlawWorldComponent.KEY.get(world).hasActiveOutlaw()) {
            return false;
        }
        boolean started = PsychoModeApi.start(player, PROFILE_ID);
        if (started) {
            KillerQueenWorldComponent.KEY.get(player.getWorld()).startPsychoSession(player.getUuid());
            if (!debug) {
                player.getItemCooldownManager().set(ModItems.PSYCHO_KILLER_QUEEN, KillerQueenConstants.PSYCHO_COOLDOWN_TICKS);
            }
            if (player.getWorld() instanceof ServerWorld serverWorld) {
                /* 立即终止其它杀手侧疯魔并施加召集者冷却，不能等下一次普通 server tick。 */
                KillerQueenManager.tickWorld(serverWorld);
            }
        }
        return started;
    }

    public static void pauseForOutlaw(@NotNull ServerWorld world) {
        KillerQueenWorldComponent state = KillerQueenWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (state.hasPsychoSession(player.getUuid()) && PsychoModeApi.isActive(player, PROFILE_ID)) {
                PsychoModeState snapshot = PsychoModeApi.pause(player);
                if (snapshot != null) state.suspendPsycho(player.getUuid(), snapshot);
            }
        }
    }

    public static void resumeAfterOutlaw(@NotNull ServerWorld world) {
        if (OutlawWorldComponent.KEY.get(world).hasActiveOutlaw()) return;
        KillerQueenWorldComponent state = KillerQueenWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            PsychoModeState snapshot = state.getSuspendedPsycho(player.getUuid());
            if (snapshot == null) continue;
            if (!GameFunctions.isPlayerAliveAndSurvival(player) || !KillerQueenRules.isKillerQueen(player)) {
                KillerQueenManager.finishPsychoSession(player, true);
                continue;
            }
            if (PsychoModeApi.resume(player, snapshot)) state.clearSuspendedPsycho(player.getUuid());
        }
    }
}
