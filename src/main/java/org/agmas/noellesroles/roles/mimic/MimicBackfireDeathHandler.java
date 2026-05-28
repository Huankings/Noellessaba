package org.agmas.noellesroles.roles.mimic;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 模仿者误杀无辜者后的反噬自杀处理器。
 */
public final class MimicBackfireDeathHandler {

    /**
     * 与旧实现保持一致的“愧疚自杀”死因。
     */
    private static final Identifier BACKFIRE_DEATH_REASON = Identifier.of(Noellesroles.MOD_ID, "modded_backfire");

    private MimicBackfireDeathHandler() {
    }

    /**
     * 当 Mimic 把无辜玩家推下车后，自己也会因愧疚而自杀。
     *
     * <p>这里刻意不把被误杀的受害者当成 gameplay killer 传给 {@code killPlayer}，
     * 而是只写到 {@code replay_actor} 里。
     * 这样既能保证回放文案完整，又不会误触额外的击杀奖励或其它“真正 killer”相关逻辑。</p>
     */
    public static boolean allowDeath(PlayerEntity playerEntity, PlayerEntity killer, Identifier deathReason) {
        if (!GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(deathReason) || killer == null) {
            return true;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(playerEntity.getWorld());
        if (!gameWorldComponent.isRole(killer, Noellesroles.MIMIC) || !gameWorldComponent.isInnocent(playerEntity)) {
            return true;
        }

        NbtCompound replayDeathData = new NbtCompound();
        replayDeathData.putUuid("replay_actor", playerEntity.getUuid());
        GameFunctions.killPlayer(killer, true, null, BACKFIRE_DEATH_REASON, replayDeathData);
        return true;
    }
}
