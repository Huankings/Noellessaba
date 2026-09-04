package org.agmas.noellesroles.roles.assassin;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.NotNull;

/**
 * 刺客物品冷却的服务端辅助方法。
 *
 * <p>这里不再保存或同步 tooltip 专用来源状态；实际冷却完全由 ItemCooldownManager 管理，
 * Wathe 的公开 tooltip API 会直接读取同一条冷却记录。</p>
 */
public final class AssassinCooldownHelper {
    public static final int START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);

    private AssassinCooldownHelper() {
    }

    public static void reset(@NotNull PlayerEntity player) {
        player.getItemCooldownManager().remove(ModItems.BAYONET);
        player.getItemCooldownManager().remove(ModItems.SILENCED_REVOLVER);
        player.getItemCooldownManager().remove(ModItems.SILENT_GRENADE);
    }

    /** 购买刷新商品时只操作真正决定能否使用刺刀的原版冷却条目。 */
    public static boolean tryRefreshBayonetCooldown(@NotNull PlayerEntity player) {
        if (!player.getItemCooldownManager().isCoolingDown(ModItems.BAYONET)) {
            player.sendMessage(Text.translatable("shop.noellesroles.bayonet_refresh_unavailable").withColor(0xAA0000), true);
            return false;
        }
        player.getItemCooldownManager().remove(ModItems.BAYONET);
        return true;
    }
}
