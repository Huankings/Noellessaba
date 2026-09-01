package org.agmas.noellesroles.death;

import dev.doctor4t.wathe.api.event.AllowPlayerDeath;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityDeathHandler;
import org.agmas.noellesroles.roles.angel.AngelDeathProtectionHandler;
import org.agmas.noellesroles.roles.angel.AngelDeathCleanupHandler;
import org.agmas.noellesroles.roles.assassin.AssassinBodySpawnHandler;
import org.agmas.noellesroles.roles.bartender.BartenderDeathProtectionHandler;
import org.agmas.noellesroles.roles.bomber.BomberDeathHandler;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterDeathHandler;
import org.agmas.noellesroles.roles.conductor.ConductorDeathRewardHandler;
import org.agmas.noellesroles.roles.controller.ControllerDeathProtectionHandler;
import org.agmas.noellesroles.roles.controller.ControllerDeathHandler;
import org.agmas.noellesroles.roles.convener.ConvenerDeathProtectionHandler;
import org.agmas.noellesroles.roles.cook.CookDeathProtectionHandler;
import org.agmas.noellesroles.roles.coroner.CoronerBodySpawnHandler;
import org.agmas.noellesroles.roles.dreamer.DreamerDeathProtectionHandler;
import org.agmas.noellesroles.roles.executioner.ExecutionerBackfireDeathHandler;
import org.agmas.noellesroles.roles.executioner.ExecutionerDeathHandler;
import org.agmas.noellesroles.roles.jester.JesterDeathProtectionHandler;
import org.agmas.noellesroles.roles.jason.JasonDeathHandler;
import org.agmas.noellesroles.roles.kidnapper.KidnapperDeathRewardHandler;
import org.agmas.noellesroles.roles.insane_damned_paranoid_killer.InsaneDamnedKillerDeathHandler;
import org.agmas.noellesroles.roles.magician.MagicianPlaybackDeathHandler;
import org.agmas.noellesroles.roles.mimic.MimicBackfireDeathHandler;
import org.agmas.noellesroles.roles.morphling.MorphlingDeathHandler;
import org.agmas.noellesroles.roles.necromancer.NecromancerDeathHandler;
import org.agmas.noellesroles.roles.noisemaker.NoisemakerBodySpawnHandler;
import org.agmas.noellesroles.roles.physician.PhysicianDeathProtectionHandler;
import org.agmas.noellesroles.roles.prophet.ProphetDeathCleanupHandler;
import org.agmas.noellesroles.roles.prophet.ProphetDeathProtectionHandler;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterDeathHandler;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistDeathProtectionHandler;
import org.agmas.noellesroles.roles.stalker.StalkerDeathHandler;
import org.agmas.noellesroles.roles.stalker.StalkerDeathProtectionHandler;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperDeathHandler;
import org.agmas.noellesroles.roles.voodoo.VoodooDeathHandler;
import org.agmas.noellesroles.roles.vecna.VecnaDeathHandler;

/**
 * noellesroles 的死亡事件总引导器。
 *
 * <p>这个类的职责非常单一：</p>
 * <p>1. 只负责把原先写在旧入口事件注册里的大段死亡监听拆开；</p>
 * <p>2. 严格保留原来的执行顺序与短路行为；</p>
 * <p>3. 让每个职业自己的死亡特判回到各自的包内维护。</p>
 *
 * <p>之所以没有让每个职业都自己单独调用
 * {@link AllowPlayerDeath#EVENT} 注册，是因为 wathe 的死亡事件是“按顺序短路”的，
 * 而旧代码里还有“监听器中途直接 return true，跳过后半段保护但保留后续监听器”的细粒度语义。
 * 为了百分百复刻旧行为，这里采用“统一注册监听器 + 按顺序分发到各职业处理器”的方式。</p>
 */
public final class NoellesRolesDeathBootstrap {

    /**
     * 防止初始化流程被重复调用时，事件被重复注册。
     *
     * <p>虽然目前正常启动路径只会进一次，
     * 但加上这层保护后，后续如果有人重构启动顺序，也不容易误注册两遍。</p>
     */
    private static boolean initialized = false;

    private NoellesRolesDeathBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        registerDeathApiHandlers();
        registerProtectionChain();
        registerBackfireChain();
    }

    /**
     * 注册 Wathe DeathApi 上的分阶段死亡机制。
     *
     * <p>实际逻辑仍按职业/词条拆在各自包内；这里仅负责让这些 handler 参与启动，
     * 避免扩展继续把 killPlayer 的局部变量和返回点当成公共接口。</p>
     */
    private static void registerDeathApiHandlers() {
        /*
         * DeathApi 迁移后的执行模型：
         * 1. DeathProcessHandler 先写“正在处理死亡”标记，给巫毒/附体等递归死亡防重入；
         * 2. Timekeeper / DualPersonality 这类会吞掉或改写死亡的机制用高优先级拦截；
         * 3. 具体职业的奖励、转职、尸体信息和死亡后清理仍拆在各自包里；
         * 4. 最终清理由 DeathProcessHandler 在 afterAttempt 的最低优先级完成。
         *
         * 这里不写业务逻辑，只维护启动顺序，避免以后又把 killPlayer 的局部变量注入点当成 API。
         */
        DeathProcessHandler.init();
        TimekeeperDeathHandler.init();
        DualPersonalityDeathHandler.init();
        ControllerDeathHandler.init();
        VoodooDeathHandler.init();
        ConductorDeathRewardHandler.init();
        KidnapperDeathRewardHandler.init();
        ExecutionerDeathHandler.init();
        StalkerDeathHandler.init();
        BountyHunterDeathHandler.init();
        MorphlingDeathHandler.init();
        InsaneDamnedKillerDeathHandler.init();
        NecromancerDeathHandler.init();
        BomberDeathHandler.init();
        AngelDeathCleanupHandler.init();
        ProphetDeathCleanupHandler.init();
        CoronerBodySpawnHandler.init();
        AssassinBodySpawnHandler.init();
        NoisemakerBodySpawnHandler.init();
        MagicianPlaybackDeathHandler.init();
        JasonDeathHandler.init();
        ShadowJesterDeathHandler.init();
        VecnaDeathHandler.init();
    }

    /**
     * 注册第一段“受害者自身保命 / 免死 / 强制放行”链路。
     *
     * <p>这一段必须保持与旧代码完全同序：</p>
     * <p>Angel -> Controller -> Stalker -> 强制放行 -> Jester -> Dreamer -> Bartender -> Prophet</p>
     *
     * <p>任何一个处理器返回 {@code false} 都会像旧实现一样立刻短路，
     * 后面的职业不再继续判定。</p>
     */
    private static void registerProtectionChain() {
        AllowPlayerDeath.EVENT.register((playerEntity, killer, deathReason) -> {
            if (!AngelDeathProtectionHandler.allowDeath(playerEntity, killer, deathReason)) {
                return false;
            }
            if (!ControllerDeathProtectionHandler.allowDeath(playerEntity, killer, deathReason)) {
                return false;
            }
            if (!StalkerDeathProtectionHandler.allowDeath(playerEntity, killer, deathReason)) {
                return false;
            }
            if (!SpiritualistDeathProtectionHandler.allowDeath(playerEntity, killer, deathReason)) {
                return false;
            }
            /*
             * 召集者的巫毒免疫和反伤护盾需要在“强制放行”之前判断。
             * 否则巫毒这类扩展死因会直接穿过护盾链，导致 StupidExpress 原本的独立防护语义丢失。
             */
            if (!ConvenerDeathProtectionHandler.allowDeath(playerEntity, killer, deathReason)) {
                return false;
            }

            /*
             * 这一条必须放在中间，而不是做成单独监听器。
             * 原因见 CommonForcedDeathHandler 的类注释：
             * 旧逻辑里它只跳过后半段保护，不会阻断后续第二段 backfire 监听器。
             */
            if (CommonForcedDeathHandler.shouldForceAllow(deathReason)) {
                return true;
            }

            if (!JesterDeathProtectionHandler.allowDeath(playerEntity, killer, deathReason)) {
                return false;
            }
            if (!DreamerDeathProtectionHandler.allowDeath(playerEntity, killer, deathReason)) {
                return false;
            }
            if (!CookDeathProtectionHandler.allowDeath(playerEntity, killer, deathReason)) {
                return false;
            }
            if (!PhysicianDeathProtectionHandler.allowDeath(playerEntity, killer, deathReason)) {
                return false;
            }
            if (!BartenderDeathProtectionHandler.allowDeath(playerEntity, killer, deathReason)) {
                return false;
            }
            if (!ProphetDeathProtectionHandler.allowDeath(playerEntity, killer, deathReason)) {
                return false;
            }
            return true;
        });
    }

    /**
     * 注册第二段“攻击者反噬 / 追责自杀”链路。
     *
     * <p>这段对应旧代码里的第二个 {@code AllowPlayerDeath.EVENT.register}。
     * 它不负责拦截死亡，只负责在特定死因成立时触发额外连锁效果。</p>
     */
    private static void registerBackfireChain() {
        AllowPlayerDeath.EVENT.register((playerEntity, killer, deathReason) -> {
            if (!MimicBackfireDeathHandler.allowDeath(playerEntity, killer, deathReason)) {
                return false;
            }
            if (!ExecutionerBackfireDeathHandler.allowDeath(playerEntity, killer, deathReason)) {
                return false;
            }
            return true;
        });
    }
}
