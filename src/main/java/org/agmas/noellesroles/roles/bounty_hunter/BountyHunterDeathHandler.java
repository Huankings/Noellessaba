package org.agmas.noellesroles.roles.bounty_hunter;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.UUID;

/**
 * 赏金猎人的“任意方式击杀悬赏目标”奖励。
 */
public final class BountyHunterDeathHandler {
    private static boolean initialized = false;

    private BountyHunterDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerAfterAttempt(
                NoellesRolesCore.id("bounty_hunter_kill_reward"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH + 10,
                context -> {
                    /*
                     * 赏金奖励要监听“任意方式击杀悬赏目标”，不能只绑在赏金枪上。
                     * 因此这里放在 DeathApi 的 afterAttempt，并且必须检查 confirmedDeath()：
                     * 被护盾、免死或致死转化拦下的攻击都不算完成悬赏。
                     */
                    if (!context.confirmedDeath() || context.serverVictim() == null) {
                        return;
                    }

                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.serverVictim().getWorld());
                    UUID deceasedUuid = context.serverVictim().getUuid();

                    /*
                     * 换目标和发奖励是两件不同的事：
                     * 1. 只有赏金猎人亲手击杀自己的当前悬赏目标，才获得赏金金币；
                     * 2. 只要当前目标已经确认死亡，无论凶手是谁、是否存在凶手，都必须立即换目标。
                     *
                     * 这里的优先级略高于时停者启动时间狭缝的处理器。即使以后 bootstrap 注册顺序调整，
                     * 也会先消费这次 confirmedDeath，再把死者转成特殊存活旁观，避免重新依赖注册先后。
                     */
                    for (UUID bountyHunterUuid : gameWorld.getAllWithRole(NoellesRoleRegistry.BOUNTY_HUNTER)) {
                        if (!(context.serverVictim().getWorld().getPlayerByUuid(bountyHunterUuid)
                                instanceof ServerPlayerEntity bountyHunter)) {
                            continue;
                        }

                        BountyHunterPlayerComponent component = BountyHunterPlayerComponent.KEY.get(bountyHunter);
                        if (!component.isCurrentBountyTarget(context.victim())) {
                            continue;
                        }

                        if (context.killer() instanceof ServerPlayerEntity killer
                                && killer.getUuid().equals(bountyHunter.getUuid())
                                && !deceasedUuid.equals(bountyHunter.getUuid())) {
                            PlayerShopComponent.KEY.get(bountyHunter)
                                    .addToBalance(BountyHunterConstants.BOUNTY_REWARD_COINS);
                        }
                        component.retargetAfterConfirmedDeath(deceasedUuid);
                    }
                }
        );
    }
}
