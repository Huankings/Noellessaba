package org.agmas.noellesroles.record;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.replay.DefaultReplayFormatters;
import dev.doctor4t.wathe.record.replay.ReplayGenerator;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * noellesroles 自己的回放文案格式化器。
 *
 * <p>用户要求把扩展职业专属文案和 wathe 主体分开维护，因此这里不去污染主模组的默认 formatter，
 * 而是借用 ReplayRegistry 的扩展入口单独注册。</p>
 */
public final class NoellesRolesReplayFormatters {
    private NoellesRolesReplayFormatters() {
    }

    private static @Nullable Text actorText(GameRecordEvent event, GameRecordManager.MatchRecord match) {
        if (!event.data().containsUuid("actor")) {
            return null;
        }
        return ReplayGenerator.formatPlayerName(event.data().getUuid("actor"), ReplayGenerator.getPlayerInfoCache(match));
    }

    private static @Nullable Text targetText(GameRecordEvent event, GameRecordManager.MatchRecord match) {
        if (!event.data().containsUuid("target")) {
            return null;
        }
        return ReplayGenerator.formatPlayerName(event.data().getUuid("target"), ReplayGenerator.getPlayerInfoCache(match));
    }

    private static @Nullable Text victimFromGlobal(GameRecordEvent event, GameRecordManager.MatchRecord match) {
        if (!event.data().containsUuid("victim")) {
            return null;
        }
        return ReplayGenerator.formatPlayerName(event.data().getUuid("victim"), ReplayGenerator.getPlayerInfoCache(match));
    }

    private static @Nullable Text ownerFromGlobal(GameRecordEvent event, GameRecordManager.MatchRecord match) {
        if (!event.data().containsUuid("owner")) {
            return null;
        }
        return ReplayGenerator.formatPlayerName(event.data().getUuid("owner"), ReplayGenerator.getPlayerInfoCache(match));
    }

    private static @Nullable Text bomberFromGlobal(GameRecordEvent event, GameRecordManager.MatchRecord match) {
        if (!event.data().containsUuid("bomber")) {
            return null;
        }
        return ReplayGenerator.formatPlayerName(event.data().getUuid("bomber"), ReplayGenerator.getPlayerInfoCache(match));
    }

    private static @Nullable Text playerFromKey(GameRecordEvent event, GameRecordManager.MatchRecord match, String key) {
        if (!event.data().containsUuid(key)) {
            return null;
        }
        return ReplayGenerator.formatPlayerName(event.data().getUuid(key), ReplayGenerator.getPlayerInfoCache(match));
    }

    private static MutableText roleText(@Nullable String rawRoleId) {
        Identifier roleId = rawRoleId == null || rawRoleId.isEmpty() ? null : Identifier.tryParse(rawRoleId);
        if (roleId == null) {
            return Text.translatable("replay.role.unknown");
        }

        Role role = WatheRoles.getRole(roleId);
        int roleColor = role != null ? role.color() : 0xFFFFFF;
        String translationKey = "wathe".equals(roleId.getNamespace())
                ? "announcement.title." + roleId.getPath()
                : "announcement.role." + roleId.getNamespace() + "." + roleId.getPath();
        String fallback = prettifyIdentifierPath(roleId.getPath());
        return Text.translatableWithFallback(translationKey, fallback)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(roleColor)));
    }

    private static String prettifyIdentifierPath(String path) {
        String[] parts = path.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? path : builder.toString();
    }

    private static Text formatStage(String translationKey, int color) {
        return Text.translatable(translationKey).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    private static MutableText deathReasonText(@Nullable String rawDeathReasonId) {
        Identifier deathReasonId = rawDeathReasonId == null || rawDeathReasonId.isEmpty() ? null : Identifier.tryParse(rawDeathReasonId);
        if (deathReasonId == null) {
            return Text.translatable("death_reason.wathe.generic");
        }
        return Text.translatable("death_reason." + deathReasonId.toString().replace(':', '.'));
    }

    @Nullable
    public static Text formatDefenseVialUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.item_use.noellesroles.defense_vial", actor);
    }

    @Nullable
    public static Text formatDelusionVialUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.item_use.noellesroles.delusion_vial", actor);
    }

    @Nullable
    public static Text formatSedativeUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.item_use.noellesroles.sedative", actor);
    }

    @Nullable
    public static Text formatDefensePlatterTake(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable("replay.platter_take.noellesroles.defense_vial", actor, ReplayGenerator.formatItemName(event.data(), world));
    }

    @Nullable
    public static Text formatDelusionPlatterTake(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable("replay.platter_take.noellesroles.delusion_vial", actor, ReplayGenerator.formatItemName(event.data(), world));
    }

    @Nullable
    public static Text formatSedativePlatterTake(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable("replay.platter_take.noellesroles.sedative", actor, ReplayGenerator.formatItemName(event.data(), world));
    }

    @Nullable
    public static Text formatDefenseConsume(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        Text itemName = ReplayGenerator.formatItemName(event.data(), world);
        String key = switch (event.data().getString("consume_type")) {
            case "drink_cocktail" -> "replay.consume.noellesroles.defense_vial.drink_cocktail";
            case "drink_potion" -> "replay.consume.noellesroles.defense_vial.drink_potion";
            default -> "replay.consume.noellesroles.defense_vial.eat_food";
        };
        return Text.translatable(key, actor, itemName);
    }

    @Nullable
    public static Text formatDelusionConsume(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        Text itemName = ReplayGenerator.formatItemName(event.data(), world);
        String key = switch (event.data().getString("consume_type")) {
            case "drink_cocktail" -> "replay.consume.noellesroles.delusion_vial.drink_cocktail";
            case "drink_potion" -> "replay.consume.noellesroles.delusion_vial.drink_potion";
            default -> "replay.consume.noellesroles.delusion_vial.eat_food";
        };
        return Text.translatable(key, actor, itemName);
    }

    @Nullable
    public static Text formatSedativeConsume(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        Text itemName = ReplayGenerator.formatItemName(event.data(), world);
        String key = switch (event.data().getString("consume_type")) {
            case "drink_cocktail" -> "replay.consume.noellesroles.sedative.drink_cocktail";
            case "drink_potion" -> "replay.consume.noellesroles.sedative.drink_potion";
            default -> "replay.consume.noellesroles.sedative.eat_food";
        };
        return Text.translatable(key, actor, itemName);
    }

    @Nullable
    public static Text formatDefenseShieldBlocked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        if (victim == null) {
            return null;
        }
        Text itemName = DefaultReplayFormatters.formatBlockedDamageName(event.data(), world);
        if (event.data().containsUuid("actor")) {
            Text attacker = actorText(event, match);
            if (attacker != null) {
                return Text.translatable("replay.shield_blocked.noellesroles.defense_vial.by_item", victim, attacker, itemName);
            }
        }
        return Text.translatable("replay.shield_blocked.noellesroles.defense_vial.item", victim, itemName);
    }

    @Nullable
    public static Text formatDelusionStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        return victim == null ? null : Text.translatable("replay.global.noellesroles.delusion_started", victim);
    }

    @Nullable
    public static Text formatDelusionEnded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        return victim == null ? null : Text.translatable("replay.global.noellesroles.delusion_ended", victim);
    }

    @Nullable
    public static Text formatCowardDangerSensed(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.coward_danger_sensed", actor);
    }

    @Nullable
    public static Text formatCowardDangerLeft(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.coward_danger_left", actor);
    }

    @Nullable
    public static Text formatSedativeStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.sedative_started", actor);
    }

    @Nullable
    public static Text formatSedativeEnded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.sedative_ended", actor);
    }

    @Nullable
    public static Text formatTimedBombUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        if ("tray".equals(event.data().getString("placement"))) {
            return Text.translatable("replay.global.noellesroles.timed_bomb_tray_embedded", actor);
        }
        Text target = targetText(event, match);
        if (target == null) {
            return null;
        }
        boolean activated = event.data().getBoolean("timed_bomb_activated");
        String key = activated
                ? "replay.item_use.noellesroles.timed_bomb.transfer"
                : "replay.item_use.noellesroles.timed_bomb.give";
        return Text.translatable(key, actor, target);
    }

    @Nullable
    public static Text formatThrowingAxeUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.item_use.noellesroles.throwing_axe", actor);
    }

    @Nullable
    public static Text formatRoleMineUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.item_use.noellesroles.role_mine", actor);
    }

    @Nullable
    public static Text formatToolboxUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        boolean repairedBlasted = event.data().getBoolean("repaired_blasted");
        String key = repairedBlasted
                ? "replay.item_use.noellesroles.toolbox.blasted"
                : "replay.item_use.noellesroles.toolbox.jammed";
        return Text.translatable(key, actor);
    }

    @Nullable
    public static Text formatCaptureDeviceUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.item_use.noellesroles.capture_device", actor);
    }

    @Nullable
    public static Text formatFakeGrenadeUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.item_use.noellesroles.fake_grenade", actor);
    }

    @Nullable
    public static Text formatTimedBombActivated(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        return victim == null ? null : Text.translatable("replay.global.noellesroles.timed_bomb_activated", victim);
    }

    @Nullable
    public static Text formatTimedBombTrayEmbedded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.timed_bomb_tray_embedded", actor);
    }

    @Nullable
    public static Text formatTimedBombTrayTake(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        Text itemName = ReplayGenerator.formatItemName(event.data(), world);
        return Text.translatable("replay.platter_take.noellesroles.timed_bomb", actor, itemName);
    }

    @Nullable
    public static Text formatTimedBombBedEmbedded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.timed_bomb_bed_embedded", actor);
    }

    @Nullable
    public static Text formatTimedBombBedTriggered(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        return victim == null ? null : Text.translatable("replay.global.noellesroles.timed_bomb_bed_triggered", victim);
    }

    @Nullable
    public static Text formatRoleMineDetected(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        Text owner = ownerFromGlobal(event, match);
        if (victim == null || owner == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.role_mine_detected", victim, owner);
    }

    @Nullable
    public static Text formatRoleMineReport(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text owner = ownerFromGlobal(event, match);
        return owner == null ? null : Text.translatable("replay.global.noellesroles.role_mine_report", owner);
    }

    @Nullable
    public static Text formatCaptureDeviceTriggered(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        Text owner = ownerFromGlobal(event, match);
        if (victim == null || owner == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.capture_device_triggered", victim, owner);
    }

    @Nullable
    public static Text formatCaptureDeviceReport(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text owner = ownerFromGlobal(event, match);
        return owner == null ? null : Text.translatable("replay.global.noellesroles.capture_device_report", owner);
    }

    @Nullable
    public static Text formatCaptureDeviceExpired(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text owner = ownerFromGlobal(event, match);
        return owner == null ? null : Text.translatable("replay.global.noellesroles.capture_device_expired", owner);
    }

    @Nullable
    public static Text formatCaptureDeviceReleased(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        return victim == null ? null : Text.translatable("replay.global.noellesroles.capture_device_released", victim);
    }

    @Nullable
    public static Text formatPowerRestored(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.power_restored", actor);
    }

    @Nullable
    public static Text formatJesterPsychoStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        return victim == null ? null : Text.translatable("replay.global.noellesroles.jester_psycho_started", victim);
    }

    @Nullable
    public static Text formatExecutionerTargetLocked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (target == null && event.data().containsUuid("locked_target")) {
            target = ReplayGenerator.formatPlayerName(event.data().getUuid("locked_target"), ReplayGenerator.getPlayerInfoCache(match));
        }
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.executioner_target_locked", actor, target);
    }

    @Nullable
    public static Text formatExecutionerTargetChanged(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null || !event.data().containsUuid("old_target") || !event.data().containsUuid("new_target")) {
            return null;
        }
        Text oldTarget = ReplayGenerator.formatPlayerName(event.data().getUuid("old_target"), ReplayGenerator.getPlayerInfoCache(match));
        Text newTarget = ReplayGenerator.formatPlayerName(event.data().getUuid("new_target"), ReplayGenerator.getPlayerInfoCache(match));
        return Text.translatable("replay.global.noellesroles.executioner_target_changed", actor, oldTarget, newTarget);
    }

    @Nullable
    public static Text formatVultureProgress(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        if (event.data().containsUuid("victim")) {
            Text victim = victimFromGlobal(event, match);
            if (victim == null) {
                return null;
            }
            return Text.translatable("replay.global.noellesroles.vulture_ate_body", actor, victim);
        }
        return Text.translatable(
                "replay.global.noellesroles.vulture_progress",
                actor,
                event.data().getInt("bodies_eaten"),
                event.data().getInt("bodies_required")
        );
    }

    @Nullable
    public static Text formatWindMarkUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.item_use.noellesroles.wind_mark", actor);
    }

    @Nullable
    public static Text formatCrystalBallUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.item_use.noellesroles.crystal_ball", actor);
    }

    @Nullable
    public static Text formatRecallerPositionSaved(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.recaller_position_saved",
                actor,
                event.data().getInt("x"),
                event.data().getInt("y"),
                event.data().getInt("z")
        );
    }

    @Nullable
    public static Text formatRecallerTeleported(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.recaller_teleported",
                actor,
                event.data().getInt("x"),
                event.data().getInt("y"),
                event.data().getInt("z"),
                event.data().getInt("cost")
        );
    }

    @Nullable
    public static Text formatRecallerEnderPearl(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.recaller_ender_pearl", actor);
    }

    @Nullable
    public static Text formatPhantomInvisibilityStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.phantom_invisibility_started", actor);
    }

    @Nullable
    public static Text formatPhantomInvisibilityEnded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.phantom_invisibility_ended", actor);
    }

    @Nullable
    public static Text formatProphetMarked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.prophet_marked", actor, target);
    }

    @Nullable
    public static Text formatProphetRemarked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text oldTarget = playerFromKey(event, match, "old_target");
        Text newTarget = playerFromKey(event, match, "new_target");
        if (actor == null || oldTarget == null || newTarget == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.prophet_remarked", actor, oldTarget, newTarget);
    }

    @Nullable
    public static Text formatProphetRevealed(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.prophet_revealed", actor, target, event.data().getInt("cost"));
    }

    @Nullable
    public static Text formatProphetVoodooImmunity(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text protectedPlayer = actorText(event, match);
        Text prophet = playerFromKey(event, match, "prophet_player");
        Text voodooCaster = playerFromKey(event, match, "voodoo_player");
        if (protectedPlayer == null || prophet == null || voodooCaster == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.prophet_voodoo_immunity",
                protectedPlayer,
                prophet,
                voodooCaster,
                deathReasonText(event.data().getString("death_reason_id"))
        );
    }

    @Nullable
    public static Text formatWinderWindMarkApplied(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.winder_wind_mark_applied", actor, target);
    }

    @Nullable
    public static Text formatWinderWindChargeUsed(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.winder_wind_charge_used", actor);
    }

    @Nullable
    public static Text formatWinderWindMarkExpired(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        return victim == null ? null : Text.translatable("replay.global.noellesroles.winder_wind_mark_expired", victim);
    }

    @Nullable
    public static Text formatWinderWindMarkTriggered(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        Text knifeUser = playerFromKey(event, match, "knife_user");
        if (victim == null || knifeUser == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.winder_wind_mark_triggered", victim, knifeUser);
    }

    @Nullable
    public static Text formatWinderFloatStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.winder_float_started", actor, target);
    }

    @Nullable
    public static Text formatWinderFloatEnded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text target = playerFromKey(event, match, "target_player");
        return target == null ? null : Text.translatable("replay.global.noellesroles.winder_float_ended", target);
    }

    @Nullable
    public static Text formatWinderFloatStoppedEarly(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.winder_float_stopped_early", actor, target);
    }

    @Nullable
    public static Text formatStalkerPhaseAdvance12(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.stalker_phase_change",
                actor,
                formatStage("replay.stage.noellesroles.stalker.phase1", 0x5B1A7A),
                formatStage("replay.stage.noellesroles.stalker.phase2", 0xFF0000)
        );
    }

    @Nullable
    public static Text formatStalkerPhaseAdvance23(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.stalker_phase_change",
                actor,
                formatStage("replay.stage.noellesroles.stalker.phase2", 0xFF0000),
                formatStage("replay.stage.noellesroles.stalker.phase3", 0x8B0000)
        );
    }

    @Nullable
    public static Text formatStalkerPhaseRegress32(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.stalker_phase_regress",
                actor,
                formatStage("replay.stage.noellesroles.stalker.phase3", 0x8B0000),
                formatStage("replay.stage.noellesroles.stalker.phase2", 0xFF0000)
        );
    }

    @Nullable
    public static Text formatNoisemakerGlowStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.noisemaker_glow_started", actor, target);
    }

    @Nullable
    public static Text formatNoisemakerGlowEnded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        return victim == null ? null : Text.translatable("replay.global.noellesroles.noisemaker_glow_ended", victim);
    }

    @Nullable
    public static Text formatMorphlingMorphStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.morphling_morph_started", actor, target);
    }

    @Nullable
    public static Text formatMorphlingMorphEnded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.morphling_morph_ended", actor);
    }

    @Nullable
    public static Text formatSwapperSwapSelected(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text playerOne = playerFromKey(event, match, "player_one");
        Text playerTwo = playerFromKey(event, match, "player_two");
        if (actor == null || playerOne == null || playerTwo == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.swapper_swap_selected", actor, playerOne, playerTwo);
    }

    @Nullable
    public static Text formatSwapperSwapExecuted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text playerOne = playerFromKey(event, match, "player_one");
        Text playerTwo = playerFromKey(event, match, "player_two");
        if (actor == null || playerOne == null || playerTwo == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.swapper_swap_executed", playerOne, playerTwo, actor);
    }

    @Nullable
    public static Text formatCorpsemakerForgedBody(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text corpseTarget = playerFromKey(event, match, "corpse_target");
        if (actor == null || corpseTarget == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.corpsemaker_forged_body",
                actor,
                corpseTarget,
                deathReasonText(event.data().getString("death_reason_id")),
                roleText(event.data().getString("fake_role_id"))
        );
    }

    @Nullable
    public static Text formatVoodooBound(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.voodoo_bound", actor, target);
    }

    @Nullable
    public static Text formatGuesserDeclared(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        MutableText guessedRole = event.data().contains("guessed_role_id")
                ? roleText(event.data().getString("guessed_role_id"))
                : Text.literal(event.data().getString("guessed_role_fallback"));
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.guesser_declared", actor, target, guessedRole);
    }

    @Nullable
    public static Text formatGuesserCorrect(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.guesser_correct", actor, target);
    }

    @Nullable
    public static Text formatGuesserWrong(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.guesser_wrong", actor, target);
    }

    @Nullable
    public static Text formatControllerPossessStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.controller_possess_started", actor, target);
    }

    @Nullable
    public static Text formatControllerPossessStoppedEarly(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.controller_possess_stopped_early", actor, target);
    }

    @Nullable
    public static Text formatControllerPossessEnded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.controller_possess_ended", actor);
    }

    @Nullable
    public static Text formatAngelGuardShieldBlocked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text guarded = targetText(event, match);
        Text angel = playerFromKey(event, match, "angel_player");
        if (guarded == null || angel == null) {
            return null;
        }

        Text damageName = DefaultReplayFormatters.formatBlockedDamageName(event.data(), world);
        if (event.data().containsUuid("actor")) {
            Text attacker = actorText(event, match);
            if (attacker != null) {
                return Text.translatable("replay.global.noellesroles.angel_guard_blocked_full", guarded, angel, attacker, damageName);
            }
        }
        return Text.translatable("replay.shield_blocked.noellesroles.angel_guard.item", guarded, damageName);
    }

    @Nullable
    public static Text formatAngelSootheCast(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.angel_soothe_cast", actor);
    }

    @Nullable
    public static Text formatAngelSoothed(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.angel_soothed", target, actor);
    }

    @Nullable
    public static Text formatAngelGuardSelected(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.angel_guard_selected", actor, target);
    }

    @Nullable
    public static Text formatAngelSacrificeDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text angel = targetText(event, match);
        Text guarded = playerFromKey(event, match, "target_player");
        if (angel == null || guarded == null) {
            return null;
        }
        return Text.translatable("replay.death.noellesroles.angel_sacrifice.died", angel, guarded);
    }

    @Nullable
    public static Text formatSedativeOverdoseDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        return victim == null ? null : Text.translatable("replay.death.noellesroles.sedative_overdose.died", victim);
    }
}
