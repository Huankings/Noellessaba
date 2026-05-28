package org.agmas.noellesroles.roles.executioner;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.Objects;

/**
 * 仇杀客误杀后的反噬自杀处理器。
 */
public final class ExecutionerBackfireDeathHandler {

    /**
     * 与旧实现保持一致的专属反噬死因。
     *
     * <p>这里不挪到主类里，目的是把这段“只服务于仇杀客 / 模仿者反噬”的局部语义收回到处理器内部。</p>
     */
    private static final Identifier BACKFIRE_DEATH_REASON = Identifier.of(Noellesroles.MOD_ID, "modded_backfire");

    private ExecutionerBackfireDeathHandler() {
    }

    /**
     * 当仇杀客用枪击杀了错误目标时，让其自己因愧疚而自杀。
     *
     * <p>{@code replay_actor} 必须继续保留，
     * 因为回放文本会用它来显示“因误杀了谁而愧疚自杀”，
     * 而不是把 gameplay killer 错误理解成真正击杀者。</p>
     */
    public static boolean allowDeath(PlayerEntity playerEntity, PlayerEntity killer, Identifier deathReason) {
        if (!GameConstants.DeathReasons.GUN.equals(deathReason) || killer == null) {
            return true;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(playerEntity.getWorld());
        if (!gameWorldComponent.isRole(killer, Noellesroles.EXECUTIONER)) {
            return true;
        }
        if (Objects.equals(ExecutionerPlayerComponent.KEY.get(killer).target, playerEntity.getUuid())) {
            return true;
        }

        NbtCompound replayDeathData = new NbtCompound();
        replayDeathData.putUuid("replay_actor", playerEntity.getUuid());
        GameFunctions.killPlayer(killer, true, null, BACKFIRE_DEATH_REASON, replayDeathData);
        return true;
    }
}
