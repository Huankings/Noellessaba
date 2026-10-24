package org.agmas.noellesroles.roles.party_animal;

import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.KnifeCooldownApi;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.api.timekeeper.TimekeeperSnapshotApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 派对狂对变声目标的匕首与左轮冷却修正。 */
public final class PartyAnimalCombatHandler {
    private PartyAnimalCombatHandler() {
    }

    public static void init() {
        KnifeCooldownApi.registerCooldownModifier(NoellesRolesCore.id("party_animal/knife_cooldown"), 1000,
                (context, current) -> GameWorldRole.isPartyAnimal(context.attacker()) ? modify(current, context.target()) : current);
        GunShotApi.registerCooldownModifier(NoellesRolesCore.id("party_animal/gun_cooldown"), 1000,
                (context, current) -> context.stack().isOf(WatheItems.REVOLVER)
                        && GameWorldRole.isPartyAnimal(context.shooter()) ? modify(current, context.target()) : current);
    }

    private static int modify(int current, PlayerEntity target) {
        if (target == null) return current;
        /* 回溯期间禁止未来时间线的攻击链消费等级或写入新的武器冷却。 */
        if (target.getWorld() instanceof ServerWorld serverWorld
                && TimekeeperSnapshotApi.isRewinding(serverWorld)) {
            return current;
        }
        int level = Math.abs(HeliumBuzzPlayerComponent.KEY.get(target).consumeCooldownLevel());
        if (level <= 0) return current;
        double multiplier = switch (level) {
            case 1 -> PartyAnimalConstants.LEVEL_1_COOLDOWN_MULTIPLIER;
            case 2 -> PartyAnimalConstants.LEVEL_2_COOLDOWN_MULTIPLIER;
            default -> PartyAnimalConstants.LEVEL_3_COOLDOWN_MULTIPLIER;
        };
        return (int) Math.floor(current * multiplier);
    }

    private static final class GameWorldRole {
        private static boolean isPartyAnimal(PlayerEntity player) {
            return dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(player.getWorld())
                    .isRole(player, org.agmas.noellesroles.registry.NoellesRoleRegistry.PARTY_ANIMAL);
        }
    }
}
