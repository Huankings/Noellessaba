package org.agmas.noellesroles.client.mixin.modifiers.violator;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.roles.engineer.StunnedPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 违禁者解锁局内跳跃键。
 *
 * <p>Wathe 目前只提供整局层面的 allowJump 开关，没有“某个词条绕过跳跃禁用”的公开 API，
 * 所以这里保留 kinssaba 的窄客户端 mixin。注入点放在 RETURN，
 * 只在原本被 Wathe 禁用后把 jumpKey 改回真实按键状态，不影响其它按键逻辑。</p>
 */
@Mixin(value = KeyBinding.class, priority = 5000)
public abstract class ViolatorKeyBindingMixin {
    @Unique
    private void noellesroles$unlockJumpForViolator(@NotNull CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        KeyBinding key = (KeyBinding) (Object) this;
        if (!key.equals(client.options.jumpKey)) {
            return;
        }

        if (!WatheClient.isPlayerAliveAndInSurvival()
                || StunnedPlayerComponent.KEY.get(client.player).isStunned()
                || !WorldModifierComponent.KEY.get(client.player.getWorld()).isModifier(client.player, NoellesModifierRegistry.VIOLATOR)) {
            return;
        }

        cir.setReturnValue(this.noellesroles$keyPressed());
    }

    @Inject(method = "wasPressed", at = @At("RETURN"), cancellable = true)
    private void noellesroles$wasPressed(@NotNull CallbackInfoReturnable<Boolean> cir) {
        noellesroles$unlockJumpForViolator(cir);
    }

    @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
    private void noellesroles$isPressed(@NotNull CallbackInfoReturnable<Boolean> cir) {
        noellesroles$unlockJumpForViolator(cir);
    }

    @Accessor("pressed")
    abstract boolean noellesroles$keyPressed();
}
