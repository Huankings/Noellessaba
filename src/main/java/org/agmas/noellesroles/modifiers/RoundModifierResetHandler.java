package org.agmas.noellesroles.modifiers;

import dev.doctor4t.wathe.api.event.GameEvents;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import org.agmas.harpymodloader.events.ModifierAssigned;
import org.agmas.harpymodloader.events.ResetPlayerEvent;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityManager;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;

/** 恋人与双重人格世界级回合状态的公开生命周期接入。 */
public final class RoundModifierResetHandler {
    private static final EntityAttributeModifier TINY_MODIFIER = new EntityAttributeModifier(
            Identifier.of(NoellesRolesCore.MOD_ID, "tiny_modifier"),
            -0.15,
            EntityAttributeModifier.Operation.ADD_VALUE
    );

    private RoundModifierResetHandler() {
    }

    public static void init() {
        /* 施加和清理放在同一个词条 handler，避免属性修饰符 ID 分散在全局事件类。 */
        ModifierAssigned.EVENT.register((player, modifier) -> {
            if (modifier.equals(NoellesModifierRegistry.TINY)) {
                player.getAttributeInstance(EntityAttributes.GENERIC_SCALE).removeModifier(TINY_MODIFIER);
                player.getAttributeInstance(EntityAttributes.GENERIC_SCALE).addPersistentModifier(TINY_MODIFIER);
            }
            if (modifier.equals(NoellesModifierRegistry.FEATHER)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, StatusEffectInstance.INFINITE, 0, true, false));
            }
        });
        ResetPlayerEvent.EVENT.register(player -> {
            /* 羽毛和迷你词条的运行效果必须随玩家硬重置清除，词条重新分配时会重新施加。 */
            player.removeStatusEffect(StatusEffects.SLOW_FALLING);
            player.getAttributeInstance(EntityAttributes.GENERIC_SCALE)
                    .removeModifier(TINY_MODIFIER);
        });
        GameEvents.ON_BEGIN_INITIALIZE.register((world, game) -> {
            /*
             * 这两份组件会参与新局词条配对、存活判定和 HUD，因此必须早于任何职业/词条分配清理。
             * ON_FINISH_INITIALIZE 已经太晚；Wathe 的 ON_BEGIN_INITIALIZE 正是为此时序提供的公开入口。
             */
            LoversPairComponent.KEY.get(world).clear();
            DualPersonalityComponent.KEY.get(world).clear();
        });

        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                /* 结算画面结束后再清副人格实体/语音等收尾状态，保留原有展示时序。 */
                DualPersonalityManager.clearRoundState(serverWorld);
            }
        });
    }
}
