package org.agmas.noellesroles.roles.cook;

import dev.doctor4t.wathe.api.tray.TrayTakeRegistry;
import dev.doctor4t.wathe.api.tray.TrayTakeDecision;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import net.minecraft.component.DataComponentTypes;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 厨师从托盘取普通可食用食物时，背包和主手合计最多保留三份。 */
public final class CookTrayTakeRules {
    private CookTrayTakeRules() {
    }

    public static void init() {
        TrayTakeRegistry.registerGroupRule("noellesroles:cook", 110, context -> {
            boolean cook = GameWorldComponent.KEY.get(context.player().getWorld()).isRole(context.player(), NoellesRoleRegistry.COOK);
            boolean food = context.candidate().get(DataComponentTypes.FOOD) != null
                    && !(context.candidate().getItem() instanceof dev.doctor4t.wathe.item.CocktailItem);
            return cook && food
                    ? new TrayTakeDecision("noellesroles:cook:food", 3, TrayTakeDecision.Mode.DISTINCT_TYPES)
                    : null;
        });
    }
}
