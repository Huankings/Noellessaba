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
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWatchMode;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

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

    private static @Nullable Text playerText(@Nullable UUID uuid, GameRecordManager.MatchRecord match) {
        return uuid == null ? null : ReplayGenerator.formatPlayerName(uuid, ReplayGenerator.getPlayerInfoCache(match));
    }

    private static @Nullable UUID uuid(NbtCompound data, String key) {
        return data.containsUuid(key) ? data.getUuid(key) : null;
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

    private static Text whiteBracketedItem(NbtCompound data, ServerWorld world) {
        /*
         * 用户希望飞斧回放变成“[%s]”的通用模板，并且中括号内用白色显示。
         * 这里不在 lang 里硬塞颜色码，而是把物品名解析出来后统一包一层白色 Text，
         * 这样普通飞斧、增速飞斧、爆炸飞斧未来继续复用同一个 formatter 时也不会丢本地化名字。
         */
        Text item = ReplayGenerator.resolveItemName(data, world);
        return Text.empty()
                .append(Text.literal("[").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))))
                .append(item.copy().setStyle(item.getStyle().withColor(TextColor.fromRgb(0xFFFFFF))))
                .append(Text.literal("]").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))));
    }

    private static MutableText deathReasonText(@Nullable String rawDeathReasonId) {
        Identifier deathReasonId = rawDeathReasonId == null || rawDeathReasonId.isEmpty() ? null : Identifier.tryParse(rawDeathReasonId);
        if (deathReasonId == null) {
            return Text.translatable("death_reason.wathe.generic");
        }
        return Text.translatable("death_reason." + deathReasonId.toString().replace(':', '.'));
    }

    // 回放里服务员事件需要展示“完成了哪个任务”，所以把 task lang key 单独抽出来格式化。
    private static Text taskText(GameRecordEvent event) {
        String taskKey = event.data().getString("task");
        if (taskKey == null || taskKey.isEmpty()) {
            return Text.translatable("replay.task.unknown");
        }
        return Text.translatable(taskKey);
    }

    /*
     * 带试剂或托盘效果的服务员递送，需要同时展示效果翻译 key 和本地化 fallback。
     * 这样在不同语言环境下，既能优先显示 lang 里的正式名称，也不会因为语言包缺失而空白。
     */
    private static Text effectText(GameRecordEvent event) {
        String effectKey = event.data().getString("effect_translation_key");
        String fallback = event.data().getString("effect_fallback");
        if (effectKey == null || effectKey.isEmpty()) {
            return Text.translatable("replay.item.unknown");
        }
        return Text.translatableWithFallback(effectKey, fallback == null || fallback.isEmpty() ? effectKey : fallback);
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
    public static Text formatAllergicShieldBlocked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        if (victim == null) {
            return null;
        }

        Text attacker = actorText(event, match);
        Text damageName = DefaultReplayFormatters.formatBlockedDamageName(event.data(), world);
        /*
         * 过敏护盾回放按用户指定的“被保护者 / 来源 / 伤害名”三段展示。
         * 某些非玩家伤害没有 actor，此时用本地化未知来源兜底，避免 formatter 返回 null 丢事件。
         */
        return Text.translatable(
                "replay.shield_blocked.noellesroles.allergic.by_item",
                victim,
                attacker == null ? Text.translatable("replay.source.unknown") : attacker,
                damageName
        );
    }

    @Nullable
    public static Text formatDreamImprintUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.item_use.noellesroles.dream_imprint", actor, target);
    }

    @Nullable
    public static Text formatHackerReveal(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.skill_use.noellesroles.hacker", actor, target);
    }

    @Nullable
    public static Text formatBellringerReduceTime(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable(
                "replay.skill_use.noellesroles.bellringer",
                actor,
                event.data().getInt("seconds"),
                event.data().getInt("price")
        );
    }

    @Nullable
    public static Text formatDetectiveCheck(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (actor == null || target == null) {
            return null;
        }

        boolean innocent = event.data().getBoolean("innocent");
        return Text.translatable(
                innocent
                        ? "replay.skill_use.noellesroles.detective.innocent"
                        : "replay.skill_use.noellesroles.detective.notinnocent",
                actor,
                target
        );
    }

    @Nullable
    public static Text formatStarstruckAbility(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.skill_use.noellesroles.starstruck", actor);
    }

    @Nullable
    public static Text formatCleanerClearItems(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable("replay.skill_use.noellesroles.cleaner", actor, event.data().getInt("price"));
    }

    @Nullable
    public static Text formatHunterRefresh(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable("replay.skill_use.noellesroles.hunter", actor, event.data().getInt("price"));
    }

    @Nullable
    public static Text formatRobotNightVision(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.skill_use.noellesroles.robot", actor);
    }

    @Nullable
    public static Text formatKidnapperRelease(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        Text target = targetText(event, match);
        if (target != null) {
            return Text.translatable("replay.skill_use.noellesroles.kidnapper.release", actor, target);
        }
        return Text.translatable("replay.skill_use.noellesroles.kidnapper.release_end", actor);
    }

    @Nullable
    public static Text formatKnockoutDrugUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (actor == null || target == null) {
            return null;
        }
        if (event.data().getBoolean("robot_failed")) {
            return Text.translatable("replay.item_use.noellesroles.knockout_drug.failed_robot", target, actor);
        }
        return Text.translatable("replay.item_use.noellesroles.knockout_drug", actor, target);
    }

    @Nullable
    public static Text formatPoisonInjectorUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (actor == null || target == null) {
            return null;
        }
        if (event.data().getBoolean("robot_failed")) {
            return Text.translatable("replay.item_use.noellesroles.poison_injector.failed_robot", target, actor);
        }
        return Text.translatable("replay.item_use.noellesroles.poison_injector", actor, target);
    }

    @Nullable
    public static Text formatBlowgunHit(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (actor == null || target == null) {
            return null;
        }
        if (event.data().getBoolean("robot_failed")) {
            return Text.translatable("replay.item_hit.noellesroles.blowgun.failed_robot", target, actor);
        }
        return Text.translatable("replay.item_hit.noellesroles.blowgun", actor, target);
    }

    @Nullable
    public static Text formatRobotNightVisionEnd(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.robot_night_vision_end", actor);
    }

    @Nullable
    public static Text formatRobotPoisonImmune(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        Text item = ReplayGenerator.resolveItemName(event.data(), world);
        Text poisoner = playerText(uuid(event.data(), "poisoner"), match);
        return poisoner == null ? null : Text.translatable("replay.global.noellesroles.robot.poison_immune", actor, item, poisoner);
    }

    @Nullable
    public static Text formatRobotBedPoisonImmune(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        Text poisoner = playerText(uuid(event.data(), "poisoner"), match);
        return poisoner == null ? null : Text.translatable("replay.global.noellesroles.robot.bed_poison_immune", actor, poisoner);
    }

    @Nullable
    public static Text formatThiefAttempt(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.thief_attempt", actor, target);
    }

    @Nullable
    public static Text formatThiefSuccess(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.thief_success", actor, target, ReplayGenerator.formatItemName(event.data(), world));
    }

    @Nullable
    public static Text formatThiefFail(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.thief_fail", actor, target);
    }

    @Nullable
    public static Text formatStarstruckAbilityEnd(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.starstruck_end", actor);
    }

    @Nullable
    public static Text formatTapeUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.item_use.noellesroles.tape", actor, target);
    }

    @Nullable
    public static Text formatSulfuricAcidBarrelUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text corpseOwner = playerFromKey(event, match, "body_owner");
        if (actor == null || corpseOwner == null) {
            return null;
        }
        return Text.translatable("replay.item_use.noellesroles.sulfuric_acid_barrel", actor, corpseOwner);
    }

    @Nullable
    public static Text formatHuntingKnifeHit(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.item_hit.noellesroles.hunting_knife", actor, target);
    }

    @Nullable
    public static Text formatAxeHit(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable(
                "replay.item_hit.noellesroles.axe",
                actor,
                ReplayGenerator.resolveItemName(event.data(), world),
                target
        );
    }

    @Nullable
    public static Text formatTapeRemoved(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text remover = actorText(event, match);
        Text victim = targetText(event, match);
        Text silencer = playerFromKey(event, match, "silencer");
        if (remover == null || victim == null || silencer == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.tape_removed", remover, victim, silencer);
    }

    @Nullable
    public static Text formatDreamerCounts(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.dreamer_counts",
                actor,
                event.data().getInt("counts"),
                event.data().getInt("required")
        );
    }

    @Nullable
    public static Text formatDreamImprintShieldBlocked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        if (victim == null) {
            return null;
        }

        Text damageName = DefaultReplayFormatters.formatBlockedDamageName(event.data(), world);
        if (event.data().containsUuid("actor")) {
            Text attacker = actorText(event, match);
            if (attacker != null) {
                return Text.translatable("replay.shield_blocked.noellesroles.dream_imprint.by_item", victim, attacker, damageName);
            }
        }
        return Text.translatable("replay.shield_blocked.noellesroles.dream_imprint.item", victim, damageName);
    }

    @Nullable
    public static Text formatMedicalKitUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.item_use.noellesroles.medical_kit", actor, target);
    }

    @Nullable
    public static Text formatPillUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.item_use.noellesroles.pill", actor);
    }

    @Nullable
    public static Text formatPanHit(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.item_hit.noellesroles.pan", actor, target);
    }

    @Nullable
    public static Text formatPanStunEnd(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        return victim == null ? null : Text.translatable("replay.global.noellesroles.pan_stun_end", victim);
    }

    @Nullable
    public static Text formatPillShieldBlocked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        return formatSimpleShieldBlocked(
                event,
                match,
                world,
                "replay.shield_blocked.noellesroles.pill.item",
                "replay.shield_blocked.noellesroles.pill.by_item"
        );
    }

    @Nullable
    public static Text formatPanShieldBlocked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        return formatSimpleShieldBlocked(
                event,
                match,
                world,
                "replay.shield_blocked.noellesroles.pan.item",
                "replay.shield_blocked.noellesroles.pan.by_item"
        );
    }

    private static @Nullable Text formatSimpleShieldBlocked(GameRecordEvent event,
                                                            GameRecordManager.MatchRecord match,
                                                            ServerWorld world,
                                                            String itemKey,
                                                            String byItemKey) {
        Text victim = targetText(event, match);
        if (victim == null) {
            return null;
        }

        Text damageName = DefaultReplayFormatters.formatBlockedDamageName(event.data(), world);
        if (event.data().containsUuid("actor")) {
            Text attacker = actorText(event, match);
            if (attacker != null) {
                return Text.translatable(byItemKey, victim, attacker, damageName);
            }
        }
        return Text.translatable(itemKey, victim, damageName);
    }

    @Nullable
    public static Text formatAmnesiacRoleStolen(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text corpseOwner = playerFromKey(event, match, "corpse_owner");
        if (actor == null || corpseOwner == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.amnesiac_role_stolen", actor, corpseOwner);
    }

    @Nullable
    public static Text formatArsonistDoused(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.arsonist_doused", actor, target);
    }

    @Nullable
    public static Text formatArsonistLighterCooldownStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.arsonist_lighter_cooldown_started", actor);
    }

    @Nullable
    public static Text formatArsonistLighterCooldownFinished(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.arsonist_lighter_cooldown_finished", actor);
    }

    @Nullable
    public static Text formatConvenerSummon(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text corpseOwner = playerFromKey(event, match, "corpse_owner");
        if (actor == null || corpseOwner == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.convener_summon",
                actor,
                corpseOwner,
                event.data().getInt("summon_count"),
                event.data().getInt("required_summons")
        );
    }

    @Nullable
    public static Text formatConvenerCounterShieldGained(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.convener_counter_shield_gained",
                actor,
                event.data().getInt("current_layers")
        );
    }

    @Nullable
    public static Text formatAllergicPoisonTriggered(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.allergic_poison_triggered", actor);
    }

    @Nullable
    public static Text formatAllergicInstinctTriggered(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.allergic_instinct_triggered", actor);
    }

    @Nullable
    public static Text formatAllergicInstinctEnded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.allergic_instinct_ended", actor);
    }

    @Nullable
    public static Text formatAllergicShieldGained(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.allergic_shield_gained", actor);
    }

    @Nullable
    public static Text formatConvenerVoodooImmunity(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text protectedPlayer = actorText(event, match);
        Text voodooCaster = playerFromKey(event, match, "voodoo_player");
        if (protectedPlayer == null || voodooCaster == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.convener_voodoo_immunity",
                protectedPlayer,
                voodooCaster,
                Text.translatable("death_reason.noellesroles.voodoo")
        );
    }

    @Nullable
    public static Text formatConvenerCounterShieldBlocked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        return formatSimpleShieldBlocked(
                event,
                match,
                world,
                "replay.shield_blocked.noellesroles.convener_counter_shield.item",
                "replay.shield_blocked.noellesroles.convener_counter_shield.by_item"
        );
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
        return actor == null ? null : Text.translatable("replay.item_use.noellesroles.throwing_axe", actor, whiteBracketedItem(event.data(), world));
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
    public static Text formatSilentGrenadeUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.item_use.noellesroles.silent_grenade", actor);
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
    public static Text formatBountyHunterTargetLocked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (target == null && event.data().containsUuid("locked_target")) {
            target = ReplayGenerator.formatPlayerName(event.data().getUuid("locked_target"), ReplayGenerator.getPlayerInfoCache(match));
        }
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.bounty_hunter_target_locked", actor, target);
    }

    @Nullable
    public static Text formatBountyHunterTargetChanged(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null || !event.data().containsUuid("old_target") || !event.data().containsUuid("new_target")) {
            return null;
        }
        Text oldTarget = ReplayGenerator.formatPlayerName(event.data().getUuid("old_target"), ReplayGenerator.getPlayerInfoCache(match));
        Text newTarget = ReplayGenerator.formatPlayerName(event.data().getUuid("new_target"), ReplayGenerator.getPlayerInfoCache(match));
        return Text.translatable("replay.global.noellesroles.bounty_hunter_target_changed", actor, oldTarget, newTarget);
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
    public static Text formatWaiterServe(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (actor == null || target == null) {
            return null;
        }

        // 先取物品名、任务名，再按是否带试剂/毒药效果切换不同的回放句式。
        Text itemName = ReplayGenerator.formatItemName(event.data(), world);
        Text taskName = taskText(event);
        if (event.data().contains("effect_translation_key")) {
            return Text.translatable("replay.global.noellesroles.waiter_serve.effect", actor, effectText(event), itemName, target, target, taskName);
        }
        return Text.translatable("replay.global.noellesroles.waiter_serve", actor, itemName, target, target, taskName);
    }

    @Nullable
    public static Text formatWaiterSelfUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        // 自用回放不需要 target，只要 actor、物品和任务名即可。
        return Text.translatable(
                "replay.global.noellesroles.waiter_self_use",
                actor,
                ReplayGenerator.formatItemName(event.data(), world),
                taskText(event)
        );
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
    public static Text formatMorphReagentSampled(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text sample = playerFromUuidOrName(event, match, "sample_player", "sample_name");
        if (actor == null || sample == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.morph_reagent_sampled", actor, sample);
    }

    @Nullable
    public static Text formatMorphReagentMarked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text sample = playerFromUuidOrName(event, match, "sample_player", "sample_name");
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || sample == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.morph_reagent_marked", actor, sample, target);
    }

    @Nullable
    public static Text formatMorphMarkTriggered(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text sample = playerFromUuidOrName(event, match, "sample_player", "sample_name");
        if (actor == null || sample == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.morph_mark_triggered", actor, sample);
    }

    @Nullable
    public static Text formatMorphMarkEnded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.morph_mark_ended", actor);
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
    public static Text formatSpiritualistProjectionStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.spiritualist_projection_started", actor);
    }

    @Nullable
    public static Text formatSpiritualistProjectionEnded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.spiritualist_projection_ended", actor);
    }

    @Nullable
    public static Text formatSpiritualistPossessionStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.spiritualist_possession_started", actor, target);
    }

    @Nullable
    public static Text formatSpiritualistPossessionEnded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.spiritualist_possession_ended", actor, target);
    }

    private static @Nullable Text playerFromUuidOrName(GameRecordEvent event, GameRecordManager.MatchRecord match, String uuidKey, String nameKey) {
        /*
         * 魔术师皮套这类事件会同时保存 UUID 和当时锁定的名字。
         *
         * Wathe 的 ReplayGenerator.formatPlayerName 在 UUID 不属于本局初始职业快照时，
         * 会只能退成短 UUID；而我们真正想要的是“当时背包里选中的玩家名”。
         * 所以这里优先尝试 UUID，若缓存里没有可读玩家资料，再回退到随事件保存的名字。
         */
        if (event.data().containsUuid(uuidKey)) {
            java.util.Map<java.util.UUID, ReplayGenerator.PlayerInfo> playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);
            java.util.UUID uuid = event.data().getUuid(uuidKey);
            if (playerInfoCache.containsKey(uuid)) {
                return ReplayGenerator.formatPlayerName(uuid, playerInfoCache);
            }
        }
        if (event.data().contains(nameKey)) {
            return Text.literal(event.data().getString(nameKey));
        }
        if (event.data().containsUuid(uuidKey)) {
            return ReplayGenerator.formatPlayerName(event.data().getUuid(uuidKey), ReplayGenerator.getPlayerInfoCache(match));
        }
        return null;
    }

    @Nullable
    public static Text formatOperatorConnectionFailedBothDead(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text playerOne = playerFromUuidOrName(event, match, "player_one", "player_one_name");
        Text playerTwo = playerFromUuidOrName(event, match, "player_two", "player_two_name");
        if (actor == null || playerOne == null || playerTwo == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.operator_connection_failed_both_dead", actor, playerOne, playerTwo);
    }

    @Nullable
    public static Text formatOperatorConnectionFailedOneDead(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text playerOne = playerFromUuidOrName(event, match, "player_one", "player_one_name");
        Text playerTwo = playerFromUuidOrName(event, match, "player_two", "player_two_name");
        Text deadPlayer = playerFromUuidOrName(event, match, "dead_player", "dead_player_name");
        if (actor == null || playerOne == null || playerTwo == null || deadPlayer == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.operator_connection_failed_one_dead", actor, playerOne, playerTwo, deadPlayer);
    }

    @Nullable
    public static Text formatOperatorConnectionStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text playerOne = playerFromUuidOrName(event, match, "player_one", "player_one_name");
        Text playerTwo = playerFromUuidOrName(event, match, "player_two", "player_two_name");
        if (actor == null || playerOne == null || playerTwo == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.operator_connection_started", actor, playerOne, playerTwo);
    }

    @Nullable
    public static Text formatOperatorConnectionEnded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text playerOne = playerFromUuidOrName(event, match, "player_one", "player_one_name");
        Text playerTwo = playerFromUuidOrName(event, match, "player_two", "player_two_name");
        if (actor == null || playerOne == null || playerTwo == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.operator_connection_ended", actor, playerOne, playerTwo);
    }

    @Nullable
    public static Text formatOperatorConnectionInterrupted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text playerOne = playerFromUuidOrName(event, match, "player_one", "player_one_name");
        Text playerTwo = playerFromUuidOrName(event, match, "player_two", "player_two_name");
        if (actor == null || playerOne == null || playerTwo == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.operator_connection_interrupted", actor, playerOne, playerTwo);
    }

    @Nullable
    public static Text formatOperatorBroadcastFailed(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromUuidOrName(event, match, "target_player", "target_player_name");
        Text deadPlayer = playerFromUuidOrName(event, match, "dead_player", "dead_player_name");
        if (actor == null || target == null || deadPlayer == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.operator_broadcast_failed", actor, target, deadPlayer);
    }

    @Nullable
    public static Text formatOperatorBroadcastStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromUuidOrName(event, match, "target_player", "target_player_name");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.operator_broadcast_started", actor, target);
    }

    @Nullable
    public static Text formatOperatorBroadcastEnded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromUuidOrName(event, match, "target_player", "target_player_name");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.operator_broadcast_ended", actor, target);
    }

    @Nullable
    public static Text formatOperatorBroadcastInterrupted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromUuidOrName(event, match, "target_player", "target_player_name");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.operator_broadcast_interrupted", actor, target);
    }

    @Nullable
    public static Text formatRemembererRecall(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = playerFromKey(event, match, "memory_target");
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.rememberer_recall", actor, target);
    }

    @Nullable
    public static Text formatRemembererSniperReloaded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.rememberer_sniper_reloaded",
                actor,
                event.data().getInt("current_ammo"),
                event.data().getInt("max_ammo")
        );
    }

    @Nullable
    public static Text formatMagicianRecordingStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.magician_recording_started", actor);
    }

    @Nullable
    public static Text formatMagicianRecordingFinished(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.magician_recording_finished", actor);
    }

    @Nullable
    public static Text formatMagicianRecordingStoppedEarly(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.magician_recording_stopped_early", actor);
    }

    @Nullable
    public static Text formatMagicianPlaybackStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text disguise = playerFromUuidOrName(event, match, "disguise_player", "disguise_name");
        if (actor == null || disguise == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.magician_playback_started", actor, disguise);
    }

    @Nullable
    public static Text formatMagicianPlaybackFinished(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.magician_playback_finished", actor);
    }

    @Nullable
    public static Text formatMagicianPlaybackStoppedEarly(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.magician_playback_stopped_early", actor);
    }

    @Nullable
    public static Text formatMagicianPlaybackForcedEnd(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text disguise = playerFromUuidOrName(event, match, "disguise_player", "disguise_name");
        Text attacker = playerFromUuidOrName(event, match, "attacker_player", "attacker_name");
        Text weapon = event.data().contains("weapon_name")
                ? weaponNameText(event.data().getString("weapon_name"))
                : Text.translatable("replay.item.unknown");
        if (actor == null || disguise == null || attacker == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.magician_playback_forced_end", actor, disguise, attacker, weapon);
    }

    @Nullable
    public static Text formatAvariciousStoleCoins(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable(
                "replay.global.noellesroles.avaricious_stole_coins",
                actor,
                event.data().getInt("amount")
        );
    }

    @Nullable
    public static Text formatNecromancerRevived(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text revived = playerFromKey(event, match, "revived_player");
        if (actor == null || revived == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.necromancer_revived", actor, revived);
    }

    private static Text weaponNameText(String weaponName) {
        /*
         * 强制结束播放的武器名现在会尽量保存翻译 key，而不是服务端已经翻译好的字符串。
         * 这样回放界面在客户端渲染时，才能按当前语言文件显示“刺刀 / Bayonet”。
         *
         * 旧回放里如果已经存成了普通字符串，则继续 literal 显示，避免破坏历史数据。
         */
        if (weaponName.startsWith("item.")
                || weaponName.startsWith("block.")
                || weaponName.startsWith("entity.")
                || weaponName.startsWith("replay.")) {
            return Text.translatable(weaponName);
        }
        return Text.literal(weaponName);
    }

    private static Text timekeeperWatchName(String stateId) {
        return "elegant".equals(stateId)
                ? Text.translatable("text.noellesroles.timekeeper.watch.elegant")
                : Text.translatable("text.noellesroles.timekeeper.watch.dying");
    }

    private static Text timekeeperWatchModeName(String modeId) {
        return TimekeeperWatchMode.byId(modeId).text();
    }

    @Nullable
    public static Text formatTimekeeperWatchUsed(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.timekeeper_watch_used",
                actor,
                timekeeperWatchName(event.data().getString("watch_state")),
                timekeeperWatchModeName(event.data().getString("mode")),
                event.data().getInt("cost")
        );
    }

    @Nullable
    public static Text formatTimekeeperWatchBroken(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null
                ? null
                : Text.translatable(
                "replay.global.noellesroles.timekeeper_watch_broken",
                actor,
                timekeeperWatchName(event.data().getString("watch_state"))
        );
    }

    @Nullable
    public static Text formatTimekeeperWatchRepaired(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null
                ? null
                : Text.translatable(
                "replay.global.noellesroles.timekeeper_watch_repaired",
                actor,
                timekeeperWatchName(event.data().getString("watch_state")),
                event.data().getInt("cost")
        );
    }

    @Nullable
    public static Text formatTimekeeperWatchUpgraded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.timekeeper_watch_upgraded",
                actor,
                timekeeperWatchName(event.data().getString("from_state")),
                timekeeperWatchName(event.data().getString("to_state")),
                event.data().getInt("cost")
        );
    }

    @Nullable
    public static Text formatSniperRifleUse(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.item_use.noellesroles.sniper_rifle", actor);
    }

    @Nullable
    public static Text formatSniperRifleHit(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        Text target = targetText(event, match);
        if (actor == null || target == null) {
            return null;
        }
        return Text.translatable(
                "replay.item_hit.noellesroles.sniper_rifle",
                actor,
                ReplayGenerator.resolveItemName(event.data(), world),
                target
        );
    }

    @Nullable
    public static Text formatSpiritualistActiveShieldBlocked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        Text spiritualist = playerFromKey(event, match, "owner_player");
        if (victim == null || spiritualist == null) {
            return null;
        }

        Text damageName = DefaultReplayFormatters.formatBlockedDamageName(event.data(), world);
        if (event.data().containsUuid("actor")) {
            Text attacker = actorText(event, match);
            if (attacker != null) {
                return Text.translatable(
                        "replay.global.noellesroles.spiritualist_active_shield_blocked",
                        victim,
                        spiritualist,
                        attacker,
                        damageName
                );
            }
        }
        return Text.translatable(
                "replay.global.noellesroles.spiritualist_active_shield_blocked",
                victim,
                spiritualist,
                Text.literal("未知来源"),
                damageName
        );
    }

    @Nullable
    public static Text formatSpiritualistLingeringShieldBlocked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        Text spiritualist = playerFromKey(event, match, "owner_player");
        if (victim == null || spiritualist == null) {
            return null;
        }

        Text damageName = DefaultReplayFormatters.formatBlockedDamageName(event.data(), world);
        if (event.data().containsUuid("actor")) {
            Text attacker = actorText(event, match);
            if (attacker != null) {
                return Text.translatable(
                        "replay.global.noellesroles.spiritualist_lingering_shield_blocked",
                        victim,
                        spiritualist,
                        attacker,
                        damageName
                );
            }
        }
        return Text.translatable(
                "replay.global.noellesroles.spiritualist_lingering_shield_blocked",
                victim,
                spiritualist,
                Text.literal("未知来源"),
                damageName
        );
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
    public static Text formatSniperRifleDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        Text shooter = actorText(event, match);
        if (victim == null || shooter == null) {
            return null;
        }
        return Text.translatable(
                "replay.death.noellesroles.sniper_rifle.killed",
                victim,
                shooter,
                ReplayGenerator.resolveItemName(event.data(), world)
        );
    }

    @Nullable
    public static Text formatThrowingAxeDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        Text thrower = actorText(event, match);
        if (victim == null || thrower == null) {
            return null;
        }
        return Text.translatable(
                "replay.death.noellesroles.throwing_axe.killed",
                victim,
                thrower,
                whiteBracketedItem(event.data(), world)
        );
    }

    @Nullable
    public static Text formatJasonThrowingWeaponDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        Text thrower = actorText(event, match);
        if (victim == null || thrower == null) {
            return null;
        }
        return Text.translatable(
                "replay.death.noellesroles.throwing_weapon.killed",
                victim,
                thrower,
                whiteBracketedItem(event.data(), world)
        );
    }

    @Nullable
    public static Text formatJasonBleedingDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        Text thrower = actorText(event, match);
        if (victim == null) {
            return null;
        }
        return Text.translatable(
                "replay.death.noellesroles.bleeding_too_much.killed",
                victim,
                thrower == null ? Text.translatable("replay.player.unknown") : thrower
        );
    }

    @Nullable
    public static Text formatJasonBurnDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        Text thrower = actorText(event, match);
        if (victim == null) {
            return null;
        }
        return Text.translatable(
                "replay.death.noellesroles.burn.killed",
                victim,
                thrower == null ? Text.translatable("replay.player.unknown") : thrower
        );
    }

    @Nullable
    public static Text formatAxeDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        Text killer = actorText(event, match);
        if (victim == null || killer == null) {
            return null;
        }
        return Text.translatable(
                "replay.death.noellesroles.axe.killed",
                victim,
                killer,
                ReplayGenerator.resolveItemName(event.data(), world)
        );
    }

    @Nullable
    public static Text formatSpringTrapRooted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        return victim == null ? null : Text.translatable("replay.global.noellesroles.spring_trap_rooted", victim);
    }

    @Nullable
    public static Text formatSpringTrapUnrooted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        return victim == null ? null : Text.translatable("replay.global.noellesroles.spring_trap_unrooted", victim);
    }

    @Nullable
    public static Text formatJasonWounded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        Text thrower = actorText(event, match);
        if (victim == null || thrower == null) {
            return null;
        }
        return Text.translatable(
                "replay.global.noellesroles.jason_wounded",
                victim,
                thrower,
                whiteBracketedItem(event.data(), world)
        );
    }

    @Nullable
    public static Text formatJasonRescued(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        Text rescuer = actorText(event, match);
        if (victim == null || rescuer == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.jason_rescued", victim, rescuer);
    }

    @Nullable
    public static Text formatJasonJerryCanIgnited(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.jason_jerry_can_ignited", actor);
    }

    @Nullable
    public static Text formatJasonJerryCanAutoIgnited(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        /*
         * 自动燃烧优先读 owner 字段，因为这个事件可能在投掷者离线后触发；
         * 旧回放或异常数据没有 owner 时，再退回 actor / unknown，保证事件本身还能显示。
         */
        Text owner = ownerFromGlobal(event, match);
        if (owner == null) {
            owner = actorText(event, match);
        }
        if (owner == null) {
            owner = Text.translatable("replay.player.unknown");
        }
        return Text.translatable("replay.global.noellesroles.jason_jerry_can_auto_ignited", owner);
    }

    @Nullable
    public static Text formatJasonGasolineDoused(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        if (victim == null) {
            return null;
        }

        Text owner = ownerFromGlobal(event, match);
        if (owner == null) {
            owner = actorText(event, match);
        }
        if (owner == null) {
            owner = Text.translatable("replay.player.unknown");
        }
        return Text.translatable("replay.global.noellesroles.jason_gasoline_doused", victim, owner);
    }

    @Nullable
    public static Text formatJasonAbilityStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.jason_ability_started", actor);
    }

    @Nullable
    public static Text formatJasonAbilityExitRequested(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.jason_ability_exit_requested", actor);
    }

    @Nullable
    public static Text formatJasonAbilityExitFinished(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.jason_ability_exit_finished", actor);
    }

    @Nullable
    public static Text formatJasonAbilityScared(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        Text actor = actorText(event, match);
        if (victim == null || actor == null) {
            return null;
        }
        return Text.translatable("replay.global.noellesroles.jason_ability_scared", victim, actor);
    }

    @Nullable
    public static Text formatJasonAbilityScareEnded(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = victimFromGlobal(event, match);
        return victim == null ? null : Text.translatable("replay.global.noellesroles.jason_ability_scare_ended", victim);
    }

    @Nullable
    public static Text formatSilencedOutsideDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        Text silencer = playerFromKey(event, match, "silencer");
        if (victim == null || silencer == null) {
            return null;
        }
        return Text.translatable("replay.death.noellesroles.silenced_and_outside.died", victim, silencer);
    }

    @Nullable
    public static Text formatTapeRemovedLowMoodDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        Text remover = playerFromKey(event, match, "remover");
        Text silencer = playerFromKey(event, match, "silencer");
        if (victim == null || remover == null || silencer == null) {
            return null;
        }
        return Text.translatable("replay.death.noellesroles.tape_removed_low_mood.died", victim, remover, silencer);
    }

    @Nullable
    public static Text formatConvenerCounterKillDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        Text convener = actorText(event, match);
        if (victim == null) {
            return null;
        }
        if (convener == null) {
            return Text.translatable("replay.death.unknown.died", victim);
        }
        return Text.translatable("replay.death.noellesroles.convener_counter_kill.killed", victim, convener);
    }

    @Nullable
    public static Text formatArsonistIgnitedDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        Text arsonist = actorText(event, match);
        if (victim == null) {
            return null;
        }
        if (arsonist == null) {
            return Text.translatable("replay.death.unknown.died", victim);
        }
        return Text.translatable("replay.death.noellesroles.ignited.killed", victim, arsonist);
    }

    @Nullable
    public static Text formatArsonistFailedIgniteDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        /*
         * failed_ignite 是纵火犯自己未满足点燃条件时的惩罚死亡。
         * LighterItem 为了保留死亡来源会把 actor 也传成自己，但回放文案应按“自亡”显示，
         * 避免出现“某人被自己点火失败杀死”这种读起来不自然的击杀记录。
         */
        return victim == null ? null : Text.translatable("replay.death.noellesroles.failed_ignite.died", victim);
    }

    @Nullable
    public static Text formatAllergiesDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        return victim == null ? null : Text.translatable("replay.death.noellesroles.allergies.died", victim);
    }

    @Nullable
    public static Text formatBrokenHeartDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        if (victim == null) {
            return null;
        }

        Text partner = playerFromKey(event, match, "broken_heart_partner");
        if (partner == null) {
            return Text.translatable("replay.death.noellesroles.broken_heart.died", victim);
        }
        return Text.translatable("replay.death.noellesroles.broken_heart.died_with_partner", victim, partner);
    }

    @Nullable
    public static Text formatDualActiveTimeoutDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        return victim == null ? null : Text.translatable("replay.death.noellesroles.dual_active_timeout.died", victim);
    }

    @Nullable
    public static Text formatFailedInitiationDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text victim = targetText(event, match);
        return victim == null ? null : Text.translatable("replay.death.noellesroles.failed_initiation.died", victim);
    }

    @Nullable
    public static Text formatDualActiveStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text actor = actorText(event, match);
        return actor == null ? null : Text.translatable("replay.global.noellesroles.dual_active_started", actor);
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

    @Nullable
    public static Text formatSpiritualistSoulGuardDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world) {
        Text spiritualist = targetText(event, match);
        Text protectedHost = playerFromKey(event, match, "target_player");
        if (spiritualist == null || protectedHost == null) {
            return null;
        }
        return Text.translatable("replay.death.noellesroles.spiritualist_soul_guard.died", spiritualist, protectedHost);
    }
}
