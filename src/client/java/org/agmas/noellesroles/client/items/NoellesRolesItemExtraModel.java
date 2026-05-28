package org.agmas.noellesroles.client.items;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;

public class NoellesRolesItemExtraModel {

    /**
     * 获取冷却模型谓词 ID（用于物品模型根据冷却状态变化）
     */
    public static Identifier getCooldownId() {
        return Identifier.of(Noellesroles.MOD_ID, "item_cooldown");
    }

    /**
     * 注册物品的额外模型（当前仅注册冷却模型，方便后续扩展）
     */
    public static void registerExtraModel(@NotNull Item item) {
        ModelPredicateProviderRegistry.register(item, getCooldownId(), (itemStack, world, entity, seed) -> {
            if (MinecraftClient.getInstance().player == null) return 0.0F;
            return MinecraftClient.getInstance().player.getItemCooldownManager().isCoolingDown(item) ? 1.0F : 0.0F;
        });
        // 未来可以在此添加其他自定义模型谓词，例如：
        // ModelPredicateProviderRegistry.register(item, getSomeOtherId(), ...);
    }
}