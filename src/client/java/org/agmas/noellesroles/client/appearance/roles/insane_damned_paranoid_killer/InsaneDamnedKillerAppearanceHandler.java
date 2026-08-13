package org.agmas.noellesroles.client.appearance.roles.insane_damned_paranoid_killer;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearancePriorities;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import dev.doctor4t.wathe.game.GameFunctions;
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
                (viewer, target, originalName) -> {
                    if (!InsaneDamnedKillerPlayerComponent.isActiveCorpseMode(target)) {
                        return null;
                    }

                    /*
                     * 亡语杀手尸体伪装仍然要对正常存活玩家隐藏真名；
                     * 但旁观/创造视角属于原版 RoleNameRenderer 会显示名字的观察视角，
                     * 这里不能再把名字强行抹空，否则导播、复盘和管理员会失去原本能看的信息。
                     */
                    return GameFunctions.isPlayerSpectatingOrCreative(viewer) ? originalName : Text.literal("");
                }
        );
    }
}
