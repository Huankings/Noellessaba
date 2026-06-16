package org.agmas.noellesroles.roles.magician;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 魔术师播放代理击杀转接的临时上下文。
 *
 * <p>注意：这个类必须放在非 mixin 包里。
 * Mixin 系统会把 {@code org.agmas.noellesroles.mixin.*} 视为专用 mixin 包，
 * 其中的普通内部类/辅助类如果被运行时代码直接加载，会触发 IllegalClassLoadError。</p>
 */
public record MagicianPlaybackKillCreditScope(
        @Nullable MagicianReplayActorContext.Scope replayScope,
        @Nullable UUID ownerUuid,
        boolean victimWasAlive
) {
    public static MagicianPlaybackKillCreditScope empty() {
        return new MagicianPlaybackKillCreditScope(null, null, false);
    }
}
