package org.agmas.noellesroles.client.instinct;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.instinct.killer.KillerNeutralInstinctHandler;
import org.agmas.noellesroles.client.instinct.modifiers.allergic.AllergicInstinctHandler;
import org.agmas.noellesroles.client.instinct.modifiers.dual_personality.DualPersonalityInstinctHandler;
import org.agmas.noellesroles.client.instinct.modifiers.lovers.LoversInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.angel.AngelInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.amnesiac.AmnesiacInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.arsonist.ArsonistInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.bartender.BartenderInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.bomber.BomberInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.bounty_hunter.BountyHunterInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.convener.ConvenerInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.cook.CookInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.drugmaker.DrugmakerInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.dreamer.DreamerInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.executioner.ExecutionerInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.hacker.HackerInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.initiate.InitiateInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.jason.JasonInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.insane_damned_paranoid_killer.InsaneDamnedKillerInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.jester.JesterInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.licensed_villain.LicensedVillainInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.mimic.MimicInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.morphling.MorphlingInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.physician.PhysicianInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.starstruck.StarstruckInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.spring_trap.SpringTrapInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.thief.ThiefInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.timekeeper.TimekeeperRiftInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.waiter.WaiterInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.winder.WinderInstinctHandler;

public final class NoellesInstinctHandlers {
    public static final int PRIORITY_HIGH_INSTINCT_COLOR = 100;
    public static final int PRIORITY_ABILITY_MARK = 100;
    public static final int PRIORITY_ROLE_INSTINCT_COLOR = 150;
    public static final int PRIORITY_SPECIAL_NEUTRAL_COLOR = 200;
    public static final int PRIORITY_CONVENER_COLOR = 1000;
    public static final int PRIORITY_DUAL_PERSONALITY = 10000;
    public static final int PRIORITY_CONVENER_SUPPRESSION = 20000;
    public static final int PRIORITY_TIMEKEEPER_RIFT_SUPPRESSION = 30000;
    public static final int PRIORITY_CORPSE_DISGUISE_SUPPRESSION = 29950;

    private NoellesInstinctHandlers() {
    }

    public static void register() {
        TimekeeperRiftInstinctHandler.register();
        InsaneDamnedKillerInstinctHandler.register();
        SpringTrapInstinctHandler.register();
        JasonInstinctHandler.register();
        JesterInstinctHandler.register();
        WinderInstinctHandler.register();
        BartenderInstinctHandler.register();
        AngelInstinctHandler.register();
        ExecutionerInstinctHandler.register();
        BountyHunterInstinctHandler.register();
        BomberInstinctHandler.register();
        MimicInstinctHandler.register();
        MorphlingInstinctHandler.register();
        WaiterInstinctHandler.register();
        CookInstinctHandler.register();
        PhysicianInstinctHandler.register();
        DreamerInstinctHandler.register();
        HackerInstinctHandler.register();
        StarstruckInstinctHandler.register();
        DrugmakerInstinctHandler.register();
        AmnesiacInstinctHandler.register();
        ArsonistInstinctHandler.register();
        ConvenerInstinctHandler.register();
        InitiateInstinctHandler.register();
        ThiefInstinctHandler.register();
        LicensedVillainInstinctHandler.register();
        AllergicInstinctHandler.register();
        LoversInstinctHandler.register();
        DualPersonalityInstinctHandler.register();
        KillerNeutralInstinctHandler.register();
    }

    public static Identifier id(String path) {
        return Identifier.of(NoellesRolesCore.MOD_ID, "instinct/" + path);
    }
}
