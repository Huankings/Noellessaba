package org.agmas.noellesroles.roles.timekeeper;

import net.minecraft.text.Text;

/**
 * 怀表当前选择的技能模式。
 *
 * <p>模式存进物品数据组件，而不是只存在客户端。
 * 左键切换时客户端会发包给服务端落盘，右键使用时服务端再读取这份模式，
 * 这样即使玩家切换物品栏、丢出怀表或网络延迟，也不会出现客户端显示和服务端判定不一致。</p>
 */
public enum TimekeeperWatchMode {
    ITEM_ACCELERATE("item_accelerate", "item.noellesroles.dying_watch.mode.item_accelerate"),
    ABILITY_ACCELERATE("ability_accelerate", "item.noellesroles.dying_watch.mode.ability_accelerate"),
    REWIND("rewind", "item.noellesroles.dying_watch.mode.rewind");

    private final String id;
    private final String translationKey;

    TimekeeperWatchMode(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String id() {
        return this.id;
    }

    public Text text() {
        return Text.translatable(this.translationKey);
    }

    public int getTimeCost() {
        return this == REWIND ? TimekeeperConstants.REWIND_TIME_COST : TimekeeperConstants.ACCELERATE_TIME_COST;
    }

    public TimekeeperWatchMode next() {
        TimekeeperWatchMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static TimekeeperWatchMode byId(String id) {
        for (TimekeeperWatchMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return ITEM_ACCELERATE;
    }
}
