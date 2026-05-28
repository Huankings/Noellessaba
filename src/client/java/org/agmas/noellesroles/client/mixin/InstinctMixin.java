package org.agmas.noellesroles.client.mixin;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.framing.DelusionPlayerComponent;
import org.agmas.noellesroles.roles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.roles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.roles.angel.AngelPlayerComponent;
import org.agmas.noellesroles.roles.executioner.ExecutionerPlayerComponent;
import org.agmas.noellesroles.roles.winder.WindMarkPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;

@Mixin(WatheClient.class)
public abstract class InstinctMixin {

    ///处理角色透视有关源码
    @Shadow public static KeyBinding instinctKeybind;

    @Inject(method = "isInstinctEnabled", at = @At("HEAD"), cancellable = true)
    private static void b(CallbackInfoReturnable<Boolean> cir) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());
        if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.JESTER)) {
            if (instinctKeybind.isPressed()) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    @Inject(method = "getInstinctHighlight", at = @At("HEAD"), cancellable = true)
    private static void getInstinctHighlightColor(Entity target, CallbackInfoReturnable<Integer> cir) {
        GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(MinecraftClient.getInstance().player.getWorld());

        if (target instanceof PlayerEntity) {
            if (!((PlayerEntity)target).isSpectator()) {
                WindMarkPlayerComponent windMarkPlayerComponent = WindMarkPlayerComponent.KEY.get((PlayerEntity) target);
                BartenderPlayerComponent bartenderPlayerComponent = BartenderPlayerComponent.KEY.get((PlayerEntity) target);
                DelusionPlayerComponent delusionPlayerComponent = DelusionPlayerComponent.KEY.get((PlayerEntity) target);
                PlayerPoisonComponent playerPoisonComponent =  PlayerPoisonComponent.KEY.get((PlayerEntity) target);
                if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.WINDER)
                        && WatheClient.isPlayerAliveAndInSurvival()
                        && windMarkPlayerComponent.hasActiveMark()
                        && GameFunctions.isPlayerAliveAndSurvival((PlayerEntity) target)
                        && Wathe.isSkyVisibleAdjacent(target)) {
                    cir.setReturnValue(Noellesroles.WINDER.color());
                    cir.cancel();
                }
                if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.BARTENDER) && bartenderPlayerComponent.glowTicks > 0) {
                    cir.setReturnValue(Color.GREEN.getRGB());
                }
                if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.BARTENDER) && bartenderPlayerComponent.armor > 0) {
                    cir.setReturnValue(Color.BLUE.getRGB());
                    cir.cancel();
                }
                if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.BARTENDER) && (playerPoisonComponent.poisonTicks > 0 || delusionPlayerComponent.isActive())) {
                    cir.setReturnValue(Color.RED.getRGB());
                }
                /*
                 * 天使的“守护目标透视”改成和酒保 / 仇杀客一致的常驻透视：
                 * 只要当前客户端玩家还是存活中的天使，并且这名玩家正是自己守护的目标，
                 * 就直接返回职业颜色。
                 *
                 * 这里故意不再依赖 WatheClient.isInstinctEnabled()，
                 * 这样就不会要求玩家额外按住本能键才能看到被守护者。
                 */
                if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.ANGEL)
                        && WatheClient.isPlayerAliveAndInSurvival()) {
                    AngelPlayerComponent angelComponent = AngelPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
                    if (angelComponent.getGuardedTarget() != null && angelComponent.getGuardedTarget().equals(target.getUuid())) {
                        cir.setReturnValue(Noellesroles.ANGEL.color());
                        cir.cancel();
                    }
                }
            }
        }
        if (target instanceof PlayerEntity) {
            if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.EXECUTIONER)) {
                ExecutionerPlayerComponent executionerPlayerComponent = (ExecutionerPlayerComponent) ExecutionerPlayerComponent.KEY.get((PlayerEntity) MinecraftClient.getInstance().player);
                if (executionerPlayerComponent.target.equals(target.getUuid())) {
                    cir.setReturnValue(Color.YELLOW.getRGB());
                    cir.cancel();
                }
            }
            if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.JESTER) && WatheClient.isInstinctEnabled()) {
                    cir.setReturnValue(Color.PINK.getRGB());
                    cir.cancel();
            }
            if (!((PlayerEntity)target).isSpectator() && WatheClient.isInstinctEnabled()) {
                if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.BOMBER)) {
                    BomberPlayerComponent bomberPlayerComponent = BomberPlayerComponent.KEY.get((PlayerEntity) target);
                    if (bomberPlayerComponent.hasBomb()) {
                        cir.setReturnValue(Noellesroles.BOMBER.color());
                        cir.cancel();
                    }
                }
            }
            if (!((PlayerEntity)target).isSpectator() && WatheClient.isInstinctEnabled()) {
                if (gameWorldComponent.isRole((PlayerEntity) target, Noellesroles.MIMIC) && WatheClient.isKiller()  && WatheClient.isPlayerAliveAndInSurvival()) {
                    cir.setReturnValue(MathHelper.hsvToRgb(0.0F, 1.0F, 0.6F));
                    cir.cancel();
                }
            }
            if (!((PlayerEntity)target).isSpectator() && WatheClient.isInstinctEnabled()) {
                Role role = gameWorldComponent.getRole((PlayerEntity) target);
                if (role != null) {
                    if (WatheClient.isKiller() && WatheClient.isPlayerAliveAndInSurvival()) {
                        if (Noellesroles.KILLER_SIDED_NEUTRALS.contains(role)) {
                            cir.setReturnValue(role.color());
                            cir.cancel();
                        } else if (!role.isInnocent() && !role.canUseKiller()) {
                           cir.setReturnValue(5168437);
                           cir.cancel();
                        }
                    }
                }
            }




            if (!((PlayerEntity)target).isSpectator() && WatheClient.isInstinctEnabled()) {
                if (gameWorldComponent.isRole(MinecraftClient.getInstance().player, Noellesroles.EXECUTIONER) && WatheClient.isPlayerAliveAndInSurvival()) {
                    cir.setReturnValue(Noellesroles.EXECUTIONER.color());
                    cir.cancel();
                }
            }
        }
    }
}
