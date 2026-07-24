package org.agmas.noellesroles.roles.starstruck;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.task.TaskCompletionApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.NoellesRolesParticles;

/**
 * 星界使者主动能力。
 */
public final class StarstruckAbility {
    private static boolean taskHookRegistered = false;

    private StarstruckAbility() {
    }

    public static void registerTaskCompletionApi() {
        if (taskHookRegistered) {
            return;
        }
        taskHookRegistered = true;

        /*
         * 星界使者的“任务减冷却”必须挂在 Wathe 的任务完成 API 上。
         * 这样只有任务真的完成时才会触发，不会被其它增加心情值的效果误判成任务奖励。
         */
        TaskCompletionApi.AFTER_TASK_COMPLETE.register(context -> {
            if (context.role() != NoellesRoleRegistry.STARSTRUCK || !StarstruckConstants.TASK_REDUCES_COOLDOWN) {
                return;
            }

            AbilityPlayerComponent.KEY.get(context.player())
                    .changeCooldown(-StarstruckConstants.TASK_COOLDOWN_REDUCTION_TICKS);
        });
    }

    public static void handle(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRunning()
                || !gameWorld.isRole(player, NoellesRoleRegistry.STARSTRUCK)
                || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown > 0) {
            return;
        }

        /*
         * 先写冷却再写持续状态，和 StarryExpress 原实现一致。
         * 如果后续有别的扩展监听能力使用回放，也能看到已经进入冷却的稳定状态。
         */
        ability.setCooldown(StarstruckConstants.ABILITY_COOLDOWN_TICKS);
        StarstruckPlayerComponent.KEY.get(player).setTicks(StarstruckConstants.ABILITY_DURATION_TICKS);
        GameRecordManager.recordSkillUse(player, NoellesEventIds.STARSTRUCK_ABILITY_EVENT, null, null);

        player.getServerWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );
        player.getServerWorld().spawnParticles(
                NoellesRolesParticles.STARSTRUCK_SPARKLE,
                player.getX(),
                player.getY(),
                player.getZ(),
                StarstruckConstants.ABILITY_CAST_PARTICLE_COUNT,
                0.5D,
                1.5D,
                0.5D,
                0.0D
        );
    }
}
