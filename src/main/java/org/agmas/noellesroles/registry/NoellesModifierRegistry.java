package org.agmas.noellesroles.registry;

import dev.doctor4t.wathe.api.economy.EconomyApi;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.harpymodloader.modifiers.HMLModifiers;
import org.agmas.harpymodloader.modifiers.Modifier;
import org.agmas.noellesroles.modifiers.magnate.MagnateConstants;
import org.agmas.noellesroles.modifiers.taskmaster.TaskmasterConstants;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * NoellesRoles 的词条注册中心。
 *
 * <p>词条和职业使用不同的 Harpy 注册 API，拆成独立类可以避免后续把两套概念继续混在入口类里。</p>
 */
public final class NoellesModifierRegistry {
    //变色龙
    public static final Modifier CHAMELEON = HMLModifiers.registerModifier(new Modifier(NoellesRoleIds.CHAMELEON_ID, new Color(198, 255, 137, 255).getRGB(), null, null, false, false));
    //羽化者
    public static final Modifier FEATHER = HMLModifiers.registerModifier(new Modifier(NoellesRoleIds.FEATHER_ID, new Color(255, 236, 161, 255).getRGB(), null, null, false, false));
    //盗墓者
    public static final Modifier GRAVEROBBER = HMLModifiers.registerModifier(new Modifier(NoellesRoleIds.GRAVEROBBER_ID, new Color(174, 95, 95, 255).getRGB(), null, null, true, false));
    //猜测者
    public static final Modifier GUESSER = HMLModifiers.registerModifier(new Modifier(
            NoellesRoleIds.GUESSER_ID,
            new Color(158, 43, 25, 255).getRGB(),
            new ArrayList<>(List.of(NoellesRoleRegistry.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES)),
            null,
            true,
            false
    ));
    //富豪
    public static final Modifier MAGNATE = HMLModifiers.registerModifier(new Modifier(NoellesRoleIds.MAGNATE_ID, MagnateConstants.COLOR, null, null, false, false)
            .setEligibilityPredicate((gameWorld, player, modifier) -> {
                /*
                 * 富豪只应该分配给已经拥有通用被动收入的玩家。
                 * 这里直接询问 Wathe 经济 API，而不是维护静态职业名单；
                 * 后续其他扩展只要注册了被动收入，NoellesRoles 的富豪就能自然兼容。
                 */
                if (!(player instanceof ServerPlayerEntity serverPlayer) || !(player.getWorld() instanceof ServerWorld serverWorld)) {
                    return false;
                }
                return EconomyApi.canReceivePassiveIncome(serverWorld, gameWorld, serverPlayer);
            }));
    //任务大师
    public static final Modifier TASKMASTER = HMLModifiers.registerModifier(new Modifier(NoellesRoleIds.TASKMASTER_ID, TaskmasterConstants.COLOR, null, null, false, false)

            .setEligibilityPredicate((gameWorld, player, modifier) -> {
                /*
                 * 任务大师的收益依赖金币 HUD/金币余额，因此生成条件也统一看 Wathe 公开 API。
                 * 这能覆盖 NoellesRoles、Wathe 本体和其他扩展中明确注册了金币 HUD 的角色。
                 */
                return EconomyApi.shouldRenderBalanceHud(gameWorld, player);
            }));

    //小孩子
    public static final Modifier TINY = HMLModifiers.registerModifier(new Modifier(
            NoellesRoleIds.TINY_ID,
            new Color(255, 166, 0).getRGB(),
            new ArrayList<>(List.of(NoellesRoleRegistry.MORPHLING)),
            null,
            false,
            false
    ));

    private NoellesModifierRegistry() {
    }
}
