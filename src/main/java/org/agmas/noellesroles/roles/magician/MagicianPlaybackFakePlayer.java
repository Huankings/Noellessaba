package org.agmas.noellesroles.roles.magician;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 魔术师播放阶段使用的服务端代理玩家。
 *
 * <p>这里不把它真正显示给客户端，也不让它顶替可见皮套实体，
 * 它只负责一件事：
 * 让服务端继续把播放期间的右键、开门、扔雷、蓄力、使用物品等逻辑，
 * 当成一次“真实玩家交互”去跑原有代码链。
 *
 * <p>它继承 Fabric 的 {@link FakePlayer}，并且刻意沿用魔术师本体的 UUID：
 * 1. 一些只存 UUID 的玩法状态，例如下毒者、床效果拥有者等，会天然归属到魔术师本体；
 * 2. 真正需要金币结算 / 击杀归属时，再由额外 mixin 把 fake player 的 killer 还原成真实魔术师。</p>
 */
public class MagicianPlaybackFakePlayer extends FakePlayer {

    private final UUID magicianOwnerUuid;
    private UUID replayActorUuid;
    private String replayActorName;

    public MagicianPlaybackFakePlayer(
            @NotNull ServerWorld world,
            @NotNull GameProfile profile,
            @NotNull UUID magicianOwnerUuid,
            UUID replayActorUuid,
            String replayActorName
    ) {
        super(world, profile);
        this.magicianOwnerUuid = magicianOwnerUuid;
        this.replayActorUuid = replayActorUuid;
        this.replayActorName = replayActorName == null ? "" : replayActorName;
    }

    public UUID getMagicianOwnerUuid() {
        return this.magicianOwnerUuid;
    }

    public UUID getReplayActorUuid() {
        return this.replayActorUuid;
    }

    public String getReplayActorName() {
        return this.replayActorName;
    }

    public void setReplayIdentity(UUID replayActorUuid, String replayActorName) {
        this.replayActorUuid = replayActorUuid;
        this.replayActorName = replayActorName == null ? "" : replayActorName;
    }

    public void setReplayItemUseTimeLeft(int ticks) {
        /*
         * FakePlayer 不会像真实在线玩家那样完整走客户端输入和自然蓄力同步。
         * 播放时由录制帧把剩余使用时间写回来，松手释放时才能拿到正确的蓄力时长。
         */
        this.itemUseTimeLeft = Math.max(0, ticks);
    }
}
