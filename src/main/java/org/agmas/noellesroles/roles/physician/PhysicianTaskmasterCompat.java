package org.agmas.noellesroles.roles.physician;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.harpymodloader.modifiers.Modifier;

import java.lang.reflect.Field;

/**
 * 对 kinssaba Taskmaster 词条的可选兼容。
 *
 * <p>NoellesRoles 不能直接依赖 kinssaba 源码，否则两个扩展会形成硬依赖。
 * 这里仅在 kinssaba 已加载时反射读取它的 TASKMASTER 字段；失败时退回普通奖励。</p>
 */
public final class PhysicianTaskmasterCompat {
    private static Modifier cachedTaskmaster;
    private static boolean lookedUp = false;

    private PhysicianTaskmasterCompat() {
    }

    public static boolean hasTaskmaster(PlayerEntity player) {
        Modifier taskmaster = resolveTaskmaster();
        return taskmaster != null && WorldModifierComponent.KEY.get(player.getWorld()).isModifier(player, taskmaster);
    }

    private static Modifier resolveTaskmaster() {
        if (lookedUp) {
            return cachedTaskmaster;
        }
        lookedUp = true;
        if (!FabricLoader.getInstance().isModLoaded("kinswathe")) {
            return null;
        }

        try {
            Class<?> roleClass = Class.forName("org.BsXinQin.kinswathe.KinsWatheRoles");
            Field field = roleClass.getField("TASKMASTER");
            Object value = field.get(null);
            if (value instanceof Modifier modifier) {
                cachedTaskmaster = modifier;
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {
            cachedTaskmaster = null;
        }
        return cachedTaskmaster;
    }
}
