package org.agmas.noellesroles.client.input;

import org.agmas.noellesroles.client.modifiers.dual_personality.DualPersonalityInputRules;
import org.agmas.noellesroles.client.modifiers.violator.ViolatorInputRules;
import org.agmas.noellesroles.client.roles.controller.ControllerInputRules;
import org.agmas.noellesroles.client.roles.engineer.StunnedInputRules;
import org.agmas.noellesroles.client.roles.jason.JasonInputRules;
import org.agmas.noellesroles.client.roles.kidnapper.KidnapperInputRules;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistInputRules;
import org.agmas.noellesroles.client.roles.timekeeper.TimekeeperInputRules;

/**
 * NoellesRoles 对 Wathe 客户端输入 API 的聚合注册入口。
 *
 * <p>本类只维护初始化顺序；具体职业状态、禁用键位和清理动作必须留在各自职业/词条类中，
 * 避免重新形成一个同时认识全部组件的大型输入类。</p>
 */
public final class NoellesClientInputBootstrap {
    private static boolean registered;

    private NoellesClientInputBootstrap() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ControllerInputRules.register();
        StunnedInputRules.register();
        SpiritualistInputRules.register();
        TimekeeperInputRules.register();
        KidnapperInputRules.register();
        JasonInputRules.register();
        ViolatorInputRules.register();
        DualPersonalityInputRules.register();
        NoellesWeaponActionRules.register();
    }
}
