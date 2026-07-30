package org.agmas.noellesroles.roles.angel;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 天使死亡后解除守护关系并提示被守护者。
 */
public final class AngelDeathCleanupHandler {
    private static boolean initialized = false;

    private AngelDeathCleanupHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerBeforeMoodReset(
                NoellesRolesCore.id("angel_death_cleanup"),
                DeathApi.DEFAULT_PRIORITY,
                context -> {
                    /*
                     * 天使死亡后要解除守护关系，但仍需要读取死亡前的 AngelPlayerComponent 状态。
                     * 因此放在 beforeMoodReset：Wathe 已确认死亡，组件还没被通用心情/死亡清理覆盖。
                     */
                    if (!(context.victim() instanceof ServerPlayerEntity angelPlayer)) {
                        return;
                    }
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.victim().getWorld());
                    if (!gameWorld.isRole(context.victim(), NoellesRoleRegistry.ANGEL)) {
                        return;
                    }

                    AngelPlayerComponent angelComponent = AngelPlayerComponent.KEY.get(angelPlayer);
                    ServerPlayerEntity guardedTarget = angelComponent.resolveGuardedTarget();
                    boolean sacrificeDeath = angelComponent.isSacrificeDeathInProgress();
                    angelComponent.clearGuardSilently();
                    angelComponent.setSacrificeDeathInProgress(false);

                    if (guardedTarget != null && !sacrificeDeath) {
                        // 如果不是主动替死导致的死亡，被守护者需要收到“守护者已死”的提示和音效。
                        guardedTarget.sendMessage(
                                Text.translatable("message.noellesroles.angel.guardian_died", angelPlayer.getDisplayName())
                                        .withColor(NoellesRoleRegistry.ANGEL.color()),
                                true
                        );
                        guardedTarget.playSoundToPlayer(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.0F);
                    }
                }
        );
    }
}
