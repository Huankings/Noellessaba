package org.agmas.noellesroles.client.appearance.shared;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearancePriorities;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;

/**
 * 隐身词条的准心名字规则。
 *
 * <p>这个规则和具体职业无关：只要目标实体处于 Minecraft 原生隐身状态，
 * Wathe 准心名字就返回空文本，避免玩家通过 HUD 读到隐身目标名字。</p>
 */
public final class InvisibleNameHudHandler {
    private InvisibleNameHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerName(
                NoellesAppearanceSupport.id("shared/role_name/invisible_name"),
                NoellesAppearancePriorities.SHARED_NAME_RULES,
                (viewer, target, originalName) -> target.isInvisible() ? Text.literal("") : null
        );
    }
}
