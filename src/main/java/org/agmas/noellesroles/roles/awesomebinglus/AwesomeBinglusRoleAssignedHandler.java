package org.agmas.noellesroles.roles.awesomebinglus;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;

/**
 * 记者职业分配处理器。
 */
public final class AwesomeBinglusRoleAssignedHandler {

    /**
     * 旧实现里开局会直接发 12 张纸条和一把撬棍。
     *
     * <p>这里改用循环只是减少重复代码，不改变实际发放数量与顺序。</p>
     */
    private static final int STARTING_NOTE_COUNT = 12;

    private AwesomeBinglusRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.AWESOME_BINGLUS)) {
            return;
        }

        for (int i = 0; i < STARTING_NOTE_COUNT; i++) {
            player.giveItemStack(WatheItems.NOTE.getDefaultStack());
        }
        player.giveItemStack(WatheItems.CROWBAR.getDefaultStack());
    }
}
