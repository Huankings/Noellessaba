package org.agmas.noellesroles.client.bootstrap;

import org.agmas.noellesroles.client.modifiers.dual_personality.DualPersonalityKeybinds;
import org.agmas.noellesroles.client.modifiers.dual_personality.DualPersonalityTimeHud;
import org.agmas.noellesroles.client.roles.convener.ConvenerMoodHud;
import org.agmas.noellesroles.client.roles.cook.CookMoodHud;
import org.agmas.noellesroles.client.roles.dreamer.DreamerMoodHud;
import org.agmas.noellesroles.client.roles.executioner.ExecutionerMoodHud;
import org.agmas.noellesroles.client.roles.hacker.HackerMoodHud;
import org.agmas.noellesroles.client.roles.jason.JasonAbilityFogHandler;
import org.agmas.noellesroles.client.roles.jason.JasonMoodHud;
import org.agmas.noellesroles.client.roles.jester.JesterMoodHud;
import org.agmas.noellesroles.client.roles.licensed_villain.LicensedVillainMoodHud;
import org.agmas.noellesroles.client.roles.lich.LichMoodHud;
import org.agmas.noellesroles.client.roles.outlaw.OutlawTimeHud;
import org.agmas.noellesroles.client.roles.rememberer.RemembererMoodHud;
import org.agmas.noellesroles.client.roles.robot.RobotMoodHud;
import org.agmas.noellesroles.client.roles.shadow_jester.ShadowJesterMoodHud;
import org.agmas.noellesroles.client.roles.spring_trap.SpringTrapMoodHud;
import org.agmas.noellesroles.client.roles.starstruck.StarstruckMoodHud;
import org.agmas.noellesroles.client.roles.vecna.VecnaMoodHud;
import org.agmas.noellesroles.client.roles.killer_queen.KillerQueenMoodHud;

/**
 * 维护旧客户端入口中仍属于职业/词条的注册调用。
 *
 * <p>这些类自身已经按职业拆分，本聚合器只保留调用顺序，避免职责重新回到
 * Fabric 客户端入口。</p>
 */
public final class NoellesClientRoleBootstrap {
    private NoellesClientRoleBootstrap() {
    }

    public static void register() {
        DualPersonalityTimeHud.register();
        OutlawTimeHud.register();
        DualPersonalityKeybinds.init();
        ExecutionerMoodHud.register();
        JesterMoodHud.register();
        RemembererMoodHud.register();
        DreamerMoodHud.register();
        HackerMoodHud.register();
        CookMoodHud.register();
        StarstruckMoodHud.register();
        RobotMoodHud.register();
        ConvenerMoodHud.register();
        LicensedVillainMoodHud.register();
        JasonMoodHud.register();
        LichMoodHud.register();
        VecnaMoodHud.register();
        JasonAbilityFogHandler.register();
        SpringTrapMoodHud.register();
        ShadowJesterMoodHud.register();
        KillerQueenMoodHud.register();
    }
}
