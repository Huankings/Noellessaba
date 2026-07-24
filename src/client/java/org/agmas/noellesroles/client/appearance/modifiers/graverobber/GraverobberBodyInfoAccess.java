package org.agmas.noellesroles.client.appearance.modifiers.graverobber;

import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.ClientPlayerEntity;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 盗墓者和验尸官共用的尸体信息权限入口。
 *
 * <p>盗墓者本质上是“复用验尸官尸体信息”的 modifier，因此这里不再让各个 HUD
 * handler 自己分别写一遍 Coroner / Vulture / Graverobber / spectator 的判断。
 * 以后如果还想继续扩展“谁能读尸体、谁会被理智值挡住、谁会看到乱码”，
 * 也只需要改这里一处。</p>
 */
public final class GraverobberBodyInfoAccess {
    private GraverobberBodyInfoAccess() {
    }

    public static boolean canSeeBodyReadout(@NotNull ClientPlayerEntity viewer) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
        return GameFunctions.isPlayerSpectatingOrCreative(viewer)
                || gameWorld.isRole(viewer, NoellesRoleRegistry.CORONER)
                || gameWorld.isRole(viewer, NoellesRoleRegistry.VULTURE)
                || isGraverobber(viewer);
    }

    public static boolean canSeeExaminePrompt(@NotNull ClientPlayerEntity viewer) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
        return gameWorld.isRole(viewer, NoellesRoleRegistry.CORONER) || isGraverobber(viewer);
    }

    public static boolean shouldBlockCoronerReadoutForSanity(@NotNull ClientPlayerEntity viewer) {
        /*
         * 理智不足只限制“验尸官 / 盗墓者”的尸体信息链路，
         * 不能顺手把秃鹫的吃尸提示也一起吞掉。
         */
        if (!GameFunctions.isPlayerAliveAndSurvival(viewer)) {
            return false;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
        if (!gameWorld.isRole(viewer, NoellesRoleRegistry.CORONER) && !isGraverobber(viewer)) {
            return false;
        }

        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(viewer);
        return mood.isLowerThanMid();
    }

    public static boolean shouldObfuscateVulturedBodyInfo(@NotNull ClientPlayerEntity viewer) {
        /*
         * 秃鹫啃过的尸体对“仍在局内存活”的玩家继续保持乱码。
         * 已经不在存活状态的玩家，仍然可以读回完整的死因、时间和身份。
         */
        return GameFunctions.isPlayerAliveAndSurvival(viewer);
    }

    public static boolean isGraverobber(@NotNull ClientPlayerEntity viewer) {
        return WorldModifierComponent.KEY.get(viewer.getWorld()).isModifier(viewer.getUuid(), NoellesModifierRegistry.GRAVEROBBER);
    }
}
