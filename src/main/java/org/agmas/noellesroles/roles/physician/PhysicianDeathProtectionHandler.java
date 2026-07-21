package org.agmas.noellesroles.roles.physician;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 医师药丸护盾死亡保护。
 *
 * <p>本处理器在 NoellesRoles 的强制死亡规则之后执行。
 * 因此落轨、炸弹、镇静过量、狙击枪等不可挡死因会先被放行，不会被药丸吞掉。</p>
 */
public final class PhysicianDeathProtectionHandler {
    private PhysicianDeathProtectionHandler() {
    }

    public static boolean allowDeath(PlayerEntity player, PlayerEntity killer, Identifier deathReason) {
        PhysicianPlayerComponent physician = PhysicianPlayerComponent.KEY.get(player);
        if (!physician.hasPillArmor()) {
            return true;
        }

        if (player instanceof ServerPlayerEntity victimPlayer) {
            NbtCompound blockedReplayData = GameFunctions.createBlockedDamageReplayData(killer, deathReason);
            GameRecordManager.recordShieldBlocked(
                    victimPlayer,
                    killer instanceof ServerPlayerEntity killerPlayer ? killerPlayer : null,
                    Noellesroles.PILL_SHIELD_SOURCE,
                    GameFunctions.getReplayItemId(blockedReplayData),
                    blockedReplayData
            );
        }

        physician.playArmorSound();
        physician.consumePillArmor();
        return false;
    }
}
