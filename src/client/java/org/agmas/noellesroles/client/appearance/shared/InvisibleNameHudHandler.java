package org.agmas.noellesroles.client.appearance.shared;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearancePriorities;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.jetbrains.annotations.Nullable;

/**
 * 隐身词条的准心名字规则。
 *
 * <p>这个规则和具体职业无关：只要目标实体处于 Minecraft 原生隐身状态，
 * 局内存活玩家的 Wathe 准心名字就返回空文本，避免玩家通过 HUD 读到隐身目标名字。</p>
 *
 * <p>Wathe 定义的非存活旁观 / 创造视角通常用于管理员、测试和复盘，
 * 这类视角不再参与局内信息博弈，因此不能被原版隐身挡掉准心玩家名。</p>
 */
public final class InvisibleNameHudHandler {
    private InvisibleNameHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerName(
                NoellesAppearanceSupport.id("shared/role_name/invisible_name"),
                NoellesAppearancePriorities.SHARED_NAME_RULES,
                (viewer, target, originalName) -> resolveName(viewer, target)
        );
    }

    private static @Nullable Text resolveName(ClientPlayerEntity viewer, PlayerEntity target) {
        if (!target.isInvisible()) {
            /*
             * 目标没有 Minecraft 原版隐身时，本规则完全不参与名字解析。
             * 返回 null 代表 PASS，继续交给变形、召集者、双重人格等其它名字规则或 Wathe 原名兜底。
             */
            return null;
        }

        if (GameFunctions.isPlayerSpectatingOrCreative(viewer)) {
            /*
             * Wathe 的 isPlayerSpectatingOrCreative 表示“旁观 / 创造且没有特殊存活授权”的视角。
             * 这类玩家通常是死亡后观察者、管理员或测试调试者，需要能通过准心继续辨认原版隐身目标。
             *
             * 这里同样返回 null，而不是直接返回 originalName，是为了只取消“原版隐身隐藏名字”这一层。
             * 如果目标同时处于变形、伪装或其它更低优先级名字规则中，最终显示名仍按现有 RoleNameHudApi 链决定。
             */
            return null;
        }

        /*
         * 局内存活玩家仍然受到原版隐身的信息保护。
         * 返回空文本是一个明确覆盖结果，会阻止 Wathe 回落到目标原本显示名。
         */
        return Text.literal("");
    }
}
