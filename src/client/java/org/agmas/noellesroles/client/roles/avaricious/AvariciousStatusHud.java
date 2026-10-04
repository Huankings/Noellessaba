package org.agmas.noellesroles.client.roles.avaricious;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.avaricious.AvariciousConstants;
import org.agmas.noellesroles.roles.avaricious.AvariciousPayoutComponent;

/**
 * 扒手右下角金币收益 HUD。
 */
public final class AvariciousStatusHud {
    private static final int GOLD_TEXT_COLOR = 0xFFAA00;
    private static final int RIGHT_MARGIN = 8;
    private static final int BOTTOM_MARGIN = 10;

    private AvariciousStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/avaricious/status", NoellesRoleRegistry.AVARICIOUS, context -> {
            /*
             * Wathe 的存活覆盖会让时间狭缝玩家仍通过通用存活 HUD 过滤，
             * 但扒手在狭缝内已经不能偷钱，所以这里必须使用扒手自己的参与者判定。
             */
            if (context.debugHudVisible()
                    || !AvariciousConstants.isEligibleParticipant(context.player())) {
                return;
            }

            Text timerLine = Text.translatable(
                    "hud.noellesroles.avaricious.payout_timer",
                    getSecondsUntilNextPayout(context.player())
            );
            Text expectedPayoutLine = Text.translatable(
                    "hud.noellesroles.avaricious.expected_payout",
                    getExpectedPayout(context.player())
            );

            int baseX = context.width() - RIGHT_MARGIN;
            int payoutLineY = context.height() - BOTTOM_MARGIN;
            int timerLineY = payoutLineY - context.textRenderer().fontHeight - 2;

            context.drawContext().drawTextWithShadow(context.textRenderer(), timerLine, baseX - context.textRenderer().getWidth(timerLine), timerLineY, GOLD_TEXT_COLOR);
            context.drawContext().drawTextWithShadow(context.textRenderer(), expectedPayoutLine, baseX - context.textRenderer().getWidth(expectedPayoutLine), payoutLineY, NoellesRoleRegistry.AVARICIOUS.color());
        });
    }

    private static int getSecondsUntilNextPayout(ClientPlayerEntity player) {
        AvariciousPayoutComponent payoutComponent = AvariciousPayoutComponent.KEY.get(player.getWorld());

        if (!payoutComponent.hasTimer()) {
            /*
             * 服务端开局需要一个 tick 写入并同步起点。
             * 客户端未收到前先显示完整周期，避免出现负数或跳动文本。
             */
            return Math.max(1, (AvariciousConstants.TIMER_TICKS + 19) / 20);
        }

        // 服务端组件保存的是独立剩余 tick，不会因 Wathe 加时/减时而跳变。
        return Math.max(1, (payoutComponent.getRemainingTicks() + 19) / 20);
    }

    private static int getExpectedPayout(ClientPlayerEntity player) {
        int nearbyPlayers = 0;
        for (PlayerEntity other : player.getWorld().getPlayers()) {
            if (other == player || !AvariciousConstants.isEligibleParticipant(other)) {
                continue;
            }
            if (other.distanceTo(player) <= AvariciousConstants.MAX_DISTANCE) {
                nearbyPlayers++;
            }
        }
        return nearbyPlayers * AvariciousConstants.PAYOUT_PER_PLAYER;
    }
}
