package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ThiefItemTracker {
    // 记录掉落的物品
    private static final Set<UUID> ACTIVE_ENTITY_ITEMS = new HashSet<>();
    private static boolean weaponAvailable = false;
    private static boolean initialized = false;
    private static int scanTicker = 0;

    private ThiefItemTracker() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // 注意世界中出现的物品
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity item && shouldTrack(item)) {
                trackEntityItem(item);
                refresh(world);
            }
        });

        // 注意世界中消失的物品
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity item) {
                untrackEntityItem(item);
                refresh(world);
            }
        });

        /*
         * 原 StupidExpress 通过击杀和商店 mixin 触发库存重扫。
         * 迁入 NoellesRoles 后改成低频服务端扫描：少两个跨系统 mixin，
         * 同时仍能覆盖购买、偷取、丢弃、死亡掉落等所有让武器可用性变化的路径。
         */
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            scanTicker++;
            if (scanTicker < ThiefConstants.TRACKER_INVENTORY_SCAN_INTERVAL_TICKS) {
                return;
            }
            scanTicker = 0;
            refreshAll(server.getWorlds());
        });

        // 当游戏模式开始初始化时检查库存
        GameEvents.ON_FINISH_INITIALIZE.register((world, gameWorldComponent) -> {
            if (world instanceof ServerWorld serverWorld) {
                refresh(serverWorld);
            }
        });

        // 在游戏开始/结束时重置
        GameEvents.ON_GAME_START.register(gameMode -> reset());
        GameEvents.ON_GAME_STOP.register(gameMode -> reset());
    }

    public static boolean isWeaponAvailable() {
        return weaponAvailable;
    }

    public static void refresh(ServerWorld world) {
        /*
         * 小偷胜利规则读的是全局 weaponAvailable。
         * 如果只用当前世界的扫描结果覆盖它，多维度服务端可能出现“主世界有可偷武器，
         * 但末尾刷新的其它维度没有武器，于是把状态误清空”的情况。
         * 因此这里从当前世界回到服务器维度集合做聚合判断；单元事件刷新和定时刷新共用同一套语义。
         */
        if (world.getServer() != null) {
            refreshAll(world.getServer().getWorlds());
            return;
        }
        weaponAvailable = hasTrackedEntityWeapon() || hasInventoryWeapon(world);
    }

    private static void refreshAll(Iterable<ServerWorld> worlds) {
        if (hasTrackedEntityWeapon()) {
            weaponAvailable = true;
            return;
        }
        for (ServerWorld world : worlds) {
            if (hasInventoryWeapon(world)) {
                weaponAvailable = true;
                return;
            }
        }
        weaponAvailable = false;
    }

    private static void reset() {
        ACTIVE_ENTITY_ITEMS.clear();
        weaponAvailable = false;
        scanTicker = 0;
    }

    private static void trackEntityItem(ItemEntity item) {
        if (item == null || !item.isAlive()) {
            return;
        }
        ACTIVE_ENTITY_ITEMS.add(item.getUuid());
    }

    private static void untrackEntityItem(ItemEntity item) {
        if (item == null) {
            return;
        }
        ACTIVE_ENTITY_ITEMS.remove(item.getUuid());
    }

    private static boolean hasTrackedEntityWeapon() {
        return !ACTIVE_ENTITY_ITEMS.isEmpty();
    }

    // 扫描所有活着玩家的背包物品
    private static boolean hasInventoryWeapon(ServerWorld world) {
        for (var player : world.getPlayers(GameFunctions::isPlayerAliveAndSurvival)) {
            for (ItemStack stack : player.getInventory().main) {
                if (!stack.isEmpty() && ThiefItemRules.isKeepGameGoing(stack.getItem())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean shouldTrack(ItemEntity itemEntity) {
        if (itemEntity == null) {
            return false;
        }
        return ThiefItemRules.isKeepGameGoing(itemEntity.getStack().getItem());
    }
}
