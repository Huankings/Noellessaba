package org.agmas.noellesroles.mixin.roles.timekeeper;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperRiftHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 时停者“时间狭缝”死亡入口。
 *
 * <p>这里选择注入 Wathe 的四参 killPlayer，而不是各个武器/能力：
 * 只要某次死亡最终走过 Wathe 的统一死亡流程，都会被狭缝机制看见；
 * 同时通过 HEAD/RETURN 记录死亡前后状态，能够避开护盾、免死和重复 killPlayer 调用。</p>
 */
@Mixin(GameFunctions.class)
public abstract class TimekeeperDeathRiftMixin {
    @Unique
    private static final ThreadLocal<Deque<Boolean>> noellesroles$timekeeperVictimAliveScopes =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(
            method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At("HEAD")
    )
    private static void noellesroles$rememberVictimAliveBeforeTimekeeperRift(
            PlayerEntity victim,
            boolean spawnBody,
            @Nullable PlayerEntity killer,
            Identifier deathReason,
            CallbackInfo ci
    ) {
        noellesroles$timekeeperVictimAliveScopes.get().push(GameFunctions.isPlayerAliveAndSurvival(victim));
    }

    @Inject(
            method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At("RETURN")
    )
    private static void noellesroles$startTimeRiftAfterConfirmedDeath(
            PlayerEntity victim,
            boolean spawnBody,
            @Nullable PlayerEntity killer,
            Identifier deathReason,
            CallbackInfo ci
    ) {
        boolean victimWasAlive = noellesroles$popVictimAliveState();
        try {
            if (victimWasAlive
                    && victim instanceof ServerPlayerEntity serverVictim
                    && !GameFunctions.isPlayerAliveAndSurvival(serverVictim)) {
                TimekeeperRiftHandler.tryStartRiftAfterDeath(serverVictim);
            }
        } finally {
            if (noellesroles$timekeeperVictimAliveScopes.get().isEmpty()) {
                noellesroles$timekeeperVictimAliveScopes.remove();
            }
        }
    }

    @Unique
    private static boolean noellesroles$popVictimAliveState() {
        Deque<Boolean> scopes = noellesroles$timekeeperVictimAliveScopes.get();
        return !scopes.isEmpty() && scopes.pop();
    }
}
