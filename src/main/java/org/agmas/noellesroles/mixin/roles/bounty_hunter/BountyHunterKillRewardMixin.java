package org.agmas.noellesroles.mixin.roles.bounty_hunter;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterConstants;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterPlayerComponent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 赏金猎人的“任意方式击杀悬赏目标”奖励。
 *
 * <p>奖励放在 GameFunctions.killPlayer 返回后判断，而不是放在具体武器里，
 * 这样刀、枪、爆炸物、其他扩展伤害只要最终把悬赏目标杀死，都能统一拿到 50 金币。</p>
 */
@Mixin(GameFunctions.class)
public abstract class BountyHunterKillRewardMixin {
    @Unique
    private static final ThreadLocal<Deque<Boolean>> noellesroles$bountyVictimAliveScopes =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(
            method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At("HEAD")
    )
    private static void noellesroles$pushBountyVictimState(
            PlayerEntity victim,
            boolean spawnBody,
            @Nullable PlayerEntity killer,
            Identifier deathReason,
            CallbackInfo ci
    ) {
        noellesroles$bountyVictimAliveScopes.get().push(GameFunctions.isPlayerAliveAndSurvival(victim));
    }

    @Inject(
            method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At("RETURN")
    )
    private static void noellesroles$grantBountyReward(
            PlayerEntity victim,
            boolean spawnBody,
            @Nullable PlayerEntity killer,
            Identifier deathReason,
            CallbackInfo ci
    ) {
        boolean victimWasAlive = noellesroles$popBountyVictimAliveState();
        try {
            if (!victimWasAlive
                    || GameFunctions.isPlayerAliveAndSurvival(victim)
                    || !(killer instanceof ServerPlayerEntity bountyHunter)
                    || victim.getUuid().equals(bountyHunter.getUuid())) {
                return;
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(bountyHunter.getWorld());
            if (!gameWorld.isRole(bountyHunter, NoellesRoleRegistry.BOUNTY_HUNTER)) {
                return;
            }

            BountyHunterPlayerComponent component = BountyHunterPlayerComponent.KEY.get(bountyHunter);
            if (component.isCurrentBountyTarget(victim)) {
                PlayerShopComponent.KEY.get(bountyHunter).addToBalance(BountyHunterConstants.BOUNTY_REWARD_COINS);
            }
        } finally {
            if (noellesroles$bountyVictimAliveScopes.get().isEmpty()) {
                noellesroles$bountyVictimAliveScopes.remove();
            }
        }
    }

    @Unique
    private static boolean noellesroles$popBountyVictimAliveState() {
        Deque<Boolean> scopes = noellesroles$bountyVictimAliveScopes.get();
        return !scopes.isEmpty() && scopes.pop();
    }
}
