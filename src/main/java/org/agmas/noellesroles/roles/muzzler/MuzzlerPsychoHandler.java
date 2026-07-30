package org.agmas.noellesroles.roles.muzzler;

import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.api.psycho.PsychoModeProfile;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 静语者的静音疯魔 profile。
 *
 * <p>旧版用三个 mixin 分别拦 bat_hit、psycho_drone 启动和已播放循环。
 * 现在把“静语者启动默认疯魔时换成静音 profile”注册进 Wathe API，
 * 音效是否播放由 profile 自己声明，客户端背景音扫描自然只会听到非静音 profile。</p>
 */
public final class MuzzlerPsychoHandler {
    public static final Identifier PROFILE_ID = NoellesRolesCore.id("muzzler_silent_psycho");

    private MuzzlerPsychoHandler() {
    }

    public static void init() {
        PsychoModeProfile profile = PsychoModeProfile.copyOf(PsychoModeApi.createDefaultProfile(), PROFILE_ID)
                .nameTranslationKey("psycho_mode.noellesroles.muzzler")
                .shieldNameTranslationKey("psycho_shield.noellesroles.muzzler")
                .hitSound(null)
                .backgroundSound(WatheSounds.AMBIENT_PSYCHO_DRONE, false)
                .build();
        PsychoModeApi.registerProfile(profile);

        PsychoModeApi.registerStartProfileProvider(NoellesRolesCore.id("muzzler/psycho_start_profile"), PsychoModeApi.DEFAULT_PRIORITY + 100, (player, requestedProfile) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            return requestedProfile.id().equals(PsychoModeApi.DEFAULT_PROFILE_ID) && gameWorld.isRole(player, NoellesRoleRegistry.MUZZLER)
                    ? profile
                    : null;
        });
    }
}
