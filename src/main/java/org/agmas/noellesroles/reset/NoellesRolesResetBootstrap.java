package org.agmas.noellesroles.reset;

import org.agmas.noellesroles.modifiers.RoundModifierResetHandler;
import org.agmas.noellesroles.roles.angel.AngelResetHandler;
import org.agmas.noellesroles.roles.assassin.AssassinResetHandler;
import org.agmas.noellesroles.roles.bartender.BartenderResetHandler;
import org.agmas.noellesroles.roles.bomber.BomberResetHandler;
import org.agmas.noellesroles.roles.controller.ControllerResetHandler;
import org.agmas.noellesroles.roles.coroner.CoronerResetHandler;
import org.agmas.noellesroles.roles.coward.CowardResetHandler;
import org.agmas.noellesroles.roles.executioner.ExecutionerResetHandler;
import org.agmas.noellesroles.roles.magician.MagicianResetHandler;
import org.agmas.noellesroles.roles.morphling.MorphlingResetHandler;
import org.agmas.noellesroles.roles.noisemaker.NoisemakerResetHandler;
import org.agmas.noellesroles.roles.operator.OperatorResetHandler;
import org.agmas.noellesroles.roles.prophet.ProphetResetHandler;
import org.agmas.noellesroles.roles.recaller.RecallerResetHandler;
import org.agmas.noellesroles.roles.rememberer.RemembererResetHandler;
import org.agmas.noellesroles.roles.robber.RobberResetHandler;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistResetHandler;
import org.agmas.noellesroles.roles.stalker.StalkerResetHandler;
import org.agmas.noellesroles.roles.voodoo.VoodooResetHandler;
import org.agmas.noellesroles.roles.vulture.VultureResetHandler;
import org.agmas.noellesroles.roles.winder.WinderResetHandler;

/**
 * NoellesRoles 玩家/回合重置规则的聚合初始化入口。
 *
 * <p>具体清理逻辑按职业或词条留在自己的包中；本类只调用 init，不直接读任何职业组件。
 * 玩家级清理统一接 Harpy ResetPlayerEvent，世界级开局前清理接 Wathe ON_BEGIN_INITIALIZE，
 * 因而不再需要对 GameFunctions.resetPlayer/initializeGame/finalizeGame 做深层 mixin。</p>
 */
public final class NoellesRolesResetBootstrap {
    private static boolean initialized;

    private NoellesRolesResetBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        AngelResetHandler.init();
        AssassinResetHandler.init();
        BartenderResetHandler.init();
        BomberResetHandler.init();
        ControllerResetHandler.init();
        CoronerResetHandler.init();
        CowardResetHandler.init();
        ExecutionerResetHandler.init();
        MagicianResetHandler.init();
        MorphlingResetHandler.init();
        NoisemakerResetHandler.init();
        OperatorResetHandler.init();
        ProphetResetHandler.init();
        RecallerResetHandler.init();
        RemembererResetHandler.init();
        RobberResetHandler.init();
        SpiritualistResetHandler.init();
        StalkerResetHandler.init();
        VoodooResetHandler.init();
        VultureResetHandler.init();
        WinderResetHandler.init();
        RoundModifierResetHandler.init();
    }
}
