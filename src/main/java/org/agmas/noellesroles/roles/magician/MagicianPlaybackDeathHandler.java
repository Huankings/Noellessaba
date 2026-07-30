package org.agmas.noellesroles.roles.magician;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.api.death.KillerRewardResult;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * 魔术师播放代理造成延迟击杀时，把默认收益和回放上下文转给真实魔术师。
 */
public final class MagicianPlaybackDeathHandler {
    private static final ThreadLocal<Deque<MagicianPlaybackKillCreditScope>> PLAYBACK_KILL_SCOPES =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static boolean initialized = false;

    private MagicianPlaybackDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerBeforeAttempt(
                NoellesRolesCore.id("magician_playback_kill_context"),
                DeathApi.PRIORITY_REPLAY_CONTEXT,
                MagicianPlaybackDeathHandler::pushContext
        );
        DeathApi.registerDefaultKillerRewardRule(
                NoellesRolesCore.id("magician_playback_default_reward_redirect"),
                DeathApi.PRIORITY_REPLAY_CONTEXT,
                /*
                 * 播放体是服务端代理，不是真正玩家。
                 * 如果让 Wathe 默认奖励发给它，金币会丢进假玩家身上；
                 * 因此先 DENY 默认收益，再在 afterAttempt 里转给真实魔术师。
                 */
                (context, defaultValue) -> context.killer() instanceof MagicianPlaybackFakePlayer
                        ? KillerRewardResult.DENY
                        : KillerRewardResult.PASS
        );
        DeathApi.registerAfterAttempt(
                NoellesRolesCore.id("magician_playback_kill_reward"),
                DeathApi.PRIORITY_REPLAY_CONTEXT,
                MagicianPlaybackDeathHandler::popAndReward
        );
    }

    private static void pushContext(dev.doctor4t.wathe.api.death.DeathContext context) {
        if (!(context.killer() instanceof MagicianPlaybackFakePlayer playbackKiller)
                || !(context.victim().getWorld() instanceof ServerWorld serverWorld)) {
            /*
             * 每次 beforeAttempt 都压入一个 scope，即使当前死亡不是播放体造成的。
             * afterAttempt 会对称 pop，避免递归死亡时栈层错位。
             */
            PLAYBACK_KILL_SCOPES.get().push(MagicianPlaybackKillCreditScope.empty());
            return;
        }

        UUID ownerUuid = playbackKiller.getMagicianOwnerUuid();
        ServerPlayerEntity magician = serverWorld.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (magician == null) {
            PLAYBACK_KILL_SCOPES.get().push(MagicianPlaybackKillCreditScope.empty());
            return;
        }

        /*
         * recordDeath 仍在 Wathe killPlayer 内部执行。
         * 这里提前压入皮套身份上下文，GameRecordManager 的现有回放 mixin
         * 就能继续把 death/item_hit 等通用事件显示成皮套玩家行为。
         */
        MagicianReplayActorContext.Scope replayScope = MagicianReplayActorContext.push(
                ownerUuid,
                playbackKiller.getReplayActorUuid(),
                playbackKiller.getReplayActorName()
        );
        PLAYBACK_KILL_SCOPES.get().push(new MagicianPlaybackKillCreditScope(
                replayScope,
                ownerUuid,
                context.victimAliveAtStart()
        ));
    }

    private static void popAndReward(dev.doctor4t.wathe.api.death.DeathContext context) {
        MagicianPlaybackKillCreditScope scope = popScope();
        try {
            if (scope.replayScope() != null) {
                scope.replayScope().close();
            }

            if (scope.ownerUuid() == null
                    || !scope.victimWasAlive()
                    || !context.confirmedDeath()
                    || !(context.victim().getWorld() instanceof ServerWorld serverWorld)
                    || context.victim().getUuid().equals(scope.ownerUuid())) {
                return;
            }

            ServerPlayerEntity magician = serverWorld.getServer().getPlayerManager().getPlayer(scope.ownerUuid());
            if (magician == null || !GameFunctions.isPlayerAliveAndSurvival(magician)) {
                return;
            }

            if (GameWorldComponent.KEY.get(magician.getWorld()).canUseKillerFeatures(magician)) {
                /*
                 * 只在目标确认死亡、真实魔术师仍存活且拥有杀手能力时发默认击杀收益。
                 * 这和 Wathe 原本“杀手能力角色击杀得钱”的规则一致，只是把归属从播放代理转回本人。
                 */
                PlayerShopComponent.KEY.get(magician).addToBalance(GameConstants.MONEY_PER_KILL);
            }
        } finally {
            if (PLAYBACK_KILL_SCOPES.get().isEmpty()) {
                PLAYBACK_KILL_SCOPES.remove();
            }
        }
    }

    private static MagicianPlaybackKillCreditScope popScope() {
        Deque<MagicianPlaybackKillCreditScope> scopes = PLAYBACK_KILL_SCOPES.get();
        return scopes.isEmpty() ? MagicianPlaybackKillCreditScope.empty() : scopes.pop();
    }
}
