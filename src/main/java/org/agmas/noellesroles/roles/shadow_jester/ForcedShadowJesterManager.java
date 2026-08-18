package org.agmas.noellesroles.roles.shadow_jester;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.api.assignment.RoleAssignmentPhaseContext;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 影子小丑的“下一局强制配对”管理器。
 *
 * <p>它只保存还没有开局前的 pending 队列，不会删除或改写当前局已经存在的影子小丑配对。
 * 这是为了精确满足指令语义：{@code remove shadow_jester} 只撤销下一局安排，不能把本局机制半路拆掉。</p>
 */
public final class ForcedShadowJesterManager {
    private static final Map<UUID, UUID> pendingPartners = new LinkedHashMap<>();
    private static final Map<UUID, String> pendingNames = new HashMap<>();
    private static final Map<UUID, Role> backedUpForcedRoles = new HashMap<>();
    private static final Set<UUID> shadowForcedRolePlayers = new HashSet<>();

    private ForcedShadowJesterManager() {
    }

    public static void setPendingPair(ServerPlayerEntity first, ServerPlayerEntity second) {
        UUID firstUuid = first.getUuid();
        UUID secondUuid = second.getUuid();

        /*
         * 一个玩家同一时间只能属于一对待指定影子小丑。
         * 这里只拆掉和 first / second 重叠的旧 pair：A+B 后再指定 C+D 时，
         * A+B 会继续留在 pending 队列里，方便管理员之后分别查询/移除不同测试组；
         * A+B 后再指定 A+C 时，A+B 会被拆掉并替换成 A+C，避免一人多配对。
         */
        removePendingPair(firstUuid);
        removePendingPair(secondUuid);

        pendingPartners.put(firstUuid, secondUuid);
        pendingPartners.put(secondUuid, firstUuid);
        pendingNames.put(firstUuid, first.getNameForScoreboard());
        pendingNames.put(secondUuid, second.getNameForScoreboard());

        /*
         * 影子小丑的指令配对必须尽早写入 Harpy 的强制职业表。
         *
         * Harpy 在原版杀手/义警抽取前会读取 FORCED_MODDED_ROLE_FLIP：
         * 非杀手强制职业会被排除出随机杀手候选池。之前这里只保存 NoellesRoles
         * 自己的 pending 队列，导致这两名玩家仍可能先被 Wathe 抽成杀手，
         * 随后又在平民替换阶段被影子小丑覆盖，最终 2 狼局就会少狼或无狼。
         *
         * 这里同步成 Harpy forced role 后，影子小丑会像 /forceRole 中立职业一样
         * 先占用平民候选，再由本职业的配对回调建立“一对”的世界组件状态。
         */
        applyShadowForcedRole(first);
        applyShadowForcedRole(second);
    }

    public static @Nullable RemovedPair removePendingPair(ServerPlayerEntity player) {
        UUID partner = removePendingPair(player.getUuid());
        return partner == null ? null : new RemovedPair(player.getUuid(), partner);
    }

    private static @Nullable UUID removePendingPair(UUID playerUuid) {
        UUID partnerUuid = pendingPartners.remove(playerUuid);
        pendingNames.remove(playerUuid);
        if (partnerUuid != null) {
            pendingPartners.remove(partnerUuid);
            pendingNames.remove(partnerUuid);
            restoreBackedUpForcedRole(playerUuid);
            restoreBackedUpForcedRole(partnerUuid);
        }
        return partnerUuid;
    }

    public static ApplyResult consumeAndApplyPendingPair(RoleAssignmentPhaseContext context) {
        if (pendingPartners.isEmpty()) {
            return ApplyResult.empty();
        }

        List<PendingPair> requestedPairs = getUniquePendingPairs();
        Set<UUID> consumedPendingPlayers = collectPendingPlayers();
        pendingPartners.clear();
        pendingNames.clear();
        for (UUID playerUuid : consumedPendingPlayers) {
            forgetShadowForcedRole(playerUuid);
        }

        PendingPair selectedPair = null;
        ServerPlayerEntity first = null;
        ServerPlayerEntity second = null;
        int skippedPairs = 0;
        for (PendingPair pair : requestedPairs) {
            ServerPlayerEntity pairFirst = findPlayer(context, pair.first());
            ServerPlayerEntity pairSecond = findPlayer(context, pair.second());
            if (pairFirst == null || pairSecond == null || pairFirst.getUuid().equals(pairSecond.getUuid())) {
                /*
                 * 指定的两名玩家只要有一方没参与本局，本次 pending 就直接作废。
                 * 不保留到下一局，避免管理员以为已经清理了，下一局却突然生效。
                 */
                skippedPairs++;
                continue;
            }
            if (selectedPair == null) {
                selectedPair = pair;
                first = pairFirst;
                second = pairSecond;
            } else {
                /*
                 * 目前影子小丑的世界组件、阶段、任务、缔结申请、音乐和胜利规则都是“一对”状态机。
                 * 命令层允许保留多条 pending 是为了调试时可覆盖/可移除；真正进局时只启用第一对有效配对，
                 * 其余有效 pending 在本局作废，避免多对影子小丑共享同一个组件而互相污染。
                 */
                skippedPairs++;
            }
        }
        if (selectedPair == null || first == null || second == null) {
            return new ApplyResult(0, skippedPairs);
        }

        GameWorldComponent gameWorld = context.gameWorldComponent();
        /*
         * 强制配对优先于随机结果。若 Harpy 已经随机出一名影子小丑但不在指定 pair 中，
         * 就把那名玩家还原成普通平民，避免本局出现第三个影子小丑。
         */
        for (ServerPlayerEntity player : context.players()) {
            if (gameWorld.isRole(player, NoellesRoleRegistry.SHADOW_JESTER)
                    && !player.getUuid().equals(selectedPair.first())
                    && !player.getUuid().equals(selectedPair.second())) {
                context.assignRole(player, WatheRoles.CIVILIAN);
            }
        }

        assignShadowJesterIfNeeded(context, first);
        assignShadowJesterIfNeeded(context, second);
        ShadowJesterComponent.KEY.get(context.serverWorld()).setPair(first.getUuid(), second.getUuid(), true);
        ShadowJesterTaskHandler.prepareInitialTasks(first);
        ShadowJesterTaskHandler.prepareInitialTasks(second);
        return new ApplyResult(1, skippedPairs);
    }

    private static Set<UUID> collectPendingPlayers() {
        Set<UUID> pendingPlayers = new HashSet<>(pendingPartners.keySet());
        pendingPlayers.addAll(pendingPartners.values());
        return pendingPlayers;
    }

    private static List<PendingPair> getUniquePendingPairs() {
        List<PendingPair> pairs = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        for (Map.Entry<UUID, UUID> entry : pendingPartners.entrySet()) {
            UUID first = entry.getKey();
            UUID second = entry.getValue();
            if (visited.contains(first) || visited.contains(second)) {
                continue;
            }
            pairs.add(new PendingPair(first, second));
            visited.add(first);
            visited.add(second);
        }
        return pairs;
    }

    private static void assignShadowJesterIfNeeded(RoleAssignmentPhaseContext context, ServerPlayerEntity player) {
        /*
         * setshadow_jester 现在会提前写入 Harpy forced role，正常情况下 Harpy
         * 中立替换循环已经把两名指定玩家都分成影子小丑。这里仍保留兜底 assign，
         * 但避免重复触发 ModdedRoleAssigned，防止开局清理、冷却和日志被执行两遍。
         */
        if (!context.gameWorldComponent().isRole(player, NoellesRoleRegistry.SHADOW_JESTER)) {
            context.assignRole(player, NoellesRoleRegistry.SHADOW_JESTER);
        }
    }

    private static void applyShadowForcedRole(ServerPlayerEntity player) {
        UUID playerUuid = player.getUuid();
        if (!shadowForcedRolePlayers.contains(playerUuid)) {
            Role existing = Harpymodloader.FORCED_MODDED_ROLE_FLIP.get(playerUuid);
            if (existing != null) {
                backedUpForcedRoles.put(playerUuid, existing);
            }
        }
        putForcedRole(playerUuid, NoellesRoleRegistry.SHADOW_JESTER);
        shadowForcedRolePlayers.add(playerUuid);
    }

    private static void restoreBackedUpForcedRole(UUID playerUuid) {
        if (!shadowForcedRolePlayers.remove(playerUuid)) {
            return;
        }

        /*
         * remove shadow_jester 的语义是删除“下一局 pending 配对”。
         * 如果管理员在 setshadow_jester 之前已经用 /forceRole 指定过这个玩家，
         * 删除 pending 时应恢复那条调试指令；如果 setshadow_jester 之后又执行了新的
         * /forceRole，当前 forced role 已经不是影子小丑，就尊重新指令，不再回写旧备份。
         */
        Role current = Harpymodloader.FORCED_MODDED_ROLE_FLIP.get(playerUuid);
        if (current == NoellesRoleRegistry.SHADOW_JESTER) {
            removeForcedRole(playerUuid, NoellesRoleRegistry.SHADOW_JESTER);
            if (backedUpForcedRoles.containsKey(playerUuid)) {
                putForcedRole(playerUuid, backedUpForcedRoles.get(playerUuid));
            }
        }
        backedUpForcedRoles.remove(playerUuid);
    }

    private static void forgetShadowForcedRole(UUID playerUuid) {
        if (!shadowForcedRolePlayers.remove(playerUuid)) {
            return;
        }
        /*
         * 开局分配阶段消费 pending 后，影子小丑指令已经完成使命。
         * 这里不恢复旧 /forceRole：Harpy 的强制职业本来就是“一次开局消费一次”，
         * 而本次开局以 setshadow_jester 为准，旧备份不应再影响后续替换阶段。
         */
        if (Harpymodloader.FORCED_MODDED_ROLE_FLIP.get(playerUuid) == NoellesRoleRegistry.SHADOW_JESTER) {
            removeForcedRole(playerUuid, NoellesRoleRegistry.SHADOW_JESTER);
        }
        backedUpForcedRoles.remove(playerUuid);
    }

    private static void putForcedRole(UUID playerUuid, Role role) {
        Role previousRole = Harpymodloader.FORCED_MODDED_ROLE_FLIP.put(playerUuid, role);
        if (previousRole != null && previousRole != role) {
            removeFromForcedRoleList(previousRole, playerUuid);
        }
        List<UUID> players = Harpymodloader.FORCED_MODDED_ROLE.computeIfAbsent(role, ignored -> new ArrayList<>());
        if (!players.contains(playerUuid)) {
            players.add(playerUuid);
        }
    }

    private static void removeForcedRole(UUID playerUuid, Role role) {
        if (Harpymodloader.FORCED_MODDED_ROLE_FLIP.get(playerUuid) == role) {
            Harpymodloader.FORCED_MODDED_ROLE_FLIP.remove(playerUuid);
        }
        removeFromForcedRoleList(role, playerUuid);
    }

    private static void removeFromForcedRoleList(Role role, UUID playerUuid) {
        List<UUID> players = Harpymodloader.FORCED_MODDED_ROLE.get(role);
        if (players == null) {
            return;
        }
        players.remove(playerUuid);
        if (players.isEmpty()) {
            Harpymodloader.FORCED_MODDED_ROLE.remove(role);
        }
    }

    private static @Nullable ServerPlayerEntity findPlayer(RoleAssignmentPhaseContext context, UUID uuid) {
        for (ServerPlayerEntity player : context.players()) {
            if (player.getUuid().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    public static String describePlayer(UUID uuid) {
        return pendingNames.getOrDefault(uuid, uuid.toString());
    }

    public record RemovedPair(UUID player, UUID partner) {
    }

    private record PendingPair(UUID first, UUID second) {
    }

    public record ApplyResult(int appliedPairs, int skippedPairs) {
        public static ApplyResult empty() {
            return new ApplyResult(0, 0);
        }

        public boolean changedAnything() {
            return this.appliedPairs > 0 || this.skippedPairs > 0;
        }
    }
}
