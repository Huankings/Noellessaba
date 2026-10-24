package org.agmas.noellesroles.reset;

import org.agmas.noellesroles.modifiers.RoundModifierResetHandler;
import org.agmas.noellesroles.framing.FramingResetHandler;
import org.agmas.noellesroles.roles.angel.AngelResetHandler;
import org.agmas.noellesroles.roles.assassin.AssassinResetHandler;
import org.agmas.noellesroles.roles.bartender.BartenderResetHandler;
import org.agmas.noellesroles.roles.bomber.BomberResetHandler;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterResetHandler;
import org.agmas.noellesroles.roles.controller.ControllerResetHandler;
import org.agmas.noellesroles.roles.convener.ConvenerResetHandler;
import org.agmas.noellesroles.roles.cook.CookResetHandler;
import org.agmas.noellesroles.roles.coroner.CoronerResetHandler;
import org.agmas.noellesroles.roles.coward.CowardResetHandler;
import org.agmas.noellesroles.roles.dreamer.DreamerResetHandler;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerResetHandler;
import org.agmas.noellesroles.roles.engineer.EngineerResetHandler;
import org.agmas.noellesroles.roles.executioner.ExecutionerResetHandler;
import org.agmas.noellesroles.roles.hacker.HackerResetHandler;
import org.agmas.noellesroles.roles.hunter.HunterResetHandler;
import org.agmas.noellesroles.roles.insane_damned_paranoid_killer.InsaneDamnedKillerResetHandler;
import org.agmas.noellesroles.roles.jason.JasonResetHandler;
import org.agmas.noellesroles.roles.kidnapper.KidnapperResetHandler;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenResetHandler;
import org.agmas.noellesroles.roles.licensed_villain.LicensedVillainResetHandler;
import org.agmas.noellesroles.roles.magician.MagicianResetHandler;
import org.agmas.noellesroles.roles.morphling.MorphlingResetHandler;
import org.agmas.noellesroles.roles.muzzler.MuzzlerResetHandler;
import org.agmas.noellesroles.roles.myers.MyersResetHandler;
import org.agmas.noellesroles.roles.noisemaker.NoisemakerResetHandler;
import org.agmas.noellesroles.roles.operator.OperatorResetHandler;
import org.agmas.noellesroles.roles.outlaw.OutlawResetHandler;
import org.agmas.noellesroles.roles.perceiver.PerceiverResetHandler;
import org.agmas.noellesroles.roles.phantom.PhantomResetHandler;
import org.agmas.noellesroles.roles.physician.PhysicianResetHandler;
import org.agmas.noellesroles.roles.prophet.ProphetResetHandler;
import org.agmas.noellesroles.roles.recaller.RecallerResetHandler;
import org.agmas.noellesroles.roles.rememberer.RemembererResetHandler;
import org.agmas.noellesroles.roles.robber.RobberResetHandler;
import org.agmas.noellesroles.roles.robot.RobotResetHandler;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterResetHandler;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapResetHandler;
import org.agmas.noellesroles.roles.starstruck.StarstruckResetHandler;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistResetHandler;
import org.agmas.noellesroles.roles.stalker.StalkerResetHandler;
import org.agmas.noellesroles.roles.swapper.SwapperResetHandler;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperResetHandler;
import org.agmas.noellesroles.roles.trapper.TrapperResetHandler;
import org.agmas.noellesroles.roles.vecna.VecnaResetHandler;
import org.agmas.noellesroles.roles.voodoo.VoodooResetHandler;
import org.agmas.noellesroles.roles.vulture.VultureResetHandler;
import org.agmas.noellesroles.roles.winder.WinderResetHandler;
import org.agmas.noellesroles.roles.waiter.WaiterResetHandler;
import org.agmas.noellesroles.roles.arsonist.ArsonistResetHandler;
import org.agmas.noellesroles.roles.party_animal.PartyAnimalResetHandler;

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
        ArsonistResetHandler.init();
        AssassinResetHandler.init();
        BartenderResetHandler.init();
        BomberResetHandler.init();
        BountyHunterResetHandler.init();
        ConvenerResetHandler.init();
        ControllerResetHandler.init();
        CookResetHandler.init();
        CoronerResetHandler.init();
        CowardResetHandler.init();
        DreamerResetHandler.init();
        DrugmakerResetHandler.init();
        EngineerResetHandler.init();
        ExecutionerResetHandler.init();
        FramingResetHandler.init();
        HackerResetHandler.init();
        HunterResetHandler.init();
        InsaneDamnedKillerResetHandler.init();
        JasonResetHandler.init();
        KidnapperResetHandler.init();
        KillerQueenResetHandler.init();
        LicensedVillainResetHandler.init();
        MagicianResetHandler.init();
        MorphlingResetHandler.init();
        MuzzlerResetHandler.init();
        MyersResetHandler.init();
        NoisemakerResetHandler.init();
        OperatorResetHandler.init();
        OutlawResetHandler.init();
        PerceiverResetHandler.init();
        PhantomResetHandler.init();
        PhysicianResetHandler.init();
        ProphetResetHandler.init();
        RecallerResetHandler.init();
        RemembererResetHandler.init();
        RobberResetHandler.init();
        RobotResetHandler.init();
        ShadowJesterResetHandler.init();
        SpringTrapResetHandler.init();
        StarstruckResetHandler.init();
        SpiritualistResetHandler.init();
        StalkerResetHandler.init();
        SwapperResetHandler.init();
        TimekeeperResetHandler.init();
        TrapperResetHandler.init();
        VecnaResetHandler.init();
        VoodooResetHandler.init();
        VultureResetHandler.init();
        WaiterResetHandler.init();
        WinderResetHandler.init();
        RoundModifierResetHandler.init();
        PartyAnimalResetHandler.init();
    }
}
