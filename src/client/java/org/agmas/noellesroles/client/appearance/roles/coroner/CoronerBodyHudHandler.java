package org.agmas.noellesroles.client.appearance.roles.coroner;

import dev.doctor4t.wathe.api.client.gui.PlayerBodyHudApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.client.appearance.modifiers.graverobber.GraverobberBodyInfoAccess;
import org.agmas.noellesroles.roles.vulture.BodyConsumedComponent;

import java.util.List;

/**
 * 验尸官与盗墓者对 Wathe 通用尸体 HUD 的权限规则。
 *
 * <p>本类不再读取/绘制死因和职业，只声明哪些固定行可见以及尸体被秃鹫吞噬后哪些行乱码。
 * 玩家名继续由 Wathe 限制为只有非存活观察者可见，避免活人识破尸体外观伪装。</p>
 */
public final class CoronerBodyHudHandler {
    private CoronerBodyHudHandler() {
    }

    public static void register() {
        registerForensicLine(PlayerBodyHudApi.BodyHudLine.DEATH, "death");
        registerForensicLine(PlayerBodyHudApi.BodyHudLine.IDENTITY, "identity");
        registerForensicLine(PlayerBodyHudApi.BodyHudLine.AFFILIATION, "affiliation");

        PlayerBodyHudApi.registerAdditionalLines(
                NoellesAppearanceSupport.id("body/coroner_sanity"),
                PlayerBodyHudApi.DEFAULT_PRIORITY,
                context -> {
                    if (!GameFunctions.isPlayerAliveAndSurvival(context.viewer())
                            || !GraverobberBodyInfoAccess.canSeeBodyReadout(context.viewer())
                            || !GraverobberBodyInfoAccess.shouldBlockCoronerReadoutForSanity(context.viewer())) {
                        return List.of();
                    }
                    return List.of(Text.translatable("hud.coroner.sanity_requirements").formatted(Formatting.YELLOW));
                }
        );
    }

    private static void registerForensicLine(PlayerBodyHudApi.BodyHudLine line, String idPath) {
        PlayerBodyHudApi.registerLineRule(
                NoellesAppearanceSupport.id("body/coroner_" + idPath),
                line,
                PlayerBodyHudApi.DEFAULT_PRIORITY,
                (context, ignored) -> {
                    if (!GameFunctions.isPlayerAliveAndSurvival(context.viewer())
                            || !GraverobberBodyInfoAccess.canSeeBodyReadout(context.viewer())) {
                        return PlayerBodyHudApi.BodyHudDecision.PASS;
                    }
                    if (GraverobberBodyInfoAccess.shouldBlockCoronerReadoutForSanity(context.viewer())) {
                        return PlayerBodyHudApi.BodyHudDecision.HIDE;
                    }
                    return BodyConsumedComponent.KEY.get(context.body()).consumed
                            ? PlayerBodyHudApi.BodyHudDecision.OBFUSCATE
                            : PlayerBodyHudApi.BodyHudDecision.SHOW;
                }
        );
    }
}
