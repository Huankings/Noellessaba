package org.agmas.noellesroles.roles.party_animal;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.api.timekeeper.TimekeeperSnapshotApi;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 派对狂能力、等级叠加和变声目标处理。 */
public final class PartyAnimalAbility {
    private PartyAnimalAbility() {
    }

    public static void handle(ServerPlayerEntity user, int targetEntityId) {
        GameWorldComponent game = GameWorldComponent.KEY.get(user.getWorld());
        if (!game.isRole(user, NoellesRoleRegistry.PARTY_ANIMAL)
                || !GameFunctions.isPlayerAliveAndSurvival(user)
                || TimekeeperSnapshotApi.isRewinding(user.getServerWorld())) return;

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(user);
        if (ability.cooldown > 0) return;
        PartyAnimalPlayerComponent state = PartyAnimalPlayerComponent.KEY.get(user);

        if (state.hasMarkedTarget()) {
            ServerPlayerEntity target = user.getServer().getPlayerManager().getPlayer(state.getMarkedTarget());
            state.clearMark();
            if (target == null || !GameFunctions.isPlayerAliveAndSurvival(target)) {
                ability.setCooldown(PartyAnimalConstants.MARK_FAILURE_COOLDOWN_TICKS);
                return;
            }
            int next = calculateNextLevel(HeliumBuzzPlayerComponent.KEY.get(target).getEffectiveLevel(), state.getSelectedType());
            if (next == Integer.MIN_VALUE) {
                ability.setCooldown(PartyAnimalConstants.MARK_FAILURE_COOLDOWN_TICKS);
                return;
            }
            applyNormalLevel(user, target, next, false);
            ability.setCooldown(PartyAnimalConstants.ABILITY_COOLDOWN_TICKS);
            user.sendMessage(Text.translatable("hud.noellesroles.party_animal.cast_target", target.getName(), typeText(state.getSelectedType()), displayLevel(next)).withColor(PartyAnimalConstants.ROLE_COLOR), true);
            recordCast(user, target, next, false, state.getSelectedType());
            return;
        }

        ServerPlayerEntity target = resolveTarget(user, targetEntityId);
        if (target != null && target != user) {
            int current = HeliumBuzzPlayerComponent.KEY.get(target).getEffectiveLevel();
            if (calculateNextLevel(current, state.getSelectedType()) == Integer.MIN_VALUE) {
                user.sendMessage(Text.translatable("hud.noellesroles.party_animal.target_max", typeText(state.getSelectedType()), PartyAnimalConstants.MAX_LEVEL).withColor(PartyAnimalConstants.ROLE_COLOR), true);
                return;
            }
            state.mark(target.getUuid(), target.getName().getString());
            user.sendMessage(Text.translatable("hud.noellesroles.party_animal.marked", target.getName()).withColor(PartyAnimalConstants.ROLE_COLOR), true);
            recordMark(user, target);
            return;
        }

        int next = calculateNextLevel(HeliumBuzzPlayerComponent.KEY.get(user).getEffectiveLevel(), state.getSelectedType());
        if (next == Integer.MIN_VALUE) {
            user.sendMessage(Text.translatable("hud.noellesroles.party_animal.self_max", typeText(state.getSelectedType()), PartyAnimalConstants.MAX_LEVEL).withColor(PartyAnimalConstants.ROLE_COLOR), true);
            return;
        }
        applyNormalLevel(user, user, next, true);
        ability.setCooldown(PartyAnimalConstants.ABILITY_COOLDOWN_TICKS);
        user.sendMessage(Text.translatable("hud.noellesroles.party_animal.cast_self", typeText(state.getSelectedType()), displayLevel(next)).withColor(PartyAnimalConstants.ROLE_COLOR), true);
        recordCast(user, null, next, true, state.getSelectedType());
    }

    /** 服务端每 tick 检查标记超时、目标失效和调试等级导致的方向上限。 */
    public static void tick(ServerPlayerEntity user) {
        /* 回溯期间能力状态由历史快照驱动，不能再按当前目标有效性写入新冷却。 */
        if (TimekeeperSnapshotApi.isRewinding(user.getServerWorld())) {
            return;
        }
        if (!GameWorldComponent.KEY.get(user.getWorld()).isRole(user, NoellesRoleRegistry.PARTY_ANIMAL)) {
            return;
        }
        PartyAnimalPlayerComponent state = PartyAnimalPlayerComponent.KEY.get(user);
        if (state.consumeMarkExpiredThisTick()) {
            AbilityPlayerComponent.KEY.get(user).setCooldown(PartyAnimalConstants.MARK_FAILURE_COOLDOWN_TICKS);
            return;
        }
        if (!state.hasMarkedTarget()) return;
        ServerPlayerEntity target = user.getServer().getPlayerManager().getPlayer(state.getMarkedTarget());
        if (target == null || !GameFunctions.isPlayerAliveAndSurvival(target)
                || calculateNextLevel(HeliumBuzzPlayerComponent.KEY.get(target).getEffectiveLevel(), state.getSelectedType()) == Integer.MIN_VALUE) {
            state.clearMark();
            AbilityPlayerComponent.KEY.get(user).setCooldown(PartyAnimalConstants.MARK_FAILURE_COOLDOWN_TICKS);
        }
    }

    private static ServerPlayerEntity resolveTarget(ServerPlayerEntity user, int id) {
        Entity entity = user.getWorld().getEntityById(id);
        if (!(entity instanceof ServerPlayerEntity target)
                || target == user
                || !GameFunctions.isPlayerAliveAndSurvival(target)
                || user.squaredDistanceTo(target) > PartyAnimalConstants.TARGET_RANGE_BLOCKS * PartyAnimalConstants.TARGET_RANGE_BLOCKS
                || !user.canSee(target)) return null;
        return target;
    }

    /** 返回 Integer.MIN_VALUE 表示当前方向已经达到上限。 */
    public static int calculateNextLevel(int current, int direction) {
        int next = current + (direction < 0 ? -1 : 1);
        if (direction > 0 && next > PartyAnimalConstants.MAX_LEVEL) return Integer.MIN_VALUE;
        if (direction < 0 && next < -PartyAnimalConstants.MAX_LEVEL) return Integer.MIN_VALUE;
        return next;
    }

    private static void applyNormalLevel(ServerPlayerEntity actor, ServerPlayerEntity target, int level, boolean self) {
        HeliumBuzzPlayerComponent.KEY.get(target).setNormalLevel(level);
    }

    private static String typeKey(int direction) {
        return direction < 0 ? "hud.noellesroles.party_animal.pitch_down" : "hud.noellesroles.party_animal.pitch_up";
    }

    private static Text typeText(int direction) {
        return Text.translatable(typeKey(direction));
    }

    private static int displayLevel(int level) {
        return Math.abs(level);
    }

    private static void recordMark(ServerPlayerEntity actor, ServerPlayerEntity target) {
        NbtCompound data = new NbtCompound();
        data.putUuid("actor", actor.getUuid());
        data.putUuid("target", target.getUuid());
        GameRecordManager.recordGlobalEvent(actor.getServerWorld(), NoellesEventIds.PARTY_ANIMAL_MARK_EVENT, actor, data);
    }

    private static void recordCast(ServerPlayerEntity actor, ServerPlayerEntity target, int level, boolean self, int direction) {
        NbtCompound data = new NbtCompound();
        data.putUuid("actor", actor.getUuid());
        data.putInt("level", level);
        data.putBoolean("self", self);
        data.putString("type_key", typeKey(direction));
        if (target != null) data.putUuid("target", target.getUuid());
        GameRecordManager.recordGlobalEvent(actor.getServerWorld(), self ? NoellesEventIds.PARTY_ANIMAL_SELF_PITCH_EVENT : NoellesEventIds.PARTY_ANIMAL_TARGET_PITCH_EVENT, actor, data);
    }
}
