package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 杰森投掷油桶配套的一次性打火机。
 *
 * <p>这个物品只负责“请求点燃自己已经落地的油桶”。
 * 真正的油桶查找、火焰范围和烧死玩家都由 {@link JasonFireWorldComponent} 在服务端统一处理。</p>
 */
public final class JasonOnceLighterItem extends Item {
    private static final String ROOT_KEY = "JasonOnceLighter";
    private static final String CAN_UUID_KEY = "CanUuid";
    private static final String OWNER_UUID_KEY = "OwnerUuid";

    public JasonOnceLighterItem(Settings settings) {
        super(settings);
    }

    public static @NotNull ItemStack createBoundStack(@NotNull UUID canUuid, @NotNull UUID ownerUuid) {
        ItemStack stack = ModItems.ONCE_LIGHTER.getDefaultStack();
        bindToCan(stack, canUuid, ownerUuid);
        return stack;
    }

    public static void bindToCan(@NotNull ItemStack stack, @NotNull UUID canUuid, @NotNull UUID ownerUuid) {
        /*
         * 绑定数据写进 CUSTOM_DATA，而不是额外建全局 Map。
         * 这样打火机被丢到地上、被别人捡走、被时停者回溯背包时，绑定关系都会跟着 ItemStack 自己移动。
         */
        NbtCompound customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        NbtCompound root = new NbtCompound();
        root.putUuid(CAN_UUID_KEY, canUuid);
        root.putUuid(OWNER_UUID_KEY, ownerUuid);
        customData.put(ROOT_KEY, root);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
    }

    public static @Nullable UUID getBoundCanUuid(@NotNull ItemStack stack) {
        NbtCompound root = root(stack);
        return root.containsUuid(CAN_UUID_KEY) ? root.getUuid(CAN_UUID_KEY) : null;
    }

    public static boolean isBoundToCan(@NotNull ItemStack stack, @NotNull UUID canUuid) {
        return stack.isOf(ModItems.ONCE_LIGHTER) && canUuid.equals(getBoundCanUuid(stack));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!GameFunctions.isPlayerAliveAndSurvival(user) || JasonWoundManager.isWoundedActionLocked(user)) {
            return TypedActionResult.fail(stack);
        }
        if (world.isClient) {
            return TypedActionResult.consume(stack);
        }
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return TypedActionResult.fail(stack);
        }

        UUID canUuid = getBoundCanUuid(stack);
        if (canUuid == null) {
            /*
             * 旧逻辑产生的无绑定打火机没有办法确定要点燃哪个油桶。
             * 这里直接失败且不消耗，避免再次回到“默认点第一个油桶”的错误行为。
             */
            return TypedActionResult.fail(stack);
        }

        boolean ignited = JasonFireWorldComponent.KEY.get(serverPlayer.getServerWorld()).tryIgniteCan(serverPlayer, canUuid);
        if (!ignited) {
            return TypedActionResult.fail(stack);
        }

        ItemStack resultStack = stack;
        if (!serverPlayer.isCreative()) {
            stack.decrement(1);
        } else {
            /*
             * 正常局内玩家不会依赖创造模式，但调试时仍按“一次性”结果返回空栈，
             * 防止后续全局清理已经删掉物品后，use() 的返回值又把同一枚打火机塞回手里。
             */
            resultStack = ItemStack.EMPTY;
        }
        JasonFireWorldComponent.removeOnceLightersForCan(serverPlayer.getServer(), canUuid);
        return TypedActionResult.success(resultStack, false);
    }

    private static @NotNull NbtCompound root(@NotNull ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound data = component.copyNbt();
        return data.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE) ? data.getCompound(ROOT_KEY) : new NbtCompound();
    }
}
