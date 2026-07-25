package org.agmas.noellesroles.modifiers;

import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.harpymodloader.events.ModifierAssigned;
import org.agmas.harpymodloader.modifiers.Modifier;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.List;

/**
 * NoellesRoles 词条被 Harpy 随机分配后的补充逻辑。
 *
 * <p>Harpy 只知道“给某个玩家一个词条”，但恋人和双重人格都需要成对状态。
 * 因此这里监听 ModifierAssigned：Harpy 抽到第一名后，Noelles 再从本局合法候选中补第二名，
 * 同时把真实配对写进对应世界组件。</p>
 */
public final class NoellesModifierAssignmentHandler {
    private static boolean initialized;

    private NoellesModifierAssignmentHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ModifierAssigned.EVENT.register(NoellesModifierAssignmentHandler::handleAssigned);
    }

    private static void handleAssigned(net.minecraft.entity.player.PlayerEntity player, Modifier modifier) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || !(serverPlayer.getWorld() instanceof ServerWorld world)) {
            return;
        }
        if (NoellesModifierRegistry.LOVERS.equals(modifier)) {
            assignRandomLoverPartner(serverPlayer, world);
        } else if (NoellesModifierRegistry.DUAL_PERSONALITY.equals(modifier)) {
            assignRandomDualPersonalitySub(serverPlayer, world);
        }
    }

    private static void assignRandomLoverPartner(ServerPlayerEntity lover, ServerWorld world) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(world);
        WorldModifierComponent worldModifierComponent = WorldModifierComponent.KEY.get(world);

        // 旧写法是不断 randomPlayer 直到抽中合法目标。
        // 但如果这局里根本不存在第二个可成为恋人的玩家，就会无限循环并触发服务器 watchdog。
        // 这里改成先筛出全部合法候选人，再从列表里随机选一个。
        List<ServerPlayerEntity> candidates = world.getPlayers(candidate -> !candidate.equals(lover)
                && gameWorldComponent.getRole(candidate) != null
                && gameWorldComponent.isInnocent(candidate)
                && !gameWorldComponent.isRole(candidate, WatheRoles.VIGILANTE)
                && !worldModifierComponent.isModifier(candidate, NoellesModifierRegistry.LOVERS));

        if (candidates.isEmpty()) {
            // 候选为空时直接安全跳过，不再让整局初始化卡死。
            NoellesRolesCore.LOGGER.warn("恋人词条分配已跳过：玩家 {} 在本局中没有可用的第二恋人候选者。", lover.getNameForScoreboard());
            return;
        }

        ServerPlayerEntity loverTwo = candidates.get(world.random.nextInt(candidates.size()));

        // assign both lovers
        // 只给另一位补词条即可；当前触发事件的这位已经是第一位恋人。
        if (!worldModifierComponent.isModifier(loverTwo, NoellesModifierRegistry.LOVERS)) {
            worldModifierComponent.addModifier(loverTwo.getUuid(), NoellesModifierRegistry.LOVERS);
        }
        LoversPairComponent.KEY.get(world).setRandomPair(lover.getUuid(), loverTwo.getUuid());
        worldModifierComponent.sync();
    }

    private static void assignRandomDualPersonalitySub(ServerPlayerEntity mainPersonality, ServerWorld world) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(world);
        WorldModifierComponent worldModifierComponent = WorldModifierComponent.KEY.get(world);

        /*
         * Harpy 随机抽到的这个玩家作为主人格。
         * 这里再从本局其它有职业、且尚未拥有双重人格词条的玩家中补一个副人格。
         * 不要求阵营相同，因为用户确认双重人格可以和各种职业/词条叠加。
         */
        List<ServerPlayerEntity> candidates = world.getPlayers(candidate -> !candidate.equals(mainPersonality)
                && gameWorldComponent.getRole(candidate) != null
                && !worldModifierComponent.isModifier(candidate, NoellesModifierRegistry.DUAL_PERSONALITY));

        if (candidates.isEmpty()) {
            NoellesRolesCore.LOGGER.warn("双重人格词条分配已跳过：玩家 {} 在本局中没有可用的副人格候选者。", mainPersonality.getNameForScoreboard());
            return;
        }

        ServerPlayerEntity subPersonality = candidates.get(world.random.nextInt(candidates.size()));
        if (!worldModifierComponent.isModifier(subPersonality, NoellesModifierRegistry.DUAL_PERSONALITY)) {
            // 副人格也要补 Harpy 词条，否则 HUD/胜利判定只会认主人格。
            worldModifierComponent.addModifier(subPersonality.getUuid(), NoellesModifierRegistry.DUAL_PERSONALITY);
        }
        // 世界组件保存真正的主副关系和初始 active/dormant 状态。
        DualPersonalityComponent.KEY.get(world).setRandomPair(mainPersonality.getUuid(), subPersonality.getUuid());
        worldModifierComponent.sync();
    }
}
