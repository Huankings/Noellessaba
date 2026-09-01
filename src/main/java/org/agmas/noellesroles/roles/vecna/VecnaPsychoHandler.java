package org.agmas.noellesroles.roles.vecna;

import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.api.psycho.PsychoModeProfile;
import dev.doctor4t.wathe.api.psycho.PsychoShieldContext;
import dev.doctor4t.wathe.api.psycho.PsychoShieldResult;
import dev.doctor4t.wathe.api.psycho.PsychoVisualSettings;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 维克那颠倒疯魔 profile 与致死伤害扣时规则。 */
public final class VecnaPsychoHandler {
    public static final Identifier PROFILE_ID = NoellesRolesCore.id("psycho_vecna");
    private static boolean initialized;
    private VecnaPsychoHandler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        PsychoModeProfile profile = PsychoModeProfile.copyOf(PsychoModeApi.createDefaultProfile(), PROFILE_ID)
                .nameTranslationKey("psycho_mode.noellesroles.vecna")
                .shieldNameTranslationKey("psycho_shield.noellesroles.vecna")
                // 显式指定 Wathe 通用结束事件，确保回放使用“%s的%s结束”格式。
                .endEventId(Wathe.id("psycho_mode_end"))
                .durationTicks(VecnaConstants.PSYCHO_DURATION_TICKS)
                .armour(1)
                .grantedItems(List.of(WatheItems.BAT.getDefaultStack()))
                // 疯魔期间使用 Wathe 默认球棒，球棒击杀必须沿用标准 bat 死因；reverse 只用于标记反噬。
                .meleeKill(true, GameConstants.DeathReasons.BAT)
                .backgroundSound(NoellesRolesSounds.AMBIENT_VECNA, true)
                .visualSettings(PsychoVisualSettings.skin(
                        NoellesRolesCore.id("textures/entity/vecna.png"),
                        NoellesRolesCore.id("textures/entity/vecna_thin.png"), true))
                .build();
        PsychoModeApi.registerProfile(profile);
        PsychoModeApi.registerShieldRule(NoellesRolesCore.id("vecna_psycho_fatal_hit"), 1000, VecnaPsychoHandler::resolveShield);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                Set<UUID> activeVecnas = new HashSet<>();
                for (var player : world.getPlayers()) {
                    if (PsychoModeApi.isActive(player, PROFILE_ID)) activeVecnas.add(player.getUuid());
                }
                if (!activeVecnas.isEmpty()) {
                    // GameTimeComponent 默认每 tick 减 1，这里再加 2 后净增 1 tick，实现按秒正向增加。
                    dev.doctor4t.wathe.cca.GameTimeComponent.KEY.get(world).addTime(2 * activeVecnas.size());
                }
                for (var other : world.getPlayers()) {
                    // 疯魔逆转只对“其他存活玩家”生效，绝不写入能力标记字段。
                    boolean inverted = GameFunctions.isPlayerAliveAndSurvival(other)
                            && activeVecnas.stream().anyMatch(uuid -> !uuid.equals(other.getUuid()));
                    VecnaPlayerComponent.KEY.get(other).setPsychoInverted(inverted);
                }
            }
        });
    }

    private static PsychoShieldResult resolveShield(PsychoShieldContext context) {
        if (!PsychoModeApi.isActive(context.victim(), PROFILE_ID)) return PsychoShieldResult.PASS;
        // 只有 Wathe 已经判定为致死的 killPlayer 请求才会进入该接口，普通小额扣血不会触发。
        context.component().setPsychoTicks(Math.max(0,
                context.component().getPsychoTicks() - VecnaConstants.PSYCHO_FATAL_HIT_PENALTY_TICKS));
        context.component().sync();
        return context.component().getPsychoTicks() > 0 ? PsychoShieldResult.BLOCK : PsychoShieldResult.BYPASS;
    }

    public static boolean start(PlayerEntity player) {
        if (!GameFunctions.isPlayerAliveAndSurvival(player)
                || !GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.VECNA)
                || player.getItemCooldownManager().isCoolingDown(org.agmas.noellesroles.ModItems.PSYCHO_VECNA)) return false;
        boolean started = PsychoModeApi.start(player, PROFILE_ID);
        if (started) player.getItemCooldownManager().set(org.agmas.noellesroles.ModItems.PSYCHO_VECNA, VecnaConstants.PSYCHO_COOLDOWN_TICKS);
        return started;
    }
}
