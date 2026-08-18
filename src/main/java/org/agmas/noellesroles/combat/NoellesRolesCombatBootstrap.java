package org.agmas.noellesroles.combat;

import org.agmas.noellesroles.roles.assassin.AssassinGunHandler;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterGunHandler;
import org.agmas.noellesroles.roles.coward.CowardGunCooldownHandler;
import org.agmas.noellesroles.roles.executioner.ExecutionerGunPenaltyHandler;
import org.agmas.noellesroles.roles.jester.JesterGunTargetHandler;
import org.agmas.noellesroles.roles.licensed_villain.LicensedVillainGunPenaltyHandler;
import org.agmas.noellesroles.roles.magician.MagicianGunHandler;
import org.agmas.noellesroles.roles.morphling.MorphlingGunPenaltyHandler;
import org.agmas.noellesroles.roles.robber.RobberGunHandler;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterGunHandler;

/**
 * NoellesRoles 枪械 API 接入总引导器。
 *
 * <p>这里只编排初始化顺序；每个职业/词条的枪击逻辑仍放在自己的包里，
 * 避免重新把所有枪械规则塞回一个难维护的大类。</p>
 */
public final class NoellesRolesCombatBootstrap {
    private static boolean initialized = false;

    private NoellesRolesCombatBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        /*
         * 注册顺序基本按“全局观察/记录 -> 自定义枪完整接管 -> 冷却/目标/反火规则”排列。
         * GunShotApi 最终仍会按 priority 排序，因此这里主要承担可读性：
         * 后续新增职业时，应继续只在这里调用对应职业自己的 init()。
         */
        MagicianGunHandler.init();
        BountyHunterGunHandler.init();
        RobberGunHandler.init();
        AssassinGunHandler.init();
        CowardGunCooldownHandler.init();
        JesterGunTargetHandler.init();
        ExecutionerGunPenaltyHandler.init();
        LicensedVillainGunPenaltyHandler.init();
        ShadowJesterGunHandler.init();
        MorphlingGunPenaltyHandler.init();
    }
}
