package org.agmas.noellesroles.client.mixin.roles.timekeeper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 时间狭缝期间恢复旁观飞行的向上移动。
 *
 * <p>时间狭缝把死者维持在 Wathe 仍视作“存活”的旁观状态，
 * 这样可以禁用死亡频道、本能透视和真实身份信息；但 Wathe 的局内按键限制也会因此继续生效。
 * 当地图配置禁用活人跳跃时，Wathe 会在 KeyBinding 层把 jumpKey 的返回值改成 false，
 * 导致狭缝玩家虽然处于 spectator，却无法按跳跃键向上飞。</p>
 *
 * <p>这里不修改 Wathe 的全局 allowJump，也不放开普通活人的跳跃限制；
 * 只在本地玩家确实处于时间狭缝、当前没有打开聊天/背包等界面、且不处于时间回溯播放时，
 * 直接读取玩家真实绑定的跳跃键物理状态，然后把 jumpKey#isPressed 恢复为这个状态。
 * 数字键传送仍由 TimekeeperRiftHotbarKeyMixin 单独禁用，回溯期间的攻击/使用残留输入
 * 仍由 TimekeeperRewindKeyBindingMixin 处理。</p>
 */
@Mixin(value = KeyBinding.class, priority = 5000)
public abstract class TimekeeperRiftJumpKeyMixin {
    @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
    private void noellesroles$allowTimeRiftJumpMovement(@NotNull CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!noellesroles$shouldRestoreJumpKey(client)) {
            return;
        }

        /*
         * 不能直接读取 KeyBinding 原本的 pressed 字段：
         * Wathe 同时会改写 matchesKey，某些情况下 KeyBinding 自己甚至收不到“这个键正在按下”的更新。
         * 参考灵术师灵魂相机的做法，直接读取当前绑定键的物理状态，既能绕过 Wathe 的 jumpKey 封锁，
         * 又能尊重玩家在设置里改过的跳跃键，包含键盘、扫描码和鼠标侧键绑定。
         */
        cir.setReturnValue(noellesroles$isJumpPhysicallyPressed(client));
    }

    @Unique
    private boolean noellesroles$shouldRestoreJumpKey(MinecraftClient client) {
        if (client == null
                || client.player == null
                || client.world == null
                || client.options == null
                || client.currentScreen != null) {
            return false;
        }

        KeyBinding key = (KeyBinding) (Object) this;
        if (!key.equals(client.options.jumpKey)) {
            return false;
        }

        /*
         * 回溯播放是另一种状态：未受保护玩家会被服务端冻结并被快照回滚。
         * 即使狭缝玩家马上要被回溯复活，也不应该在“倒放历史”的几秒里恢复本地跳跃输入。
         */
        TimekeeperWorldComponent worldComponent = TimekeeperWorldComponent.KEY.get(client.world);
        return !worldComponent.isRewinding()
                && TimekeeperPlayerComponent.KEY.get(client.player).isInTimeRift();
    }

    @Unique
    private boolean noellesroles$isJumpPhysicallyPressed(MinecraftClient client) {
        InputUtil.Key jumpBoundKey =
                ((TimekeeperKeyBindingAccessor) client.options.jumpKey).noellesroles$getBoundKey();
        long windowHandle = client.getWindow().getHandle();

        if (jumpBoundKey.getCategory() == InputUtil.Type.KEYSYM
                || jumpBoundKey.getCategory() == InputUtil.Type.SCANCODE) {
            return InputUtil.isKeyPressed(windowHandle, jumpBoundKey.getCode());
        }
        if (jumpBoundKey.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(windowHandle, jumpBoundKey.getCode()) == GLFW.GLFW_PRESS;
        }
        return false;
    }
}
