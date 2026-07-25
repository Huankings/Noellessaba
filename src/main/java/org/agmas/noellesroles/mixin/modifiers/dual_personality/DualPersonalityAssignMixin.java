package org.agmas.noellesroles.mixin.modifiers.dual_personality;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.harpymodloader.modded_murder.ModdedMurderGameMode;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityManager;
import org.agmas.noellesroles.modifiers.dual_personality.ForcedDualPersonalityManager;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ModdedMurderGameMode.class)
public class DualPersonalityAssignMixin {

    /*
     * assignModifiers 内部可能多次经过我们选择的注入点。
     * 这个标记保证一局只消费一次强制双重人格队列。
     */
    @Unique
    private boolean noellesroles$forcedDualPersonalityApplied;

    @Inject(method = "assignModifiers", at = @At("HEAD"))
    private void noellesroles$prepareDualPersonalityModifierLimit(
            int desiredRoleCount,
            ServerWorld serverWorld,
            GameWorldComponent gameWorldComponent,
            List<ServerPlayerEntity> players,
            CallbackInfo ci
    ) {
        /*
         * Harpy 在分配词条前读取 MODIFIER_MAX。
         * 这里按本局参局人数刷新 dual_personality 是否进入随机池，
         * 同时重置“强制队列是否已消费”的本局标记。
         */
        this.noellesroles$forcedDualPersonalityApplied = false;
        DualPersonalityManager.refreshModifierMaximum(serverWorld, players);
    }

    @Inject(
            method = "assignModifiers",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;isEmpty()Z",
                    ordinal = 0
            )
    )
    private void noellesroles$applyForcedDualPersonalityBeforeModifierAnnouncement(
            int desiredRoleCount,
            ServerWorld serverWorld,
            GameWorldComponent gameWorldComponent,
            List<ServerPlayerEntity> players,
            CallbackInfo ci
    ) {
        if (this.noellesroles$forcedDualPersonalityApplied) {
            return;
        }
        this.noellesroles$forcedDualPersonalityApplied = true;

        /*
         * 这个注入点位于词条列表已基本确定、但还没向玩家播报之前。
         * 因此强制配对可以覆盖随机结果，并且播报/HUD 会看到最终正确的双重人格词条。
         */
        ForcedDualPersonalityManager.ApplyResult result = ForcedDualPersonalityManager.consumeAndApplyPendingPairs(serverWorld, players);
        if (result.changedAnything()) {
            NoellesRolesCore.LOGGER.info(
                    "已应用强制双重人格配对：成功 {} 对，因玩家未参与而作废 {} 对。",
                    result.appliedPairs(),
                    result.skippedPairs()
            );
        }
    }
}
