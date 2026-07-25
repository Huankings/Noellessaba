package org.agmas.noellesroles.client.roles.robot;

import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.robot.RobotConstants;

/**
 * 机器人夜视能力 HUD。
 */
public final class RobotStatusHud {
    private RobotStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/robot/status", NoellesRoleRegistry.ROBOT, context -> {
            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            Text line = ability.cooldown > 0
                    ? Text.translatable("tip.noellesroles.cooldown", ability.cooldown / 20)
                    /*
                     * 机器人能力固定是夜视，不再复用通用“使用能力”文案。
                     * 单独拆 key 后，后续如果机器人能力 HUD 需要继续细分，也不会影响其它职业的通用提示。
                     */
                    : Text.translatable("tip.noellesroles.robot.night_vision", NoellesrolesClient.abilityBind.getBoundKeyLocalizedText());

            NoellesHudSupport.drawBottomRightLine(context, line, RobotConstants.ROLE_COLOR);
        });
    }
}
