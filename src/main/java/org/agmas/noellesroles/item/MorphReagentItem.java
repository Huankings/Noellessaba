package org.agmas.noellesroles.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.morphling.MorphlingReagentService;

import java.util.List;

/**
 * 变形怪的变形试剂。
 *
 * <p>物品本身只负责把右键入口统一转交给 {@link MorphlingReagentService}，
 * 不在这里判断职业、目标或奖励。这样“直接右键实体”和“空手右键准心目标”
 * 使用完全同一套服务端规则。</p>
 */
public final class MorphReagentItem extends Item {
    public MorphReagentItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (user.getWorld().isClient()) {
            if (!MorphlingReagentService.hasSample(stack)) {
                /*
                 * 客户端也进入使用状态，服务端才能在 onStoppedUsing 收到“松开右键”信号。
                 * 这个信号用于清掉采样 gate，防止一次点击同时完成采样和标记。
                 */
                user.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }
            return ActionResult.PASS;
        }

        if (!(user instanceof ServerPlayerEntity morphling)) {
            return ActionResult.PASS;
        }

        boolean hadSample = MorphlingReagentService.hasSample(stack);
        ActionResult result = MorphlingReagentService.useReagent(morphling, stack, entity).getResult();
        startWaitingForReleaseAfterSampling(stack, user, hand, hadSample);
        return result;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            if (!MorphlingReagentService.hasSample(stack)) {
                user.setCurrentHand(hand);
                return TypedActionResult.consume(stack);
            }
            return TypedActionResult.success(stack, true);
        }

        if (user instanceof ServerPlayerEntity morphling) {
            boolean hadSample = MorphlingReagentService.hasSample(stack);
            TypedActionResult<ItemStack> result = MorphlingReagentService.useReagent(morphling, stack, null);
            startWaitingForReleaseAfterSampling(stack, user, hand, hadSample);
            return result;
        }
        return TypedActionResult.fail(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!world.isClient() && user instanceof ServerPlayerEntity morphling) {
            MorphlingReagentService.clearReagentReleaseGate(morphling);
        }
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        String sampleName = MorphlingReagentService.sampleNameForTooltip(stack);
        tooltip.add(Text.translatable(
                "item.noellesroles.morph_reagent.tooltip.sample",
                sampleName.isBlank() ? Text.translatable("item.noellesroles.morph_reagent.tooltip.none") : Text.literal(sampleName)
        ).styled(style -> style.withColor(0x808080).withItalic(false)));
        for (int i = 1; i <= 4; i++) {
            tooltip.add(Text.translatable("item.noellesroles.morph_reagent.tooltip.line" + i)
                    .styled(style -> style.withColor(0x808080).withItalic(false)));
        }
    }

    private static void startWaitingForReleaseAfterSampling(
            ItemStack stack,
            PlayerEntity user,
            Hand hand,
            boolean hadSample
    ) {
        if (!hadSample && MorphlingReagentService.hasSample(stack)) {
            user.setCurrentHand(hand);
        }
    }
}
