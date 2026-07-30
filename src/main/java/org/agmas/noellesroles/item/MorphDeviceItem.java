package org.agmas.noellesroles.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.morphling.MorphlingReagentService;

import java.util.List;

/**
 * 变形怪的变形遥控器。
 *
 * <p>遥控器自身不保存任何状态，只负责扫描当前变形怪名下的待触发试剂标记并统一启动。
 * 这样即便一个变形怪连续标记多人，也不需要把目标列表复制到物品 NBT 里。</p>
 */
public final class MorphDeviceItem extends Item {
    public MorphDeviceItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return TypedActionResult.success(stack, true);
        }
        if (user instanceof ServerPlayerEntity morphling) {
            return MorphlingReagentService.useDevice(morphling, stack);
        }
        return TypedActionResult.fail(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.noellesroles.morph_device.tooltip")
                .styled(style -> style.withColor(0x808080).withItalic(false)));
    }
}
