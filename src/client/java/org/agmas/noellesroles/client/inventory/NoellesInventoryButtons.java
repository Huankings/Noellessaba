package org.agmas.noellesroles.client.inventory;

import org.agmas.noellesroles.client.ui.modifiers.guesser.GuesserInventoryButtons;
import org.agmas.noellesroles.client.ui.roles.brainwasher.BrainwasherInventoryButtons;
import org.agmas.noellesroles.client.ui.roles.controller.ControllerInventoryButtons;
import org.agmas.noellesroles.client.ui.roles.convener.ConvenerInventoryButtons;
import org.agmas.noellesroles.client.ui.roles.coroner.CoronerInventoryButtons;
import org.agmas.noellesroles.client.ui.roles.corpsemaker.CorpsemakerInventoryButtons;
import org.agmas.noellesroles.client.ui.roles.goddess.GoddessInventoryButtons;
import org.agmas.noellesroles.client.ui.roles.magician.MagicianInventoryButtons;
import org.agmas.noellesroles.client.ui.roles.morphling.MorphlingInventoryButtons;
import org.agmas.noellesroles.client.ui.roles.noisemaker.NoisemakerInventoryButtons;
import org.agmas.noellesroles.client.ui.roles.operator.OperatorInventoryButtons;
import org.agmas.noellesroles.client.ui.roles.swapper.SwapperInventoryButtons;
import org.agmas.noellesroles.client.ui.roles.voodoo.VoodooInventoryButtons;
import org.agmas.noellesroles.client.ui.roles.winder.WinderInventoryButtons;

/**
 * NoellesRoles 背包按钮注册入口。
 *
 * <p>具体按钮逻辑按职业拆到各自的 {@code client.ui.roles.<role>} 包里；
 * 这里仅保留集中注册顺序，避免再次变成维护困难的巨型总类。</p>
 */
public final class NoellesInventoryButtons {
    private NoellesInventoryButtons() {
    }

    public static void register() {
        SwapperInventoryButtons.register();
        MorphlingInventoryButtons.register();
        VoodooInventoryButtons.register();
        WinderInventoryButtons.register();
        MagicianInventoryButtons.register();
        OperatorInventoryButtons.register();
        CoronerInventoryButtons.register();
        BrainwasherInventoryButtons.register();
        GoddessInventoryButtons.register();
        NoisemakerInventoryButtons.register();
        ControllerInventoryButtons.register();
        GuesserInventoryButtons.register();
        CorpsemakerInventoryButtons.register();
        ConvenerInventoryButtons.register();
    }
}
