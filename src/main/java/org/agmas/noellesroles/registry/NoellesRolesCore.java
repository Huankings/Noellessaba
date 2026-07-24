package org.agmas.noellesroles.registry;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NoellesRoles 最基础的模组标识入口。
 *
 * <p>后续新增注册类时统一从这里取 {@link #MOD_ID} 和 {@link #id(String)}，
 * 避免再把入口类 {@code Noellesroles} 当成公共常量仓库使用。</p>
 */
public final class NoellesRolesCore {
    public static final String MOD_ID = "noellesroles";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private NoellesRolesCore() {
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
