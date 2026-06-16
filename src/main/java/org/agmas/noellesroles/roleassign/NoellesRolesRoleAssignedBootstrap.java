package org.agmas.noellesroles.roleassign;

import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.roles.angel.AngelRoleAssignedHandler;
import org.agmas.noellesroles.roles.assassin.AssassinRoleAssignedHandler;
import org.agmas.noellesroles.roles.awesomebinglus.AwesomeBinglusRoleAssignedHandler;
import org.agmas.noellesroles.roles.bettervigilante.BetterVigilanteRoleAssignedHandler;
import org.agmas.noellesroles.roles.bomber.BomberRoleAssignedHandler;
import org.agmas.noellesroles.roles.brainwasher.BrainwasherRoleAssignedHandler;
import org.agmas.noellesroles.roles.conductor.ConductorRoleAssignedHandler;
import org.agmas.noellesroles.roles.controller.ControllerRoleAssignedHandler;
import org.agmas.noellesroles.roles.coward.CowardRoleAssignedHandler;
import org.agmas.noellesroles.roles.corpsemaker.CorpsemakerRoleAssignedHandler;
import org.agmas.noellesroles.roles.executioner.ExecutionerRoleAssignedHandler;
import org.agmas.noellesroles.roles.goddess.GoddessRoleAssignedHandler;
import org.agmas.noellesroles.roles.jester.JesterRoleAssignedHandler;
import org.agmas.noellesroles.roles.magician.MagicianRoleAssignedHandler;
import org.agmas.noellesroles.roles.mimic.MimicRoleAssignedHandler;
import org.agmas.noellesroles.roles.operator.OperatorRoleAssignedHandler;
import org.agmas.noellesroles.roles.prophet.ProphetRoleAssignedHandler;
import org.agmas.noellesroles.roles.rememberer.RemembererRoleAssignedHandler;
import org.agmas.noellesroles.roles.robber.RobberRoleAssignedHandler;
import org.agmas.noellesroles.roles.stalker.StalkerRoleAssignedHandler;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistRoleAssignedHandler;
import org.agmas.noellesroles.roles.vulture.VultureRoleAssignedHandler;
import org.agmas.noellesroles.roles.winder.WinderRoleAssignedHandler;

/**
 * noellesroles 的职业分配事件总引导器。
 *
 * <p>这次迁移的核心目标是“把职业分配逻辑拆回各职业自己管理”，
 * 但同时又不能改变原先那一大段 {@code ModdedRoleAssigned.EVENT.register} 的执行语义。</p>
 *
 * <p>因此这里采用和死亡事件同样的保守方案：</p>
 * <p>1. 对外只注册一次 {@link ModdedRoleAssigned} 监听；</p>
 * <p>2. 监听器内部再按旧代码的完全相同顺序，把流程分发到各职业处理器；</p>
 * <p>3. 这样既能把代码从主类拆开，又不会因为“多个监听器注册顺序变化”导致冷却、发物品、组件重置的先后被改坏。</p>
 *
 * <p>这里特意保留顺序，原因是 {@link ModdedRoleAssigned} 与死亡事件不同：
 * 它不会短路，而是会把所有监听器全部执行一遍。
 * 所以像“先给全职业写入通用冷却，再让某些职业覆盖成 0 / 30”的逻辑，
 * 一旦拆成多个分散监听器，就会变得非常依赖注册时机。
 * 统一引导能最大限度保证零语义漂移。</p>
 */
public final class NoellesRolesRoleAssignedBootstrap {

    private static boolean initialized = false;

    private NoellesRolesRoleAssignedBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ModdedRoleAssigned.EVENT.register((player, role) -> {
            applyDefaultAbilityCooldown(player);

            /*
             * 下面的调用顺序刻意与旧代码保持一致。
             * 如果后续要新增新的职业分配逻辑，也应先确认它在旧链路里应当位于哪个位置，再插入这里。
             */
            ControllerRoleAssignedHandler.onRoleAssigned(player, role);
            CorpsemakerRoleAssignedHandler.onRoleAssigned(player, role);
            BrainwasherRoleAssignedHandler.onRoleAssigned(player, role);
            BomberRoleAssignedHandler.onRoleAssigned(player, role);
            RobberRoleAssignedHandler.onRoleAssigned(player, role);
            AssassinRoleAssignedHandler.onRoleAssigned(player, role);
            StalkerRoleAssignedHandler.onRoleAssigned(player, role);
            GoddessRoleAssignedHandler.onRoleAssigned(player, role);
            AngelRoleAssignedHandler.onRoleAssigned(player, role);
            CowardRoleAssignedHandler.onRoleAssigned(player, role);
            WinderRoleAssignedHandler.onRoleAssigned(player, role);
            OperatorRoleAssignedHandler.onRoleAssigned(player, role);
            MagicianRoleAssignedHandler.onRoleAssigned(player, role);
            SpiritualistRoleAssignedHandler.onRoleAssigned(player, role);
            ProphetRoleAssignedHandler.onRoleAssigned(player, role);
            RemembererRoleAssignedHandler.onRoleAssigned(player, role);
            ExecutionerRoleAssignedHandler.onRoleAssigned(player, role);
            VultureRoleAssignedHandler.onRoleAssigned(player, role);
            BetterVigilanteRoleAssignedHandler.onRoleAssigned(player, role);
            MimicRoleAssignedHandler.onRoleAssigned(player, role);
            JesterRoleAssignedHandler.onRoleAssigned(player, role);
            ConductorRoleAssignedHandler.onRoleAssigned(player, role);
            AwesomeBinglusRoleAssignedHandler.onRoleAssigned(player, role);
        });
    }

    /**
     * 应用 noellesroles 原先的“通用能力初始冷却”基线。
     *
     * <p>这里故意继续沿用旧实现的写法：直接改字段，不立刻 sync。
     * 这样能最大程度复刻原逻辑。
     * 某些职业后面会再调用 {@code setCooldown()} 或者手动 {@code sync()} 覆盖它，
     * 这些细节都由各自处理器继续保持。</p>
     */
    private static void applyDefaultAbilityCooldown(net.minecraft.entity.player.PlayerEntity player) {
        AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(player);
        abilityPlayerComponent.cooldown = NoellesRolesConfig.HANDLER.instance().generalCooldownTicks;
    }
}
