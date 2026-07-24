package org.agmas.noellesroles.registry;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * NoellesRoles 里跨多个系统共享的角色分组。
 *
 * <p>这些集合原本在入口初始化时手动填充。现在改成类加载时一次性构造，
 * 这样客户端 HUD、服务端商店和跨模组反射读取时都能拿到稳定内容。</p>
 */
public final class NoellesRoleGroups {
    public static final ArrayList<Role> VANNILA_ROLES = new ArrayList<>(List.of(
            WatheRoles.KILLER,
            WatheRoles.VIGILANTE,
            WatheRoles.CIVILIAN,
            WatheRoles.LOOSE_END
    ));
    public static final ArrayList<Identifier> VANNILA_ROLE_IDS = new ArrayList<>(List.of(
            WatheRoles.LOOSE_END.identifier(),
            WatheRoles.VIGILANTE.identifier(),
            WatheRoles.CIVILIAN.identifier(),
            WatheRoles.KILLER.identifier()
    ));
    public static final ArrayList<Role> KILLER_SIDED_NEUTRALS = new ArrayList<>(List.of(
            NoellesRoleRegistry.VULTURE,
            NoellesRoleRegistry.JESTER,
            NoellesRoleRegistry.EXECUTIONER,
            NoellesRoleRegistry.DREAMER,
            NoellesRoleRegistry.HACKER
    ));

    private NoellesRoleGroups() {
    }

    /**
     * 显式触发类加载。
     *
     * <p>入口引导器会调用它，让初始化顺序在代码上更直观；
     * 集合本身仍由静态字段保证只构造一次。</p>
     */
    public static void init() {
    }
}
