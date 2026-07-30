package org.agmas.noellesroles.roles.voodoo;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.death.DeathProcessComponent;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 巫毒师死亡时的绑定目标连锁死亡。
 */
public final class VoodooDeathHandler {
    private static boolean initialized = false;

    private VoodooDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerBeforeAttempt(
                NoellesRolesCore.id("voodoo_bound_death"),
                DeathApi.PRIORITY_DEATH_PROCESS_STATE - 200,
                context -> {
                    /*
                     * 巫毒连锁保留旧配置语义：
                     * 默认只有“有 killer 的死亡”会带走绑定目标；
                     * 开启 voodooNonKillerDeaths 后，坠车/毒药等无 killer 死亡也能触发。
                     */
                    if (!NoellesRolesConfig.HANDLER.instance().voodooNonKillerDeaths && context.killer() == null) {
                        return;
                    }

                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.victim().getWorld());
                    if (!gameWorld.isRole(context.victim(), NoellesRoleRegistry.VOODOO)) {
                        return;
                    }

                    VoodooPlayerComponent component = VoodooPlayerComponent.KEY.get(context.victim());
                    if (component.target == null) {
                        return;
                    }

                    PlayerEntity voodooed = context.victim().getWorld().getPlayerByUuid(component.target);
                    if (voodooed == null
                            || voodooed == context.victim()
                            || !GameFunctions.isPlayerAliveAndSurvival(voodooed)
                            || DeathProcessComponent.KEY.get(voodooed).isProcessing()) {
                        return;
                    }

                    /*
                     * 只把巫毒来源写进额外回放数据；若后续被护盾或免死拦下，
                     * Wathe 不会记录假死亡回放。
                     * gameplay killer 传 null，避免把原攻击者错误记成“直接杀死绑定目标的人”。
                     */
                    NbtCompound replayDeathData = new NbtCompound();
                    replayDeathData.putUuid("replay_actor", context.victim().getUuid());
                    GameFunctions.killPlayer(voodooed, true, null, NoellesDeathReasons.VOODOO_MAGIC_DEATH_REASON, replayDeathData);
                }
        );
    }
}
