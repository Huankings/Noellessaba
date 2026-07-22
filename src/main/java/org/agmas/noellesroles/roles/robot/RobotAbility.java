package org.agmas.noellesroles.roles.robot;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;

/**
 * 机器人夜视能力。
 */
public final class RobotAbility {
    private RobotAbility() {
    }

    public static void handle(@NotNull PlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        if (!gameWorld.isRole(player, Noellesroles.ROBOT)
                || !GameFunctions.isPlayerAliveAndSurvival(player)
                || ability.cooldown > 0) {
            return;
        }

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NIGHT_VISION,
                RobotConstants.ABILITY_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));
        /*
         * 夜视结束回放由组件 tick 负责。这里单独保存剩余 tick，
         * 是为了让“使用能力”和“效果自然结束”成为两条稳定的服务端事件。
         */
        RobotPlayerComponent.KEY.get(player).startNightVision(RobotConstants.ABILITY_DURATION_TICKS);
        player.playSoundToPlayer(SoundEvents.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1.0F, 1.0F);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordSkillUse(serverPlayer, Noellesroles.ROBOT_NIGHT_VISION_EVENT, null, null);
        }
        ability.setCooldown(RobotConstants.ABILITY_COOLDOWN_TICKS);
    }
}
