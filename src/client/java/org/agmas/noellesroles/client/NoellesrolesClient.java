package org.agmas.noellesroles.client;

import net.fabricmc.api.ClientModInitializer;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceHandlers;
import org.agmas.noellesroles.client.bootstrap.NoellesClientConnectionBootstrap;
import org.agmas.noellesroles.client.bootstrap.NoellesClientEntityRendererBootstrap;
import org.agmas.noellesroles.client.bootstrap.NoellesClientItemBootstrap;
import org.agmas.noellesroles.client.bootstrap.NoellesClientNetworkingBootstrap;
import org.agmas.noellesroles.client.bootstrap.NoellesClientParticleBootstrap;
import org.agmas.noellesroles.client.bootstrap.NoellesClientPsychoBootstrap;
import org.agmas.noellesroles.client.bootstrap.NoellesClientRoleBootstrap;
import org.agmas.noellesroles.client.bootstrap.NoellesClientTickBootstrap;
import org.agmas.noellesroles.client.crosshair.NoellesCrosshairHandlers;
import org.agmas.noellesroles.client.hud.NoellesHudHandlers;
import org.agmas.noellesroles.client.input.NoellesClientKeybindings;
import org.agmas.noellesroles.client.input.NoellesClientPreAttackHandlers;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.client.inventory.NoellesInventoryButtons;
import org.agmas.noellesroles.client.mood.NoellesPsychosisHandlers;
import org.agmas.noellesroles.client.movement.NoellesClientMovementBootstrap;
import org.agmas.noellesroles.client.visibility.NoellesHeldItemVisibilityHandlers;

/** Fabric 客户端主入口，只负责客户端职责模块的初始化编排。 */
public final class NoellesrolesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        NoellesClientKeybindings.register();
        NoellesInstinctHandlers.register();
        NoellesAppearanceHandlers.register();
        NoellesHudHandlers.register();
        NoellesCrosshairHandlers.register();
        NoellesClientMovementBootstrap.init();
        NoellesHeldItemVisibilityHandlers.register();
        NoellesPsychosisHandlers.register();
        NoellesInventoryButtons.register();
        NoellesClientRoleBootstrap.register();
        NoellesClientPsychoBootstrap.register();
        NoellesClientParticleBootstrap.register();
        NoellesClientItemBootstrap.register();
        NoellesClientNetworkingBootstrap.register();
        NoellesClientConnectionBootstrap.register();
        NoellesClientTickBootstrap.register();
        NoellesClientEntityRendererBootstrap.register();
        NoellesClientPreAttackHandlers.register();
    }
}
