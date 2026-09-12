package org.agmas.noellesroles.roles.licensed_villain;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.outlaw.OutlawWorldComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;

import java.util.List;
import java.util.UUID;

/** 执照恶棍时刻的触发、暂停、恢复、物品和回放总管理器。 */
public final class LicensedVillainMomentManager {
    private LicensedVillainMomentManager() {
    }

    public static void tickWorld(ServerWorld world) {
        /*
         * 回溯播放期间，职业、存活状态、背包和世界组件都在逐帧恢复。
         * 此时执行正常时间线的进入/暂停副作用会污染历史帧，所以只在回溯结束后统一收束。
         */
        if (TimekeeperWorldComponent.KEY.get(world).isRewinding()) {
            return;
        }
        reconcileNow(world);
    }

    public static void reconcileNow(ServerWorld world) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        LicensedVillainMomentWorldComponent component = LicensedVillainMomentWorldComponent.KEY.get(world);

        /* 调试改职业或异常复活也必须清掉旧时刻，避免音乐和 2 秒冷却脱离职业继续存在。 */
        for (UUID uuid : component.players()) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null && (!gameWorld.isRole(player, NoellesRoleRegistry.LICENSED_VILLAIN)
                    || !isActuallyAlive(player))) {
                finishMoment(player, false);
            }
        }

        boolean outlawActive = OutlawWorldComponent.KEY.get(world).hasActiveOutlaw();
        boolean livingShadowJester = hasLivingShadowJester(world, gameWorld);
        boolean factionEliminated = isGoodOrKillerFactionEliminated(world, gameWorld);

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!gameWorld.isRole(player, NoellesRoleRegistry.LICENSED_VILLAIN) || !isActuallyAlive(player)) {
                continue;
            }

            UUID uuid = player.getUuid();
            if (component.hasStarted(uuid)) {
                if (outlawActive || livingShadowJester) {
                    suspendMoment(player);
                } else {
                    resumeMoment(player);
                }
                continue;
            }

            /*
             * 阵营语义严格使用 Wathe 的显式 Faction：
             * 好人只包含 CIVILIAN 与 VIGILANTE，杀手只包含 KILLER；
             * 杀手侧中立不会被混进杀手阵营存活判断。
             */
            if (factionEliminated && !livingShadowJester && !outlawActive) {
                startMoment(player);
            }
        }
    }

    public static void pauseAllForOutlaw(ServerWorld world) {
        LicensedVillainMomentWorldComponent component = LicensedVillainMomentWorldComponent.KEY.get(world);
        for (UUID uuid : component.players()) {
            if (component.isActive(uuid)) {
                /* 玩家即使恰好离线也先切到暂停态，避免其 UUID 继续维持全场音乐。 */
                component.setActive(uuid, false);
            }
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null) {
                restoreNormalRevolverCooldown(player);
            }
        }
        removeMomentGrantedItems(world);
    }

    public static void finishMoment(ServerPlayerEntity player, boolean recordReplay) {
        LicensedVillainMomentWorldComponent component = LicensedVillainMomentWorldComponent.KEY.get(player.getServerWorld());
        if (!component.remove(player.getUuid())) {
            return;
        }

        removeMomentGrantedItems(player.getServerWorld());
        restoreNormalRevolverCooldown(player);
        /* 调试环境若同时存在多名执照恶棍，回收后立即补回其它仍活跃玩家的临时撬棍。 */
        for (UUID uuid : component.players()) {
            ServerPlayerEntity remaining = player.getServer().getPlayerManager().getPlayer(uuid);
            if (remaining != null && component.isActive(uuid)) {
                grantMomentCrowbar(remaining);
            }
        }
        if (recordReplay) {
            GameRecordManager.recordGlobalEvent(
                    player.getServerWorld(),
                    NoellesEventIds.LICENSED_VILLAIN_MOMENT_ENDED_EVENT,
                    player,
                    null
            );
        }
    }

    private static void startMoment(ServerPlayerEntity player) {
        LicensedVillainMomentWorldComponent component = LicensedVillainMomentWorldComponent.KEY.get(player.getServerWorld());
        component.setActive(player.getUuid(), true);
        grantMomentCrowbar(player);
        player.getItemCooldownManager().remove(WatheItems.REVOLVER);
        GameRecordManager.recordGlobalEvent(
                player.getServerWorld(),
                NoellesEventIds.LICENSED_VILLAIN_MOMENT_STARTED_EVENT,
                player,
                null
        );
    }

    private static void suspendMoment(ServerPlayerEntity player) {
        LicensedVillainMomentWorldComponent component = LicensedVillainMomentWorldComponent.KEY.get(player.getServerWorld());
        if (component.isActive(player.getUuid())) {
            component.setActive(player.getUuid(), false);
            removeMomentGrantedItems(player.getServerWorld());
            restoreNormalRevolverCooldown(player);
        } else {
            /* 暂停期间重新上线的玩家也要立即移除其持久化背包里的临时时刻物品。 */
            removeMomentGrantedItemsFromInventories(player.getServerWorld());
        }
    }

    private static void resumeMoment(ServerPlayerEntity player) {
        LicensedVillainMomentWorldComponent component = LicensedVillainMomentWorldComponent.KEY.get(player.getServerWorld());
        if (!component.hasStarted(player.getUuid()) || component.isActive(player.getUuid())) {
            return;
        }
        component.setActive(player.getUuid(), true);
        grantMomentCrowbar(player);
        player.getItemCooldownManager().remove(WatheItems.REVOLVER);
        /* 暂停后的恢复属于同一次执照恶棍时刻，按需求不重复写入“进入时刻”回放。 */
    }

    private static boolean isGoodOrKillerFactionEliminated(ServerWorld world, GameWorldComponent gameWorld) {
        List<ServerPlayerEntity> alive = world.getPlayers(LicensedVillainMomentManager::isActuallyAlive);
        boolean goodAlive = alive.stream().anyMatch(player -> {
            Role role = gameWorld.getRole(player);
            return role != null && (role.getFaction() == Faction.CIVILIAN || role.getFaction() == Faction.VIGILANTE);
        });
        boolean killerAlive = alive.stream().anyMatch(player -> {
            Role role = gameWorld.getRole(player);
            return role != null && role.getFaction() == Faction.KILLER;
        });
        return !goodAlive || !killerAlive;
    }

    private static boolean hasLivingShadowJester(ServerWorld world, GameWorldComponent gameWorld) {
        return world.getPlayers().stream().anyMatch(player ->
                gameWorld.isRole(player, NoellesRoleRegistry.SHADOW_JESTER) && isActuallyAlive(player));
    }

    private static boolean isActuallyAlive(ServerPlayerEntity player) {
        /* 时间狭缝是死亡后的临时特殊旁观；这里按“已经死亡”语义排除，不能继续阻挡时刻触发。 */
        return GameFunctions.isPlayerAliveAndSurvival(player)
                && !TimekeeperPlayerComponent.KEY.get(player).isInTimeRift();
    }

    private static void grantMomentCrowbar(ServerPlayerEntity player) {
        if (player.getInventory().contains(stack -> stack.isOf(WatheItems.CROWBAR))) {
            return;
        }
        ItemStack crowbar = WatheItems.CROWBAR.getDefaultStack();
        crowbar.set(ModItems.LICENSED_VILLAIN_MOMENT_GRANTED, true);
        player.getInventory().offerOrDrop(crowbar);
    }

    private static void removeMomentGrantedItems(ServerWorld world) {
        /*
         * 标记跟随 ItemStack，即使物品被丢弃或转移，也能精确找到时刻授予的副本。
         * 这里只删除带来源标记的物品，不会误删普通 Wathe 撬棍。
         */
        removeMomentGrantedItemsFromInventories(world);
        for (ItemEntity itemEntity : world.getEntitiesByType(TypeFilter.equals(ItemEntity.class), ignored -> true)) {
            if (itemEntity.getStack().getOrDefault(ModItems.LICENSED_VILLAIN_MOMENT_GRANTED, false)) {
                itemEntity.discard();
            }
        }
    }

    private static void removeMomentGrantedItemsFromInventories(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.getInventory().remove(
                    stack -> stack.getOrDefault(ModItems.LICENSED_VILLAIN_MOMENT_GRANTED, false),
                    Integer.MAX_VALUE,
                    player.getInventory()
            );
            player.getInventory().markDirty();
            player.playerScreenHandler.sendContentUpdates();
        }
    }

    private static void restoreNormalRevolverCooldown(ServerPlayerEntity player) {
        if (player.getItemCooldownManager().isCoolingDown(WatheItems.REVOLVER)) {
            player.getItemCooldownManager().set(WatheItems.REVOLVER, GameConstants.getRevolverCooldown(player));
        }
    }
}
