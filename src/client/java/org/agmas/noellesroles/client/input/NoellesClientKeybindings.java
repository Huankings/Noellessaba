package org.agmas.noellesroles.client.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.lwjgl.glfw.GLFW;

/**
 * NoellesRoles 客户端公共按键注册处。
 *
 * <p>能力键属于客户端基础设施，不属于某一个职业。把它放在独立类中后，
 * HUD、输入锁定和职业输入处理器只依赖“按键 API”，不会再反向依赖 Fabric
 * 客户端主入口。</p>
 */
public final class NoellesClientKeybindings {
    private static KeyBinding abilityBind;

    private NoellesClientKeybindings() {
    }

    public static void register() {
        if (abilityBind != null) {
            return;
        }
        abilityBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + NoellesRolesCore.MOD_ID + ".ability",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.wathe.keybinds"
        ));
    }

    public static KeyBinding getAbilityBind() {
        return abilityBind;
    }
}
