package org.agmas.noellesroles.modifiers.magnate;

import dev.doctor4t.wathe.api.economy.EconomyApi;
import net.minecraft.util.Identifier;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.Noellesroles;

public final class MagnateEconomyHandler {
    private static boolean initialized = false;

    private MagnateEconomyHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        EconomyApi.registerPassiveIncomeModifier(
                Identifier.of(Noellesroles.MOD_ID, "magnate_double_passive_income"),
                EconomyApi.DEFAULT_PRIORITY,
                (context, currentIncome) -> {
                    WorldModifierComponent modifier = WorldModifierComponent.KEY.get(context.world());
                    if (!modifier.isModifier(context.player(), Noellesroles.MAGNATE)) {
                        return currentIncome;
                    }

                    /*
                     * Wathe 的被动收入修改器发生在“最终套用阵营金币上限”之前。
                     * 富豪只表达“本次收入应该多拿一份基础值”，实际余额上限仍交给 Wathe 统一裁剪，
                     * 这样金币接近上限时不会因为词条倍增而突破本体经济规则。
                     */
                    return currentIncome + context.baseIncome() * MagnateConstants.EXTRA_BASE_INCOME_COPIES;
                }
        );
    }
}
