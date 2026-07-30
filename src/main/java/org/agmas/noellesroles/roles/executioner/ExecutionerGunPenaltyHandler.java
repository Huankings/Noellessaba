package org.agmas.noellesroles.roles.executioner;

import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.RevolverPenaltyContext;
import dev.doctor4t.wathe.api.combat.RevolverPenaltyResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityManager;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.UUID;

/**
 * 刽子手目标、巫毒配置和双重人格双活阶段对左轮误伤惩罚的豁免。
 */
public final class ExecutionerGunPenaltyHandler {
    private static boolean initialized = false;

    private ExecutionerGunPenaltyHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GunShotApi.registerInnocentRevolverPenaltyRule(
                NoellesRolesCore.id("executioner_target_revolver_penalty"),
                /*
                 * 这条优先级高于普通职业豁免。
                 * 仇杀目标、巫毒配置和双重人格双活阶段都属于“目标虽然看起来无辜，
                 * 但本次不应触发 Wathe 误伤惩罚”的更具体规则。
                 */
                200,
                ExecutionerGunPenaltyHandler::resolvePenalty
        );
    }

    private static RevolverPenaltyResult resolvePenalty(RevolverPenaltyContext context) {
        PlayerEntity target = context.target();
        for (UUID uuid : context.game().getAllWithRole(NoellesRoleRegistry.EXECUTIONER)) {
            PlayerEntity executioner = target.getWorld().getPlayerByUuid(uuid);
            if (executioner == null) {
                continue;
            }
            ExecutionerPlayerComponent component = ExecutionerPlayerComponent.KEY.get(executioner);
            if (target.getUuid().equals(component.target)) {
                // 击中任意仇杀客的指定目标时，不掉枪也不反火。
                return RevolverPenaltyResult.SKIP;
            }
        }

        if (context.game().isRole(target, NoellesRoleRegistry.VOODOO)
                && NoellesRolesConfig.HANDLER.instance().voodooShotLikeEvil) {
            // 配置允许时，巫毒师在左轮惩罚中按“邪恶目标”处理。
            return RevolverPenaltyResult.SKIP;
        }

        if (context.shooter() instanceof ServerPlayerEntity shooter
                && DualPersonalityManager.shouldSuppressInnocentRevolverPenalty(
                shooter,
                target,
                context.targetNormallyInnocent()
        )) {
            // 双重人格双活阶段的外观/阵营判定由 DualPersonalityManager 集中决定。
            return RevolverPenaltyResult.SKIP;
        }
        return RevolverPenaltyResult.PASS;
    }
}
