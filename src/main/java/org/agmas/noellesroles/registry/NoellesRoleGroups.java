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
    /*
     * Wathe/Harpy 的底层分配只区分 Faction.NEUTRAL 这一大类。
     * NoellesRoles 的玩法还需要继续拆出“普通中立”和“独立中立”：
     * 普通中立会转职或自行选择站边；独立中立拥有自己的独胜规则，不能和普通中立混用判断。
     */
    public static final ArrayList<Role> ORDINARY_NEUTRALS = new ArrayList<>(List.of(
            NoellesRoleRegistry.AMNESIAC,
            NoellesRoleRegistry.INITIATE
    ));
    public static final ArrayList<Role> INDEPENDENT_NEUTRALS = new ArrayList<>(List.of(
            NoellesRoleRegistry.ARSONIST,
            NoellesRoleRegistry.CONVENER,
            NoellesRoleRegistry.THIEF,
            NoellesRoleRegistry.LICENSED_VILLAIN,
            NoellesRoleRegistry.SHADOW_JESTER
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
