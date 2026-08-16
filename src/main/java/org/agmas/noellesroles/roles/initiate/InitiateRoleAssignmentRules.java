package org.agmas.noellesroles.roles.initiate;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.harpymodloader.api.assignment.RoleAssignmentApi;
import org.agmas.harpymodloader.api.assignment.RoleAssignmentPhase;
import org.agmas.harpymodloader.api.assignment.RoleAssignmentPhaseContext;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 初学者成对生成规则。
 *
 * <p>Harpy 仍然只随机抽一个初学者名额；当平民/中立替换阶段结束时，
 * 这里通过公开阶段回调检查本局是否恰好出现 1 名初学者，并从另一名中立玩家中补齐第二人。
 * 这样可以保留旧玩法节奏，同时不再 mixin Harpy 的分配方法。</p>
 */
public final class InitiateRoleAssignmentRules {
    private static boolean initialized = false;

    private InitiateRoleAssignmentRules() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        RoleAssignmentApi.registerAfterPhaseHandler(
                NoellesRolesCore.id("initiate_pair_assignment"),
                RoleAssignmentPhase.CIVILIAN_REPLACEMENT,
                0,
                InitiateRoleAssignmentRules::assignSecondInitiate
        );
    }

    private static void assignSecondInitiate(RoleAssignmentPhaseContext context) {
        GameWorldComponent gameWorldComponent = context.gameWorldComponent();
        long initiateCount = context.players().stream()
                .filter(player -> gameWorldComponent.isRole(player, NoellesRoleRegistry.INITIATE))
                .count();
        if (initiateCount != 1) {
            return;
        }

        List<ServerPlayerEntity> candidates = new ArrayList<>(context.players());
        candidates.removeIf(player -> gameWorldComponent.isInnocent(player)
                || gameWorldComponent.canUseKillerFeatures(player)
                || gameWorldComponent.isRole(player, NoellesRoleRegistry.INITIATE));
        Collections.shuffle(candidates);
        if (candidates.isEmpty()) {
            NoellesRolesCore.LOGGER.warn("初学者已随机到 1 人，但没有可用的第二名中立候选者用于补齐配对。");
            return;
        }

        ServerPlayerEntity secondInitiate = candidates.getFirst();
        context.assignRole(secondInitiate, NoellesRoleRegistry.INITIATE);
        clearNonStarterItems(secondInitiate);
    }

    private static void clearNonStarterItems(ServerPlayerEntity player) {
        /*
         * 被补位玩家原本可能已经拿到其它中立职业的开局物品。
         * 改成初学者后只保留 Wathe 基础钥匙和信件，避免旧职业道具残留影响本局平衡。
         */
        for (int i = 0; i < player.getInventory().main.size(); i++) {
            ItemStack stack = player.getInventory().main.get(i);
            if (stack.isEmpty() || stack.isOf(WatheItems.KEY) || stack.isOf(WatheItems.LETTER)) {
                continue;
            }
            player.getInventory().main.set(i, ItemStack.EMPTY);
        }
        player.getInventory().markDirty();
    }
}
