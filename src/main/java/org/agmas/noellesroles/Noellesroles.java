package org.agmas.noellesroles;

import net.fabricmc.api.ModInitializer;
import org.agmas.noellesroles.bootstrap.NoellesRolesBootstrap;

/**
 * Fabric 主入口。
 *
 * <p>这个类现在只负责启动总引导器，不再直接承载职业、事件、网络、经济、回放等注册细节。
 * 旧版 {@code Noellesroles.X} 兼容字段已经迁移/移除；后续维护请引用 registry/bootstrap 包里的专用类。</p>
 */
public class Noellesroles implements ModInitializer {
    @Override
    public void onInitialize() {
        NoellesRolesBootstrap.init();
    }
}
