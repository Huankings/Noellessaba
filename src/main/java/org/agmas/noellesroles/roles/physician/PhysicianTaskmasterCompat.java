package org.agmas.noellesroles.roles.physician;

import net.minecraft.entity.player.PlayerEntity;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.Noellesroles;

/**
 * 医师与任务大师词条的本地联动。
 *
 * <p>任务大师已经从 kinssaba 搬运到 NoellesRoles，
 * 因此这里不再通过反射读取外部模组字段，直接检查本模组注册的词条即可。</p>
 */
public final class PhysicianTaskmasterCompat {
    private PhysicianTaskmasterCompat() {
    }

    public static boolean hasTaskmaster(PlayerEntity player) {
        /*
         * 医疗包的奖励结算发生在服务端物品使用逻辑里。
         * 这里通过 Harpy 的世界词条组件读取玩家当前词条，保证结果与实际分配状态一致。
         */
        return WorldModifierComponent.KEY.get(player.getWorld()).isModifier(player, Noellesroles.TASKMASTER);
    }
}
