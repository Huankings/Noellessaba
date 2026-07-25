package org.agmas.noellesroles;


import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.death.DeathProcessComponent;
import org.agmas.noellesroles.framing.DelusionPlayerComponent;
import org.agmas.noellesroles.modifiers.allergic.AllergicPlayerComponent;
import org.agmas.noellesroles.modifiers.chameleon.ChameleonPlayerComponent;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;
import org.agmas.noellesroles.roles.Noisemaker.NoisemakerGlowTargetComponent;
import org.agmas.noellesroles.roles.Noisemaker.NoisemakerPlayerComponent;
import org.agmas.noellesroles.roles.arsonist.DousedPlayerComponent;
import org.agmas.noellesroles.roles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.roles.convener.ConvenerDisguiseComponent;
import org.agmas.noellesroles.roles.convener.ConvenerMomentumComponent;
import org.agmas.noellesroles.roles.convener.ConvenerPlayerComponent;
import org.agmas.noellesroles.roles.engineer.StunnedPlayerComponent;
import org.agmas.noellesroles.roles.controller.ControlledPlayerComponent;
import org.agmas.noellesroles.roles.controller.ControllerPlayerComponent;
import org.agmas.noellesroles.roles.cook.CookPlayerComponent;
import org.agmas.noellesroles.roles.coward.CowardPlayerComponent;
import org.agmas.noellesroles.roles.coward.SedativePlayerComponent;
import org.agmas.noellesroles.roles.coroner.BodyDeathReasonComponent;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;
import org.agmas.noellesroles.roles.dreamer.DreamerComponent;
import org.agmas.noellesroles.roles.dreamer.DreamerKillerComponent;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerPlayerComponent;
import org.agmas.noellesroles.roles.engineer.EngineerPlayerComponent;
import org.agmas.noellesroles.roles.executioner.ExecutionerPlayerComponent;
import org.agmas.noellesroles.roles.hacker.HackerComponent;
import org.agmas.noellesroles.roles.hacker.HackerPhoneComponent;
import org.agmas.noellesroles.roles.hacker.HackerSafeTimeComponent;
import org.agmas.noellesroles.roles.hunter.HunterPlayerComponent;
import org.agmas.noellesroles.roles.kidnapper.KidnapperComponent;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.roles.magician.MagicianPlayerComponent;
import org.agmas.noellesroles.roles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.roles.angel.AngelPlayerComponent;
import org.agmas.noellesroles.roles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.roles.assassin.HiddenBodiesWorldComponent;
import org.agmas.noellesroles.roles.avaricious.AvariciousPayoutComponent;
import org.agmas.noellesroles.roles.phantom.PhantomPlayerComponent;
import org.agmas.noellesroles.roles.physician.PhysicianPlayerComponent;
import org.agmas.noellesroles.roles.prophet.ProphetPlayerComponent;
import org.agmas.noellesroles.roles.recaller.RecallerPlayerComponent;
import org.agmas.noellesroles.roles.robot.RobotPlayerComponent;
import org.agmas.noellesroles.roles.robber.RobberPlayerComponent;
import org.agmas.noellesroles.roles.muzzler.SilencePlayerComponent;
import org.agmas.noellesroles.roles.necromancer.NecromancerWorldComponent;
import org.agmas.noellesroles.roles.stalker.StalkerPlayerComponent;
import org.agmas.noellesroles.roles.starstruck.StarstruckPlayerComponent;
import org.agmas.noellesroles.roles.operator.OperatorPlayerComponent;
import org.agmas.noellesroles.roles.rememberer.RemembererPlayerComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistHostComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.agmas.noellesroles.roles.voodoo.VoodooPlayerComponent;
import org.agmas.noellesroles.roles.vulture.VulturePlayerComponent;
import org.agmas.noellesroles.roles.waiter.WaiterPlayerComponent;
import org.agmas.noellesroles.roles.winder.WindMarkPlayerComponent;
import org.agmas.noellesroles.roles.winder.WinderPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;

public class NoellesRolesComponents implements EntityComponentInitializer, WorldComponentInitializer {
    public NoellesRolesComponents() {
    }

    public void registerEntityComponentFactories(@NotNull EntityComponentFactoryRegistry registry) {
        registry.beginRegistration(PlayerEntity.class, MorphlingPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(MorphlingPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, BartenderPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(BartenderPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, DelusionPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(DelusionPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, VoodooPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(VoodooPlayerComponent::new);
        registry.beginRegistration(PlayerBodyEntity.class, BodyDeathReasonComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(BodyDeathReasonComponent::new);
        registry.beginRegistration(PlayerEntity.class, AbilityPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(AbilityPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, DousedPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(DousedPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, ConvenerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ConvenerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, ConvenerDisguiseComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ConvenerDisguiseComponent::new);
        registry.beginRegistration(PlayerEntity.class, ConvenerMomentumComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ConvenerMomentumComponent::new);
        registry.beginRegistration(PlayerEntity.class, PhantomPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(PhantomPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, ExecutionerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ExecutionerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, RecallerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(RecallerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, ProphetPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ProphetPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, VulturePlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(VulturePlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, ChameleonPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ChameleonPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, NoisemakerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(NoisemakerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, NoisemakerGlowTargetComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(NoisemakerGlowTargetComponent::new);
        registry.beginRegistration(PlayerEntity.class, CoronerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(CoronerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, ControllerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ControllerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, ControlledPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(ControlledPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, DeathProcessComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(DeathProcessComponent::new);
        registry.beginRegistration(PlayerEntity.class, StunnedPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(StunnedPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, StalkerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(StalkerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, BomberPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(BomberPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, RobberPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(RobberPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, EngineerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(EngineerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, AssassinPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(AssassinPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, WinderPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(WinderPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, MagicianPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(MagicianPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, WindMarkPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(WindMarkPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, OperatorPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(OperatorPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, AngelPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(AngelPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, CowardPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(CowardPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SedativePlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SedativePlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, RemembererPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(RemembererPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, DreamerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(DreamerComponent::new);
        registry.beginRegistration(PlayerEntity.class, DreamerKillerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(DreamerKillerComponent::new);
        registry.beginRegistration(PlayerEntity.class, HackerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(HackerComponent::new);
        registry.beginRegistration(PlayerEntity.class, HackerPhoneComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(HackerPhoneComponent::new);
        registry.beginRegistration(PlayerEntity.class, WaiterPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(WaiterPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, CookPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(CookPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, PhysicianPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(PhysicianPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SpiritualistPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SpiritualistPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SpiritualistHostComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SpiritualistHostComponent::new);
        registry.beginRegistration(PlayerEntity.class, StarstruckPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(StarstruckPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, SilencePlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(SilencePlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, HunterPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(HunterPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, DrugmakerPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(DrugmakerPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, KidnapperComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(KidnapperComponent::new);
        registry.beginRegistration(PlayerEntity.class, RobotPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(RobotPlayerComponent::new);
        registry.beginRegistration(PlayerEntity.class, AllergicPlayerComponent.KEY).respawnStrategy(RespawnCopyStrategy.NEVER_COPY).end(AllergicPlayerComponent::new);
    }

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry worldComponentFactoryRegistry) {
        worldComponentFactoryRegistry.register(ConfigWorldComponent.KEY, ConfigWorldComponent::new);
        worldComponentFactoryRegistry.register(HiddenBodiesWorldComponent.KEY, HiddenBodiesWorldComponent::new);
        worldComponentFactoryRegistry.register(HackerSafeTimeComponent.KEY, HackerSafeTimeComponent::new);
        worldComponentFactoryRegistry.register(AvariciousPayoutComponent.KEY, AvariciousPayoutComponent::new);
        worldComponentFactoryRegistry.register(NecromancerWorldComponent.KEY, NecromancerWorldComponent::new);
        worldComponentFactoryRegistry.register(LoversPairComponent.KEY, LoversPairComponent::new);
        worldComponentFactoryRegistry.register(DualPersonalityComponent.KEY, DualPersonalityComponent::new);
    }
}
