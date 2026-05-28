package org.agmas.noellesroles.death;

import dev.doctor4t.wathe.api.event.AllowPlayerDeath;
import org.agmas.noellesroles.roles.angel.AngelDeathProtectionHandler;
import org.agmas.noellesroles.roles.bartender.BartenderDeathProtectionHandler;
import org.agmas.noellesroles.roles.controller.ControllerDeathProtectionHandler;
import org.agmas.noellesroles.roles.executioner.ExecutionerBackfireDeathHandler;
import org.agmas.noellesroles.roles.jester.JesterDeathProtectionHandler;
import org.agmas.noellesroles.roles.mimic.MimicBackfireDeathHandler;
import org.agmas.noellesroles.roles.prophet.ProphetDeathProtectionHandler;
import org.agmas.noellesroles.roles.stalker.StalkerDeathProtectionHandler;

/**
 * noellesroles 的死亡事件总引导器。
 *
 * <p>这个类的职责非常单一：</p>
 * <p>1. 只负责把原先写在 {@code Noellesroles.registerEvents()} 里的大段死亡监听拆开；</p>
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

        registerProtectionChain();
        registerBackfireChain();
    }

    /**
     * 注册第一段“受害者自身保命 / 免死 / 强制放行”链路。
     *
     * <p>这一段必须保持与旧代码完全同序：</p>
     * <p>Angel -> Controller -> Stalker -> 强制放行 -> Jester -> Bartender -> Prophet</p>
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
