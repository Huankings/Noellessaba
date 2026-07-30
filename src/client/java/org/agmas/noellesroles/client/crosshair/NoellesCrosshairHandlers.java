package org.agmas.noellesroles.client.crosshair;

import org.agmas.noellesroles.client.roles.assassin.AssassinBayonetCrosshair;
import org.agmas.noellesroles.client.roles.bounty_hunter.BountyHunterGunCrosshair;
import org.agmas.noellesroles.client.roles.cook.CookPanCrosshair;
import org.agmas.noellesroles.client.roles.drugmaker.DrugmakerCrosshair;
import org.agmas.noellesroles.client.roles.hunter.HunterKnifeCrosshair;
import org.agmas.noellesroles.client.roles.morphling.MorphlingReagentCrosshair;
import org.agmas.noellesroles.client.roles.prophet.ProphetCrystalBallCrosshair;
import org.agmas.noellesroles.client.roles.rememberer.RemembererRecallCrosshair;
import org.agmas.noellesroles.client.roles.rememberer.RemembererSniperCrosshair;
import org.agmas.noellesroles.client.roles.robber.RobberGunCrosshair;
import org.agmas.noellesroles.client.roles.thief.ThiefCrosshair;
import org.agmas.noellesroles.client.roles.timekeeper.TimekeeperWatchCrosshairOverlay;
import org.agmas.noellesroles.client.roles.waiter.WaiterCrosshair;

/**
 * NoellesRoles 接入 Wathe 准心图标 API 的总入口。
 *
 * <p>本类只维护注册顺序。具体的准心条件、目标检测和绘制细节继续按职业拆在
 * {@code client/roles/<role>} 包里，避免把不同职业的 UI 逻辑重新塞回一个大类。</p>
 */
public final class NoellesCrosshairHandlers {
    private NoellesCrosshairHandlers() {
    }

    public static void register() {
        ProphetCrystalBallCrosshair.register();
        RobberGunCrosshair.register();
        BountyHunterGunCrosshair.register();
        AssassinBayonetCrosshair.register();
        RemembererRecallCrosshair.register();
        RemembererSniperCrosshair.register();
        MorphlingReagentCrosshair.register();
        WaiterCrosshair.register();
        CookPanCrosshair.register();
        HunterKnifeCrosshair.register();
        DrugmakerCrosshair.register();
        ThiefCrosshair.register();
        TimekeeperWatchCrosshairOverlay.register();
    }
}
