package org.agmas.noellesroles.roles.executioner;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.config.HarpyModLoaderConfig;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

/**
 * 刽子手目标死亡时的身份转换。
 */
public final class ExecutionerDeathHandler {
    /**
     * 是否要求仇杀目标真正完成死亡流程后，仇杀客才能转职。
     *
     * <p>默认关闭，方便调试死亡尝试、护盾和免死链：</p>
     * <p>false：只要目标进入一次死亡尝试，就沿用旧行为立即转职，即使后续死亡被保护拦下；</p>
     * <p>true：只有 {@link dev.doctor4t.wathe.api.death.DeathContext#confirmedDeath()} 为 true 才转职。</p>
     */
    public static final boolean REQUIRE_CONFIRMED_TARGET_DEATH_FOR_TRANSFORMATION = false;

    private static boolean initialized = false;

    private ExecutionerDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerBeforeAttempt(
                NoellesRolesCore.id("executioner_confirm"),
                DeathApi.DEFAULT_PRIORITY,
                context -> {
                    if (!REQUIRE_CONFIRMED_TARGET_DEATH_FOR_TRANSFORMATION) {
                        handleTargetDeath(context);
                    }
                }
        );
        DeathApi.registerAfterAttempt(
                NoellesRolesCore.id("executioner_retarget_after_confirmed_death"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH + 10,
                context -> {
                    /*
                     * 严格模式把转职延后到 Wathe 明确确认死亡之后；默认调试模式则已在
                     * beforeAttempt 完成转职。两个分支共用同一套目标/凶手校验，避免行为漂移。
                     */
                    if (REQUIRE_CONFIRMED_TARGET_DEATH_FOR_TRANSFORMATION && context.confirmedDeath()) {
                        handleTargetDeath(context);
                    }
                    retargetAfterConfirmedDeath(context);
                }
        );
    }

    private static void retargetAfterConfirmedDeath(dev.doctor4t.wathe.api.death.DeathContext context) {
        if (!context.confirmedDeath() || context.serverVictim() == null) {
            return;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.serverVictim().getWorld());
        for (UUID uuid : gameWorld.getAllWithRole(NoellesRoleRegistry.EXECUTIONER)) {
            PlayerEntity executioner = context.serverVictim().getWorld().getPlayerByUuid(uuid);
            if (executioner instanceof ServerPlayerEntity serverExecutioner) {
                ExecutionerPlayerComponent.KEY.get(serverExecutioner)
                        .retargetAfterConfirmedDeath(context.serverVictim().getUuid());
            }
        }
    }

    private static void handleTargetDeath(dev.doctor4t.wathe.api.death.DeathContext context) {
        if (context.victim().getWorld().isClient()) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.victim().getWorld());
        for (UUID uuid : gameWorld.getAllWithRole(NoellesRoleRegistry.EXECUTIONER)) {
            PlayerEntity executioner = context.victim().getWorld().getPlayerByUuid(uuid);
            if (!(executioner instanceof ServerPlayerEntity serverExecutioner)) {
                continue;
            }

            boolean invalidKill = false;
            if (context.killer() instanceof ServerPlayerEntity) {
                Role killerRole = gameWorld.getRole(context.killer());
                /*
                 * 旧 ExecutionerConfirmMixin 的限制：
                 * 目标若是被真正杀手能力角色杀死，不算仇杀客完成目标。
                 * 这样仇杀客不能靠杀手队友顺手击杀目标来白拿转职。
                 */
                invalidKill = killerRole != null && killerRole.canUseKiller();
            }

            ExecutionerPlayerComponent executionerComponent = ExecutionerPlayerComponent.KEY.get(serverExecutioner);
            PlayerShopComponent shop = PlayerShopComponent.KEY.get(serverExecutioner);
            if (!executionerComponent.target.equals(context.victim().getUuid()) || invalidKill) {
                continue;
            }

            executionerComponent.won = true;
            /*
             * 目标死亡后，仇杀客立刻转成一个当前未禁用的杀手职业，并刷新职业分配事件。
             * 这里沿用 beforeAttempt 阶段，是为了保留旧 mixin 在死亡请求入口完成转职的时机；
             * 后续 Wathe 记录死亡和结算时，仇杀客已经被视作新的杀手阵营成员。
             */
            ArrayList<Role> shuffledKillerRoles = new ArrayList<>(WatheRoles.ROLES);
            shuffledKillerRoles.removeIf(role -> Harpymodloader.VANNILA_ROLES.contains(role)
                    || !role.canUseKiller()
                    || HarpyModLoaderConfig.HANDLER.instance().disabled.contains(role.identifier().toString()));
            if (shuffledKillerRoles.isEmpty()) {
                shuffledKillerRoles.add(WatheRoles.KILLER);
            }
            Collections.shuffle(shuffledKillerRoles);

            Role newRole = shuffledKillerRoles.getFirst();
            gameWorld.addRole(executioner, newRole);
            ModdedRoleAssigned.EVENT.invoker().assignModdedRole(executioner, newRole);
            shop.setBalance(200);
            PlayerPoisonComponent.KEY.get(executioner).reset();
            if (Harpymodloader.VANNILA_ROLES.contains(gameWorld.getRole(executioner))) {
                ServerPlayNetworking.send(serverExecutioner, new AnnounceWelcomePayload(
                        RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(WatheRoles.KILLER),
                        gameWorld.getAllKillerTeamPlayers().size(),
                        0
                ));
            } else {
                ServerPlayNetworking.send(serverExecutioner, new AnnounceWelcomePayload(
                        RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(Harpymodloader.autogeneratedAnnouncements.get(gameWorld.getRole(executioner))),
                        gameWorld.getAllKillerTeamPlayers().size(),
                        0
                ));
            }
        }
    }
}
