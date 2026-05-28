package org.agmas.noellesroles.roles.phantom;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;

public final class PhantomAbility {

    private PhantomAbility() {}

    /**
     * 处理幻灵的技能使用
     * @param player 幻灵玩家
     */
    public static void handle(ServerPlayerEntity player) {
        var gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.PHANTOM)) return;

        var ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown > 0) return;

        // 添加固定时长隐身，并同步记录这次隐身的生命周期。
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, PhantomConstants.INVISIBILITY_DURATION_TICKS, 0, true, false, true));
        PhantomPlayerComponent.KEY.get(player).startInvisibility();
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.PHANTOM_INVISIBILITY_STARTED_EVENT, player, null);
        ability.cooldown = PhantomConstants.ABILITY_COOLDOWN_TICKS;
        ability.sync();
    }
}
