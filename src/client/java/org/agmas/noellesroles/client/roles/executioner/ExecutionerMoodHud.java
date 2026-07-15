package org.agmas.noellesroles.client.roles.executioner;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.MoodHudContext;
import dev.doctor4t.wathe.api.client.mood.MoodHudStyle;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

public final class ExecutionerMoodHud {
    /**
     * 仇杀客成功转杀手后，burn 动态图标显示 1 秒。
     *
     * <p>Minecraft 默认 20 tick = 1 秒；后续想改成 2 秒时只需要调这个常量。</p>
     */
    public static final int BURN_ICON_TICKS = 20;
    private static final int BURN_STYLE_PRIORITY = 10_000;
    private static final int BURN_FRAME_WIDTH = 14;
    private static final int BURN_FRAME_HEIGHT = 17;
    private static final int BURN_FRAME_COUNT = 7;
    private static final int BURN_TEXTURE_HEIGHT = BURN_FRAME_HEIGHT * BURN_FRAME_COUNT;

    private static final Identifier EXECUTIONER_MOOD = Identifier.of(Noellesroles.MOD_ID, "hud/mood_executioner");
    private static final Identifier EXECUTIONER_BURN_TEXTURE = Identifier.of(Noellesroles.MOD_ID, "textures/gui/sprites/hud/mood_executioner_burn.png");
    private static final MoodHudStyle EXECUTIONER_STYLE = MoodHudStyle
            .builder(EXECUTIONER_MOOD)
            .barColor(Noellesroles.EXECUTIONER.color())
            .build();
    private static final MoodHudStyle BURN_STYLE = MoodHudStyle
            .builder(context -> null)
            .icon(ExecutionerMoodHud::renderBurnIcon)
            .bar(ExecutionerMoodHud::renderBurnBar)
            .build();

    private static Role previousRole = null;
    private static int burnTicks = 0;

    private ExecutionerMoodHud() {
    }

    public static void register() {
        MoodHudApi.registerRoleStyle(Noellesroles.EXECUTIONER, EXECUTIONER_STYLE);
        MoodHudApi.registerMoodProvider(
                Identifier.of(Noellesroles.MOD_ID, "mood/executioner_burn"),
                BURN_STYLE_PRIORITY,
                context -> burnTicks > 0 ? BURN_STYLE : null
        );

        ClientTickEvents.END_CLIENT_TICK.register(ExecutionerMoodHud::tickBurnState);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    private static void tickBurnState(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null || client.world == null) {
            reset();
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        Role currentRole = gameWorld.getRole(player);
        if (!gameWorld.isRunning() || !WatheClient.isPlayerAliveAndInSurvival()) {
            previousRole = currentRole;
            burnTicks = 0;
            return;
        }

        /*
         * Executioner 成功后服务端会直接把玩家 role 改成随机杀手职业，
         * 所以不能靠“当前仍是 Executioner”来判断 burn 图标。
         * 这里检测“上一 tick 是 Executioner、这一 tick 已经不是”的转职边沿，
         * 然后本地显示 20 tick 的动态图标覆盖新杀手图标。
         */
        boolean justBecameKiller = previousRole == Noellesroles.EXECUTIONER
                && currentRole != null
                && currentRole != Noellesroles.EXECUTIONER
                && currentRole.canUseKiller();
        previousRole = currentRole;

        if (justBecameKiller) {
            burnTicks = BURN_ICON_TICKS;
        } else if (burnTicks > 0) {
            burnTicks--;
        }
    }

    private static void renderBurnBar(MoodHudContext context, int width, float alpha) {
        if (width <= 0 || alpha <= 0.0F) {
            return;
        }

        /*
         * burn 期间图标临时覆盖当前新职业；进度条颜色则跟随当前 role。
         * 这样 1 秒动画结束后不会出现“图标是 burn，条还固定成仇杀客棕色”的割裂感。
         *
         * 注意：NoellesRoles 里不少职业色来自 java.awt.Color#getRGB()，这种颜色自带 0xFF alpha。
         * 手写心情条不能直接 colour | (hudAlpha << 24)，否则 HUD alpha 淡到 0 时，
         * 输入颜色里的 0xFF 仍会让心情条保持不透明。
         * 因此这里先保留低 24 位 RGB，再叠加 Wathe 本帧算出的 alpha。
         */
        int colour = context.role() == null ? Noellesroles.EXECUTIONER.color() : context.role().color();
        int rgb = colour & 0x00FFFFFF;
        int alphaByte = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));
        context.drawContext().fill(0, 0, width, 1, rgb | (alphaByte << 24));
    }

    private static void renderBurnIcon(MoodHudContext context) {
        /*
         * 不再走 drawGuiTexture("hud/mood_executioner_burn")。
         * 原因是 GUI sprite atlas 会按 .png.mcmeta 做全局循环动画：
         * frametime=1 且 7 帧时，1 秒内会自动循环好几次，并且起始帧不受转职事件控制。
         *
         * 这里改成手动裁剪竖向 7 帧图：
         * 1. burnTicks 从 20 倒数到 0；
         * 2. elapsedTicks 表示这次转职动画已经播放了多久；
         * 3. frameIndex 最多推进到最后一帧，然后保持最后一帧直到 1 秒效果结束。
         */
        int elapsedTicks = BURN_ICON_TICKS - burnTicks;
        int frameIndex = Math.min(BURN_FRAME_COUNT - 1, Math.max(0, elapsedTicks * BURN_FRAME_COUNT / BURN_ICON_TICKS));
        context.drawContext().drawTexture(
                EXECUTIONER_BURN_TEXTURE,
                5,
                6,
                0,
                frameIndex * BURN_FRAME_HEIGHT,
                BURN_FRAME_WIDTH,
                BURN_FRAME_HEIGHT,
                BURN_FRAME_WIDTH,
                BURN_TEXTURE_HEIGHT
        );
    }

    public static void reset() {
        previousRole = null;
        burnTicks = 0;
    }
}
