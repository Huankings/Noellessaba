package org.agmas.noellesroles.bootstrap;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.agmas.noellesroles.packet.host.AbilityC2SPacket;
import org.agmas.noellesroles.packet.item.BayonetKnockbackC2SPacket;
import org.agmas.noellesroles.packet.item.BayonetStabC2SPacket;
import org.agmas.noellesroles.packet.item.BlowgunC2SPacket;
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
import org.agmas.noellesroles.packet.role.spiritualist.SpiritualistPossessionViewS2CPacket;
import org.agmas.noellesroles.packet.role.stalker.StalkerDashC2SPacket;
import org.agmas.noellesroles.packet.role.stalker.StalkerGazeC2SPacket;
import org.agmas.noellesroles.packet.role.swapper.SwapperC2SPacket;
import org.agmas.noellesroles.packet.role.vulture.VultureEatC2SPacket;

/**
 * 客户端/服务端自定义 payload codec 注册。
 */
public final class NoellesRolesPayloadTypes {
    private NoellesRolesPayloadTypes() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(MorphC2SPacket.ID, MorphC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AbilityC2SPacket.ID, AbilityC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SwapperC2SPacket.ID, SwapperC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(VultureEatC2SPacket.ID, VultureEatC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(GuessC2SPacket.ID, GuessC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(DualPersonalitySwitchC2SPacket.ID, DualPersonalitySwitchC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(DualPersonalitySwitchKeyLabelC2SPacket.ID, DualPersonalitySwitchKeyLabelC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(OperatorC2SPacket.ID, OperatorC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(NoisemakerGlowC2SPacket.ID, NoisemakerGlowC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ControllerPossessC2SPacket.ID, ControllerPossessC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ControllerReleaseC2SPacket.ID, ControllerReleaseC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CorpsemakerC2SPacket.ID, CorpsemakerC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(BrainwasherC2SPacket.ID, BrainwasherC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(GoddessC2SPacket.ID, GoddessC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(StalkerGazeC2SPacket.ID, StalkerGazeC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(StalkerDashC2SPacket.ID, StalkerDashC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CrystalBallMarkC2SPacket.ID, CrystalBallMarkC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(BayonetKnockbackC2SPacket.ID, BayonetKnockbackC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(BayonetStabC2SPacket.ID, BayonetStabC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(HuntingKnifeC2SPacket.ID, HuntingKnifeC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(BlowgunC2SPacket.ID, BlowgunC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(PanC2SPacket.ID, PanC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SniperRifleShootC2SPacket.ID, SniperRifleShootC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SpiritualistPossessionControlC2SPacket.ID, SpiritualistPossessionControlC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ConvenerMorphC2SPacket.ID, ConvenerMorphC2SPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SpiritualistPossessionViewS2CPacket.ID, SpiritualistPossessionViewS2CPacket.CODEC);
    }
}
