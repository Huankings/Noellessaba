package org.agmas.noellesroles.roles.bounty_hunter;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

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
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH,
                context -> {
                    /*
                     * 赏金奖励要监听“任意方式击杀悬赏目标”，不能只绑在赏金枪上。
                     * 因此这里放在 DeathApi 的 afterAttempt，并且必须检查 confirmedDeath()：
                     * 被护盾、免死或致死转化拦下的攻击都不算完成悬赏。
                     */
                    if (!context.confirmedDeath()
                            || !(context.killer() instanceof ServerPlayerEntity bountyHunter)
                            || context.victim().getUuid().equals(bountyHunter.getUuid())) {
                        return;
                    }

                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(bountyHunter.getWorld());
                    if (!gameWorld.isRole(bountyHunter, NoellesRoleRegistry.BOUNTY_HUNTER)) {
                        return;
                    }

                    BountyHunterPlayerComponent component = BountyHunterPlayerComponent.KEY.get(bountyHunter);
                    if (component.isCurrentBountyTarget(context.victim())) {
                        // 目标仍由组件维护，奖励只在“当前悬赏目标”真正死亡后发放一次。
                        PlayerShopComponent.KEY.get(bountyHunter).addToBalance(BountyHunterConstants.BOUNTY_REWARD_COINS);
                    }
                }
        );
    }
}
