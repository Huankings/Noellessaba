package org.agmas.noellesroles.client.appearance;

import org.agmas.noellesroles.client.appearance.killer_sided.ExecutionerCohortHudHandler;
import org.agmas.noellesroles.client.appearance.killer_sided.HackerCohortHudHandler;
import org.agmas.noellesroles.client.appearance.killer_sided.KillerSidedNeutralTargetHudHandler;
import org.agmas.noellesroles.client.appearance.modifiers.dual_personality.DualPersonalityAppearanceHandler;
import org.agmas.noellesroles.client.appearance.modifiers.lovers.LoversHudHandler;
import org.agmas.noellesroles.client.appearance.roles.amnesiac.AmnesiacBodyHudHandler;
import org.agmas.noellesroles.client.appearance.roles.arsonist.ArsonistTargetHudHandler;
import org.agmas.noellesroles.client.appearance.roles.controller.ControllerAppearanceHandler;
import org.agmas.noellesroles.client.appearance.roles.convener.ConvenerAppearanceHandler;
import org.agmas.noellesroles.client.appearance.roles.convener.ConvenerBodyHudHandler;
import org.agmas.noellesroles.client.appearance.roles.coroner.CoronerAppearanceHandler;
import org.agmas.noellesroles.client.appearance.roles.coroner.CoronerBodyExamineHudHandler;
import org.agmas.noellesroles.client.appearance.roles.coroner.CoronerBodyHudHandler;
import org.agmas.noellesroles.client.appearance.roles.detective.DetectiveTargetHudHandler;
import org.agmas.noellesroles.client.appearance.roles.insane_observer.InsaneObserverAppearanceHandler;
import org.agmas.noellesroles.client.appearance.roles.insane_damned_paranoid_killer.InsaneDamnedKillerAppearanceHandler;
import org.agmas.noellesroles.client.appearance.roles.morphling.MorphlingAppearanceHandler;
import org.agmas.noellesroles.client.appearance.roles.hacker.HackerTargetHudHandler;
import org.agmas.noellesroles.client.appearance.roles.muzzler.MuzzlerSilencedTipHudHandler;
import org.agmas.noellesroles.client.appearance.roles.necromancer.NecromancerBodyHudHandler;
import org.agmas.noellesroles.client.appearance.roles.physician.PhysicianBodyHudHandler;
import org.agmas.noellesroles.client.appearance.roles.shadow_jester.ShadowJesterAppearanceHandler;
import org.agmas.noellesroles.client.appearance.roles.spiritualist.SpiritualistAppearanceHandler;
import org.agmas.noellesroles.client.appearance.roles.timekeeper.TimekeeperRiftAppearanceHandler;
import org.agmas.noellesroles.client.appearance.shared.InvisibleNameHudHandler;

/**
 * NoellesRoles 接入 Wathe 外观 / 准心名字 API 的总入口。
 *
 * <p>这个类只负责聚合注册顺序，具体实现都拆到对应职业或词条类里：
 * 需要维护某个职业时直接打开对应 handler，不再把皮肤、名字、HUD 同伙规则堆在一个大类里。</p>
 */
public final class NoellesAppearanceHandlers {
    private NoellesAppearanceHandlers() {
    }

    public static void register() {
        InsaneDamnedKillerAppearanceHandler.register();
        TimekeeperRiftAppearanceHandler.register();
        SpiritualistAppearanceHandler.register();
        DualPersonalityAppearanceHandler.register();
        InsaneObserverAppearanceHandler.register();
        ConvenerAppearanceHandler.register();
        MorphlingAppearanceHandler.register();
        ShadowJesterAppearanceHandler.register();
        ControllerAppearanceHandler.register();
        CoronerAppearanceHandler.register();
        CoronerBodyHudHandler.register();
        CoronerBodyExamineHudHandler.register();
        InvisibleNameHudHandler.register();
        ExecutionerCohortHudHandler.register();
        HackerCohortHudHandler.register();
        KillerSidedNeutralTargetHudHandler.register();
        HackerTargetHudHandler.register();
        DetectiveTargetHudHandler.register();
        PhysicianBodyHudHandler.register();
        NecromancerBodyHudHandler.register();
        MuzzlerSilencedTipHudHandler.register();
        AmnesiacBodyHudHandler.register();
        ArsonistTargetHudHandler.register();
        ConvenerBodyHudHandler.register();
        LoversHudHandler.register();
    }
}
