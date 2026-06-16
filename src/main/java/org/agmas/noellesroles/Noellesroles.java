package org.agmas.noellesroles;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.api.event.AllowPlayerPunching;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.api.event.CanSeePoison;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.api.event.ShouldDropOnDeath;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.replay.ReplayRegistry;
import dev.doctor4t.wathe.util.ShopEntry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.config.HarpyModLoaderConfig;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.harpymodloader.events.ModifierAssigned;
import org.agmas.harpymodloader.events.ResetPlayerEvent;
import org.agmas.harpymodloader.modifiers.HMLModifiers;
import org.agmas.harpymodloader.modifiers.Modifier;
import org.agmas.noellesroles.roles.angel.AngelAbility;
import org.agmas.noellesroles.roles.angel.AngelConstants;
import org.agmas.noellesroles.roles.angel.AngelPlayerComponent;
import org.agmas.noellesroles.roles.brainwasher.BrainwasherAbility;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.roles.controller.ControllerPossessAbility;
import org.agmas.noellesroles.roles.controller.ControllerReleaseAbility;
import org.agmas.noellesroles.roles.coroner.CoronerMorphAbility;
import org.agmas.noellesroles.roles.corpsemaker.CorpsemakerAbility;
import org.agmas.noellesroles.roles.coward.CowardPlayerComponent;
import org.agmas.noellesroles.roles.coward.CowardConstants;
import org.agmas.noellesroles.roles.coward.SedativePlayerComponent;
import org.agmas.noellesroles.death.NoellesRolesDeathBootstrap;
import org.agmas.noellesroles.framing.DelusionPlayerComponent;
import org.agmas.noellesroles.framing.FramingShopEntry;
import org.agmas.noellesroles.roles.goddess.GoddessAbility;
import org.agmas.noellesroles.roles.magician.MagicianAbility;
import org.agmas.noellesroles.roles.magician.MagicianConstants;
import org.agmas.noellesroles.roles.magician.MagicianPlaybackManager;
import org.agmas.noellesroles.roles.magician.MagicianTargetAbility;
import org.agmas.noellesroles.modifiers.guesser.GuesserAbility;
import org.agmas.noellesroles.roles.morphling.MorphlingMorphAbility;
import org.agmas.noellesroles.packet.host.AbilityC2SPacket;
import org.agmas.noellesroles.packet.item.BayonetKnockbackC2SPacket;
import org.agmas.noellesroles.packet.item.BayonetStabC2SPacket;
import org.agmas.noellesroles.packet.item.CrystalBallMarkC2SPacket;
import org.agmas.noellesroles.packet.item.SniperRifleShootC2SPacket;
import org.agmas.noellesroles.packet.modifiers.GuessC2SPacket;
import org.agmas.noellesroles.packet.role.brainwasher.BrainwasherC2SPacket;
import org.agmas.noellesroles.packet.role.controller.ControllerPossessC2SPacket;
import org.agmas.noellesroles.packet.role.controller.ControllerReleaseC2SPacket;
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
import org.agmas.noellesroles.entities.CaptureDeviceEntity;
import org.agmas.noellesroles.entities.RoleMineEntity;
import org.agmas.noellesroles.entities.ThrowingAxeEntity;
import org.agmas.noellesroles.roles.assassin.HiddenBodiesWorldComponent;
import org.agmas.noellesroles.roles.phantom.PhantomAbility;
import org.agmas.noellesroles.roles.phantom.PhantomPlayerComponent;
import org.agmas.noellesroles.roles.prophet.ProphetAbility;
import org.agmas.noellesroles.roles.rememberer.RemembererInteractionHandler;
import org.agmas.noellesroles.roles.rememberer.RemembererSniperManager;
import org.agmas.noellesroles.roles.recaller.RecallerAbility;
import org.agmas.noellesroles.roles.operator.OperatorAbility;
import org.agmas.noellesroles.roles.operator.OperatorCommunicationManager;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistAbility;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistCommunicationManager;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistConstants;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistManager;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.agmas.noellesroles.roles.stalker.StalkerAbility;
import org.agmas.noellesroles.roles.swapper.SwapperAbility;
import org.agmas.noellesroles.roles.voodoo.VoodooTargetAbility;
import org.agmas.noellesroles.roles.vulture.VultureAbility;
import org.agmas.noellesroles.roles.winder.WinderAbility;
import org.agmas.noellesroles.roles.winder.WinderPlayerComponent;
import org.agmas.noellesroles.roles.winder.WinderTargetAbility;
import org.agmas.noellesroles.bed.NoellesRolesBedEffects;
import org.agmas.noellesroles.roleassign.NoellesRolesRoleAssignedBootstrap;
import org.agmas.noellesroles.record.NoellesRolesReplayFormatters;
import org.agmas.noellesroles.shop.NoellesRolesShopBootstrap;
import org.agmas.noellesroles.tray.NoellesRolesTrayEffects;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Noellesroles implements ModInitializer {

    public static String MOD_ID = "noellesroles";


    public static Identifier JESTER_ID = Identifier.of(MOD_ID, "jester");
    public static Identifier MORPHLING_ID = Identifier.of(MOD_ID, "morphling");
    public static Identifier CONDUCTOR_ID = Identifier.of(MOD_ID, "conductor");
    public static Identifier BARTENDER_ID = Identifier.of(MOD_ID, "bartender");
    public static Identifier WINDER_ID = Identifier.of(MOD_ID, "winder");
    public static Identifier NOISEMAKER_ID = Identifier.of(MOD_ID, "noisemaker");
    public static Identifier PHANTOM_ID = Identifier.of(MOD_ID, "phantom");
    public static Identifier AWESOME_BINGLUS_ID = Identifier.of(MOD_ID, "awesome_binglus");
    public static Identifier SWAPPER_ID = Identifier.of(MOD_ID, "swapper");
    public static Identifier GUESSER_ID = Identifier.of(MOD_ID, "guesser");
    public static Identifier VOODOO_ID = Identifier.of(MOD_ID, "voodoo");
    public static Identifier TRAPPER_ID = Identifier.of(MOD_ID, "trapper");
    public static Identifier CORONER_ID = Identifier.of(MOD_ID, "coroner");
    public static Identifier RECALLER_ID = Identifier.of(MOD_ID, "recaller");
    public static Identifier PROPHET_ID = Identifier.of(MOD_ID, "prophet");
    public static Identifier MIMIC_ID = Identifier.of(MOD_ID, "mimic");
    public static Identifier EXECUTIONER_ID = Identifier.of(MOD_ID, "executioner");
    public static Identifier VULTURE_ID = Identifier.of(MOD_ID, "vulture");
    public static Identifier BETTER_VIGILANTE_ID = Identifier.of(MOD_ID, "better_vigilante");
    public static Identifier TINY_ID = Identifier.of(MOD_ID, "tiny");
    public static Identifier CHAMELEON_ID = Identifier.of(MOD_ID, "chameleon");
    public static Identifier GRAVEROBBER_ID = Identifier.of(MOD_ID, "graverobber");
    public static Identifier FEATHER_ID = Identifier.of(MOD_ID, "feather");
    public static Identifier THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES_ID = Identifier.of(MOD_ID, "the_insane_damned_paranoid_killer");
    public static Identifier CONTROLLER_ID = Identifier.of(MOD_ID, "controller");
    public static Identifier CORPSEMAKER_ID = Identifier.of(MOD_ID, "corpsemaker");
    public static Identifier BRAINWASHER_ID = Identifier.of(MOD_ID, "brainwasher");
    public static Identifier BOMBER_ID = Identifier.of(MOD_ID, "bomber");
    public static Identifier ROBBER_ID = Identifier.of(MOD_ID, "robber");
    public static Identifier ASSASSIN_ID = Identifier.of(MOD_ID, "assassin");
    public static Identifier GODDESS_ID = Identifier.of(MOD_ID, "goddess");
    public static Identifier ENGINEER_ID = Identifier.of(MOD_ID, "engineer");
    public static Identifier STALKER_ID = Identifier.of(MOD_ID, "stalker");
    public static Identifier ANGEL_ID = Identifier.of(MOD_ID, "angel");
    public static Identifier COWARD_ID = Identifier.of(MOD_ID, "coward");
    public static Identifier REMEMBERER_ID = Identifier.of(MOD_ID, "rememberer");
    public static Identifier SPIRITUALIST_ID = Identifier.of(MOD_ID, "spiritualist");
    public static Identifier OPERATOR_ID = Identifier.of(MOD_ID, "operator");
    public static Identifier MAGICIAN_ID = Identifier.of(MOD_ID, "magician");
    public static Identifier FAKE_DEATH_REASON = Identifier.of(Noellesroles.MOD_ID, "fake");
    public static Identifier STALKER_EXECUTION_DEATH = Identifier.of(MOD_ID, "stalker_execution");
    public static Identifier DEATH_REASON_BOMB = Identifier.of(MOD_ID, "bomb");
    public static Identifier DEATH_REASON_THROWING_AXE = Identifier.of(MOD_ID, "throwing_axe");
    public static Identifier DEATH_REASON_SEDATIVE_OVERDOSE = Identifier.of(MOD_ID, "sedative_overdose");
    public static Identifier DEATH_REASON_SNIPER_RIFLE = Identifier.of(MOD_ID, "sniper_rifle");
    public static final Identifier DEFENSE_TRAY_EFFECT = Identifier.of(MOD_ID, "defense_vial");
    public static final Identifier DELUSION_TRAY_EFFECT = Identifier.of(MOD_ID, "delusion_vial");
    public static final Identifier SEDATIVE_TRAY_EFFECT = Identifier.of(MOD_ID, "sedative");
    public static final Identifier DELUSION_STARTED_EVENT = Identifier.of(MOD_ID, "delusion_started");
    public static final Identifier DELUSION_ENDED_EVENT = Identifier.of(MOD_ID, "delusion_ended");
    public static final Identifier COWARD_DANGER_SENSED_EVENT = Identifier.of(MOD_ID, "coward_danger_sensed");
    public static final Identifier COWARD_DANGER_LEFT_EVENT = Identifier.of(MOD_ID, "coward_danger_left");
    public static final Identifier SEDATIVE_STARTED_EVENT = Identifier.of(MOD_ID, "sedative_started");
    public static final Identifier SEDATIVE_ENDED_EVENT = Identifier.of(MOD_ID, "sedative_ended");
    public static final Identifier TIMED_BOMB_ACTIVATED_EVENT = Identifier.of(MOD_ID, "timed_bomb_activated");
    public static final Identifier TIMED_BOMB_TRAY_EMBEDDED_EVENT = Identifier.of(MOD_ID, "timed_bomb_tray_embedded");
    public static final Identifier TIMED_BOMB_BED_EMBEDDED_EVENT = Identifier.of(MOD_ID, "timed_bomb_bed_embedded");
    public static final Identifier TIMED_BOMB_BED_TRIGGERED_EVENT = Identifier.of(MOD_ID, "timed_bomb_bed_triggered");
    public static final Identifier ROLE_MINE_DETECTED_EVENT = Identifier.of(MOD_ID, "role_mine_detected");
    public static final Identifier ROLE_MINE_REPORT_EVENT = Identifier.of(MOD_ID, "role_mine_report");
    public static final Identifier CAPTURE_DEVICE_TRIGGERED_EVENT = Identifier.of(MOD_ID, "capture_device_triggered");
    public static final Identifier CAPTURE_DEVICE_REPORT_EVENT = Identifier.of(MOD_ID, "capture_device_report");
    public static final Identifier CAPTURE_DEVICE_EXPIRED_EVENT = Identifier.of(MOD_ID, "capture_device_expired");
    public static final Identifier CAPTURE_DEVICE_RELEASED_EVENT = Identifier.of(MOD_ID, "capture_device_released");
    public static final Identifier POWER_RESTORED_EVENT = Identifier.of(MOD_ID, "power_restored");
    public static final Identifier JESTER_PSYCHO_STARTED_EVENT = Identifier.of(MOD_ID, "jester_psycho_started");
    public static final Identifier EXECUTIONER_TARGET_LOCKED_EVENT = Identifier.of(MOD_ID, "executioner_target_locked");
    public static final Identifier EXECUTIONER_TARGET_CHANGED_EVENT = Identifier.of(MOD_ID, "executioner_target_changed");
    public static final Identifier VULTURE_PROGRESS_EVENT = Identifier.of(MOD_ID, "vulture_progress");
    public static final Identifier RECALLER_POSITION_SAVED_EVENT = Identifier.of(MOD_ID, "recaller_position_saved");
    public static final Identifier RECALLER_TELEPORTED_EVENT = Identifier.of(MOD_ID, "recaller_teleported");
    public static final Identifier RECALLER_ENDER_PEARL_THROWN_EVENT = Identifier.of(MOD_ID, "recaller_ender_pearl_thrown");
    public static final Identifier PHANTOM_INVISIBILITY_STARTED_EVENT = Identifier.of(MOD_ID, "phantom_invisibility_started");
    public static final Identifier PHANTOM_INVISIBILITY_ENDED_EVENT = Identifier.of(MOD_ID, "phantom_invisibility_ended");
    public static final Identifier PROPHET_MARKED_EVENT = Identifier.of(MOD_ID, "prophet_marked");
    public static final Identifier PROPHET_REMARKED_EVENT = Identifier.of(MOD_ID, "prophet_remarked");
    public static final Identifier PROPHET_REVEALED_EVENT = Identifier.of(MOD_ID, "prophet_revealed");
    public static final Identifier PROPHET_VOODOO_IMMUNITY_EVENT = Identifier.of(MOD_ID, "prophet_voodoo_immunity");
    public static final Identifier WINDER_WIND_MARK_APPLIED_EVENT = Identifier.of(MOD_ID, "winder_wind_mark_applied");
    public static final Identifier WINDER_WIND_CHARGE_USED_EVENT = Identifier.of(MOD_ID, "winder_wind_charge_used");
    public static final Identifier WINDER_WIND_MARK_EXPIRED_EVENT = Identifier.of(MOD_ID, "winder_wind_mark_expired");
    public static final Identifier WINDER_WIND_MARK_TRIGGERED_EVENT = Identifier.of(MOD_ID, "winder_wind_mark_triggered");
    public static final Identifier WINDER_FLOAT_STARTED_EVENT = Identifier.of(MOD_ID, "winder_float_started");
    public static final Identifier WINDER_FLOAT_ENDED_EVENT = Identifier.of(MOD_ID, "winder_float_ended");
    public static final Identifier WINDER_FLOAT_STOPPED_EARLY_EVENT = Identifier.of(MOD_ID, "winder_float_stopped_early");
    public static final Identifier STALKER_PHASE_ADVANCE_1_TO_2_EVENT = Identifier.of(MOD_ID, "stalker_phase_1_to_2");
    public static final Identifier STALKER_PHASE_ADVANCE_2_TO_3_EVENT = Identifier.of(MOD_ID, "stalker_phase_2_to_3");
    public static final Identifier STALKER_PHASE_REGRESS_3_TO_2_EVENT = Identifier.of(MOD_ID, "stalker_phase_3_to_2");
    public static final Identifier NOISEMAKER_GLOW_STARTED_EVENT = Identifier.of(MOD_ID, "noisemaker_glow_started");
    public static final Identifier NOISEMAKER_GLOW_ENDED_EVENT = Identifier.of(MOD_ID, "noisemaker_glow_ended");
    public static final Identifier MORPHLING_MORPH_STARTED_EVENT = Identifier.of(MOD_ID, "morphling_morph_started");
    public static final Identifier MORPHLING_MORPH_ENDED_EVENT = Identifier.of(MOD_ID, "morphling_morph_ended");
    public static final Identifier SWAPPER_SWAP_SELECTED_EVENT = Identifier.of(MOD_ID, "swapper_swap_selected");
    public static final Identifier SWAPPER_SWAP_EXECUTED_EVENT = Identifier.of(MOD_ID, "swapper_swap_executed");
    public static final Identifier CORPSEMAKER_FORGED_BODY_EVENT = Identifier.of(MOD_ID, "corpsemaker_forged_body");
    public static final Identifier VOODOO_BOUND_EVENT = Identifier.of(MOD_ID, "voodoo_bound");
    public static final Identifier GUESSER_DECLARED_EVENT = Identifier.of(MOD_ID, "guesser_declared");
    public static final Identifier GUESSER_CORRECT_EVENT = Identifier.of(MOD_ID, "guesser_correct");
    public static final Identifier GUESSER_WRONG_EVENT = Identifier.of(MOD_ID, "guesser_wrong");
    public static final Identifier CONTROLLER_POSSESS_STARTED_EVENT = Identifier.of(MOD_ID, "controller_possess_started");
    public static final Identifier CONTROLLER_POSSESS_STOPPED_EARLY_EVENT = Identifier.of(MOD_ID, "controller_possess_stopped_early");
    public static final Identifier CONTROLLER_POSSESS_ENDED_EVENT = Identifier.of(MOD_ID, "controller_possess_ended");
    public static final Identifier ANGEL_SOOTHE_CAST_EVENT = Identifier.of(MOD_ID, "angel_soothe_cast");
    public static final Identifier ANGEL_SOOTHED_EVENT = Identifier.of(MOD_ID, "angel_soothed");
    public static final Identifier ANGEL_GUARD_SELECTED_EVENT = Identifier.of(MOD_ID, "angel_guard_selected");
    public static final Identifier ANGEL_SACRIFICE_EVENT = Identifier.of(MOD_ID, "angel_sacrifice");
    public static final Identifier ANGEL_GUARD_SHIELD_SOURCE = Identifier.of(MOD_ID, "angel_guard");
    public static final Identifier ANGEL_SACRIFICE_DEATH_REASON = Identifier.of(MOD_ID, "angel_sacrifice");
    public static final Identifier SPIRITUALIST_PROJECTION_STARTED_EVENT = Identifier.of(MOD_ID, "spiritualist_projection_started");
    public static final Identifier SPIRITUALIST_PROJECTION_ENDED_EVENT = Identifier.of(MOD_ID, "spiritualist_projection_ended");
    public static final Identifier SPIRITUALIST_POSSESSION_STARTED_EVENT = Identifier.of(MOD_ID, "spiritualist_possession_started");
    public static final Identifier SPIRITUALIST_POSSESSION_ENDED_EVENT = Identifier.of(MOD_ID, "spiritualist_possession_ended");
    public static final Identifier OPERATOR_CONNECTION_FAILED_BOTH_DEAD_EVENT = Identifier.of(MOD_ID, "operator_connection_failed_both_dead");
    public static final Identifier OPERATOR_CONNECTION_FAILED_ONE_DEAD_EVENT = Identifier.of(MOD_ID, "operator_connection_failed_one_dead");
    public static final Identifier OPERATOR_CONNECTION_STARTED_EVENT = Identifier.of(MOD_ID, "operator_connection_started");
    public static final Identifier OPERATOR_CONNECTION_ENDED_EVENT = Identifier.of(MOD_ID, "operator_connection_ended");
    public static final Identifier OPERATOR_CONNECTION_INTERRUPTED_EVENT = Identifier.of(MOD_ID, "operator_connection_interrupted");
    public static final Identifier OPERATOR_BROADCAST_FAILED_EVENT = Identifier.of(MOD_ID, "operator_broadcast_failed");
    public static final Identifier OPERATOR_BROADCAST_STARTED_EVENT = Identifier.of(MOD_ID, "operator_broadcast_started");
    public static final Identifier OPERATOR_BROADCAST_ENDED_EVENT = Identifier.of(MOD_ID, "operator_broadcast_ended");
    public static final Identifier OPERATOR_BROADCAST_INTERRUPTED_EVENT = Identifier.of(MOD_ID, "operator_broadcast_interrupted");
    public static final Identifier REMEMBERER_RECALL_EVENT = Identifier.of(MOD_ID, "rememberer_recall");
    public static final Identifier REMEMBERER_SNIPER_RELOADED_EVENT = Identifier.of(MOD_ID, "rememberer_sniper_reloaded");
    public static final Identifier SPIRITUALIST_ACTIVE_SHIELD_SOURCE = Identifier.of(MOD_ID, "spiritualist_active_shield");
    public static final Identifier SPIRITUALIST_LINGERING_SHIELD_SOURCE = Identifier.of(MOD_ID, "spiritualist_lingering_shield");
    public static final Identifier SPIRITUALIST_SOUL_GUARD_DEATH_REASON = Identifier.of(MOD_ID, "spiritualist_soul_guard");
    public static final Identifier MAGICIAN_RECORDING_STARTED_EVENT = Identifier.of(MOD_ID, "magician_recording_started");
    public static final Identifier MAGICIAN_RECORDING_FINISHED_EVENT = Identifier.of(MOD_ID, "magician_recording_finished");
    public static final Identifier MAGICIAN_RECORDING_STOPPED_EARLY_EVENT = Identifier.of(MOD_ID, "magician_recording_stopped_early");
    public static final Identifier MAGICIAN_PLAYBACK_STARTED_EVENT = Identifier.of(MOD_ID, "magician_playback_started");
    public static final Identifier MAGICIAN_PLAYBACK_FINISHED_EVENT = Identifier.of(MOD_ID, "magician_playback_finished");
    public static final Identifier MAGICIAN_PLAYBACK_STOPPED_EARLY_EVENT = Identifier.of(MOD_ID, "magician_playback_stopped_early");
    public static final Identifier MAGICIAN_PLAYBACK_FORCED_END_EVENT = Identifier.of(MOD_ID, "magician_playback_forced_end");




    public static HashMap<Role, RoleAnnouncementTexts.RoleAnnouncementText> roleRoleAnnouncementTextHashMap = new HashMap<>();
    //造尸怪(杀手)
    public static Role CORPSEMAKER = WatheRoles.registerRole(new Role(CORPSEMAKER_ID, new Color(12, 0, 228).getRGB(), false, true, Role.MoodType.FAKE,-1, true));
    //潜行者(杀手)
    public static Role STALKER = WatheRoles.registerRole(new Role(STALKER_ID, new Color(186, 85, 211).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //附体师(杀手)
    public static Role CONTROLLER = WatheRoles.registerRole(new Role(CONTROLLER_ID, new Color(128, 0, 128).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //炸弹客(杀手)
    public static Role BOMBER = WatheRoles.registerRole(new Role(BOMBER_ID, new Color(50, 50, 50).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //强盗(杀手)
    public static Role ROBBER = WatheRoles.registerRole(new Role(ROBBER_ID, new Color(220, 82, 50).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //刺客(杀手)
    public static Role ASSASSIN = WatheRoles.registerRole(new Role(ASSASSIN_ID, new Color(34, 68, 36).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //狂信者(杀手中立)
    public static Role JESTER = WatheRoles.registerRole(new Role(JESTER_ID,new Color(255,86,243).getRGB() ,false,false, Role.MoodType.FAKE,-1,true));
    //变形怪(杀手)
    public static Role MORPHLING =WatheRoles.registerRole(new Role(MORPHLING_ID, new Color(170, 2, 61).getRGB(),false,true, Role.MoodType.FAKE,-1,true));
    //列车长(好人)
    public static Role CONDUCTOR =WatheRoles.registerRole(new Role(CONDUCTOR_ID, new Color(255, 205, 84).getRGB(),true,false, Role.MoodType.REAL,WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    //记者(好人)
    public static Role AWESOME_BINGLUS = WatheRoles.registerRole(new Role(AWESOME_BINGLUS_ID, new Color(155, 255, 168).getRGB(),true,false, Role.MoodType.REAL,WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    //工程师(好人)
    public static Role ENGINEER = WatheRoles.registerRole(new Role(ENGINEER_ID, new Color(100, 149, 237).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //酒保(好人)
    public static Role BARTENDER =WatheRoles.registerRole(new Role(BARTENDER_ID, new Color(217,241,240).getRGB(),true,false, Role.MoodType.REAL,WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    //风灵师(好人)
    public static Role WINDER = WatheRoles.registerRole(new Role(WINDER_ID, new Color(66, 215, 215).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //灵术师(好人)
    public static Role SPIRITUALIST = WatheRoles.registerRole(new Role(SPIRITUALIST_ID, SpiritualistConstants.ROLE_COLOR, true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //接线员(好人)
    public static Role OPERATOR = WatheRoles.registerRole(new Role(OPERATOR_ID, new Color(75, 221, 192).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //魔术师(杀手)
    public static Role MAGICIAN = WatheRoles.registerRole(new Role(MAGICIAN_ID, MagicianConstants.ROLE_COLOR, false, true, Role.MoodType.FAKE, -1, true));
    //大嗓门(好人)
    public static Role NOISEMAKER =WatheRoles.registerRole(new Role(NOISEMAKER_ID, new Color(200, 255, 0).getRGB(),true,false, Role.MoodType.REAL,WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    //交换者(杀手)
    public static Role SWAPPER = WatheRoles.registerRole(new Role(SWAPPER_ID, new Color(57, 4, 170).getRGB(),false,true, Role.MoodType.FAKE,-1,true));
    //幻灵(杀手)
    public static Role PHANTOM =WatheRoles.registerRole(new Role(PHANTOM_ID, new Color(80, 5, 5, 192).getRGB(),false,true, Role.MoodType.FAKE,-1,true));
    //巫毒师(好人)
    public static Role VOODOO =WatheRoles.registerRole(new Role(VOODOO_ID, new Color(128, 114, 253).getRGB(),true,false,Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    //亡语杀手(杀手)
    public static Role THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES =WatheRoles.registerRole(new Role(THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES_ID, new Color(255, 0, 0, 192).getRGB(),false,true, Role.MoodType.FAKE,-1,true));
    //调查官(好人)
    public static Role TRAPPER =WatheRoles.registerRole(new Role(TRAPPER_ID, new Color(132, 186, 167).getRGB(),true,false,Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    //验尸官(好人)
    public static Role CORONER =WatheRoles.registerRole(new Role(CORONER_ID, new Color(122, 122, 122).getRGB(),true,false,Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    //仇杀客(杀手中立)
    public static Role EXECUTIONER =WatheRoles.registerRole(new Role(EXECUTIONER_ID, new Color(74, 27, 5).getRGB(),false,false,Role.MoodType.FAKE, WatheRoles.CIVILIAN.getMaxSprintTime()*3/2,true));
    //回溯者(好人)
    public static Role RECALLER = WatheRoles.registerRole(new Role(RECALLER_ID, new Color(158, 255, 255).getRGB(),true,false,Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    //先知(好人)
    public static Role PROPHET = WatheRoles.registerRole(new Role(PROPHET_ID, new Color(207, 42, 177).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //秃鹫(杀手中立)
    public static Role VULTURE =WatheRoles.registerRole(new Role(VULTURE_ID, new Color(181, 103, 0).getRGB(),false,false,Role.MoodType.FAKE, WatheRoles.CIVILIAN.getMaxSprintTime()+100,true));
    //更好的义警(义警)
    public static Role BETTER_VIGILANTE =WatheRoles.registerVigilanteRole(new Role(BETTER_VIGILANTE_ID, new Color(0, 255, 255).getRGB(),true,false,Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(),false));
    //洗脑师(杀手)
    public static Role BRAINWASHER = WatheRoles.registerRole(new Role(BRAINWASHER_ID, new Color(255, 105, 180).getRGB(), false, true, Role.MoodType.FAKE, -1, true));
    //圣母(好人)
    public static Role GODDESS = WatheRoles.registerRole(new Role(GODDESS_ID, Color.WHITE.getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //天使(好人)
    public static Role ANGEL = WatheRoles.registerRole(new Role(ANGEL_ID, new Color(236, 220, 239).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //胆小鬼(好人)
    public static Role COWARD = WatheRoles.registerCivilianRole(new Role(COWARD_ID, new Color(208, 232, 140).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));
    //追忆者(好人)
    public static Role REMEMBERER = WatheRoles.registerCivilianRole(new Role(REMEMBERER_ID, new Color(46, 46, 66).getRGB(), true, false, Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(), false));

    //public static Role GUESSER =WatheRoles.registerRole(new Role(GUESSER_ID, new Color(158, 43, 25, 191).getRGB(),false,true, Role.MoodType.FAKE,-1,true));
    //模仿者(好人)
    public static Role MIMIC = WatheRoles.registerRole(new Role(MIMIC_ID, new Color(255, 137, 155).getRGB(),true,false,Role.MoodType.REAL, WatheRoles.CIVILIAN.getMaxSprintTime(),false));

    //小孩子
    public static Modifier TINY = HMLModifiers.registerModifier(new Modifier(TINY_ID, new Color(255, 166, 0).getRGB(), new ArrayList<>(List.of(MORPHLING)),null,false,false));
    //变色龙
    public static Modifier CHAMELEON = HMLModifiers.registerModifier(new Modifier(CHAMELEON_ID, new Color(198, 255, 137, 255).getRGB(),null,null,false,false));
    //猜测者
    public static Modifier GUESSER = HMLModifiers.registerModifier(new Modifier(GUESSER_ID, new Color(158, 43, 25, 255).getRGB(),new ArrayList<>(List.of(THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES)),null,true,false));
    //盗墓者
    public static Modifier GRAVEROBBER = HMLModifiers.registerModifier(new Modifier(GRAVEROBBER_ID, new Color(174, 95, 95, 255).getRGB(),null,null,true,false));
    //羽化者
    public static Modifier FEATHER = HMLModifiers.registerModifier(new Modifier(FEATHER_ID, new Color(255, 236, 161, 255).getRGB(),null,null,false,false));



    public static final CustomPayload.Id<MorphC2SPacket> MORPH_PACKET = MorphC2SPacket.ID;
    public static final CustomPayload.Id<SwapperC2SPacket> SWAP_PACKET = SwapperC2SPacket.ID;
    public static final CustomPayload.Id<AbilityC2SPacket> ABILITY_PACKET = AbilityC2SPacket.ID;
    public static final CustomPayload.Id<VultureEatC2SPacket> VULTURE_PACKET = VultureEatC2SPacket.ID;
    public static final CustomPayload.Id<GuessC2SPacket> GUESS_PACKET = GuessC2SPacket.ID;
    public static final CustomPayload.Id<OperatorC2SPacket> OPERATOR_PACKET = OperatorC2SPacket.ID;
    public static final CustomPayload.Id<CorpsemakerC2SPacket> CORPSEMAKER_PACKET = CorpsemakerC2SPacket.ID;
    public static final CustomPayload.Id<StalkerGazeC2SPacket> STALKER_GAZE_PACKET = StalkerGazeC2SPacket.ID;
    public static final CustomPayload.Id<StalkerDashC2SPacket> STALKER_DASH_PACKET = StalkerDashC2SPacket.ID;
    public static final ArrayList<Role> VANNILA_ROLES = new ArrayList<>();
    public static final ArrayList<Identifier> VANNILA_ROLE_IDS = new ArrayList<>();
    public static final ArrayList<Role> KILLER_SIDED_NEUTRALS = new ArrayList<>();

    public static ArrayList<ShopEntry> FRAMING_ROLES_SHOP = new ArrayList<>();

    public static Identifier VOODOO_MAGIC_DEATH_REASON = Identifier.of(Noellesroles.MOD_ID, "voodoo");
    public static Identifier GUESS_EXPLODE_DEATH_REASON = Identifier.of(Noellesroles.MOD_ID, "guess_explode");
    public static Identifier GUESS_EXPLODE_NEARBY_DEATH_REASON = Identifier.of(Noellesroles.MOD_ID, "guess_explode_nearby");

    @Override
    public void onInitialize() {
        VANNILA_ROLES.add(WatheRoles.KILLER);
        VANNILA_ROLES.add(WatheRoles.VIGILANTE);
        VANNILA_ROLES.add(WatheRoles.CIVILIAN);
        VANNILA_ROLES.add(WatheRoles.LOOSE_END);

        KILLER_SIDED_NEUTRALS.add(VULTURE);
        KILLER_SIDED_NEUTRALS.add(JESTER);
        KILLER_SIDED_NEUTRALS.add(EXECUTIONER);

        VANNILA_ROLE_IDS.add(WatheRoles.LOOSE_END.identifier());
        VANNILA_ROLE_IDS.add(WatheRoles.VIGILANTE.identifier());
        VANNILA_ROLE_IDS.add(WatheRoles.CIVILIAN.identifier());
        VANNILA_ROLE_IDS.add(WatheRoles.KILLER.identifier());

        FRAMING_ROLES_SHOP.add(new FramingShopEntry(WatheItems.LOCKPICK.getDefaultStack(), 50, ShopEntry.Type.TOOL));
        FRAMING_ROLES_SHOP.add(new FramingShopEntry(ModItems.DELUSION_VIAL.getDefaultStack(), 30, ShopEntry.Type.POISON));
        FRAMING_ROLES_SHOP.add(new FramingShopEntry(WatheItems.FIRECRACKER.getDefaultStack(), 5, ShopEntry.Type.TOOL));
        FRAMING_ROLES_SHOP.add(new FramingShopEntry(WatheItems.NOTE.getDefaultStack(), 5, ShopEntry.Type.TOOL));

        NoellesRolesConfig.HANDLER.load();
        ModItems.init();
        NoellesRolesEntities.init();
        NoellesRolesTrayEffects.register();
        NoellesRolesBedEffects.register();
        registerReplayFormatters();
        SpiritualistCommunicationManager.init();
        OperatorCommunicationManager.init();
        RemembererInteractionHandler.init();
        RemembererSniperManager.init();
        MagicianPlaybackManager.init();


        Harpymodloader.setRoleMaximum(CONDUCTOR_ID,1);
        Harpymodloader.setRoleMaximum(EXECUTIONER_ID,1);
        Harpymodloader.setRoleMaximum(VULTURE_ID,1);
        Harpymodloader.setRoleMaximum(JESTER_ID,1);
        Harpymodloader.setRoleMaximum(BETTER_VIGILANTE_ID,0);


        PayloadTypeRegistry.playC2S().register(MorphC2SPacket.ID, MorphC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(AbilityC2SPacket.ID, AbilityC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SwapperC2SPacket.ID, SwapperC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(VultureEatC2SPacket.ID, VultureEatC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(GuessC2SPacket.ID, GuessC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(OperatorC2SPacket.ID, OperatorC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(NoisemakerGlowC2SPacket.ID, NoisemakerGlowC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ControllerPossessC2SPacket.ID, ControllerPossessC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ControllerReleaseC2SPacket.ID, ControllerReleaseC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CorpsemakerC2SPacket.ID,CorpsemakerC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(BrainwasherC2SPacket.ID, BrainwasherC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(GoddessC2SPacket.ID, GoddessC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(StalkerGazeC2SPacket.ID, StalkerGazeC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(StalkerDashC2SPacket.ID, StalkerDashC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CrystalBallMarkC2SPacket.ID, CrystalBallMarkC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(BayonetKnockbackC2SPacket.ID, BayonetKnockbackC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(BayonetStabC2SPacket.ID, BayonetStabC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SniperRifleShootC2SPacket.ID, SniperRifleShootC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(SpiritualistPossessionControlC2SPacket.ID, SpiritualistPossessionControlC2SPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SpiritualistPossessionViewS2CPacket.ID, SpiritualistPossessionViewS2CPacket.CODEC);
        NoellesRolesShopBootstrap.init();
        ServerPlayNetworking.registerGlobalReceiver(NoisemakerGlowC2SPacket.ID,
                (packet, context) -> NoisemakerGlowC2SPacket.handle(packet, context.player().networkHandler));

        registerEvents();

        registerPackets();

        // 回合结束后主动清理会残留在地图中的扩展实体，避免下一把继续影响对局。
        GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            // 回合结束时一并清空交换者的延迟交换队列，
            // 防止上一把尚未执行的交换延迟到下一把才触发。
            SwapperAbility.clearPendingSwaps();
            HiddenBodiesWorldComponent.KEY.get(serverWorld).reset();
            MagicianPlaybackManager.cleanupAllPlaybackEntities(serverWorld);

            for (ThrowingAxeEntity entity : serverWorld.getEntitiesByType(TypeFilter.equals(ThrowingAxeEntity.class), ignored -> true)) {
                entity.discard();
            }
            for (RoleMineEntity entity : serverWorld.getEntitiesByType(TypeFilter.equals(RoleMineEntity.class), ignored -> true)) {
                entity.discard();
            }
            for (CaptureDeviceEntity entity : serverWorld.getEntitiesByType(TypeFilter.equals(CaptureDeviceEntity.class), ignored -> true)) {
                entity.discard();
            }
        });

        if (NoellesRolesConfig.HANDLER.instance().allowCivillianGuessers) {
            GUESSER.killerOnly = false;
        }
        //NoellesRolesEntities.init();

    }

    EntityAttributeModifier tinyModifier = new EntityAttributeModifier(Identifier.of(MOD_ID, "tiny_modifier"), -0.15, EntityAttributeModifier.Operation.ADD_VALUE);



        public void registerEvents() {




        //
        // 死亡保护 / 反伤逻辑
        //
        /*
         * 这里不再把所有死亡逻辑硬塞进主类。
         * 现在由死亡引导器统一注册监听器，再按旧顺序分发到各职业自己的处理器。
         *
         * 这样做有两个直接收益：
         * 1. 每个职业的死亡特判都回到自己的包里，后续维护时更容易定位；
         * 2. 仍然完整保留了旧实现对短路顺序、回放字段和连锁死亡时机的要求。
         */
        NoellesRolesDeathBootstrap.init();

        AllowPlayerPunching.EVENT.register(((playerEntity, playerEntity1) -> {
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(playerEntity.getWorld());
            return (gameWorldComponent.isRole(playerEntity, Noellesroles.MIMIC) && playerEntity.getMainHandStack().isOf(ModItems.FAKE_KNIFE))
                    || (gameWorldComponent.isRole(playerEntity, Noellesroles.ASSASSIN) && playerEntity.getMainHandStack().isOf(ModItems.BAYONET));
        }));
        ModifierAssigned.EVENT.register(((playerEntity, modifier) -> {
            if (modifier.equals(TINY)) {
                playerEntity.getAttributeInstance(EntityAttributes.GENERIC_SCALE).removeModifier(tinyModifier);
                playerEntity.getAttributeInstance(EntityAttributes.GENERIC_SCALE).addPersistentModifier(tinyModifier);
            }
            if (modifier.equals(FEATHER)) {
                playerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, StatusEffectInstance.INFINITE, 0, true, false));
            }
        }));
        ResetPlayerEvent.EVENT.register(((playerEntity) -> {
            playerEntity.removeStatusEffect(StatusEffects.SLOW_FALLING);
            playerEntity.getAttributeInstance(EntityAttributes.GENERIC_SCALE).removeModifier(tinyModifier);
            DelusionPlayerComponent.KEY.get(playerEntity).reset();
            PhantomPlayerComponent.KEY.get(playerEntity).reset();
        }));
        CanSeePoison.EVENT.register((player)->{
            GameWorldComponent gameWorldComponent = (GameWorldComponent) GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorldComponent.isRole((PlayerEntity) player, Noellesroles.BARTENDER)) {
                return true;
            }
            return false;
        });
        ShouldDropOnDeath.EVENT.register(((itemStack,identifier) -> {
            return itemStack.isOf(ModItems.MASTER_KEY);
        }));
        /*
         * 职业分配时的初始化逻辑已经迁移到各职业自己的处理器里。
         * 这里保留一个统一引导入口，原因是 ModdedRoleAssigned 会把所有监听器全部执行一遍，
         * 为了不改变“先写通用冷却，再由特定职业覆盖”的旧顺序，
         * 我们仍然只注册一次监听器，再在引导器内部按原顺序分发。
         */
        NoellesRolesRoleAssignedBootstrap.init();


        ServerTickEvents.END_SERVER_TICK.register(((server) -> {
            for (ServerWorld world : server.getWorlds()) {
                GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
                for (ServerPlayerEntity player : world.getPlayers()) {
                    if (gameWorld.isRole(player, ANGEL)) {
                        PlayerMoodComponent.KEY.get(player).setMoodDrainMultiplier(AngelConstants.MOOD_DRAIN_MULTIPLIER);
                    } else if (gameWorld.isRole(player, COWARD)) {
                        PlayerMoodComponent.KEY.get(player).setMoodDrainMultiplier(CowardPlayerComponent.KEY.get(player).getCurrentSanMultiplier());
                    } else {
                        PlayerMoodComponent.KEY.get(player).setMoodDrainMultiplier(1.0f);
                    }
                }
            }

            // 每 tick 推进交换者的延迟交换任务。
            // 交换不会在发动瞬间立刻生效，而是在这里等待随机延迟结束后再执行。
            SwapperAbility.tickPendingSwaps(server);

            /*
             * 回溯者购买的末影珍珠本身是 Minecraft 原版物品，
             * 因此这里不去覆盖原物品逻辑，而是在服务端检测“新生成出来的末影珍珠实体”。
             *
             * 只有满足以下条件时才记录：
             * 1. 珍珠的 owner 是玩家；
             * 2. 该玩家当前职业确实是回溯者；
             * 3. 这颗珍珠此前还没有被打过回放标记。
             *
             * 这样既能保证只在真正成功投出时记录，也不会误伤其他职业或重复播报。
             */
            for (ServerWorld world : server.getWorlds()) {
                for (EnderPearlEntity pearl : world.getEntitiesByType(EntityType.ENDER_PEARL, entity -> true)) {
                    if (pearl.getCommandTags().contains("noellesroles_replay_recorded")) {
                        continue;
                    }
                    if (!(pearl.getOwner() instanceof net.minecraft.server.network.ServerPlayerEntity owner)) {
                        continue;
                    }
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
                    if (!gameWorld.isRole(owner, RECALLER)) {
                        continue;
                    }
                    GameRecordManager.recordGlobalEvent(world, RECALLER_ENDER_PEARL_THROWN_EVENT, owner, null);
                    pearl.addCommandTag("noellesroles_replay_recorded");
                }

                /*
                 * 原版风弹同样走“实体真正生成后再记录”的策略。
                 *
                 * 这样可以避免：
                 * 1. 玩家空挥右键但没有成功发射时误记事件；
                 * 2. 后续若有别的职业也能接触到风弹时，被无差别混进风灵师专属回放里。
                 */
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
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
                    if (!gameWorld.isRole(owner, WINDER)) {
                        continue;
                    }
                    GameRecordManager.recordGlobalEvent(world, WINDER_WIND_CHARGE_USED_EVENT, owner, null);
                    windCharge.addCommandTag("noellesroles_replay_recorded");
                }
            }

            if (server.getPlayerManager().getCurrentPlayerCount() >= 12) {
                Harpymodloader.setRoleMaximum(MIMIC,1);
            } else {
                Harpymodloader.setRoleMaximum(MIMIC,0);
            }
            if (server.getPlayerManager().getCurrentPlayerCount() >= 8) {
                Harpymodloader.setRoleMaximum(VULTURE,1);
            } else {
                Harpymodloader.setRoleMaximum(VULTURE,0);
            }

            /*
             * 更好的义警属于“义警阵营替换职业”，
             * 只有当本局理论上的义警位数达到 4 及以上时，才允许进入职业池。
             *
             * 这里按当前服务器人数和 Wathe 的义警分母实时换算，
             * 这样后续如果房主改了 vigilanteDividend，这个限制也会自动跟着生效。
             */
            int vigilanteSlots = 0;
            if (!server.getPlayerManager().getPlayerList().isEmpty()) {
                ServerWorld overworld = server.getOverworld();
                GameWorldComponent gameWorld = GameWorldComponent.KEY.get(overworld);
                vigilanteSlots = (int) Math.floor((float) server.getPlayerManager().getCurrentPlayerCount() / (float) gameWorld.getVigilanteDividend());
            }
            Harpymodloader.setRoleMaximum(BETTER_VIGILANTE, vigilanteSlots >= 4 ? 1 : 0);
        }));
        if (!NoellesRolesConfig.HANDLER.instance().shitpostRoles) {
            HarpyModLoaderConfig.HANDLER.load();
            if (!HarpyModLoaderConfig.HANDLER.instance().disabled.contains(AWESOME_BINGLUS_ID.getPath())) {
                HarpyModLoaderConfig.HANDLER.instance().disabled.add(AWESOME_BINGLUS_ID.getPath());
            }
            if (!HarpyModLoaderConfig.HANDLER.instance().disabled.contains(BETTER_VIGILANTE_ID.getPath())) {
                HarpyModLoaderConfig.HANDLER.instance().disabled.add(BETTER_VIGILANTE_ID.getPath());
            }
            if (!HarpyModLoaderConfig.HANDLER.instance().disabled.contains(THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES_ID.getPath())) {
                HarpyModLoaderConfig.HANDLER.instance().disabled.add(THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES_ID.getPath());
            }
            HarpyModLoaderConfig.HANDLER.save();
        }


    }


    public void registerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(StalkerGazeC2SPacket.ID, (payload, context) -> {
            context.server().execute(() -> StalkerAbility.handleGaze(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(StalkerDashC2SPacket.ID, (payload, context) -> {
            context.server().execute(() -> StalkerAbility.handleDash(payload, context.player()));
        });

        ServerPlayNetworking.registerGlobalReceiver(CrystalBallMarkC2SPacket.ID, (payload, context) -> {
            context.server().execute(() -> ProphetAbility.handleCrystalBallMark(context.player(), payload.targetId(), payload.offHand()));
        });
        ServerPlayNetworking.registerGlobalReceiver(BayonetKnockbackC2SPacket.ID, new BayonetKnockbackC2SPacket.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(BayonetStabC2SPacket.ID, new BayonetStabC2SPacket.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(SniperRifleShootC2SPacket.ID, SniperRifleShootC2SPacket::handle);


        ServerPlayNetworking.registerGlobalReceiver(GoddessC2SPacket.ID, (payload, context) -> {
            context.server().execute(() -> GoddessAbility.handle(payload, context.player()));
        });

        ServerPlayNetworking.registerGlobalReceiver(BrainwasherC2SPacket.ID, (payload, context) -> {
            context.server().execute(() -> BrainwasherAbility.handle(payload, context.player()));
        });


        ServerPlayNetworking.registerGlobalReceiver(CorpsemakerC2SPacket.ID, (payload, context) -> {
            CorpsemakerAbility.handle(payload, context.player());
        });



        ServerPlayNetworking.registerGlobalReceiver(ControllerPossessC2SPacket.ID, (payload, context) -> {
            context.server().execute(() -> ControllerPossessAbility.handle(payload, context.player()));
        });

        ServerPlayNetworking.registerGlobalReceiver(ControllerReleaseC2SPacket.ID, (payload, context) -> {
            context.server().execute(() -> ControllerReleaseAbility.handle(payload, context.player()));
        });

        ServerPlayNetworking.registerGlobalReceiver(SpiritualistPossessionControlC2SPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
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

                /*
                 * 右键起手单独做一次“立即执行”。
                 *
                 * 原先完全依赖 server tick 里的状态边沿判定时，
                 * 灵术师点击 use 到宿主真正交互之间总会额外慢半拍。
                 * 这里在收到“这一帧首次开始按 use”的瞬间就先执行一次，
                 * 后续持续按住仍然交给原本的 tick 逻辑维护。
                 */
                if (!wasUsing && payload.using()) {
                    var host = SpiritualistManager.getCurrentPossessionTarget(player);
                    if (host != null) {
                        SpiritualistManager.handleImmediatePossessionUse(player, host);
                    }
                }
            });
        });


        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.MORPH_PACKET, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                var world = player.getWorld();
                var gameWorld = GameWorldComponent.KEY.get(world);

                // 依次检查角色，注意验尸官有卸除伪装逻辑，需优先处理
                if (gameWorld.isRole(player, Noellesroles.CORONER)) {
                    CoronerMorphAbility.handle(payload, player);
                } else if (gameWorld.isRole(player, Noellesroles.VOODOO)) {
                    VoodooTargetAbility.handle(payload, player);
                } else if (gameWorld.isRole(player, Noellesroles.WINDER)) {
                    WinderTargetAbility.handle(payload, player);
                } else if (gameWorld.isRole(player, Noellesroles.MAGICIAN)) {
                    MagicianTargetAbility.handle(payload, player);
                } else if (gameWorld.isRole(player, Noellesroles.MORPHLING)) {
                    MorphlingMorphAbility.handle(payload, player);
                }
                // 其他可能使用该数据包的角色可在此扩展
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.VULTURE_PACKET, (payload, context) -> {
            context.server().execute(() -> VultureAbility.handle(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.SWAP_PACKET, (payload, context) -> {
            context.server().execute(() -> SwapperAbility.handle(payload, context.player()));
        });


        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.GUESS_PACKET, (payload, context) -> {
            context.server().execute(() -> GuesserAbility.handle(payload, context.player()));
        });

        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.OPERATOR_PACKET, (payload, context) -> {
            context.server().execute(() -> OperatorAbility.handle(payload, context.player()));
        });

        ServerPlayNetworking.registerGlobalReceiver(Noellesroles.ABILITY_PACKET, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                var gameWorld = GameWorldComponent.KEY.get(player.getWorld());

                if (gameWorld.isRole(player, Noellesroles.RECALLER)) {
                    RecallerAbility.handle(player);
                } else if (gameWorld.isRole(player, Noellesroles.ANGEL)) {
                    AngelAbility.handle(player, payload.targetId());
                } else if (gameWorld.isRole(player, Noellesroles.PROPHET)) {
                    ProphetAbility.handle(player);
                } else if (gameWorld.isRole(player, Noellesroles.PHANTOM)) {
                    PhantomAbility.handle(player);
                } else if (gameWorld.isRole(player, Noellesroles.WINDER)) {
                    WinderAbility.handle(player);
                } else if (gameWorld.isRole(player, Noellesroles.MAGICIAN)) {
                    MagicianAbility.handle(player);
                } else if (gameWorld.isRole(player, Noellesroles.SPIRITUALIST)) {
                    SpiritualistAbility.handle(player, payload.targetId());
                }
                // 可继续添加其他使用ABILITY_PACKET的角色
            });
        });
    }

    /**
     * 注册 noellesroles 自己的试剂回放格式化器。
     *
     * <p>wathe 负责统一采集事件；这里负责把 noellesroles 的专属事件翻译成对应语言文本。</p>
     */
    private static void registerReplayFormatters() {
        ReplayRegistry.registerItemUseFormatter(net.minecraft.registry.Registries.ITEM.getId(ModItems.DEFENSE_VIAL), NoellesRolesReplayFormatters::formatDefenseVialUse);
        ReplayRegistry.registerItemUseFormatter(net.minecraft.registry.Registries.ITEM.getId(ModItems.DELUSION_VIAL), NoellesRolesReplayFormatters::formatDelusionVialUse);
        ReplayRegistry.registerItemUseFormatter(net.minecraft.registry.Registries.ITEM.getId(ModItems.SEDATIVE), NoellesRolesReplayFormatters::formatSedativeUse);
        ReplayRegistry.registerItemUseFormatter(net.minecraft.registry.Registries.ITEM.getId(ModItems.TIMED_BOMB), NoellesRolesReplayFormatters::formatTimedBombUse);
        ReplayRegistry.registerItemUseFormatter(net.minecraft.registry.Registries.ITEM.getId(ModItems.THROWING_AXE), NoellesRolesReplayFormatters::formatThrowingAxeUse);
        ReplayRegistry.registerItemUseFormatter(net.minecraft.registry.Registries.ITEM.getId(ModItems.ROLE_MINE), NoellesRolesReplayFormatters::formatRoleMineUse);
        ReplayRegistry.registerItemUseFormatter(net.minecraft.registry.Registries.ITEM.getId(ModItems.TOOLBOX), NoellesRolesReplayFormatters::formatToolboxUse);
        ReplayRegistry.registerItemUseFormatter(net.minecraft.registry.Registries.ITEM.getId(ModItems.CAPTURE_DEVICE), NoellesRolesReplayFormatters::formatCaptureDeviceUse);
        ReplayRegistry.registerItemUseFormatter(net.minecraft.registry.Registries.ITEM.getId(ModItems.WIND_MARK), NoellesRolesReplayFormatters::formatWindMarkUse);
        ReplayRegistry.registerItemUseFormatter(net.minecraft.registry.Registries.ITEM.getId(ModItems.CRYSTAL_BALL), NoellesRolesReplayFormatters::formatCrystalBallUse);
        ReplayRegistry.registerItemUseFormatter(net.minecraft.registry.Registries.ITEM.getId(ModItems.SNIPER_RIFLE), NoellesRolesReplayFormatters::formatSniperRifleUse);
        ReplayRegistry.registerItemUseFormatter(net.minecraft.registry.Registries.ITEM.getId(Items.ENDER_PEARL), NoellesRolesReplayFormatters::formatRecallerEnderPearl);
        ReplayRegistry.registerItemUseFormatter(net.minecraft.registry.Registries.ITEM.getId(ModItems.FAKE_GRENADE), NoellesRolesReplayFormatters::formatFakeGrenadeUse);
        ReplayRegistry.registerItemUseFormatter(net.minecraft.registry.Registries.ITEM.getId(ModItems.SILENT_GRENADE), NoellesRolesReplayFormatters::formatSilentGrenadeUse);
        ReplayRegistry.registerItemHitFormatter(net.minecraft.registry.Registries.ITEM.getId(ModItems.SNIPER_RIFLE), NoellesRolesReplayFormatters::formatSniperRifleHit);

        /*
         * 托盘放置事件优先按 tray effect 分发。
         *
         * 这样可以明确区分：
         * 1. “玩家普通使用了这个物品”
         * 2. “玩家把这个效果塞进了托盘”
         *
         * 对防御试剂、幻觉试剂、定时炸弹这类事件来说，第二种语义才是我们真正想回放的内容。
         */
        ReplayRegistry.registerTrayEffectPlacementFormatter(DEFENSE_TRAY_EFFECT, NoellesRolesReplayFormatters::formatDefenseVialUse);
        ReplayRegistry.registerTrayEffectPlacementFormatter(DELUSION_TRAY_EFFECT, NoellesRolesReplayFormatters::formatDelusionVialUse);
        ReplayRegistry.registerTrayEffectPlacementFormatter(SEDATIVE_TRAY_EFFECT, NoellesRolesReplayFormatters::formatSedativeUse);
        ReplayRegistry.registerTrayEffectPlacementFormatter(TIMED_BOMB_TRAY_EMBEDDED_EVENT, NoellesRolesReplayFormatters::formatTimedBombTrayEmbedded);
        ReplayRegistry.registerBedEffectPlacementFormatter(TIMED_BOMB_BED_EMBEDDED_EVENT, NoellesRolesReplayFormatters::formatTimedBombBedEmbedded);

        ReplayRegistry.registerTrayEffectTakeFormatter(DEFENSE_TRAY_EFFECT, NoellesRolesReplayFormatters::formatDefensePlatterTake);
        ReplayRegistry.registerTrayEffectTakeFormatter(DELUSION_TRAY_EFFECT, NoellesRolesReplayFormatters::formatDelusionPlatterTake);
        ReplayRegistry.registerTrayEffectTakeFormatter(SEDATIVE_TRAY_EFFECT, NoellesRolesReplayFormatters::formatSedativePlatterTake);
        ReplayRegistry.registerTrayEffectTakeFormatter(TIMED_BOMB_TRAY_EMBEDDED_EVENT, NoellesRolesReplayFormatters::formatTimedBombTrayTake);

        ReplayRegistry.registerTrayEffectConsumeFormatter(DEFENSE_TRAY_EFFECT, NoellesRolesReplayFormatters::formatDefenseConsume);
        ReplayRegistry.registerTrayEffectConsumeFormatter(DELUSION_TRAY_EFFECT, NoellesRolesReplayFormatters::formatDelusionConsume);
        ReplayRegistry.registerTrayEffectConsumeFormatter(SEDATIVE_TRAY_EFFECT, NoellesRolesReplayFormatters::formatSedativeConsume);

        ReplayRegistry.registerShieldSourceFormatter(DEFENSE_TRAY_EFFECT, NoellesRolesReplayFormatters::formatDefenseShieldBlocked);
        ReplayRegistry.registerShieldSourceFormatter(ANGEL_GUARD_SHIELD_SOURCE, NoellesRolesReplayFormatters::formatAngelGuardShieldBlocked);
        ReplayRegistry.registerShieldSourceFormatter(SPIRITUALIST_ACTIVE_SHIELD_SOURCE, NoellesRolesReplayFormatters::formatSpiritualistActiveShieldBlocked);
        ReplayRegistry.registerShieldSourceFormatter(SPIRITUALIST_LINGERING_SHIELD_SOURCE, NoellesRolesReplayFormatters::formatSpiritualistLingeringShieldBlocked);

        ReplayRegistry.registerGlobalEventFormatter(DELUSION_STARTED_EVENT, NoellesRolesReplayFormatters::formatDelusionStarted);
        ReplayRegistry.registerGlobalEventFormatter(DELUSION_ENDED_EVENT, NoellesRolesReplayFormatters::formatDelusionEnded);
        ReplayRegistry.registerGlobalEventFormatter(COWARD_DANGER_SENSED_EVENT, NoellesRolesReplayFormatters::formatCowardDangerSensed);
        ReplayRegistry.registerGlobalEventFormatter(COWARD_DANGER_LEFT_EVENT, NoellesRolesReplayFormatters::formatCowardDangerLeft);
        ReplayRegistry.registerGlobalEventFormatter(SEDATIVE_STARTED_EVENT, NoellesRolesReplayFormatters::formatSedativeStarted);
        ReplayRegistry.registerGlobalEventFormatter(SEDATIVE_ENDED_EVENT, NoellesRolesReplayFormatters::formatSedativeEnded);
        ReplayRegistry.registerGlobalEventFormatter(TIMED_BOMB_ACTIVATED_EVENT, NoellesRolesReplayFormatters::formatTimedBombActivated);
        ReplayRegistry.registerGlobalEventFormatter(TIMED_BOMB_TRAY_EMBEDDED_EVENT, NoellesRolesReplayFormatters::formatTimedBombTrayEmbedded);
        ReplayRegistry.registerGlobalEventFormatter(TIMED_BOMB_BED_EMBEDDED_EVENT, NoellesRolesReplayFormatters::formatTimedBombBedEmbedded);
        ReplayRegistry.registerGlobalEventFormatter(TIMED_BOMB_BED_TRIGGERED_EVENT, NoellesRolesReplayFormatters::formatTimedBombBedTriggered);
        ReplayRegistry.registerGlobalEventFormatter(ROLE_MINE_DETECTED_EVENT, NoellesRolesReplayFormatters::formatRoleMineDetected);
        ReplayRegistry.registerGlobalEventFormatter(ROLE_MINE_REPORT_EVENT, NoellesRolesReplayFormatters::formatRoleMineReport);
        ReplayRegistry.registerGlobalEventFormatter(CAPTURE_DEVICE_TRIGGERED_EVENT, NoellesRolesReplayFormatters::formatCaptureDeviceTriggered);
        ReplayRegistry.registerGlobalEventFormatter(CAPTURE_DEVICE_REPORT_EVENT, NoellesRolesReplayFormatters::formatCaptureDeviceReport);
        ReplayRegistry.registerGlobalEventFormatter(CAPTURE_DEVICE_EXPIRED_EVENT, NoellesRolesReplayFormatters::formatCaptureDeviceExpired);
        ReplayRegistry.registerGlobalEventFormatter(CAPTURE_DEVICE_RELEASED_EVENT, NoellesRolesReplayFormatters::formatCaptureDeviceReleased);
        ReplayRegistry.registerGlobalEventFormatter(POWER_RESTORED_EVENT, NoellesRolesReplayFormatters::formatPowerRestored);
        ReplayRegistry.registerGlobalEventFormatter(JESTER_PSYCHO_STARTED_EVENT, NoellesRolesReplayFormatters::formatJesterPsychoStarted);
        ReplayRegistry.registerGlobalEventFormatter(EXECUTIONER_TARGET_LOCKED_EVENT, NoellesRolesReplayFormatters::formatExecutionerTargetLocked);
        ReplayRegistry.registerGlobalEventFormatter(EXECUTIONER_TARGET_CHANGED_EVENT, NoellesRolesReplayFormatters::formatExecutionerTargetChanged);
        ReplayRegistry.registerGlobalEventFormatter(VULTURE_PROGRESS_EVENT, NoellesRolesReplayFormatters::formatVultureProgress);
        ReplayRegistry.registerGlobalEventFormatter(RECALLER_POSITION_SAVED_EVENT, NoellesRolesReplayFormatters::formatRecallerPositionSaved);
        ReplayRegistry.registerGlobalEventFormatter(RECALLER_TELEPORTED_EVENT, NoellesRolesReplayFormatters::formatRecallerTeleported);
        ReplayRegistry.registerGlobalEventFormatter(RECALLER_ENDER_PEARL_THROWN_EVENT, NoellesRolesReplayFormatters::formatRecallerEnderPearl);
        ReplayRegistry.registerGlobalEventFormatter(PHANTOM_INVISIBILITY_STARTED_EVENT, NoellesRolesReplayFormatters::formatPhantomInvisibilityStarted);
        ReplayRegistry.registerGlobalEventFormatter(PHANTOM_INVISIBILITY_ENDED_EVENT, NoellesRolesReplayFormatters::formatPhantomInvisibilityEnded);
        ReplayRegistry.registerGlobalEventFormatter(PROPHET_MARKED_EVENT, NoellesRolesReplayFormatters::formatProphetMarked);
        ReplayRegistry.registerGlobalEventFormatter(PROPHET_REMARKED_EVENT, NoellesRolesReplayFormatters::formatProphetRemarked);
        ReplayRegistry.registerGlobalEventFormatter(PROPHET_REVEALED_EVENT, NoellesRolesReplayFormatters::formatProphetRevealed);
        ReplayRegistry.registerGlobalEventFormatter(PROPHET_VOODOO_IMMUNITY_EVENT, NoellesRolesReplayFormatters::formatProphetVoodooImmunity);
        ReplayRegistry.registerGlobalEventFormatter(WINDER_WIND_MARK_APPLIED_EVENT, NoellesRolesReplayFormatters::formatWinderWindMarkApplied);
        ReplayRegistry.registerGlobalEventFormatter(WINDER_WIND_CHARGE_USED_EVENT, NoellesRolesReplayFormatters::formatWinderWindChargeUsed);
        ReplayRegistry.registerGlobalEventFormatter(WINDER_WIND_MARK_EXPIRED_EVENT, NoellesRolesReplayFormatters::formatWinderWindMarkExpired);
        ReplayRegistry.registerGlobalEventFormatter(WINDER_WIND_MARK_TRIGGERED_EVENT, NoellesRolesReplayFormatters::formatWinderWindMarkTriggered);
        ReplayRegistry.registerGlobalEventFormatter(WINDER_FLOAT_STARTED_EVENT, NoellesRolesReplayFormatters::formatWinderFloatStarted);
        ReplayRegistry.registerGlobalEventFormatter(WINDER_FLOAT_ENDED_EVENT, NoellesRolesReplayFormatters::formatWinderFloatEnded);
        ReplayRegistry.registerGlobalEventFormatter(WINDER_FLOAT_STOPPED_EARLY_EVENT, NoellesRolesReplayFormatters::formatWinderFloatStoppedEarly);
        ReplayRegistry.registerGlobalEventFormatter(STALKER_PHASE_ADVANCE_1_TO_2_EVENT, NoellesRolesReplayFormatters::formatStalkerPhaseAdvance12);
        ReplayRegistry.registerGlobalEventFormatter(STALKER_PHASE_ADVANCE_2_TO_3_EVENT, NoellesRolesReplayFormatters::formatStalkerPhaseAdvance23);
        ReplayRegistry.registerGlobalEventFormatter(STALKER_PHASE_REGRESS_3_TO_2_EVENT, NoellesRolesReplayFormatters::formatStalkerPhaseRegress32);
        ReplayRegistry.registerGlobalEventFormatter(NOISEMAKER_GLOW_STARTED_EVENT, NoellesRolesReplayFormatters::formatNoisemakerGlowStarted);
        ReplayRegistry.registerGlobalEventFormatter(NOISEMAKER_GLOW_ENDED_EVENT, NoellesRolesReplayFormatters::formatNoisemakerGlowEnded);
        ReplayRegistry.registerGlobalEventFormatter(MORPHLING_MORPH_STARTED_EVENT, NoellesRolesReplayFormatters::formatMorphlingMorphStarted);
        ReplayRegistry.registerGlobalEventFormatter(MORPHLING_MORPH_ENDED_EVENT, NoellesRolesReplayFormatters::formatMorphlingMorphEnded);
        ReplayRegistry.registerGlobalEventFormatter(SWAPPER_SWAP_SELECTED_EVENT, NoellesRolesReplayFormatters::formatSwapperSwapSelected);
        ReplayRegistry.registerGlobalEventFormatter(SWAPPER_SWAP_EXECUTED_EVENT, NoellesRolesReplayFormatters::formatSwapperSwapExecuted);
        ReplayRegistry.registerGlobalEventFormatter(CORPSEMAKER_FORGED_BODY_EVENT, NoellesRolesReplayFormatters::formatCorpsemakerForgedBody);
        ReplayRegistry.registerGlobalEventFormatter(VOODOO_BOUND_EVENT, NoellesRolesReplayFormatters::formatVoodooBound);
        ReplayRegistry.registerGlobalEventFormatter(GUESSER_DECLARED_EVENT, NoellesRolesReplayFormatters::formatGuesserDeclared);
        ReplayRegistry.registerGlobalEventFormatter(GUESSER_CORRECT_EVENT, NoellesRolesReplayFormatters::formatGuesserCorrect);
        ReplayRegistry.registerGlobalEventFormatter(GUESSER_WRONG_EVENT, NoellesRolesReplayFormatters::formatGuesserWrong);
        ReplayRegistry.registerGlobalEventFormatter(CONTROLLER_POSSESS_STARTED_EVENT, NoellesRolesReplayFormatters::formatControllerPossessStarted);
        ReplayRegistry.registerGlobalEventFormatter(CONTROLLER_POSSESS_STOPPED_EARLY_EVENT, NoellesRolesReplayFormatters::formatControllerPossessStoppedEarly);
        ReplayRegistry.registerGlobalEventFormatter(CONTROLLER_POSSESS_ENDED_EVENT, NoellesRolesReplayFormatters::formatControllerPossessEnded);
        ReplayRegistry.registerGlobalEventFormatter(ANGEL_SOOTHE_CAST_EVENT, NoellesRolesReplayFormatters::formatAngelSootheCast);
        ReplayRegistry.registerGlobalEventFormatter(ANGEL_SOOTHED_EVENT, NoellesRolesReplayFormatters::formatAngelSoothed);
        ReplayRegistry.registerGlobalEventFormatter(ANGEL_GUARD_SELECTED_EVENT, NoellesRolesReplayFormatters::formatAngelGuardSelected);
        ReplayRegistry.registerGlobalEventFormatter(SPIRITUALIST_PROJECTION_STARTED_EVENT, NoellesRolesReplayFormatters::formatSpiritualistProjectionStarted);
        ReplayRegistry.registerGlobalEventFormatter(SPIRITUALIST_PROJECTION_ENDED_EVENT, NoellesRolesReplayFormatters::formatSpiritualistProjectionEnded);
        ReplayRegistry.registerGlobalEventFormatter(SPIRITUALIST_POSSESSION_STARTED_EVENT, NoellesRolesReplayFormatters::formatSpiritualistPossessionStarted);
        ReplayRegistry.registerGlobalEventFormatter(SPIRITUALIST_POSSESSION_ENDED_EVENT, NoellesRolesReplayFormatters::formatSpiritualistPossessionEnded);
        ReplayRegistry.registerGlobalEventFormatter(OPERATOR_CONNECTION_FAILED_BOTH_DEAD_EVENT, NoellesRolesReplayFormatters::formatOperatorConnectionFailedBothDead);
        ReplayRegistry.registerGlobalEventFormatter(OPERATOR_CONNECTION_FAILED_ONE_DEAD_EVENT, NoellesRolesReplayFormatters::formatOperatorConnectionFailedOneDead);
        ReplayRegistry.registerGlobalEventFormatter(OPERATOR_CONNECTION_STARTED_EVENT, NoellesRolesReplayFormatters::formatOperatorConnectionStarted);
        ReplayRegistry.registerGlobalEventFormatter(OPERATOR_CONNECTION_ENDED_EVENT, NoellesRolesReplayFormatters::formatOperatorConnectionEnded);
        ReplayRegistry.registerGlobalEventFormatter(OPERATOR_CONNECTION_INTERRUPTED_EVENT, NoellesRolesReplayFormatters::formatOperatorConnectionInterrupted);
        ReplayRegistry.registerGlobalEventFormatter(OPERATOR_BROADCAST_FAILED_EVENT, NoellesRolesReplayFormatters::formatOperatorBroadcastFailed);
        ReplayRegistry.registerGlobalEventFormatter(OPERATOR_BROADCAST_STARTED_EVENT, NoellesRolesReplayFormatters::formatOperatorBroadcastStarted);
        ReplayRegistry.registerGlobalEventFormatter(OPERATOR_BROADCAST_ENDED_EVENT, NoellesRolesReplayFormatters::formatOperatorBroadcastEnded);
        ReplayRegistry.registerGlobalEventFormatter(OPERATOR_BROADCAST_INTERRUPTED_EVENT, NoellesRolesReplayFormatters::formatOperatorBroadcastInterrupted);
        ReplayRegistry.registerGlobalEventFormatter(REMEMBERER_RECALL_EVENT, NoellesRolesReplayFormatters::formatRemembererRecall);
        ReplayRegistry.registerGlobalEventFormatter(REMEMBERER_SNIPER_RELOADED_EVENT, NoellesRolesReplayFormatters::formatRemembererSniperReloaded);
        ReplayRegistry.registerGlobalEventFormatter(MAGICIAN_RECORDING_STARTED_EVENT, NoellesRolesReplayFormatters::formatMagicianRecordingStarted);
        ReplayRegistry.registerGlobalEventFormatter(MAGICIAN_RECORDING_FINISHED_EVENT, NoellesRolesReplayFormatters::formatMagicianRecordingFinished);
        ReplayRegistry.registerGlobalEventFormatter(MAGICIAN_RECORDING_STOPPED_EARLY_EVENT, NoellesRolesReplayFormatters::formatMagicianRecordingStoppedEarly);
        ReplayRegistry.registerGlobalEventFormatter(MAGICIAN_PLAYBACK_STARTED_EVENT, NoellesRolesReplayFormatters::formatMagicianPlaybackStarted);
        ReplayRegistry.registerGlobalEventFormatter(MAGICIAN_PLAYBACK_FINISHED_EVENT, NoellesRolesReplayFormatters::formatMagicianPlaybackFinished);
        ReplayRegistry.registerGlobalEventFormatter(MAGICIAN_PLAYBACK_STOPPED_EARLY_EVENT, NoellesRolesReplayFormatters::formatMagicianPlaybackStoppedEarly);
        ReplayRegistry.registerGlobalEventFormatter(MAGICIAN_PLAYBACK_FORCED_END_EVENT, NoellesRolesReplayFormatters::formatMagicianPlaybackForcedEnd);
        ReplayRegistry.registerDeathReasonFormatter(ANGEL_SACRIFICE_DEATH_REASON, NoellesRolesReplayFormatters::formatAngelSacrificeDeath);
        ReplayRegistry.registerDeathReasonFormatter(DEATH_REASON_SEDATIVE_OVERDOSE, NoellesRolesReplayFormatters::formatSedativeOverdoseDeath);
        ReplayRegistry.registerDeathReasonFormatter(SPIRITUALIST_SOUL_GUARD_DEATH_REASON, NoellesRolesReplayFormatters::formatSpiritualistSoulGuardDeath);
        ReplayRegistry.registerDeathReasonFormatter(DEATH_REASON_SNIPER_RIFLE, NoellesRolesReplayFormatters::formatSniperRifleDeath);
    }
}
