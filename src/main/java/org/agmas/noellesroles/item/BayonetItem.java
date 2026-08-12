package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.item.KnifeItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.packet.item.BayonetStabC2SPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 刺刀。
 *
 * <p>它和 Wathe 原版匕首最大的区别有两点：</p>
 * <p>1. 右键不进入蓄力，客户端会立即尝试对准近处玩家发起暗杀；</p>
 * <p>2. 由于本模组 main/client 源集拆分，主源码不能直接引用 client networking，
 * 因此这里沿用 KinsWathe 猎刀的做法，用反射发送 C2S 数据包。</p>
 */
public class BayonetItem extends Item {

    public BayonetItem(Settings settings) {
        super(settings);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity user, @NotNull Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        /*
         * 刺刀现在走“攻击键式挥击”而不是“瞬间起手又立刻打断的蓄力动作”。
         * 这样右键时观感会更接近一次利落的挥刀，不会再出现原先那种抽搐感。
         *
         * 这里在 use 阶段就直接挥手，而不是等到服务端命中后再补一次，
         * 原因是玩家即便空挥也应该立刻看到动作反馈。
         */
        user.swingHand(hand, true);

        /*
         * 刺刀不需要像匕首那样进入“举刀蓄力”状态。
         * 这里属于真实刺杀发包入口，必须使用 ATTACK 语义：
         * 尸体伪装玩家会拒绝 TARGET 来隐藏准心变化，但不能因此免疫刺刀伤害。
         * 准心 HUD 仍然走 getBayonetTarget，避免玩家通过图标变化识破伪装。
         */
        if (world.isClient) {
            HitResult collision = getBayonetAttackTarget(user);
            if (collision instanceof EntityHitResult entityHitResult) {
                sendBayonetPacket(new BayonetStabC2SPacket(entityHitResult.getEntity().getId()));
            }
        }

        /*
         * 这里不再返回 consume 的“使用中”语义，避免客户端把刺刀当成一段被秒打断的 use 动作，
         * 从而继续产生抖动。实际动作表现已经由上面的 swingHand 明确接管。
         */
        return TypedActionResult.success(stack, false);
    }

    public static @Nullable HitResult getBayonetTarget(@NotNull PlayerEntity user) {
        /*
         * 准心和 HUD 只应该关心“它看起来是不是一个可锁定目标”。
         * 因此这里保留 Wathe 匕首的 TARGET 语义，不会暴露亡语杀手的尸体伪装。
         */
        return KnifeItem.getKnifeTarget(user);
    }

    public static @Nullable HitResult getBayonetAttackTarget(@NotNull PlayerEntity user) {
        /*
         * 右键刺杀是真实伤害入口，必须与 Wathe 匕首一样使用 ATTACK 语义。
         * 服务端 BayonetStabC2SPacket 仍会再次校验距离、冷却、存活和 canAttackPlayer。
         */
        return KnifeItem.getKnifeAttackTarget(user, user.getMainHandStack());
    }

    private static void sendBayonetPacket(@NotNull CustomPayload payload) {
        try {
            Class<?> networkingClass = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            Method sendMethod = networkingClass.getMethod("send", CustomPayload.class);
            sendMethod.invoke(null, payload);
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException | NoSuchMethodException ignored) {
            // 客户端 networking 不可用时，直接忽略本次发包，避免 main 源集硬依赖 client 类崩溃。
        }
    }
}
