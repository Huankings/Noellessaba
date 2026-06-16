package org.agmas.noellesroles.roles.magician;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 魔术师播放阶段的“回放显示身份”线程上下文。
 *
 * <p>很多 Wathe / 扩展模组现成的行为都会在内部自己调用：
 * 1. {@code GameRecordManager.recordItemUse(...)}；
 * 2. {@code recordItemHit(...)}；
 * 3. {@code recordDoorInteraction(...)}；
 * 4. {@code recordDeath(...)}。
 *
 * <p>这些逻辑本来只知道“当前是谁在服务端执行动作”，并不知道这是魔术师皮套回放。
 * 因此这里单独放一层线程上下文，让回放期间触发的记录事件都能自动补上：
 * 1. 真正拥有者是谁；
 * 2. 此刻应该显示成谁的皮套身份。
 *
 * <p>这样后面的回放 formatter 只要优先读取这份上下文附带的数据，
 * 就能把大量原本已经存在的 Wathe 事件文案，统一改成“看起来像皮套在做”。</p>
 */
public final class MagicianReplayActorContext {

    private static final ThreadLocal<ReplayActorInfo> CURRENT = new ThreadLocal<>();

    private MagicianReplayActorContext() {
    }

    public static Scope push(@Nullable UUID magicianOwner, @Nullable UUID replayActor, @Nullable String replayActorName) {
        ReplayActorInfo previous = CURRENT.get();
        CURRENT.set(new ReplayActorInfo(magicianOwner, replayActor, replayActorName));
        return new Scope(previous);
    }

    public static @Nullable ReplayActorInfo current() {
        return CURRENT.get();
    }

    public static final class Scope implements AutoCloseable {
        @Nullable
        private final ReplayActorInfo previous;
        private boolean closed = false;

        private Scope(@Nullable ReplayActorInfo previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            if (this.previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(this.previous);
            }
        }
    }

    public record ReplayActorInfo(
            @Nullable UUID magicianOwner,
            @Nullable UUID replayActor,
            @Nullable String replayActorName
    ) {
    }
}
