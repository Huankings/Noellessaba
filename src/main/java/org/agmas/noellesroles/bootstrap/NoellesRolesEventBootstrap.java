package org.agmas.noellesroles.bootstrap;

import org.agmas.noellesroles.registry.NoellesRoleIds;
import dev.doctor4t.wathe.api.event.AllowPlayerPunching;
import dev.doctor4t.wathe.api.event.CanSeePoison;
import dev.doctor4t.wathe.api.event.ShouldDropOnDeath;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.config.HarpyModLoaderConfig;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.combat.NoellesRolesCombatBootstrap;
import org.agmas.noellesroles.death.NoellesRolesDeathBootstrap;
import org.agmas.noellesroles.modifiers.allergic.AllergicModifierHandler;
import org.agmas.noellesroles.modifiers.violator.ViolatorConstants;
import org.agmas.noellesroles.roleassign.NoellesRolesRoleAssignedBootstrap;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.angel.AngelConstants;
import org.agmas.noellesroles.roles.avaricious.AvariciousPayoutHandler;
import org.agmas.noellesroles.roles.cook.CookConstants;
import org.agmas.noellesroles.roles.cook.CookPsychoHandler;
import org.agmas.noellesroles.roles.coward.CowardPlayerComponent;
import org.agmas.noellesroles.roles.hacker.HackerConstants;
import org.agmas.noellesroles.roles.jason.JasonAbilityManager;
import org.agmas.noellesroles.roles.jason.JasonAbilityPlayerComponent;
import org.agmas.noellesroles.roles.jason.JasonWoundManager;
import org.agmas.noellesroles.roles.licensed_villain.LicensedVillainConstants;
import org.agmas.noellesroles.roles.licensed_villain.LicensedVillainMomentManager;
import org.agmas.noellesroles.roles.mimic.MimicConstants;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterConstants;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterManager;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperRiftHandler;
import org.agmas.noellesroles.roles.swapper.SwapperAbility;
import org.agmas.noellesroles.roles.outlaw.OutlawManager;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenManager;
import org.agmas.noellesroles.roles.party_animal.PartyAnimalAbility;

/**
 * NoellesRoles 的事件和生命周期总引导器。
 *
 * <p>这里负责把原先入口类中的各种事件监听拆到独立方法里，
 * 但仍然保持统一注册，避免监听器顺序意外变化。</p>
 */
public final class NoellesRolesEventBootstrap {
    private static boolean initialized = false;
    private NoellesRolesEventBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        NoellesRolesDeathBootstrap.init();
        NoellesRolesCombatBootstrap.init();
        JasonWoundManager.init();
        JasonAbilityManager.init();
        AllergicModifierHandler.init();
        AvariciousPayoutHandler.init();
        registerCombatAndStateEvents();
        NoellesRolesRoleAssignedBootstrap.init();
        registerServerTickEvents();
        applyHarpyDisabledRoles();
    }

    private static void registerCombatAndStateEvents() {
        AllowPlayerPunching.EVENT.register((playerEntity, playerEntity1) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(playerEntity.getWorld());
            return (gameWorldComponent.isRole(playerEntity, NoellesRoleRegistry.MIMIC) && playerEntity.getMainHandStack().isOf(ModItems.FAKE_KNIFE))
                    || (gameWorldComponent.isRole(playerEntity, NoellesRoleRegistry.ASSASSIN) && playerEntity.getMainHandStack().isOf(ModItems.BAYONET))
                    || playerEntity.getMainHandStack().isOf(ModItems.HUNTING_KNIFE);
        });
        CanSeePoison.EVENT.register(player -> GameWorldComponent.KEY.get(player.getWorld()).isRole((PlayerEntity) player, NoellesRoleRegistry.BARTENDER));
        ShouldDropOnDeath.EVENT.register((itemStack, identifier) -> itemStack.isOf(ModItems.MASTER_KEY));
    }

    private static void registerServerTickEvents() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
                /*
                 * 时间狭缝是“时停者仍能回溯”的临时窗口，而不是永久死亡保护。
                 * 每个服务端 tick 都重新检查：
                 * 1. 时停者是否仍存活且持有可用怀表；
                 * 2. 狭缝玩家是否已经成为胜利结算唯一阻碍。
                 * 这样最后一个杀手/独立阻拦者死亡时不会被 30 秒狭缝拖住结算，
                 * 时停者自己在窗口内死亡时，狭缝玩家也会马上回到普通死亡旁观和死亡语音频道。
                 */
                TimekeeperRiftHandler.tickActiveRifts(world);
                OutlawManager.tickWorld(world);
                ShadowJesterManager.tickWorld(world);
                /* 先让影子小丑完成阶段重算，再判断低优先级的执照恶棍时刻是否可以进入。 */
                LicensedVillainMomentManager.tickWorld(world);
                KillerQueenManager.tickWorld(world);
                for (ServerPlayerEntity player : world.getPlayers()) {
                    PartyAnimalAbility.tick(player);
                    float moodDrainMultiplier;
                    if (gameWorld.isRole(player, NoellesRoleRegistry.ANGEL)) {
                        moodDrainMultiplier = AngelConstants.MOOD_DRAIN_MULTIPLIER;
                    } else if (gameWorld.isRole(player, NoellesRoleRegistry.COWARD)) {
                        moodDrainMultiplier = CowardPlayerComponent.KEY.get(player).getCurrentSanMultiplier();
                    } else {
                        moodDrainMultiplier = 1.0f;
                    }
                    /*
                     * 无恶不在解除后的惊吓是临时状态，不应该覆盖天使、懦夫等已有职业倍率，
                     * 而是乘在当前基础倍率之后。这样“掉 san 速度为原来 4 倍”能和其它机制自然叠加。
                     */
                    if (GameFunctions.isPlayerAliveAndSurvival(player)
                            && JasonAbilityPlayerComponent.KEY.get(player).getScaredTicks() > 0) {
                        moodDrainMultiplier *= org.agmas.noellesroles.roles.jason.JasonConstants.ABILITY_SCARE_MOOD_DRAIN_MULTIPLIER;
                    }
                    /*
                     * 厨师疯魔期间用户要求不掉心情。
                     * 这里乘以 0 而不是直接写死 0 前面的分支，是为了保留天使/懦夫/惊吓等机制的组合顺序：
                     * 只要厨师疯魔存在，最终掉心情倍率就归零。
                     */
                    if (CookPsychoHandler.isCookPsychoActive(player)) {
                        moodDrainMultiplier *= CookConstants.PSYCHO_COOK_MOOD_DRAIN_MULTIPLIER;
                    }
                    PlayerMoodComponent.KEY.get(player).setMoodDrainMultiplier(moodDrainMultiplier);
                }

                SwapperAbility.tickPendingSwaps(server);

                for (EnderPearlEntity pearl : world.getEntitiesByType(EntityType.ENDER_PEARL, entity -> true)) {
                    if (pearl.getCommandTags().contains("noellesroles_replay_recorded")) {
                        continue;
                    }
                    if (!(pearl.getOwner() instanceof net.minecraft.server.network.ServerPlayerEntity owner)) {
                        continue;
                    }
                    if (!gameWorld.isRole(owner, NoellesRoleRegistry.RECALLER)) {
                        continue;
                    }
                    dev.doctor4t.wathe.record.GameRecordManager.recordGlobalEvent(world, NoellesEventIds.RECALLER_ENDER_PEARL_THROWN_EVENT, owner, null);
                    pearl.addCommandTag("noellesroles_replay_recorded");
                }

                for (var windCharge : world.getEntitiesByType(EntityType.WIND_CHARGE, entity -> true)) {
                    if (windCharge.getCommandTags().contains("noellesroles_replay_recorded")) {
                        continue;
                    }
                    if (!(windCharge instanceof net.minecraft.entity.projectile.ProjectileEntity projectile)) {
                        continue;
                    }
                    if (!(projectile.getOwner() instanceof net.minecraft.server.network.ServerPlayerEntity owner)) {
                        continue;
                    }
                    if (!gameWorld.isRole(owner, NoellesRoleRegistry.WINDER)) {
                        continue;
                    }
                    dev.doctor4t.wathe.record.GameRecordManager.recordGlobalEvent(world, NoellesEventIds.WINDER_WIND_CHARGE_USED_EVENT, owner, null);
                    windCharge.addCommandTag("noellesroles_replay_recorded");
                }
            }

            if (server.getPlayerManager().getCurrentPlayerCount() >= 8) {
                Harpymodloader.setRoleMaximum(NoellesRoleRegistry.VULTURE, 1);
            } else {
                Harpymodloader.setRoleMaximum(NoellesRoleRegistry.VULTURE, 0);
            }
            if (server.getPlayerManager().getCurrentPlayerCount() >= HackerConstants.PLAYER_LIMIT) {
                Harpymodloader.setRoleMaximum(NoellesRoleRegistry.HACKER, 1);
            } else {
                Harpymodloader.setRoleMaximum(NoellesRoleRegistry.HACKER, 0);
            }
            /*
             * 执照恶棍沿用 kinssaba 原逻辑：按当前在线人数决定是否进入随机池。
             * 这里不使用 ready player count，是为了避免迁移后生成门槛和原 mod 出现细微漂移。
             */
            if (server.getPlayerManager().getCurrentPlayerCount() >= LicensedVillainConstants.MIN_PLAYER_COUNT) {
                Harpymodloader.setRoleMaximum(NoellesRoleRegistry.LICENSED_VILLAIN, 1);
            } else {
                Harpymodloader.setRoleMaximum(NoellesRoleRegistry.LICENSED_VILLAIN, 0);
            }

            ServerWorld overworld = server.getOverworld();
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(overworld);
            int killerSlots = (int) Math.floor((float) dev.doctor4t.wathe.game.GameFunctions.getReadyPlayerCount(overworld) / (float) gameWorld.getKillerDividend());
            Harpymodloader.setRoleMaximum(NoellesRoleRegistry.SHADOW_JESTER, killerSlots >= ShadowJesterConstants.MIN_KILLER_SLOTS_FOR_PAIR ? ShadowJesterConstants.MAX_RANDOM_PRIMARY_COUNT : 0);
            Harpymodloader.setRoleMaximum(NoellesRoleRegistry.DRUGMAKER, killerSlots >= org.agmas.noellesroles.roles.drugmaker.DrugmakerConstants.MIN_KILLER_COUNT ? 1 : 0);
            Harpymodloader.setRoleMaximum(NoellesRoleRegistry.MIMIC, killerSlots >= MimicConstants.MIMIC_MIN_KILLER_COUNT ? 1 : 0);


            int vigilanteSlots = 0;
            if (!server.getPlayerManager().getPlayerList().isEmpty()) {
                vigilanteSlots = (int) Math.floor((float) server.getPlayerManager().getCurrentPlayerCount() / (float) gameWorld.getVigilanteDividend());
            }
            Harpymodloader.setRoleMaximum(NoellesRoleRegistry.BETTER_VIGILANTE, vigilanteSlots >= 4 ? 1 : 0);
        });
    }

    private static void applyHarpyDisabledRoles() {
        boolean disableShitpostRoles = !NoellesRolesConfig.HANDLER.instance().shitpostRoles;
        boolean disableViolator = ViolatorConstants.DEFAULT_DISABLE_AUTO_GENERATION;
        if (!disableShitpostRoles && !disableViolator) {
            return;
        }

        /*
         * Harpy 的 disabled / disabledModifiers 是持久配置，必须先 load 再追加。
         * 这里用“有变更才 save”的方式，避免每次启动都无意义重写用户配置文件。
         */
        HarpyModLoaderConfig.HANDLER.load();
        boolean changed = false;

        if (disableShitpostRoles) {
            changed |= addDisabledRole(NoellesRoleIds.AWESOME_BINGLUS_ID.getPath());
            changed |= addDisabledRole(NoellesRoleIds.BETTER_VIGILANTE_ID.getPath());
            changed |= addDisabledRole(NoellesRoleIds.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES_ID.getPath());
        }

        if (disableViolator) {
            /*
             * 违禁者是词条，不是职业；Harpy 对词条禁用表存完整 Identifier，
             * 因此这里不能像职业禁用一样只写 path。
             */
            changed |= addDisabledModifier(NoellesRoleIds.VIOLATOR_ID.toString());
        }

        if (changed) {
            HarpyModLoaderConfig.HANDLER.save();
        }
    }

    private static boolean addDisabledRole(String rolePath) {
        if (HarpyModLoaderConfig.HANDLER.instance().disabled.contains(rolePath)) {
            return false;
        }
        HarpyModLoaderConfig.HANDLER.instance().disabled.add(rolePath);
        return true;
    }

    private static boolean addDisabledModifier(String modifierId) {
        if (HarpyModLoaderConfig.HANDLER.instance().disabledModifiers.contains(modifierId)) {
            return false;
        }
        HarpyModLoaderConfig.HANDLER.instance().disabledModifiers.add(modifierId);
        return true;
    }
}
