package org.agmas.noellesroles.client.visibility;

import dev.doctor4t.wathe.api.client.invisibility.HeldItemInvisibilityApi;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.controller.ControlledPlayerComponent;

import java.util.List;

/**
 * NoellesRoles 接入 Wathe 手持物不可见 API 的统一注册处。
 *
 * <p>旧版做法是在每个职业里各写一组 HeldItemFeatureRenderer / getArmPose mixin。
 * 现在这些规则全部交给 Wathe 本体处理，NoellesRoles 只负责声明“谁拿什么应该隐藏”。</p>
 */
public final class NoellesHeldItemVisibilityHandlers {
    private NoellesHeldItemVisibilityHandlers() {
    }

    public static void register() {
        /*
         * 主动隐藏规则：玩家必须是对应职业，并且手里正拿着对应物品，其他局内存活玩家才看不见。
         * Wathe 会自动处理主手/副手、手臂姿势、本地 F5 自视角，以及低心情幻觉覆盖。
         */
        HeldItemInvisibilityApi.registerHiddenItem(Noellesroles.BARTENDER, ModItems.DEFENSE_VIAL);
        HeldItemInvisibilityApi.registerHiddenItem(Noellesroles.ENGINEER, ModItems.CAPTURE_DEVICE);
        HeldItemInvisibilityApi.registerHiddenItems(Noellesroles.TRAPPER, List.of(
                Items.PAPER,
                ModItems.ROLE_MINE
        ));

        /*
         * 被动隐藏规则：Controller 附体别人时，被 controlled 的玩家无论手里拿什么都隐藏。
         * 这里不检查物品类型，直接读取 controlled 玩家自己的 CCA 状态。
         */
        HeldItemInvisibilityApi.registerRule(
                Identifier.of(Noellesroles.MOD_ID, "controlled_player_held_item_invisibility"),
                HeldItemInvisibilityApi.DEFAULT_PRIORITY,
                context -> ControlledPlayerComponent.KEY.get(context.holder()).isControlled
        );
    }
}
