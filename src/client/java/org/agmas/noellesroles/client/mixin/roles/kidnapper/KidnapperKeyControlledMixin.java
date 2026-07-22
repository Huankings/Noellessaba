package org.agmas.noellesroles.client.mixin.roles.kidnapper;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.roles.kidnapper.KidnapperComponent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KeyBinding.class, priority = 5000)
public abstract class KidnapperKeyControlledMixin {
    @Unique
    private void noellesroles$lockKidnappedKeys(@NotNull CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !WatheClient.isPlayerAliveAndInSurvival()) {
            return;
        }

        KeyBinding key = (KeyBinding) (Object) this;
        boolean useKey = key.equals(client.options.useKey);
        boolean attackKey = key.equals(client.options.attackKey);
        boolean hotbarKey = false;
        for (KeyBinding hotbarBinding : client.options.hotbarKeys) {
            if (key.equals(hotbarBinding)) {
                hotbarKey = true;
                break;
            }
        }

        if (KidnapperComponent.KEY.get(client.player).controlTicks > 0 && (useKey || attackKey || hotbarKey)) {
            /*
             * 绑匪控制仍然不锁移动键：目标会被服务端持续拉回绑匪身边，保留 kinssaba 的“盲目跟随”体验。
             *
             * 这里额外锁住数字热键栏，避免被劫持者提前把反制武器切到手上。
             * 鼠标滚轮不走 KeyBinding，所以在 KidnapperControlledMouseMixin 里单独拦截。
             */
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "wasPressed", at = @At("RETURN"), cancellable = true)
    private void noellesroles$wasPressed(@NotNull CallbackInfoReturnable<Boolean> cir) {
        this.noellesroles$lockKidnappedKeys(cir);
    }

    @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
    private void noellesroles$isPressed(@NotNull CallbackInfoReturnable<Boolean> cir) {
        this.noellesroles$lockKidnappedKeys(cir);
    }
}
