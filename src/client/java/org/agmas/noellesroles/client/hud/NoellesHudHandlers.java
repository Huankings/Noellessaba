package org.agmas.noellesroles.client.hud;

import org.agmas.noellesroles.client.hud.modifiers.dual_personality.DualPersonalityPartnerHud;
import org.agmas.noellesroles.client.hud.modifiers.lovers.LoversPartnerHud;
import org.agmas.noellesroles.client.roles.angel.AngelStatusHud;
import org.agmas.noellesroles.client.roles.avaricious.AvariciousStatusHud;
import org.agmas.noellesroles.client.roles.bellringer.BellringerStatusHud;
import org.agmas.noellesroles.client.roles.bounty_hunter.BountyHunterTargetHud;
import org.agmas.noellesroles.client.roles.cleaner.CleanerStatusHud;
import org.agmas.noellesroles.client.roles.conductor.MasterKeyHud;
import org.agmas.noellesroles.client.roles.controller.ControlledStatusHud;
import org.agmas.noellesroles.client.roles.convener.ConvenerStatusHud;
import org.agmas.noellesroles.client.roles.detective.DetectiveStatusHud;
import org.agmas.noellesroles.client.roles.dreamer.DreamerStatusHud;
import org.agmas.noellesroles.client.roles.executioner.ExecutionerTargetHud;
import org.agmas.noellesroles.client.roles.hunter.HunterStatusHud;
import org.agmas.noellesroles.client.roles.kidnapper.KidnapperControlledHud;
import org.agmas.noellesroles.client.roles.magician.MagicianPlaybackNameHud;
import org.agmas.noellesroles.client.roles.magician.MagicianStatusHud;
import org.agmas.noellesroles.client.roles.morphling.MorphlingStatusHud;
import org.agmas.noellesroles.client.roles.phantom.PhantomStatusHud;
import org.agmas.noellesroles.client.roles.prophet.ProphetStatusHud;
import org.agmas.noellesroles.client.roles.recaller.RecallerStatusHud;
import org.agmas.noellesroles.client.roles.rememberer.RemembererSniperScopeHud;
import org.agmas.noellesroles.client.roles.rememberer.RemembererStatusHud;
import org.agmas.noellesroles.client.roles.robot.RobotStatusHud;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistStatusHud;
import org.agmas.noellesroles.client.roles.stalker.StalkerStatusHud;
import org.agmas.noellesroles.client.roles.starstruck.StarstruckStatusHud;
import org.agmas.noellesroles.client.roles.thief.ThiefStatusHud;
import org.agmas.noellesroles.client.roles.vulture.VultureStatusHud;
import org.agmas.noellesroles.client.roles.winder.WinderStatusHud;

/**
 * NoellesRoles 通用屏幕 HUD 注册入口。
 *
 * <p>本类只维护注册顺序。职业/词条自己的 HUD 逻辑按职业或词条拆在各自包里，
 * 这样从旧 mixin 迁到 Wathe HUD API 后，后续维护仍然可以按职业定位。</p>
 */
public final class NoellesHudHandlers {
    private NoellesHudHandlers() {
    }

    public static void register() {
        LoversPartnerHud.register();
        DualPersonalityPartnerHud.register();
        ControlledStatusHud.register();
        KidnapperControlledHud.register();
        BellringerStatusHud.register();
        MorphlingStatusHud.register();
        MagicianStatusHud.register();
        MagicianPlaybackNameHud.register();
        PhantomStatusHud.register();
        WinderStatusHud.register();
        RecallerStatusHud.register();
        DetectiveStatusHud.register();
        ProphetStatusHud.register();
        VultureStatusHud.register();
        SpiritualistStatusHud.register();
        AngelStatusHud.register();
        RemembererStatusHud.register();
        RemembererSniperScopeHud.register();
        StalkerStatusHud.register();
        StarstruckStatusHud.register();
        AvariciousStatusHud.register();
        CleanerStatusHud.register();
        HunterStatusHud.register();
        MasterKeyHud.register();
        BountyHunterTargetHud.register();
        ExecutionerTargetHud.register();
        ConvenerStatusHud.register();
        RobotStatusHud.register();
        ThiefStatusHud.register();
        DreamerStatusHud.register();
    }
}
