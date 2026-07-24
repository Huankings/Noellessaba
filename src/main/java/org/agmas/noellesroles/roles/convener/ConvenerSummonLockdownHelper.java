package org.agmas.noellesroles.roles.convener;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.mixin.roles.convener.ItemCooldownEntryAccessor;
import org.agmas.noellesroles.mixin.roles.convener.ItemCooldownManagerAccessor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 召集成功后，对被召集活人施加短时间封控。
 */
public final class ConvenerSummonLockdownHelper {
    private static final String MOD_KINS_WATHE = "kinswathe";
    private static final String MOD_STARRY_EXPRESS = "starexpress";
    private static final String MOD_HARPY_SIMPLE_ROLES = "harpysimpleroles";
    private static final Set<String> REPORTED_REFLECTION_FAILURES = Collections.synchronizedSet(new HashSet<>());

    private ConvenerSummonLockdownHelper() {
    }

    public static void applySummonLockdown(ServerPlayerEntity player) {
        applyWeaponCooldowns(player);
        setItemCooldown(player, WatheItems.PSYCHO_MODE, ConvenerConstants.PSYCHO_MODE_COOLDOWN_TICKS);
        setItemCooldown(player, WatheItems.BLACKOUT, ConvenerConstants.BLACKOUT_COOLDOWN_TICKS);
        applyAbilityCooldowns(player);
        applySpecialEventCooldowns(player);
    }

    private static void applyWeaponCooldowns(ServerPlayerEntity player) {
        Set<Item> weaponItems = new LinkedHashSet<>();
        weaponItems.add(WatheItems.KNIFE);
        weaponItems.add(WatheItems.REVOLVER);
        weaponItems.add(WatheItems.DERRINGER);
        weaponItems.add(WatheItems.GRENADE);
        weaponItems.add(ModItems.JERRY_CAN);
        weaponItems.add(ModItems.LIGHTER);
        weaponItems.add(ModItems.THROWING_AXE);
        weaponItems.add(ModItems.ROBBER_PISTOL);
        weaponItems.add(ModItems.TIMED_BOMB);
        /*
         * kinssaba / StarryExpress 的同名实物道具已经迁入 NoellesRoles。
         * 召集者封控应直接压本仓库的 ModItems 常量，避免继续软查旧命名空间导致新道具漏掉冷却。
         */
        weaponItems.add(ModItems.BLOWGUN);
        weaponItems.add(ModItems.HUNTING_KNIFE);
        weaponItems.add(ModItems.KNOCKOUT_DRUG);
        weaponItems.add(ModItems.POISON_INJECTOR);
        weaponItems.add(ModItems.PAN);
        weaponItems.add(ModItems.TAPE);
        addRegisteredItem(weaponItems, MOD_HARPY_SIMPLE_ROLES, "toxin");
        addRegisteredItem(weaponItems, MOD_HARPY_SIMPLE_ROLES, "bandit_revolver");

        for (Item item : weaponItems) {
            setItemCooldown(player, item, ConvenerConstants.WEAPON_ITEM_COOLDOWN_TICKS);
        }
    }

    private static void applyAbilityCooldowns(ServerPlayerEntity player) {
        AbilityPlayerComponent noellesAbility = AbilityPlayerComponent.KEY.get(player);
        if (noellesAbility.cooldown < ConvenerConstants.ABILITY_COOLDOWN_TICKS) {
            noellesAbility.setCooldown(ConvenerConstants.ABILITY_COOLDOWN_TICKS);
        }

        if (FabricLoader.getInstance().isModLoaded(MOD_KINS_WATHE)) {
            applyReflectedComponentCooldown(
                    player,
                    "org.BsXinQin.kinswathe.component.AbilityPlayerComponent",
                    ConvenerConstants.ABILITY_COOLDOWN_TICKS
            );
        }
        if (FabricLoader.getInstance().isModLoaded(MOD_STARRY_EXPRESS)) {
            applyReflectedComponentCooldown(
                    player,
                    "org.aussiebox.starexpress.cca.AbilityComponent",
                    ConvenerConstants.ABILITY_COOLDOWN_TICKS
            );
        }
    }

    private static void applySpecialEventCooldowns(ServerPlayerEntity player) {
        Set<Item> specialEventItems = new LinkedHashSet<>();
        specialEventItems.add(ModItems.POWER_RESTORATION);
        /*
         * 黑客三种刷新图标也已迁到 NoellesRoles，召集封控要压当前真实商店图标。
         */
        specialEventItems.add(ModItems.ICON_WEAPON_COOLDOWN_REFRESH);
        specialEventItems.add(ModItems.ICON_ABILITY_COOLDOWN_REFRESH);
        specialEventItems.add(ModItems.ICON_POTION_EFFECT_REFRESH);

        for (Item item : specialEventItems) {
            setItemCooldown(player, item, ConvenerConstants.SPECIAL_EVENT_COOLDOWN_TICKS);
        }
    }

    private static void setItemCooldown(ServerPlayerEntity player, Item item, int cooldownTicks) {
        if (item == null || getRemainingItemCooldown(player, item) >= cooldownTicks) {
            return;
        }
        player.getItemCooldownManager().set(item, cooldownTicks);
    }

    private static int getRemainingItemCooldown(ServerPlayerEntity player, Item item) {
        ItemCooldownManager manager = player.getItemCooldownManager();
        ItemCooldownManagerAccessor managerAccessor = (ItemCooldownManagerAccessor) manager;
        Object entry = managerAccessor.noellesroles$getEntries().get(item);
        if (entry == null) {
            return 0;
        }

        // 只在现有冷却短于召集封控时才刷新，避免把其它职业/物品本身更长的惩罚冷却缩短。
        int endTick = ((ItemCooldownEntryAccessor) entry).noellesroles$getEndTick();
        return Math.max(0, endTick - managerAccessor.noellesroles$getTick());
    }

    private static void addRegisteredItem(Set<Item> items, String namespace, String path) {
        Identifier id = Identifier.of(namespace, path);
        if (Registries.ITEM.containsId(id)) {
            items.add(Registries.ITEM.get(id));
        }
    }

    /**
     * 给无编译依赖的扩展技能组件写入冷却。
     *
     * <p>这里按“KEY.get(player) -> cooldown 字段 -> sync()”桥接，
     * 失败只记一次日志，避免每次召集都刷异常。</p>
     */
    private static void applyReflectedComponentCooldown(ServerPlayerEntity player, String componentClassName, int cooldownTicks) {
        try {
            Class<?> componentClass = Class.forName(componentClassName);
            Object componentKey = readStaticField(componentClass, "KEY");
            Object component = findMethod(componentKey.getClass(), "get", 1).invoke(componentKey, player);
            if (component == null) {
                return;
            }

            int currentCooldown = readIntField(component, "cooldown");
            if (currentCooldown >= cooldownTicks) {
                return;
            }

            writeIntField(component, "cooldown", cooldownTicks);
            findMethod(componentClass, "sync", 0).invoke(component);
        } catch (ReflectiveOperationException exception) {
            reportReflectionFailure(componentClassName, exception);
        }
    }

    private static Object readStaticField(Class<?> owner, String fieldName) throws ReflectiveOperationException {
        return findField(owner, fieldName).get(null);
    }

    private static int readIntField(Object instance, String fieldName) throws ReflectiveOperationException {
        return findField(instance.getClass(), fieldName).getInt(instance);
    }

    private static void writeIntField(Object instance, String fieldName, int value) throws ReflectiveOperationException {
        findField(instance.getClass(), fieldName).setInt(instance, value);
    }

    private static Field findField(Class<?> owner, String fieldName) throws NoSuchFieldException {
        try {
            Field field = owner.getField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        }
    }

    private static Method findMethod(Class<?> owner, String methodName, int parameterCount) throws NoSuchMethodException {
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                method.setAccessible(true);
                return method;
            }
        }
        for (Method method : owner.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(owner.getName() + "#" + methodName + "/" + parameterCount);
    }

    private static void reportReflectionFailure(String componentClassName, ReflectiveOperationException exception) {
        if (!REPORTED_REFLECTION_FAILURES.add(componentClassName)) {
            return;
        }
        NoellesRolesCore.LOGGER.warn("召集者跨模组技能冷却桥接失败，已跳过组件 {}。", componentClassName, exception);
    }
}
