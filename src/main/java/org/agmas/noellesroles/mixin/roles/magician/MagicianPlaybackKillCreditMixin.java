package org.agmas.noellesroles.mixin.roles.magician;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.roles.magician.MagicianPlaybackFakePlayer;
import org.agmas.noellesroles.roles.magician.MagicianPlaybackKillCreditScope;
import org.agmas.noellesroles.roles.magician.MagicianReplayActorContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * 修正魔术师播放代理造成的“延迟击杀”归属。
 *
 * <p>匕首、枪这类即时攻击已经在播放执行器里直接把 killer 传成真实魔术师。
 * 但手雷、飞斧属于投掷实体，它们爆炸/贯穿时 owner 是 {@link MagicianPlaybackFakePlayer}。
 * 如果不在这里接管，Wathe 的击杀金币会加到这个假玩家组件上，真实魔术师就拿不到钱。</p>
 */
@Mixin(GameFunctions.class)
public abstract class MagicianPlaybackKillCreditMixin {

    @Unique
    private static final ThreadLocal<Deque<MagicianPlaybackKillCreditScope>> noellesroles$playbackKillCreditScopes =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(
            method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At("HEAD")
    )
    private static void noellesroles$pushMagicianPlaybackKillContext(
            PlayerEntity victim,
            boolean spawnBody,
            @Nullable PlayerEntity killer,
            Identifier deathReason,
            CallbackInfo ci
    ) {
        if (!(killer instanceof MagicianPlaybackFakePlayer playbackKiller)
                || !(victim.getWorld() instanceof ServerWorld serverWorld)) {
            noellesroles$playbackKillCreditScopes.get().push(MagicianPlaybackKillCreditScope.empty());
            return;
        }

        UUID ownerUuid = playbackKiller.getMagicianOwnerUuid();
        ServerPlayerEntity magician = serverWorld.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (magician == null) {
            noellesroles$playbackKillCreditScopes.get().push(MagicianPlaybackKillCreditScope.empty());
            return;
        }

        /*
         * 延迟投掷物杀人时，GameRecordManager.recordDeath 仍在本次 killPlayer 调用内部。
         * 在这里临时压入皮套身份上下文，就能让死亡回放继续显示成“皮套玩家”造成击杀，
         * 同时保留 magician_owner 给后续格式化/追踪使用。
         */
        MagicianReplayActorContext.Scope replayScope = MagicianReplayActorContext.push(
                ownerUuid,
                playbackKiller.getReplayActorUuid(),
                playbackKiller.getReplayActorName()
        );
        noellesroles$playbackKillCreditScopes.get().push(new MagicianPlaybackKillCreditScope(
                replayScope,
                ownerUuid,
                GameFunctions.isPlayerAliveAndSurvival(victim)
        ));
    }

    @Inject(
            method = "killPlayer(Lnet/minecraft/entity/player/PlayerEntity;ZLnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Identifier;)V",
            at = @At("RETURN")
    )
    private static void noellesroles$grantMagicianPlaybackKillReward(
            PlayerEntity victim,
            boolean spawnBody,
            @Nullable PlayerEntity killer,
            Identifier deathReason,
            CallbackInfo ci
    ) {
        MagicianPlaybackKillCreditScope scope = noellesroles$popPlaybackKillCreditScope();
        try {
            if (scope.replayScope() != null) {
                scope.replayScope().close();
            }

            if (scope.ownerUuid() == null
                    || !scope.victimWasAlive()
                    || GameFunctions.isPlayerAliveAndSurvival(victim)
                    || !(victim.getWorld() instanceof ServerWorld serverWorld)
                    || victim.getUuid().equals(scope.ownerUuid())) {
                return;
            }

            ServerPlayerEntity magician = serverWorld.getServer().getPlayerManager().getPlayer(scope.ownerUuid());
            if (magician == null || !GameFunctions.isPlayerAliveAndSurvival(magician)) {
                return;
            }

            /*
             * 复用 Wathe 的 killer feature 判断，避免在规则上不该获得击杀收益的状态
             * 仍然因为播放代理拿到金币。
             */
            if (GameWorldComponent.KEY.get(magician.getWorld()).canUseKillerFeatures(magician)) {
                PlayerShopComponent.KEY.get(magician).addToBalance(GameConstants.MONEY_PER_KILL);
            }
        } finally {
            if (noellesroles$playbackKillCreditScopes.get().isEmpty()) {
                noellesroles$playbackKillCreditScopes.remove();
            }
        }
    }

    @Unique
    private static MagicianPlaybackKillCreditScope noellesroles$popPlaybackKillCreditScope() {
        Deque<MagicianPlaybackKillCreditScope> scopes = noellesroles$playbackKillCreditScopes.get();
        return scopes.isEmpty() ? MagicianPlaybackKillCreditScope.empty() : scopes.pop();
    }
}
