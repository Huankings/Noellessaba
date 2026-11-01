package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityActionGuard;
import org.agmas.noellesroles.packet.role.thief.ThiefAllaySummonC2SPacket;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;

import java.util.ArrayList;
import java.util.List;

/** 小偷悦灵召唤与 G 键主动清理的服务端权威入口。 */
public final class ThiefAllayAbility {
    private ThiefAllayAbility() {
    }

    public static void handleSummon(ThiefAllaySummonC2SPacket payload, ServerPlayerEntity thief) {
        if (!canUseAbility(thief)) {
            return;
        }
        int count = payload.count();
        if (count < ThiefConstants.ALLAY_MIN_COUNT || count > ThiefConstants.ALLAY_MAX_COUNT) {
            return;
        }

        ThiefPlayerComponent component = ThiefPlayerComponent.KEY.get(thief);
        if (component.getSummonCooldownTicks() > 0) {
            thief.sendMessage(darkRed("message.noellesroles.thief.allay.cooldown",
                    Math.max(0, (component.getSummonCooldownTicks()
                            + ThiefConstants.COOLDOWN_SECONDS_ROUNDING_OFFSET)
                            / ThiefConstants.TICKS_PER_SECOND)), true);
            return;
        }
        if (component.hasLivingAllayState() || hasLivingAllays(thief.getServerWorld(), thief)) {
            thief.sendMessage(darkRed("message.noellesroles.thief.allay.active_failure"), true);
            return;
        }

        ServerPlayerEntity target = thief.getServer().getPlayerManager().getPlayer(payload.targetUuid());
        if (target == null
                || target.getServerWorld() != thief.getServerWorld()
                || !ThiefAllayTargeting.isValidTarget(target, thief.getUuid())) {
            thief.sendMessage(darkRed("message.noellesroles.thief.allay.invalid_target"), true);
            return;
        }

        List<ThiefAllayEntity> spawned = new ArrayList<>();
        for (int number = 1; number <= count; number++) {
            ThiefAllayEntity allay = NoellesRolesEntities.THIEF_ALLAY_ENTITY_TYPE.create(thief.getServerWorld());
            if (allay == null) {
                spawned.forEach(ThiefAllayEntity::discardSilently);
                component.clearAllayStates();
                return;
            }
            double angle = (Math.PI * 2.0D * (number - 1)) / count;
            Vec3d spawnPos = thief.getPos().add(
                    Math.cos(angle) * ThiefConstants.ALLAY_SPAWN_RADIUS,
                    ThiefConstants.ALLAY_SPAWN_HEIGHT,
                    Math.sin(angle) * ThiefConstants.ALLAY_SPAWN_RADIUS
            );
            allay.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, thief.getYaw(), 0.0F);
            allay.initialize(thief, target, number);
            if (!thief.getServerWorld().spawnEntity(allay)) {
                allay.discardSilently();
                spawned.forEach(ThiefAllayEntity::discardSilently);
                component.clearAllayStates();
                return;
            }
            spawned.add(allay);
        }

        component.setSummonCooldownTicks(ThiefConstants.getAllayCooldownTicks(count));
        ThiefAllayReplay.recordSummon(thief, count, target);
        thief.sendMessage(roleColored("message.noellesroles.thief.allay.summoned", count, target.getName()), true);
    }

    /**
     * G 键只负责主动杀死仍存活的所属悦灵，不改变背包冷却。
     * 每只悦灵会正常掉落已偷物品并显示“不幸身亡”，回放则只写一条汇总事件。
     */
    public static void handleKillAllays(ServerPlayerEntity thief) {
        if (!canUseAbility(thief)) {
            return;
        }
        List<ThiefAllayEntity> living = getLivingAllays(thief.getServerWorld(), thief);
        if (living.isEmpty()) {
            thief.sendMessage(darkRed("message.noellesroles.thief.allay.none_alive"), true);
            return;
        }
        for (ThiefAllayEntity allay : living) {
            allay.killByOwnerCommand();
        }
        ThiefAllayReplay.recordOwnerPurge(thief);
    }

    public static boolean hasLivingAllays(ServerWorld world, ServerPlayerEntity thief) {
        return !getLivingAllays(world, thief).isEmpty();
    }

    public static List<ThiefAllayEntity> getLivingAllays(ServerWorld world, ServerPlayerEntity thief) {
        List<ThiefAllayEntity> result = new ArrayList<>();
        for (ServerWorld serverWorld : world.getServer().getWorlds()) {
            result.addAll(serverWorld.getEntitiesByType(
                    NoellesRolesEntities.THIEF_ALLAY_ENTITY_TYPE,
                    allay -> thief.getUuid().equals(allay.getOwnerUuid()) && allay.isOperationallyAlive()
            ));
        }
        return result;
    }

    private static boolean canUseAbility(ServerPlayerEntity thief) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(thief.getWorld());
        return gameWorld.isRole(thief, NoellesRoleRegistry.THIEF)
                && gameWorld.isRunning()
                && !TimekeeperWorldComponent.KEY.get(thief.getWorld()).isRewinding()
                && GameFunctions.isPlayerAliveAndSurvival(thief)
                && !TimekeeperPlayerComponent.KEY.get(thief).isInTimeRift()
                && !DualPersonalityActionGuard.isDormant(thief);
    }

    private static Text roleColored(String key, Object... args) {
        return Text.translatable(key, args).setStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(NoellesRoleRegistry.THIEF.color()))
        );
    }

    private static Text darkRed(String key, Object... args) {
        return Text.translatable(key, args).setStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(ThiefConstants.FAILURE_ACTIONBAR_COLOR))
        );
    }
}
