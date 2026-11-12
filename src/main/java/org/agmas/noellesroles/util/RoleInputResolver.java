package org.agmas.noellesroles.util;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/** 服务端职业输入解析器：优先接收完整职业 ID，并兼容旧客户端只发送 path 的格式。 */
public final class RoleInputResolver {
    private RoleInputResolver() {
    }

    public static @Nullable Role resolve(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String normalized = input.trim();
        Identifier fullId = Identifier.tryParse(normalized);
        if (fullId != null) {
            for (Role role : WatheRoles.ROLES) {
                if (role.identifier().equals(fullId)) {
                    return role;
                }
            }
        }

        /*
         * 旧版本客户端只发送 magician、civilian 这一类 path。保留该分支可以让滚动升级期间
         * 新服务端继续接受旧包；新客户端始终发送 namespace:path，避免不同扩展同 path 冲突。
         */
        for (Role role : WatheRoles.ROLES) {
            if (role.identifier().getPath().equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        return null;
    }
}
