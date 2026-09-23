package org.agmas.noellesroles.roles.myers;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.api.psycho.PsychoModeProfile;
import dev.doctor4t.wathe.api.psycho.PsychoShieldContext;
import dev.doctor4t.wathe.api.psycho.PsychoShieldResult;
import dev.doctor4t.wathe.api.psycho.PsychoVisualSettings;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** 迈尔斯恶灵附身的 Wathe 疯魔 profile。 */
public final class MyersPsychoHandler {
    public static final Identifier PROFILE_ID = NoellesRolesCore.id("evil_possess");
    private static boolean initialized;

    private MyersPsychoHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        PsychoModeProfile profile = PsychoModeProfile.copyOf(PsychoModeApi.createDefaultProfile(), PROFILE_ID)
                .nameTranslationKey("psycho_mode.noellesroles.myers")
                .shieldNameTranslationKey("psycho_shield.noellesroles.myers")
                .endEventId(Wathe.id("psycho_mode_end"))
                .durationTicks(MyersConstants.EVIL_POSSESS_DURATION_TICKS)
                .armour(1)
                .grantedItems(List.of(ModItems.BUTCHER_KNIFE.getDefaultStack()))
                .meleeKill(false, GameConstants.DeathReasons.KNIFE)
                .backgroundSound(null, false)
                .visualSettings(PsychoVisualSettings.skin(
                        NoellesRolesCore.id("textures/entity/myers.png"),
                        NoellesRolesCore.id("textures/entity/myers_thin.png"), true
                ))
                .build();
        PsychoModeApi.registerProfile(profile);

        /*
         * 恶灵护盾默认仍显示 1 层，但每次被 Wathe 致死流程拦截前先补回一层，
         * 使 Wathe 默认的“BLOCK 后扣一层”最终仍然停在 1 层，达到实际无敌效果。
         */
        PsychoModeApi.registerShieldRule(NoellesRolesCore.id("myers/evil_possess_invulnerability"), 2000,
                MyersPsychoHandler::resolveShield);
    }

    public static boolean start(@NotNull PlayerEntity player) {
        if (!GameWorldComponentBridge.isMyers(player)) return false;
        boolean debug = GameFunctions.isPlayerSpectatingOrCreative(player);
        if (!debug && !GameFunctions.isPlayerAliveAndSurvival(player)) return false;
        if (!debug && player.getItemCooldownManager().isCoolingDown(ModItems.EVIL_POSSESS)) return false;
        boolean started = PsychoModeApi.start(player, PROFILE_ID);
        if (started) {
            if (!debug) player.getItemCooldownManager().set(ModItems.EVIL_POSSESS, MyersConstants.EVIL_POSSESS_COOLDOWN_TICKS);
            if (player.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                for (var online : serverWorld.getServer().getPlayerManager().getPlayerList()) {
                    online.playSoundToPlayer(NoellesRolesSounds.AMBIENT_EVIL_POSSESS,
                            net.minecraft.sound.SoundCategory.MASTER,
                            MyersConstants.EVIL_POSSESS_SOUND_VOLUME,
                            MyersConstants.EVIL_POSSESS_SOUND_PITCH);
                }
            }
        }
        return started;
    }

    public static boolean isActive(PlayerEntity player) {
        return PsychoModeApi.isActive(player, PROFILE_ID);
    }

    public static SoundEvent randomHitSound(net.minecraft.util.math.random.Random random) {
        return random.nextBoolean() ? NoellesRolesSounds.ITEM_BUTCHER_KNIFE_HIT_1 : NoellesRolesSounds.ITEM_BUTCHER_KNIFE_HIT_2;
    }

    /** 恶灵附身结束时给所有在线玩家播放一次结束音。 */
    public static void playReleaseSound(net.minecraft.server.network.ServerPlayerEntity source) {
        if (!(source.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld)) return;
        for (var online : serverWorld.getServer().getPlayerManager().getPlayerList()) {
            online.playSoundToPlayer(NoellesRolesSounds.AMBIENT_EVIL_RELEASE,
                    net.minecraft.sound.SoundCategory.MASTER, 1.0F, 1.0F);
        }
    }

    private static PsychoShieldResult resolveShield(PsychoShieldContext context) {
        if (!PROFILE_ID.equals(context.profile().id())) return PsychoShieldResult.PASS;
        if (GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(context.deathReason())) {
            return PsychoShieldResult.BYPASS;
        }
        context.component().setArmour(Math.max(MyersConstants.EVIL_POSSESS_MIN_INTERNAL_ARMOUR, context.component().getArmour()));
        return PsychoShieldResult.BLOCK;
    }

    private static final class GameWorldComponentBridge {
        private static boolean isMyers(PlayerEntity player) {
            return dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(player.getWorld())
                    .isRole(player, NoellesRoleRegistry.MYERS);
        }
    }
}
