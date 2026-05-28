package org.agmas.noellesroles.shop;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * NoellesRoles 的统一角色商店注册中心。
 *
 * <p>这里同时支持：</p>
 * <p>1. 固定商店：例如酒保、回溯者这类总是卖固定物品的职业</p>
 * <p>2. 动态商店：例如潜行者这种会根据阶段变化商品的职业</p>
 */
public final class NoellesRolesShopRegistry {

    private static final Map<Role, RoleShopProvider> ROLE_SHOPS = new HashMap<>();

    private NoellesRolesShopRegistry() {
    }

    /**
     * 注册一个角色的商店提供器。
     */
    public static void register(@NotNull Role role, @NotNull RoleShopProvider provider) {
        ROLE_SHOPS.put(role, provider);
    }

    /**
     * 批量注册多个角色共用同一套静态商店。
     */
    public static void registerStatic(@NotNull Supplier<List<ShopEntry>> supplier, @NotNull Role... roles) {
        RoleShopProvider provider = player -> supplier.get();
        for (Role role : roles) {
            register(role, provider);
        }
    }

    /**
     * 注册单个角色的静态商店。
     */
    public static void registerStatic(@NotNull Role role, @NotNull Supplier<List<ShopEntry>> supplier) {
        register(role, player -> supplier.get());
    }

    public static boolean hasCustomShop(@NotNull PlayerEntity player) {
        return getProviderForPlayer(player) != null;
    }

    public static @NotNull List<ShopEntry> getEntriesForPlayer(@NotNull PlayerEntity player) {
        RoleShopProvider provider = getProviderForPlayer(player);
        if (provider == null) {
            throw new IllegalStateException("No custom shop provider registered for current player role");
        }
        return provider.getShopEntries(player);
    }

    public static @Nullable RoleShopProvider getProviderForPlayer(@NotNull PlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        Role role = gameWorld.getRole(player);
        if (role == null) {
            return null;
        }
        return ROLE_SHOPS.get(role);
    }
}
