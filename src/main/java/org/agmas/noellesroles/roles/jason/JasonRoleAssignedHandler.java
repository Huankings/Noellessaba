package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 杰森职业分配后的初始状态设置。
 */
public final class JasonRoleAssignedHandler {
    private JasonRoleAssignedHandler() {
    }

    public static void onRoleAssigned(@NotNull PlayerEntity player, @Nullable Role role) {
        JasonAbilityPlayerComponent component = JasonAbilityPlayerComponent.KEY.get(player);
        if (role == NoellesRoleRegistry.JASON) {
            /*
             * 无恶不在使用独立开局冷却，不复用 NoellesRoles 旧通用 AbilityPlayerComponent。
             * 用户指定开局冷却为 40 秒，并且杰森击杀不能清掉这段开局保护期。
             */
            component.startRoundCooldown();
        } else {
            /*
             * 职业重分配时必须清空旧杰森能力状态，避免上一局的幽魂阶段、冷却或惊吓来源
             * 通过 CCA 残留影响下一局或调试换职业后的玩家。
             */
            component.reset();
        }
    }
}
