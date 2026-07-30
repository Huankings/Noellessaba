package org.agmas.noellesroles.roles.rememberer;

import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.api.psycho.PsychoShieldResult;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 追忆者狙击枪与疯魔护盾的交互规则。
 *
 * <p>狙击枪属于明确“穿盾”的伤害来源，因此接入 Wathe 的疯魔护盾规则 API，
 * 不再 mixin {@code PlayerPsychoComponent#getArmour()} 假装护盾为 0。</p>
 */
public final class RemembererPsychoShieldHandler {
    private RemembererPsychoShieldHandler() {
    }

    public static void init() {
        PsychoModeApi.registerShieldRule(
                NoellesRolesCore.id("rememberer/sniper_bypass_psycho_shield"),
                PsychoModeApi.DEFAULT_PRIORITY + 100,
                context -> NoellesDeathReasons.DEATH_REASON_SNIPER_RIFLE.equals(context.deathReason())
                        ? PsychoShieldResult.BYPASS
                        : PsychoShieldResult.PASS
        );
    }
}
