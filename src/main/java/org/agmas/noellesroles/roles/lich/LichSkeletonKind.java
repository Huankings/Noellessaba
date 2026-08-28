package org.agmas.noellesroles.roles.lich;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import org.agmas.noellesroles.ModItems;

/**
 * 巫妖两类骷髅命名。
 *
 * <p>回放里保存稳定的翻译 key，而不是保存当前语言下的中文字符串。
 * 这样之后切换语言或重放历史记录时，仍然可以正确显示“法术骷髅 / 亡灵骷髅”。</p>
 */
public enum LichSkeletonKind {
    /** 简易法杖发射的骷髅，回放显示为“法术骷髅”。 */
    SPELL("replay.name.noellesroles.spell_skeleton"),
    /** 疯魔法杖发射的骷髅，回放显示为“亡灵骷髅”。 */
    UNDEAD("replay.name.noellesroles.undead_skeleton");

    /** 死亡与命中回放里读取的 NBT 字段名。 */
    public static final String REPLAY_NAME_KEY = "skeleton_name_key";

    private final String replayNameKey;

    LichSkeletonKind(String replayNameKey) {
        this.replayNameKey = replayNameKey;
    }

    public String replayNameKey() {
        return this.replayNameKey;
    }

    public NbtCompound createReplayData(ServerWorld world, ItemStack stack) {
        NbtCompound data = GameFunctions.createReplayItemData(
                world,
                stack.isEmpty() ? ModItems.ONCE_STAFF.getDefaultStack() : stack
        );
        if (data == null) {
            data = new NbtCompound();
        }
        data.putString(REPLAY_NAME_KEY, this.replayNameKey);
        return data;
    }
}
