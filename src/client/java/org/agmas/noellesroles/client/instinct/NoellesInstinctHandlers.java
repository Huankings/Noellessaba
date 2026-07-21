package org.agmas.noellesroles.client.instinct;

import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.instinct.killer.KillerNeutralInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.angel.AngelInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.bartender.BartenderInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.bomber.BomberInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.cook.CookInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.dreamer.DreamerInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.executioner.ExecutionerInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.hacker.HackerInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.jester.JesterInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.mimic.MimicInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.physician.PhysicianInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.waiter.WaiterInstinctHandler;
import org.agmas.noellesroles.client.instinct.roles.winder.WinderInstinctHandler;

public final class NoellesInstinctHandlers {
    public static final int PRIORITY_HIGH_INSTINCT_COLOR = 100;
    public static final int PRIORITY_ABILITY_MARK = 100;

    private NoellesInstinctHandlers() {
    }

    public static void register() {
        JesterInstinctHandler.register();
        WinderInstinctHandler.register();
        BartenderInstinctHandler.register();
        AngelInstinctHandler.register();
        ExecutionerInstinctHandler.register();
        BomberInstinctHandler.register();
        MimicInstinctHandler.register();
        WaiterInstinctHandler.register();
        CookInstinctHandler.register();
        PhysicianInstinctHandler.register();
        DreamerInstinctHandler.register();
        HackerInstinctHandler.register();
        KillerNeutralInstinctHandler.register();
    }

    public static Identifier id(String path) {
        return Identifier.of(Noellesroles.MOD_ID, "instinct/" + path);
    }
}
