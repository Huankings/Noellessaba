package org.agmas.noellesroles.bootstrap;

import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.NoellesRolesParticles;
import org.agmas.noellesroles.bed.NoellesRolesBedEffects;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.registry.NoellesFramingShopEntries;
import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.roles.amnesiac.AmnesiacRoleSelectionHandler;
import org.agmas.noellesroles.roles.arsonist.ArsonistReplayTracker;
import org.agmas.noellesroles.roles.arsonist.ArsonistVictoryRule;
import org.agmas.noellesroles.roles.arsonist.OilDousingHandler;
import org.agmas.noellesroles.roles.convener.ConvenerCommunicationManager;
import org.agmas.noellesroles.roles.convener.ConvenerSummonHandler;
import org.agmas.noellesroles.roles.convener.ConvenerTaskShieldHandler;
import org.agmas.noellesroles.roles.convener.ConvenerVictoryRule;
import org.agmas.noellesroles.roles.dreamer.DreamerDelusionHandler;
import org.agmas.noellesroles.roles.hacker.HackerSafeTimeComponent;
import org.agmas.noellesroles.roles.magician.MagicianPlaybackManager;
import org.agmas.noellesroles.roles.muzzler.MuzzlerInteractionHandler;
import org.agmas.noellesroles.roles.necromancer.NecromancerRevivalHandler;
import org.agmas.noellesroles.roles.necromancer.NecromancerRoleLimitHandler;
import org.agmas.noellesroles.roles.operator.OperatorCommunicationManager;
import org.agmas.noellesroles.roles.physician.PhysicianStatusAlertHandler;
import org.agmas.noellesroles.roles.rememberer.RemembererInteractionHandler;
import org.agmas.noellesroles.roles.rememberer.RemembererSniperManager;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistCommunicationManager;
import org.agmas.noellesroles.roles.starstruck.StarstruckAbility;
import org.agmas.noellesroles.roles.waiter.WaiterInteractionHandler;
import org.agmas.noellesroles.shop.NoellesRolesShopBootstrap;
import org.agmas.noellesroles.tray.NoellesRolesTrayEffects;

/**
 * NoellesRoles 的总启动编排器。
 *
 * <p>入口类只需要调用这里，剩下的初始化顺序都留在这个类里维护。</p>
 */
public final class NoellesRolesBootstrap {
    private static boolean initialized = false;

    private NoellesRolesBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NoellesRoleGroups.init();
        NoellesRolesConfig.HANDLER.load();
        ModItems.init();
        NoellesFramingShopEntries.init();
        NoellesRolesEntities.init();
        NoellesRolesParticles.init();
        NoellesRolesTrayEffects.register();
        NoellesRolesBedEffects.register();
        NoellesRolesReplayBootstrap.register();
        SpiritualistCommunicationManager.init();
        OperatorCommunicationManager.init();
        RemembererInteractionHandler.init();
        RemembererSniperManager.init();
        DreamerDelusionHandler.init();
        PhysicianStatusAlertHandler.init();
        HackerSafeTimeComponent.init();
        WaiterInteractionHandler.init();
        StarstruckAbility.registerTaskCompletionApi();
        MuzzlerInteractionHandler.init();
        NecromancerRevivalHandler.init();
        NecromancerRoleLimitHandler.init();
        MagicianPlaybackManager.init();
        AmnesiacRoleSelectionHandler.init();
        OilDousingHandler.init();
        ArsonistReplayTracker.init();
        ArsonistVictoryRule.init();
        ConvenerCommunicationManager.init();
        ConvenerSummonHandler.init();
        ConvenerTaskShieldHandler.init();
        ConvenerVictoryRule.init();
        NoellesRoleLimitsBootstrap.initStaticLimits();
        NoellesRolesPayloadTypes.register();
        NoellesRolesShopBootstrap.init();
        NoellesRolesEconomyBootstrap.init();
        NoellesRolesEventBootstrap.init();
        NoellesRolesPacketReceivers.register();
    }
}
