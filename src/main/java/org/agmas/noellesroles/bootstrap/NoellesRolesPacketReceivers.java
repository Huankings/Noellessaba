package org.agmas.noellesroles.bootstrap;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.packet.host.AbilityC2SPacket;
import org.agmas.noellesroles.packet.item.BayonetKnockbackC2SPacket;
import org.agmas.noellesroles.packet.item.BayonetStabC2SPacket;
import org.agmas.noellesroles.packet.item.BlowgunC2SPacket;
import org.agmas.noellesroles.packet.item.BloodAxeKnockbackC2SPacket;
import org.agmas.noellesroles.packet.item.BloodAxeStabC2SPacket;
import org.agmas.noellesroles.packet.item.ColorfulAxeAttackC2SPacket;
import org.agmas.noellesroles.packet.item.DelusionSyringeC2SPacket;
import org.agmas.noellesroles.packet.item.CrystalBallMarkC2SPacket;
import org.agmas.noellesroles.packet.item.HuntingKnifeC2SPacket;
import org.agmas.noellesroles.packet.item.PanC2SPacket;
import org.agmas.noellesroles.packet.item.SniperRifleShootC2SPacket;
import org.agmas.noellesroles.packet.modifiers.GuessC2SPacket;
import org.agmas.noellesroles.packet.modifiers.dual_personality.DualPersonalitySwitchC2SPacket;
import org.agmas.noellesroles.packet.modifiers.dual_personality.DualPersonalitySwitchKeyLabelC2SPacket;
import org.agmas.noellesroles.packet.role.brainwasher.BrainwasherC2SPacket;
import org.agmas.noellesroles.packet.role.controller.ControllerPossessC2SPacket;
import org.agmas.noellesroles.packet.role.controller.ControllerReleaseC2SPacket;
import org.agmas.noellesroles.packet.role.convener.ConvenerMorphC2SPacket;
import org.agmas.noellesroles.packet.role.corpsemaker.CorpsemakerC2SPacket;
import org.agmas.noellesroles.packet.role.goddess.GoddessC2SPacket;
import org.agmas.noellesroles.packet.role.morphling.MorphC2SPacket;
import org.agmas.noellesroles.packet.role.noisemaker.NoisemakerGlowC2SPacket;
import org.agmas.noellesroles.packet.role.operator.OperatorC2SPacket;
import org.agmas.noellesroles.packet.role.spiritualist.SpiritualistPossessionControlC2SPacket;
import org.agmas.noellesroles.packet.role.stalker.StalkerDashC2SPacket;
import org.agmas.noellesroles.packet.role.stalker.StalkerGazeC2SPacket;
import org.agmas.noellesroles.packet.role.swapper.SwapperC2SPacket;
import org.agmas.noellesroles.packet.role.timekeeper.TimekeeperWatchModeC2SPacket;
import org.agmas.noellesroles.packet.role.vulture.VultureEatC2SPacket;
import org.agmas.noellesroles.roles.angel.AngelAbility;
import org.agmas.noellesroles.roles.bellringer.BellringerAbility;
import org.agmas.noellesroles.roles.brainwasher.BrainwasherAbility;
import org.agmas.noellesroles.roles.cleaner.CleanerAbility;
import org.agmas.noellesroles.roles.controller.ControllerPossessAbility;
import org.agmas.noellesroles.roles.controller.ControllerReleaseAbility;
import org.agmas.noellesroles.roles.coroner.CoronerMorphAbility;
import org.agmas.noellesroles.roles.corpsemaker.CorpsemakerAbility;
import org.agmas.noellesroles.roles.detective.DetectiveAbility;
import org.agmas.noellesroles.roles.goddess.GoddessAbility;
import org.agmas.noellesroles.roles.hunter.HunterAbility;
import org.agmas.noellesroles.roles.insane_damned_paranoid_killer.InsaneDamnedKillerAbility;
import org.agmas.noellesroles.roles.jason.JasonAbilityManager;
import org.agmas.noellesroles.roles.jason.JasonWoundManager;
import org.agmas.noellesroles.roles.magician.MagicianTargetAbility;
import org.agmas.noellesroles.roles.morphling.MorphlingMorphAbility;
import org.agmas.noellesroles.roles.operator.OperatorAbility;
import org.agmas.noellesroles.roles.phantom.PhantomAbility;
import org.agmas.noellesroles.roles.prophet.ProphetAbility;
import org.agmas.noellesroles.roles.recaller.RecallerAbility;
import org.agmas.noellesroles.roles.robot.RobotAbility;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterManager;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistAbility;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistManager;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.agmas.noellesroles.roles.stalker.StalkerAbility;
import org.agmas.noellesroles.roles.starstruck.StarstruckAbility;
import org.agmas.noellesroles.roles.swapper.SwapperAbility;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperAbility;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.agmas.noellesroles.roles.vulture.VultureAbility;
import org.agmas.noellesroles.roles.waiter.WaiterPlayerComponent;
import org.agmas.noellesroles.roles.winder.WinderAbility;
import org.agmas.noellesroles.roles.winder.WinderTargetAbility;
import org.agmas.noellesroles.roles.voodoo.VoodooTargetAbility;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityManager;

/**
 * NoellesRoles 所有网络包接收器的注册中心。
 *
 * <p>这里保留旧的“按包类型分发到各职业能力”的语义，只是把注册位置从入口类搬出来。</p>
 */
public final class NoellesRolesPacketReceivers {
    private NoellesRolesPacketReceivers() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(StalkerGazeC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            if (shouldBlockTimeRiftAction(context.player())) {
                return;
            }
            StalkerAbility.handleGaze(payload, context.player());
        }));
        ServerPlayNetworking.registerGlobalReceiver(StalkerDashC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            if (shouldBlockTimeRiftAction(context.player())) {
                return;
            }
            StalkerAbility.handleDash(payload, context.player());
        }));
        ServerPlayNetworking.registerGlobalReceiver(NoisemakerGlowC2SPacket.ID,
                (packet, context) -> context.server().execute(() -> {
                    if (shouldBlockTimeRiftAction(context.player())) {
                        return;
                    }
                    NoisemakerGlowC2SPacket.handle(packet, context.player().networkHandler);
                }));
        ServerPlayNetworking.registerGlobalReceiver(CrystalBallMarkC2SPacket.ID, (payload, context) -> context.server().execute(() -> ProphetAbility.handleCrystalBallMark(context.player(), payload.targetId(), payload.offHand())));
        ServerPlayNetworking.registerGlobalReceiver(BayonetKnockbackC2SPacket.ID, new BayonetKnockbackC2SPacket.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(BayonetStabC2SPacket.ID, new BayonetStabC2SPacket.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(BloodAxeKnockbackC2SPacket.ID, new BloodAxeKnockbackC2SPacket.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(BloodAxeStabC2SPacket.ID, new BloodAxeStabC2SPacket.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(ColorfulAxeAttackC2SPacket.ID, new ColorfulAxeAttackC2SPacket.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(DelusionSyringeC2SPacket.ID, new DelusionSyringeC2SPacket.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(HuntingKnifeC2SPacket.ID, new HuntingKnifeC2SPacket.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(PanC2SPacket.ID, new PanC2SPacket.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(SniperRifleShootC2SPacket.ID, SniperRifleShootC2SPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(ConvenerMorphC2SPacket.ID, (payload, context) -> {
            /*
             * 头像点击只是客户端展示，真正能否切换伪装必须在服务端二次校验。
             */
            context.server().execute(() -> {
                if (shouldBlockTimeRiftAction(context.player())) {
                    return;
                }
                ConvenerMorphC2SPacket.handle(payload, context.player());
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(GoddessC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            if (shouldBlockTimeRiftAction(context.player())) {
                return;
            }
            GoddessAbility.handle(payload, context.player());
        }));
        ServerPlayNetworking.registerGlobalReceiver(BrainwasherC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            if (shouldBlockTimeRiftAction(context.player())) {
                return;
            }
            BrainwasherAbility.handle(payload, context.player());
        }));
        ServerPlayNetworking.registerGlobalReceiver(CorpsemakerC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            if (shouldBlockTimeRiftAction(context.player())) {
                return;
            }
            CorpsemakerAbility.handle(payload, context.player());
        }));
        ServerPlayNetworking.registerGlobalReceiver(ControllerPossessC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            if (shouldBlockTimeRiftAction(context.player())) {
                return;
            }
            ControllerPossessAbility.handle(payload, context.player());
        }));
        ServerPlayNetworking.registerGlobalReceiver(ControllerReleaseC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            if (shouldBlockTimeRiftAction(context.player())) {
                return;
            }
            ControllerReleaseAbility.handle(payload, context.player());
        }));
        ServerPlayNetworking.registerGlobalReceiver(SpiritualistPossessionControlC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            var player = context.player();
            if (shouldBlockTimeRiftAction(player)) {
                return;
            }
            SpiritualistPlayerComponent spiritualist = SpiritualistPlayerComponent.KEY.get(player);
            if (!spiritualist.isPossessing()) {
                return;
            }

            boolean wasUsing = spiritualist.possessionUsing;
            spiritualist.updatePossessionControl(
                    payload.forward(),
                    payload.sideways(),
                    payload.yaw(),
                    payload.pitch(),
                    payload.jumping(),
                    payload.sneaking(),
                    payload.sprinting(),
                    payload.using(),
                    payload.attacking()
            );

            if (!wasUsing && payload.using()) {
                var host = SpiritualistManager.getCurrentPossessionTarget(player);
                if (host != null) {
                    SpiritualistManager.handleImmediatePossessionUse(player, host);
                }
            }
        }));

        ServerPlayNetworking.registerGlobalReceiver(MorphC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            var player = context.player();
            if (shouldBlockTimeRiftAction(player)) {
                return;
            }
            var world = player.getWorld();
            var gameWorld = GameWorldComponent.KEY.get(world);
            if (gameWorld.isRole(player, NoellesRoleRegistry.CORONER)) {
                CoronerMorphAbility.handle(payload, player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.VOODOO)) {
                VoodooTargetAbility.handle(payload, player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.WINDER)) {
                WinderTargetAbility.handle(payload, player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.MAGICIAN)) {
                MagicianTargetAbility.handle(payload, player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.MORPHLING)) {
                MorphlingMorphAbility.handle(payload, player);
            }
        }));
        ServerPlayNetworking.registerGlobalReceiver(VultureEatC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            if (shouldBlockTimeRiftAction(context.player())) {
                return;
            }
            VultureAbility.handle(payload, context.player());
        }));
        ServerPlayNetworking.registerGlobalReceiver(SwapperC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            if (shouldBlockTimeRiftAction(context.player())) {
                return;
            }
            SwapperAbility.handle(payload, context.player());
        }));
        ServerPlayNetworking.registerGlobalReceiver(GuessC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            if (shouldBlockTimeRiftAction(context.player())) {
                return;
            }
            org.agmas.noellesroles.modifiers.guesser.GuesserAbility.handle(payload, context.player());
        }));
        ServerPlayNetworking.registerGlobalReceiver(DualPersonalitySwitchC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            if (shouldBlockTimeRiftAction(context.player())) {
                return;
            }
            DualPersonalityManager.requestEarlySwitch(context.player());
        }));
        ServerPlayNetworking.registerGlobalReceiver(DualPersonalitySwitchKeyLabelC2SPacket.ID, (payload, context) -> context.server().execute(() -> DualPersonalityManager.updateSwitchKeyLabel(context.player().getUuid(), payload.keyLabel())));
        ServerPlayNetworking.registerGlobalReceiver(OperatorC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            if (shouldBlockTimeRiftAction(context.player())) {
                return;
            }
            OperatorAbility.handle(payload, context.player());
        }));
        ServerPlayNetworking.registerGlobalReceiver(BlowgunC2SPacket.ID, new BlowgunC2SPacket.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(TimekeeperWatchModeC2SPacket.ID, (payload, context) -> context.server().execute(() -> TimekeeperAbility.switchWatchMode(context.player(), payload.modeOrdinal())));
        ServerPlayNetworking.registerGlobalReceiver(AbilityC2SPacket.ID, (payload, context) -> context.server().execute(() -> {
            var player = context.player();
            if (shouldBlockTimeRiftAction(player)) {
                return;
            }
            var gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, NoellesRoleRegistry.RECALLER)) {
                RecallerAbility.handle(player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.ANGEL)) {
                AngelAbility.handle(player, payload.targetId());
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.PROPHET)) {
                ProphetAbility.handle(player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.BELLRINGER)) {
                BellringerAbility.handle(player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.DETECTIVE)) {
                DetectiveAbility.handle(player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.PHANTOM)) {
                PhantomAbility.handle(player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.WINDER)) {
                WinderAbility.handle(player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.MAGICIAN)) {
                org.agmas.noellesroles.roles.magician.MagicianAbility.handle(player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.SPIRITUALIST)) {
                SpiritualistAbility.handle(player, payload.targetId());
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.STARSTRUCK)) {
                StarstruckAbility.handle(player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.CLEANER)) {
                CleanerAbility.handle(player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.HUNTER)) {
                HunterAbility.handle(player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.ROBOT)) {
                RobotAbility.handle(player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES)) {
                InsaneDamnedKillerAbility.handle(player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.TIMEKEEPER)) {
                TimekeeperAbility.handleRepairOrUpgrade(player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.JASON)) {
                JasonAbilityManager.handleAbilityKey(player);
            } else if (gameWorld.isRole(player, NoellesRoleRegistry.SHADOW_JESTER)) {
                ShadowJesterManager.handleAbilityKey(player, payload.targetId());
            }
        }));
    }

    private static boolean shouldBlockTimeRiftAction(ServerPlayerEntity player) {
        /*
         * 时间狭缝玩家处于“已经死亡但等待 30 秒内回溯”的状态。
         * 客户端会拦住 G 键和背包键，但服务端仍必须做权威兜底：
         * 1. 客户端状态同步可能晚于进入狭缝的服务端 tick；
         * 2. 玩家可能在进入狭缝前一瞬间已经发出能力/背包按钮包；
         * 3. 后续调试或其它客户端入口也可能直接发包。
         *
         * 这里只拦 NoellesRoles 的职业/词条能力包，不影响聊天指令、语音隔离和 Wathe 自己的旁观移动。
         */
        return TimekeeperPlayerComponent.KEY.get(player).isInTimeRift()
                || JasonWoundManager.isWoundedActionLocked(player);
    }
}
