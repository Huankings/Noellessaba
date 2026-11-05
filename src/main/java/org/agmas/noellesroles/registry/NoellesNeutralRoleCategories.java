package org.agmas.noellesroles.registry;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.util.Identifier;
import org.agmas.harpymodloader.api.neutral.NeutralRoleCategories;
import org.agmas.harpymodloader.api.neutral.NeutralRoleCategoryApi;

import java.util.List;

/**
 * NoellesRoles 向 Harpy 公共中立分类 API 登记职业的唯一入口。
 *
 * <p>旧版实现暴露了三个可变 ArrayList，只有 NoellesRoles 自己能消费，
 * 其它扩展既无法复用，也容易在客户端/服务端维护出不同名单。现在分类所有权下沉到 Harpy，
 * NoellesRoles 只负责声明自己的职业归属；后续所有玩法查询统一读取公共 API。</p>
 */
public final class NoellesNeutralRoleCategories {
    private static boolean initialized;

    private NoellesNeutralRoleCategories() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        registerRoles("killer_neutral", NeutralRoleCategories.KILLER_NEUTRAL_ID, List.of(
                NoellesRoleRegistry.VULTURE,
                NoellesRoleRegistry.JESTER,
                NoellesRoleRegistry.EXECUTIONER,
                NoellesRoleRegistry.DREAMER,
                NoellesRoleRegistry.HACKER,
                NoellesRoleRegistry.KILLER_QUEEN
        ));
        registerRoles("ordinary_neutral", NeutralRoleCategories.ORDINARY_NEUTRAL_ID, List.of(
                NoellesRoleRegistry.AMNESIAC,
                NoellesRoleRegistry.INITIATE
        ));
        registerRoles("independent_neutral", NeutralRoleCategories.INDEPENDENT_NEUTRAL_ID, List.of(
                NoellesRoleRegistry.ARSONIST,
                NoellesRoleRegistry.CONVENER,
                NoellesRoleRegistry.THIEF,
                NoellesRoleRegistry.LICENSED_VILLAIN,
                NoellesRoleRegistry.SHADOW_JESTER,
                NoellesRoleRegistry.OUTLAW
        ));
    }

    private static void registerRoles(String path, Identifier categoryId, List<Role> roles) {
        for (Role role : roles) {
            /*
             * 每个职业使用独立注册 id，方便未来某个职业被更高优先级规则单独改组，
             * 不会因为共享同一个 id 而把同分类的其它职业注册一起替换掉。
             */
            NeutralRoleCategoryApi.registerRole(
                    NoellesRolesCore.id("neutral_category/" + path + "/" + role.identifier().getPath()),
                    NeutralRoleCategoryApi.DEFAULT_PRIORITY,
                    role,
                    categoryId
            );
        }
    }
}
