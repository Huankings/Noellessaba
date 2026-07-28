package org.agmas.noellesroles.roles.timekeeper;

import net.minecraft.text.Text;

/**
 * 濒毁怀表的运行状态。
 *
 * <p>同一个物品 id 通过数据组件区分普通、损坏和精致三种状态。
 * 这样商店、背包、掉落和回溯快照都只需要处理一类物品，
 * 不会因为三个独立物品互相替换导致槽位、冷却和回放难以同步。</p>
 */
public enum TimekeeperWatchState {
    NORMAL("normal", "item.noellesroles.dying_watch.state.normal"),
    BROKEN("broken", "item.noellesroles.dying_watch.state.broken"),
    ELEGANT("elegant", "item.noellesroles.dying_watch.state.elegant");

    private final String id;
    private final String translationKey;

    TimekeeperWatchState(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String id() {
        return this.id;
    }

    public Text text() {
        return Text.translatable(this.translationKey);
    }

    public boolean isBroken() {
        return this == BROKEN;
    }

    public boolean isElegant() {
        return this == ELEGANT;
    }

    public static TimekeeperWatchState byId(String id) {
        for (TimekeeperWatchState state : values()) {
            if (state.id.equals(id)) {
                return state;
            }
        }
        return NORMAL;
    }
}
