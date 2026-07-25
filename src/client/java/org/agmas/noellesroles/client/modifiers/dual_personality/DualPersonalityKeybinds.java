package org.agmas.noellesroles.client.modifiers.dual_personality;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.agmas.noellesroles.packet.modifiers.dual_personality.DualPersonalitySwitchC2SPacket;
import org.agmas.noellesroles.packet.modifiers.dual_personality.DualPersonalitySwitchKeyLabelC2SPacket;
import org.lwjgl.glfw.GLFW;

/**
 * 双重人格客户端按键注册。
 *
 * <p>U 用于请求提前切换人格。
 * 双活阶段的透视不再注册 NoellesRoles 自己的独立按键，
 * 而是直接复用 Wathe 原生本能键，避免玩家需要额外配置一套“临时本能”键位。</p>
 */
public final class DualPersonalityKeybinds {

    public static KeyBinding switchKey;
    private static String lastSyncedSwitchKeyLabel = "";

    private DualPersonalityKeybinds() {
    }

    public static void init() {
        // U 是主动切换键，普通轮换阶段只有活跃人格按下才会被服务端接受。
        switchKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.noellesroles.dual_personality_switch",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "category.wathe.keybinds"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            syncSwitchKeyLabel(client);

            if (DualPersonalityClientState.isDormant(MinecraftClient.getInstance().player)) {
                /*
                 * 如果玩家在切入休眠人格前已经按住 Shift 或数字键，
                 * 后续不一定会立刻产生新的 keyPress 事件。
                 * 因此每 tick 主动释放一次，避免原版旁观相机用“已按下”状态脱离附身。
                 */
                DualPersonalityClientState.releaseDormantBlockedKeys(client);
            }

            while (switchKey.wasPressed()) {
                if (client.getNetworkHandler() != null) {
                    // 空包只表达“我按了切换键”，服务端会再次校验是否真能切换。
                    ClientPlayNetworking.send(new DualPersonalitySwitchC2SPacket());
                }
            }
        });
    }

    public static void resetSyncedState() {
        lastSyncedSwitchKeyLabel = "";
    }

    private static void syncSwitchKeyLabel(MinecraftClient client) {
        if (client == null || client.getNetworkHandler() == null || switchKey == null) {
            return;
        }

        /*
         * KeyBinding#getBoundKeyLocalizedText() 返回的是客户端当前绑定的实际按键显示，
         * 例如 U、1、鼠标 4 等，而不是“这个功能叫什么”。
         * 这里把它同步到服务端，服务端再用在 actionbar 里，就不会出现“按下双重人格切换键键”这种重复文案。
         */
        String currentLabel = switchKey.getBoundKeyLocalizedText().getString();
        if (currentLabel.equals(lastSyncedSwitchKeyLabel)) {
            return;
        }

        lastSyncedSwitchKeyLabel = currentLabel;
        ClientPlayNetworking.send(new DualPersonalitySwitchKeyLabelC2SPacket(currentLabel));
    }
}
