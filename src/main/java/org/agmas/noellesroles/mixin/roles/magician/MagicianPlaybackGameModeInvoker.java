package org.agmas.noellesroles.mixin.roles.magician;

import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 允许魔术师播放代理静默恢复录制帧的游戏模式。
 *
 * <p>不能调用公开的 {@code changeGameMode}：播放 FakePlayer 与真实魔术师共用 UUID，
 * 公开入口会向所有客户端广播玩家列表模式更新，导致真实魔术师的 Tab 状态被播放代理污染。
 * 原版受保护的 {@code setGameMode} 只更新交互管理器和代理能力，正好符合回放需求。</p>
 */
@Mixin(ServerPlayerInteractionManager.class)
public interface MagicianPlaybackGameModeInvoker {

    /** 静默写入当前与上一游戏模式，并让原版同步更新代理玩家能力。 */
    @Invoker("setGameMode")
    void noellesroles$setReplayGameMode(GameMode gameMode, GameMode previousGameMode);
}
