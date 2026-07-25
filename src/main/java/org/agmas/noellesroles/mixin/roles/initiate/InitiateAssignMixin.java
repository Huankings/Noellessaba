package org.agmas.noellesroles.mixin.roles.initiate;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.harpymodloader.modded_murder.ModdedMurderGameMode;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 初学者成对出现的补位逻辑。
 *
 * <p>Wathe/Harpy 当前没有公开 API 能表达“某个中立职业随机到 1 个后，再从另一个中立玩家补 1 个同职业”。
 * 因此这里只对 Harpy 的平民/中立替换流程做 TAIL 注入：前面的随机分配完全交给 Harpy，
 * 这里仅在本局已经出现且只出现一个初学者时，把另一名中立候选者改成初学者。</p>
 */
@Mixin(ModdedMurderGameMode.class)
public class InitiateAssignMixin {
    @Inject(method = "assignCivilianReplacingRoles", at = @At("TAIL"))
    private void noellesroles$assignSecondInitiate(
            int desiredRoleCount,
            ServerWorld serverWorld,
            GameWorldComponent gameWorldComponent,
            List<ServerPlayerEntity> players,
            CallbackInfo ci
    ) {
        long initiateCount = players.stream()
                .filter(player -> gameWorldComponent.isRole(player, NoellesRoleRegistry.INITIATE))
                .count();
        if (initiateCount != 1) {
            return;
        }

        List<ServerPlayerEntity> candidates = new ArrayList<>(players);
        candidates.removeIf(player -> gameWorldComponent.isInnocent(player)
                || gameWorldComponent.canUseKillerFeatures(player)
                || gameWorldComponent.isRole(player, NoellesRoleRegistry.INITIATE));
        Collections.shuffle(candidates);
        if (candidates.isEmpty()) {
            NoellesRolesCore.LOGGER.warn("初学者已随机到 1 人，但没有可用的第二名中立候选者用于补齐配对。");
            return;
        }

        ServerPlayerEntity secondInitiate = candidates.getFirst();
        gameWorldComponent.addRole(secondInitiate, NoellesRoleRegistry.INITIATE);
        ModdedRoleAssigned.EVENT.invoker().assignModdedRole(secondInitiate, NoellesRoleRegistry.INITIATE);
        clearNonStarterItems(secondInitiate);
        NoellesRolesCore.LOGGER.info("{} -> {}", secondInitiate.getNameForScoreboard(), NoellesRoleRegistry.INITIATE.identifier());
    }

    private static void clearNonStarterItems(ServerPlayerEntity player) {
        /*
         * StupidExpress 旧逻辑会清掉被补位者背包中的旧职业物品，只保留钥匙和信件。
         * 这里继续只处理主背包，避免误动盔甲/副手这类 Wathe 或其它扩展可能临时使用的槽位。
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
