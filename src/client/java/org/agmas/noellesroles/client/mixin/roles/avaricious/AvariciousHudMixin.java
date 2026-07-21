package org.agmas.noellesroles.client.mixin.roles.avaricious;

import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.avaricious.AvariciousConstants;
import org.agmas.noellesroles.roles.avaricious.AvariciousPayoutComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class AvariciousHudMixin {
    private static final int GOLD_TEXT_COLOR = 0xFFAA00;
    private static final int RIGHT_MARGIN = 8;
    private static final int BOTTOM_MARGIN = 10;

    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderAvariciousHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }

        DebugHud debugHud = client.inGameHud.getDebugHud();
        if (debugHud != null && debugHud.shouldShowDebugHud()) {
            return;
        }

        ClientPlayerEntity player = client.player;
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.AVARICIOUS)) {
            return;
        }

        /*
         * HUD 只给正在参与本局的扒手看。
         * 死亡、旁观或创造模式调试时隐藏，避免旧对局的经济提示残留。
         */
        if (!WatheClient.isPlayerAliveAndInSurvival() && !player.isCreative()) {
            return;
        }

        Text timerLine = Text.translatable(
                "hud.noellesroles.avaricious.payout_timer",
                getSecondsUntilNextPayout(player)
        );
        Text expectedPayoutLine = Text.translatable(
                "hud.noellesroles.avaricious.expected_payout",
                getExpectedPayout(player)
        );

        TextRenderer renderer = this.getTextRenderer();
        int baseX = context.getScaledWindowWidth() - RIGHT_MARGIN;
        int payoutLineY = context.getScaledWindowHeight() - BOTTOM_MARGIN;
        int timerLineY = payoutLineY - renderer.fontHeight - 2;

        context.drawTextWithShadow(renderer, timerLine, baseX - renderer.getWidth(timerLine), timerLineY, GOLD_TEXT_COLOR);
        context.drawTextWithShadow(renderer, expectedPayoutLine, baseX - renderer.getWidth(expectedPayoutLine), payoutLineY, Noellesroles.AVARICIOUS.color());
    }

    private static int getSecondsUntilNextPayout(ClientPlayerEntity player) {
        GameTimeComponent timeComponent = GameTimeComponent.KEY.get(player.getWorld());
        AvariciousPayoutComponent payoutComponent = AvariciousPayoutComponent.KEY.get(player.getWorld());

        if (!payoutComponent.hasTimerStartTime()) {
            /*
             * 服务端开局需要一个 tick 写入并同步起点。
             * 客户端未收到前先显示完整周期，避免出现负数或跳动文本。
             */
            return Math.max(1, (AvariciousConstants.TIMER_TICKS + 19) / 20);
        }

        /*
         * GameTimeComponent.time 是倒计时，所以 elapsed 要用“起点 - 当前剩余时间”。
         * 这和服务端 AvariciousGoldPayoutMixin 完全一致，HUD 才会和真实发钱点对齐。
         */
        int elapsed = Math.max(0, payoutComponent.getTimerStartTime() - timeComponent.time);
        int remainder = elapsed % AvariciousConstants.TIMER_TICKS;
        int ticksRemaining = remainder == 0
                ? AvariciousConstants.TIMER_TICKS
                : AvariciousConstants.TIMER_TICKS - remainder;

        return Math.max(1, (ticksRemaining + 19) / 20);
    }

    private static int getExpectedPayout(ClientPlayerEntity player) {
        int nearbyPlayers = 0;
        for (PlayerEntity other : player.getWorld().getPlayers()) {
            if (other == player || GameFunctions.isPlayerEliminated(other)) {
                continue;
            }
            if (other.distanceTo(player) <= AvariciousConstants.MAX_DISTANCE) {
                nearbyPlayers++;
            }
        }
        return nearbyPlayers * AvariciousConstants.PAYOUT_PER_PLAYER;
    }
}
