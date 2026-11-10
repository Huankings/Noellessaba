package org.agmas.noellesroles.client.appearance.roles.coroner;

import dev.doctor4t.wathe.api.client.gui.PlayerBodyHudApi;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.client.appearance.modifiers.graverobber.GraverobberBodyInfoAccess;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 验尸官检查奖励提示。
 *
 * <p>作为 Wathe 尸体 HUD 的附加行注册，因此固定五行先完成动态排版，本提示永远位于最下方，
 * 不再和死亡时间或身份行共享硬编码 y 坐标。</p>
 */
public final class CoronerBodyExamineHudHandler {
    private CoronerBodyExamineHudHandler() {
    }

    public static void register() {
        PlayerBodyHudApi.registerAdditionalLines(
                NoellesAppearanceSupport.id("body/coroner_examine"),
                PlayerBodyHudApi.DEFAULT_PRIORITY - 100,
                context -> {
                    if (!GraverobberBodyInfoAccess.canSeeExaminePrompt(context.viewer())
                            || GraverobberBodyInfoAccess.shouldBlockCoronerReadoutForSanity(context.viewer())) {
                        return List.of();
                    }
                    CoronerPlayerComponent component = CoronerPlayerComponent.KEY.get(context.viewer());
                    boolean examined = component.examinedBodies.contains(context.body().getUuid());
                    List<Text> result = new ArrayList<>();
                    result.add(examined
                            ? Text.literal("已检查过此尸体").formatted(Formatting.GRAY)
                            : Text.literal("靠近检查尸体 (获得金币)").formatted(Formatting.YELLOW));
                    if (!examined) {
                        result.add(Text.literal(String.format("已检查: %d具尸体 | 总计: %d金币",
                                component.totalBodiesExamined,
                                component.totalGoldEarned)).formatted(Formatting.GRAY));
                    }
                    return result;
                }
        );
    }
}
