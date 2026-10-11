package org.agmas.noellesroles.client.bootstrap;

import dev.doctor4t.wathe.api.client.psycho.PsychoModeClientApi;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.roles.lich.LichConstants;

/** 注册职业疯魔背景音，使服务端同步的音效 id 能被客户端播放。 */
public final class NoellesClientPsychoBootstrap {
    private NoellesClientPsychoBootstrap() {
    }

    public static void register() {
        PsychoModeClientApi.registerBackgroundAmbience(NoellesRolesSounds.AMBIENT_SPRING_TRAP, 20);
        PsychoModeClientApi.registerBackgroundAmbience(NoellesRolesSounds.AMBIENT_JASON, 20);
        PsychoModeClientApi.registerBackgroundAmbience(
                NoellesRolesSounds.AMBIENT_LICH,
                LichConstants.PSYCHO_LICH_BACKGROUND_FADE_TICKS
        );
        PsychoModeClientApi.registerBackgroundAmbience(NoellesRolesSounds.AMBIENT_VECNA, 20);
    }
}
