package org.agmas.noellesroles.roles.assassin;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 刺客击杀后把新尸体登记为隐藏尸体。
 */
public final class AssassinBodySpawnHandler {
    private static boolean initialized = false;

    private AssassinBodySpawnHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerBodySpawn(
                NoellesRolesCore.id("assassin_hidden_body"),
                DeathApi.DEFAULT_PRIORITY,
                context -> {
                    /*
                     * 刺客隐藏尸体只对“本次确实请求生成的尸体”生效。
                     * 如果其他机制选择不生成尸体，或死亡没有 killer，就不登记隐藏索引，
                     * 避免 HiddenBodiesWorldComponent 里留下指向不存在实体的 UUID。
                     */
                    if (!context.deathContext().requestedSpawnBody() || context.killer() == null) {
                        return;
                    }
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.killer().getWorld());
                    if (gameWorld.isRole(context.killer(), NoellesRoleRegistry.ASSASSIN)) {
                        // 登记的是尸体实体 UUID，不是玩家 UUID；后续渲染/清理按实体来找。
                        HiddenBodiesWorldComponent.KEY.get(context.killer().getWorld()).addHiddenBody(context.body().getUuid());
                    }
                }
        );
    }
}
