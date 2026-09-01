package org.agmas.noellesroles.client.mixin.roles.vecna;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.agmas.noellesroles.client.mixin.roles.timekeeper.TimekeeperKeyBindingAccessor;
import org.lwjgl.glfw.GLFW;
import org.agmas.noellesroles.roles.vecna.VecnaPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 颠倒状态下交换前后、左右以及蹲下/跳跃输入。 */
@Mixin(KeyboardInput.class)
public abstract class VecnaKeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void noellesroles$invertMovement(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !VecnaPlayerComponent.KEY.get(client.player).isPsychoInverted()) return;
        Input input = (Input) (Object) this;
        input.movementForward = -input.movementForward;
        input.movementSideways = -input.movementSideways;

        /*
         * KeyboardInput.tick() 在进入这里前已经读取过 jumpKey。
         * Wathe 的 KeyBindingMixin 可能因为地图规则把 jumpKey.isPressed() 改成 false，
         * 这会让“空格键被交换成蹲下”也一起失效；同时原本的蹲下键会被交换成跳跃，
         * 又绕过 Wathe 的跳跃限制。这里重新读取两个按键的真实物理状态，再进行交换：
         * 1. 原蹲下键 -> 新跳跃键，并显式套用 Wathe allowJump 规则；
         * 2. 原跳跃键 -> 新蹲下键，不再套用跳跃限制。
         */
        boolean physicalJump = isPhysicallyPressed(client, client.options.jumpKey);
        boolean physicalSneak = client.options.sneakKey.isPressed();
        input.jumping = physicalSneak && isJumpAllowedByWathe(client);
        input.sneaking = physicalJump;
    }

    private static boolean isJumpAllowedByWathe(MinecraftClient client) {
        if (client.player == null || client.world == null) return true;
        GameWorldComponent game = GameWorldComponent.KEY.get(client.player.getWorld());
        return game == null || !game.isRunning() || game.isAlivePlayerJumpAllowed();
    }

    private static boolean isPhysicallyPressed(MinecraftClient client, KeyBinding keyBinding) {
        InputUtil.Key boundKey = ((TimekeeperKeyBindingAccessor) keyBinding).noellesroles$getBoundKey();
        long window = client.getWindow().getHandle();
        if (boundKey.getCategory() == InputUtil.Type.KEYSYM || boundKey.getCategory() == InputUtil.Type.SCANCODE) {
            return InputUtil.isKeyPressed(window, boundKey.getCode());
        }
        if (boundKey.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, boundKey.getCode()) == GLFW.GLFW_PRESS;
        }
        return false;
    }
}
