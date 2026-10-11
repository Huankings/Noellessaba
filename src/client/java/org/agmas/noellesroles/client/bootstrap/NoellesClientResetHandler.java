package org.agmas.noellesroles.client.bootstrap;

import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceClientState;
import org.agmas.noellesroles.client.appearance.roles.insane_observer.InsaneObserverClientState;
import org.agmas.noellesroles.client.items.NoellesRolesGrenadeInputHandler;
import org.agmas.noellesroles.client.modifiers.dual_personality.DualPersonalityClientState;
import org.agmas.noellesroles.client.modifiers.dual_personality.DualPersonalityKeybinds;
import org.agmas.noellesroles.client.roles.coward.CowardClientEffects;
import org.agmas.noellesroles.client.roles.executioner.ExecutionerMoodHud;
import org.agmas.noellesroles.client.roles.jason.JasonAbilityClientEffects;
import org.agmas.noellesroles.client.roles.jason.JasonAbilityClientSoundController;
import org.agmas.noellesroles.client.roles.licensed_villain.LicensedVillainMusicController;
import org.agmas.noellesroles.client.roles.myers.MyersAbilityInputHandler;
import org.agmas.noellesroles.client.roles.outlaw.OutlawMusicController;
import org.agmas.noellesroles.client.roles.rememberer.RemembererClientEffects;
import org.agmas.noellesroles.client.roles.shadow_jester.ShadowJesterMusicController;
import org.agmas.noellesroles.client.roles.stalker.StalkerAbilityInputHandler;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.agmas.noellesroles.client.roles.timekeeper.TimekeeperWatchInputHandler;
import org.agmas.noellesroles.client.roles.vulture.VultureAbilityInputHandler;
import org.agmas.noellesroles.client.ui.modifiers.guesser.GuesserPlayerWidget;
import org.agmas.noellesroles.client.ui.roles.corpsemaker.CorpsemakerState;
import org.agmas.noellesroles.client.ui.roles.operator.OperatorPlayerWidget;
import org.agmas.noellesroles.client.ui.roles.swapper.SwapperPlayerWidget;

/** JOIN/DISCONNECT 时统一清理客户端临时状态。 */
public final class NoellesClientResetHandler {
    private NoellesClientResetHandler() {
    }

    public static void register() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    public static void reset() {
        NoellesAppearanceClientState.reset();
        InsaneObserverClientState.reset();
        CowardClientEffects.reset();
        GuesserPlayerWidget.selectedPlayer = null;
        SwapperPlayerWidget.playerChoiceOne = null;
        OperatorPlayerWidget.firstChoice = null;
        CorpsemakerState.reset();
        SpiritualistClientController.reset();
        RemembererClientEffects.reset();
        JasonAbilityClientEffects.reset();
        JasonAbilityClientSoundController.reset(MinecraftClient.getInstance());
        ShadowJesterMusicController.reset(MinecraftClient.getInstance());
        LicensedVillainMusicController.reset(MinecraftClient.getInstance());
        OutlawMusicController.reset(MinecraftClient.getInstance());
        DualPersonalityClientState.resetTransientRenderState();
        DualPersonalityKeybinds.resetSyncedState();
        ExecutionerMoodHud.reset();
        MyersAbilityInputHandler.reset();
        StalkerAbilityInputHandler.reset();
        VultureAbilityInputHandler.reset();
        NoellesRolesGrenadeInputHandler.reset();
        TimekeeperWatchInputHandler.reset();
    }
}
