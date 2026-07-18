package org.agmas.noellesroles.client.appearance.roles.insane_observer;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.client.appearance.NoellesAppearancePriorities;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;

/**
 * “疯狂观察”词条的本地错乱外观规则。
 *
 * <p>这个机制根据世界配置、玩家心情和已生成的随机缓存，把玩家皮肤洗牌，并把准心名字改成乱码。
 * 它属于观察者视觉效果，不改变任何玩家真实身份。</p>
 */
public final class InsaneObserverAppearanceHandler {
    private InsaneObserverAppearanceHandler() {
    }

    public static void register() {
        PlayerAppearanceApi.registerPlayerSkin(
                NoellesAppearanceSupport.id("insane_observer/appearance/player"),
                NoellesAppearancePriorities.INSANE_OBSERVER,
                player -> isInsaneObserverMorphEnabled(player)
                        ? DisguiseRenderHelper.resolveShuffledSkinTextures(player)
                        : null
        );

        RoleNameHudApi.registerName(
                NoellesAppearanceSupport.id("insane_observer/role_name/name"),
                NoellesAppearancePriorities.INSANE_OBSERVER,
                (viewer, target, originalName) ->
                        target instanceof AbstractClientPlayerEntity clientTarget && isInsaneObserverMorphEnabled(clientTarget)
                                ? Text.literal("??!?!").formatted(Formatting.OBFUSCATED)
                                : null
        );
    }

    private static boolean isInsaneObserverMorphEnabled(AbstractClientPlayerEntity player) {
        if (WatheClient.moodComponent == null || NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE == null) {
            return false;
        }

        /*
         * 同时满足三个条件才显示错乱：
         * 1. 本局配置允许低心情观察者看见随机皮肤；
         * 2. Wathe 心情已经低于 depressed 阈值；
         * 3. NoellesRoles 已经为该玩家准备好洗牌后的皮肤缓存。
         */
        ConfigWorldComponent config = ConfigWorldComponent.KEY.get(player.getWorld());
        return config != null
                && config.insaneSeesMorphs
                && WatheClient.moodComponent.isLowerThanDepressed()
                && NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.containsKey(player.getUuid());
    }
}
