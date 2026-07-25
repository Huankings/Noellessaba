package org.agmas.noellesroles.modifiers.allergic;

import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.item.CocktailItem;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;

/**
 * 过敏患者“吃/喝后触发随机副作用”的服务端处理器。
 */
public final class AllergicConsumeHandler {
    private AllergicConsumeHandler() {
    }

    public static void handleConsume(ServerPlayerEntity player, net.minecraft.item.ItemStack stack, World world) {
        AllergicPlayerComponent allergic = AllergicPlayerComponent.KEY.get(player);
        if (!allergic.isAllergic() || !matchesAllergyType(allergic, stack)) {
            return;
        }

        int totalChance = AllergicConstants.totalChance();
        if (totalChance <= 0) {
            return;
        }

        int roll = world.getRandom().nextBetween(1, totalChance);
        if (roll <= AllergicConstants.NOTHING_CHANCE) {
            return;
        }
        roll -= AllergicConstants.NOTHING_CHANCE;

        if (roll <= AllergicConstants.INSTINCT_CHANCE) {
            triggerInstinct(player, allergic);
            return;
        }
        roll -= AllergicConstants.INSTINCT_CHANCE;

        if (roll <= AllergicConstants.SHIELD_CHANCE) {
            triggerShield(player, allergic);
            return;
        }

        triggerPoison(player);
    }

    private static boolean matchesAllergyType(AllergicPlayerComponent allergic, net.minecraft.item.ItemStack stack) {
        boolean drink = stack.getItem() instanceof CocktailItem;
        boolean food = stack.get(DataComponentTypes.FOOD) != null && !drink;

        /*
         * Starry 旧实现只把 CocktailItem 当“喝”，把其它带 FOOD 组件的物品当“吃”。
         * 药水等普通 DRINK 动作不属于该机制，避免过敏患者喝药水也触发副作用。
         */
        return (AllergicConstants.ALLERGY_TYPE_DRINK.equals(allergic.getAllergyType()) && drink)
                || (AllergicConstants.ALLERGY_TYPE_FOOD.equals(allergic.getAllergyType()) && food);
    }

    private static void triggerPoison(ServerPlayerEntity player) {
        PlayerPoisonComponent poison = PlayerPoisonComponent.KEY.get(player);
        int currentPoisonTicks = poison.poisonTicks;
        int poisonTicks = currentPoisonTicks == -1
                ? player.getWorld().getRandom().nextBetween(PlayerPoisonComponent.clampTime.getLeft(), PlayerPoisonComponent.clampTime.getRight())
                : MathHelper.clamp(
                        currentPoisonTicks - player.getWorld().getRandom().nextBetween(
                                AllergicConstants.POISON_ACCELERATION_MIN_TICKS,
                                AllergicConstants.POISON_ACCELERATION_MAX_TICKS
                        ),
                        0,
                        PlayerPoisonComponent.clampTime.getRight()
                );

        /*
         * 过敏毒是“自己触发、自己毒死自己”的特殊毒源。
         * Wathe 的 setDetailedPoisonTicks 会把 source 保留到最终死亡，
         * 因此这里直接写 Noelles 的 allergies 死因，避免再新增改写 killPlayer 参数的 mixin。
         */
        poison.setDetailedPoisonTicks(poisonTicks, player.getUuid(), NoellesDeathReasons.ALLERGIES_DEATH_REASON, new NbtCompound());
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), NoellesEventIds.ALLERGIC_POISON_TRIGGERED_EVENT, player, null);
        player.sendMessage(Text.translatable("hud.allergic.effect.poison").withColor(NoellesModifierRegistry.ALLERGIC.color()), true);
    }

    private static void triggerInstinct(ServerPlayerEntity player, AllergicPlayerComponent allergic) {
        allergic.setGlowTicks(AllergicConstants.INSTINCT_DURATION_TICKS);
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), NoellesEventIds.ALLERGIC_INSTINCT_TRIGGERED_EVENT, player, null);
        player.sendMessage(Text.translatable("hud.allergic.effect.instinct").withColor(NoellesModifierRegistry.ALLERGIC.color()), true);
    }

    private static void triggerShield(ServerPlayerEntity player, AllergicPlayerComponent allergic) {
        allergic.giveShield();
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), NoellesEventIds.ALLERGIC_SHIELD_GAINED_EVENT, player, null);
        player.sendMessage(Text.translatable("hud.allergic.effect.armor").withColor(NoellesModifierRegistry.ALLERGIC.color()), true);
    }
}
