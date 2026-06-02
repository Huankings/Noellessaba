package org.agmas.noellesroles.shop;

import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.Noisemaker.NoisemakerShopHandler;
import org.agmas.noellesroles.roles.assassin.AssassinShopHandler;
import org.agmas.noellesroles.roles.bartender.BartenderShopHandler;
import org.agmas.noellesroles.roles.bomber.BomberShopHandler;
import org.agmas.noellesroles.roles.controller.ControllerShopHandler;
import org.agmas.noellesroles.roles.coward.CowardShopHandler;
import org.agmas.noellesroles.roles.coroner.CoronerShopHandler;
import org.agmas.noellesroles.roles.engineer.EngineerShopHandler;
import org.agmas.noellesroles.roles.prophet.ProphetShopHandler;
import org.agmas.noellesroles.roles.recaller.RecallerShopHandler;
import org.agmas.noellesroles.roles.robber.RobberShopHandler;
import org.agmas.noellesroles.roles.stalker.StalkerShopHandler;
import org.agmas.noellesroles.roles.trapper.TrapperShopHandler;
import org.agmas.noellesroles.roles.winder.WinderShopHandler;

/**
 * 统一注册 NoellesRoles 角色商店。
 *
 * <p>后续如果新增一个有专属商店的职业，优先在这里注册：</p>
 * <p>1. 固定商店用 registerStatic</p>
 * <p>2. 动态商店用 register</p>
 */
public final class NoellesRolesShopBootstrap {

    private NoellesRolesShopBootstrap() {
    }

    public static void init() {
        // 好人阵营静态商店。
        NoellesRolesShopRegistry.registerStatic(Noellesroles.BARTENDER, BartenderShopHandler::getShopEntries);
        NoellesRolesShopRegistry.registerStatic(Noellesroles.ENGINEER, EngineerShopHandler::getShopEntries);
        NoellesRolesShopRegistry.registerStatic(Noellesroles.RECALLER, RecallerShopHandler::getShopEntries);
        NoellesRolesShopRegistry.registerStatic(Noellesroles.PROPHET, ProphetShopHandler::getShopEntries);
        NoellesRolesShopRegistry.registerStatic(Noellesroles.TRAPPER, TrapperShopHandler::getShopEntries);
        NoellesRolesShopRegistry.registerStatic(Noellesroles.WINDER, WinderShopHandler::getShopEntries);
        NoellesRolesShopRegistry.registerStatic(Noellesroles.NOISEMAKER, NoisemakerShopHandler::getShopEntries);
        NoellesRolesShopRegistry.registerStatic(Noellesroles.CORONER, CoronerShopHandler::getShopEntries);
        NoellesRolesShopRegistry.registerStatic(Noellesroles.COWARD, CowardShopHandler::getShopEntries);

        // 杀手阵营静态商店。
        NoellesRolesShopRegistry.registerStatic(Noellesroles.BOMBER, BomberShopHandler::getShopEntries);
        NoellesRolesShopRegistry.registerStatic(Noellesroles.ROBBER, RobberShopHandler::getShopEntries);
        NoellesRolesShopRegistry.registerStatic(Noellesroles.CONTROLLER, ControllerShopHandler::getShopEntries);
        NoellesRolesShopRegistry.registerStatic(Noellesroles.ASSASSIN, AssassinShopHandler::getShopEntries);

        // 共用一套伪装商店的职业。
        NoellesRolesShopRegistry.registerStatic(
                () -> Noellesroles.FRAMING_ROLES_SHOP,
                Noellesroles.MIMIC,
                Noellesroles.EXECUTIONER,
                Noellesroles.JESTER
        );

        // 动态商店：按阶段状态实时变化。
        NoellesRolesShopRegistry.register(Noellesroles.STALKER, StalkerShopHandler::getShopEntries);
    }
}
