package org.agmas.noellesroles.roles.bartender;

import dev.doctor4t.wathe.api.tray.TrayTakeRegistry;
import dev.doctor4t.wathe.api.tray.TrayTakeDecision;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.item.CocktailItem;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 酒保从托盘取鸡尾酒时，背包和主手合计最多保留三份。 */
public final class BartenderTrayTakeRules {
    private BartenderTrayTakeRules() {
    }

    public static void init() {
        TrayTakeRegistry.registerGroupRule("noellesroles:bartender", 110, context ->
                GameWorldComponent.KEY.get(context.player().getWorld()).isRole(context.player(), NoellesRoleRegistry.BARTENDER)
                        && context.candidate().getItem() instanceof CocktailItem
                        ? new TrayTakeDecision("noellesroles:bartender:cocktail", 3, TrayTakeDecision.Mode.DISTINCT_TYPES)
                        : null);
    }
}
