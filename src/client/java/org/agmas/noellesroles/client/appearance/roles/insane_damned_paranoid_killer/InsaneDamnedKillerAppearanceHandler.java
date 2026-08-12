package org.agmas.noellesroles.client.appearance.roles.insane_damned_paranoid_killer;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearancePriorities;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.roles.insane_damned_paranoid_killer.InsaneDamnedKillerPlayerComponent;

/**
 * 亡语杀手尸体伪装的客户端名字兜底。
 *
 * <p>正常情况下 TargetVisibilityApi 已经会让 RoleNameRenderer 的射线跳过尸体伪装玩家；
 * 这里再注册一条名字规则，是为了兜住其它可能直接调用 RoleNameHudApi.resolveName 的 HUD。
 * 即使某条自定义射线没有正确走 target filter，也只会拿到空文本，不会显示玩家真名。</p>
 */
public final class InsaneDamnedKillerAppearanceHandler {
    private InsaneDamnedKillerAppearanceHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerName(
                NoellesAppearanceSupport.id("insane_damned_killer/corpse/name"),
                NoellesAppearancePriorities.CORPSE_MODE,
                (viewer, target, originalName) -> InsaneDamnedKillerPlayerComponent.isActiveCorpseMode(target)
                        ? Text.literal("")
                        : null
        );
    }
}
