package org.agmas.noellesroles.bootstrap;

import org.agmas.noellesroles.registry.NoellesRoleIds;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.economy.EconomyApi;
import dev.doctor4t.wathe.api.event.AllowPlayerPunching;
import dev.doctor4t.wathe.api.event.CanSeePoison;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.api.event.ShouldDropOnDeath;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.config.HarpyModLoaderConfig;
import org.agmas.harpymodloader.events.ModifierAssigned;
import org.agmas.harpymodloader.events.ResetPlayerEvent;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.combat.NoellesRolesCombatBootstrap;
import org.agmas.noellesroles.death.NoellesRolesDeathBootstrap;
import org.agmas.noellesroles.framing.DelusionPlayerComponent;
import org.agmas.noellesroles.modifiers.allergic.AllergicModifierHandler;
import org.agmas.noellesroles.modifiers.violator.ViolatorConstants;
import org.agmas.noellesroles.roleassign.NoellesRolesRoleAssignedBootstrap;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesFramingShopEntries;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.angel.AngelConstants;
import org.agmas.noellesroles.roles.arsonist.ArsonistReplayTracker;
import org.agmas.noellesroles.roles.arsonist.ArsonistVictoryRule;
import org.agmas.noellesroles.roles.arsonist.DousedPlayerComponent;
import org.agmas.noellesroles.roles.arsonist.OilDousingHandler;
import org.agmas.noellesroles.roles.assassin.HiddenBodiesWorldComponent;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterPlayerComponent;
import org.agmas.noellesroles.roles.convener.ConvenerCommunicationManager;
import org.agmas.noellesroles.roles.convener.ConvenerDisguiseComponent;
import org.agmas.noellesroles.roles.convener.ConvenerMomentumComponent;
import org.agmas.noellesroles.roles.convener.ConvenerPlayerComponent;
import org.agmas.noellesroles.roles.convener.ConvenerSummonHandler;
import org.agmas.noellesroles.roles.convener.ConvenerTaskShieldHandler;
import org.agmas.noellesroles.roles.convener.ConvenerVictoryRule;
import org.agmas.noellesroles.roles.coward.CowardPlayerComponent;
import org.agmas.noellesroles.roles.coward.SedativePlayerComponent;
import org.agmas.noellesroles.roles.dreamer.DreamerComponent;
import org.agmas.noellesroles.roles.dreamer.DreamerConstants;
import org.agmas.noellesroles.roles.dreamer.DreamerDelusionHandler;
import org.agmas.noellesroles.roles.dreamer.DreamerKillerComponent;
import org.agmas.noellesroles.roles.hacker.HackerComponent;
import org.agmas.noellesroles.roles.hacker.HackerConstants;
import org.agmas.noellesroles.roles.hacker.HackerPhoneComponent;
import org.agmas.noellesroles.roles.hacker.HackerSafeTimeComponent;
import org.agmas.noellesroles.roles.hunter.HunterPlayerComponent;
import org.agmas.noellesroles.roles.insane_damned_paranoid_killer.InsaneDamnedKillerPlayerComponent;
import org.agmas.noellesroles.roles.jason.JasonAbilityManager;
import org.agmas.noellesroles.roles.jason.JasonAbilityPlayerComponent;
import org.agmas.noellesroles.roles.jason.JasonFireWorldComponent;
import org.agmas.noellesroles.roles.jason.JasonWoundManager;
import org.agmas.noellesroles.roles.jason.JasonWoundedPlayerComponent;
import org.agmas.noellesroles.roles.kidnapper.KidnapperComponent;
import org.agmas.noellesroles.roles.licensed_villain.LicensedVillainConstants;
import org.agmas.noellesroles.roles.magician.MagicianPlaybackManager;
import org.agmas.noellesroles.roles.morphling.MorphBodyDisguiseWorldComponent;
import org.agmas.noellesroles.roles.morphling.MorphlingReagentService;
import org.agmas.noellesroles.roles.morphling.MorphMarkPlayerComponent;
import org.agmas.noellesroles.roles.mimic.MimicConstants;
import org.agmas.noellesroles.roles.muzzler.MuzzlerInteractionHandler;
import org.agmas.noellesroles.roles.muzzler.SilencePlayerComponent;
import org.agmas.noellesroles.roles.necromancer.NecromancerRevivalHandler;
import org.agmas.noellesroles.roles.necromancer.NecromancerRoleLimitHandler;
import org.agmas.noellesroles.roles.operator.OperatorCommunicationManager;
import org.agmas.noellesroles.roles.phantom.PhantomPlayerComponent;
import org.agmas.noellesroles.roles.physician.PhysicianPlayerComponent;
import org.agmas.noellesroles.roles.physician.PhysicianStatusAlertHandler;
import org.agmas.noellesroles.roles.rememberer.RemembererInteractionHandler;
import org.agmas.noellesroles.roles.rememberer.RemembererSniperManager;
import org.agmas.noellesroles.roles.robot.RobotPlayerComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistCommunicationManager;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistConstants;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.agmas.noellesroles.roles.starstruck.StarstruckAbility;
import org.agmas.noellesroles.roles.starstruck.StarstruckPlayerComponent;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapAuraWorldComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperRiftHandler;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;
import org.agmas.noellesroles.roles.swapper.SwapperAbility;
import org.agmas.noellesroles.roles.waiter.WaiterInteractionHandler;
import org.agmas.noellesroles.roles.waiter.WaiterPlayerComponent;
import org.agmas.noellesroles.roles.winder.WinderPlayerComponent;
import org.jetbrains.annotations.NotNull;

/**
 * NoellesRoles 的事件和生命周期总引导器。
 *
 * <p>这里负责把原先入口类中的各种事件监听拆到独立方法里，
 * 但仍然保持统一注册，避免监听器顺序意外变化。</p>
 */
public final class NoellesRolesEventBootstrap {
    private static boolean initialized = false;
    private static final EntityAttributeModifier TINY_MODIFIER = new EntityAttributeModifier(Identifier.of(org.agmas.noellesroles.registry.NoellesRolesCore.MOD_ID, "tiny_modifier"), -0.15, EntityAttributeModifier.Operation.ADD_VALUE);

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
        registerCombatAndStateEvents();
        NoellesRolesRoleAssignedBootstrap.init();
        registerServerTickEvents();
        registerRoundCleanup();
        applyHarpyDisabledRoles();
    }

    private static void registerCombatAndStateEvents() {
        AllowPlayerPunching.EVENT.register((playerEntity, playerEntity1) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(playerEntity.getWorld());
            return (gameWorldComponent.isRole(playerEntity, NoellesRoleRegistry.MIMIC) && playerEntity.getMainHandStack().isOf(ModItems.FAKE_KNIFE))
                    || (gameWorldComponent.isRole(playerEntity, NoellesRoleRegistry.ASSASSIN) && playerEntity.getMainHandStack().isOf(ModItems.BAYONET))
                    || playerEntity.getMainHandStack().isOf(ModItems.HUNTING_KNIFE);
        });
        ModifierAssigned.EVENT.register((playerEntity, modifier) -> {
            if (modifier.equals(NoellesModifierRegistry.TINY)) {
                playerEntity.getAttributeInstance(EntityAttributes.GENERIC_SCALE).removeModifier(TINY_MODIFIER);
                playerEntity.getAttributeInstance(EntityAttributes.GENERIC_SCALE).addPersistentModifier(TINY_MODIFIER);
            }
            if (modifier.equals(NoellesModifierRegistry.FEATHER)) {
                playerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, StatusEffectInstance.INFINITE, 0, true, false));
            }
        });
        ResetPlayerEvent.EVENT.register(playerEntity -> {
            playerEntity.removeStatusEffect(StatusEffects.SLOW_FALLING);
            playerEntity.getAttributeInstance(EntityAttributes.GENERIC_SCALE).removeModifier(TINY_MODIFIER);
            DelusionPlayerComponent.KEY.get(playerEntity).reset();
            PhantomPlayerComponent.KEY.get(playerEntity).reset();
            WaiterPlayerComponent.KEY.get(playerEntity).reset();
            org.agmas.noellesroles.roles.cook.CookPlayerComponent.KEY.get(playerEntity).reset();
            PhysicianPlayerComponent.KEY.get(playerEntity).reset();
            DreamerComponent.KEY.get(playerEntity).reset();
            DreamerKillerComponent.KEY.get(playerEntity).reset();
            HackerComponent.KEY.get(playerEntity).reset();
            HackerPhoneComponent.KEY.get(playerEntity).reset();
            StarstruckPlayerComponent.KEY.get(playerEntity).reset();
            SilencePlayerComponent.KEY.get(playerEntity).reset();
            HunterPlayerComponent.KEY.get(playerEntity).reset();
            org.agmas.noellesroles.roles.drugmaker.DrugmakerPlayerComponent.KEY.get(playerEntity).reset();
            KidnapperComponent.KEY.get(playerEntity).resetAll();
            RobotPlayerComponent.KEY.get(playerEntity).reset();
            TimekeeperPlayerComponent.KEY.get(playerEntity).reset();
            DousedPlayerComponent.KEY.get(playerEntity).reset();
            DousedPlayerComponent.KEY.get(playerEntity).sync();
            ConvenerPlayerComponent.KEY.get(playerEntity).reset();
            ConvenerDisguiseComponent.KEY.get(playerEntity).clearDisguise();
            ConvenerMomentumComponent.KEY.get(playerEntity).reset();
            BountyHunterPlayerComponent.KEY.get(playerEntity).reset();
            MorphMarkPlayerComponent.KEY.get(playerEntity).clear();
            /*
             * 尸体伪装是局内临时状态，必须跟随 Harpy 的玩家重置一起清掉。
             * 这样调试重置、回合切换或其它强制重置入口不会留下“非亡语杀手仍躺尸”的同步残留。
             */
            InsaneDamnedKillerPlayerComponent.KEY.get(playerEntity).reset();
            JasonWoundManager.resetPlayer(playerEntity);
            JasonAbilityManager.resetPlayer(playerEntity);
            if (playerEntity instanceof ServerPlayerEntity serverPlayer) {
                MorphlingReagentService.clearReagentReleaseGate(serverPlayer);
            }
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
                for (ServerPlayerEntity player : world.getPlayers()) {
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
            Harpymodloader.setRoleMaximum(NoellesRoleRegistry.DRUGMAKER, killerSlots >= org.agmas.noellesroles.roles.drugmaker.DrugmakerConstants.MIN_KILLER_COUNT ? 1 : 0);
            Harpymodloader.setRoleMaximum(NoellesRoleRegistry.MIMIC, killerSlots >= MimicConstants.MIMIC_MIN_KILLER_COUNT ? 1 : 0);


            int vigilanteSlots = 0;
            if (!server.getPlayerManager().getPlayerList().isEmpty()) {
                vigilanteSlots = (int) Math.floor((float) server.getPlayerManager().getCurrentPlayerCount() / (float) gameWorld.getVigilanteDividend());
            }
            Harpymodloader.setRoleMaximum(NoellesRoleRegistry.BETTER_VIGILANTE, vigilanteSlots >= 4 ? 1 : 0);
        });
    }

    private static void registerRoundCleanup() {
        GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }
            SwapperAbility.clearPendingSwaps();
            MorphlingReagentService.clearAllReleaseGates();
            MorphBodyDisguiseWorldComponent.KEY.get(serverWorld).reset();
            HiddenBodiesWorldComponent.KEY.get(serverWorld).reset();
            SpringTrapAuraWorldComponent.KEY.get(serverWorld).reset();
            TimekeeperWorldComponent.KEY.get(serverWorld).reset();
            JasonFireWorldComponent.KEY.get(serverWorld).reset();
            JasonWoundManager.resetRoundTransientState();
            JasonAbilityManager.resetRoundTransientState(serverWorld);
            MagicianPlaybackManager.cleanupAllPlaybackEntities(serverWorld);

            for (org.agmas.noellesroles.entities.ThrowingAxeEntity entity : serverWorld.getEntitiesByType(TypeFilter.equals(org.agmas.noellesroles.entities.ThrowingAxeEntity.class), ignored -> true)) {
                entity.discard();
            }
            for (org.agmas.noellesroles.roles.jason.JasonThrownWeaponEntity entity : serverWorld.getEntitiesByType(TypeFilter.equals(org.agmas.noellesroles.roles.jason.JasonThrownWeaponEntity.class), ignored -> true)) {
                entity.discard();
            }
            for (org.agmas.noellesroles.entities.RoleMineEntity entity : serverWorld.getEntitiesByType(TypeFilter.equals(org.agmas.noellesroles.entities.RoleMineEntity.class), ignored -> true)) {
                entity.discard();
            }
            for (org.agmas.noellesroles.entities.CaptureDeviceEntity entity : serverWorld.getEntitiesByType(TypeFilter.equals(org.agmas.noellesroles.entities.CaptureDeviceEntity.class), ignored -> true)) {
                entity.discard();
            }
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
