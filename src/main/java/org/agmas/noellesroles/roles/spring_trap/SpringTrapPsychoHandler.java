package org.agmas.noellesroles.roles.spring_trap;

import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.psycho.PsychoModeProfile;
import dev.doctor4t.wathe.api.psycho.PsychoShieldResult;
import dev.doctor4t.wathe.api.psycho.PsychoVisualSettings;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.engineer.StunnedPlayerComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 弹簧陷阱状态。
 *
 * <p>这个状态复用 Wathe 疯魔 profile：临时给彩虹斧、锁快捷栏、结束回收、皮肤、环境音和 HUD 都交给 Wathe。
 * 额外的“非列车坠落不死亡，改为定身”通过疯魔护盾规则接入，避免再写 killPlayer mixin。</p>
 */
public final class SpringTrapPsychoHandler {
    public static final Identifier PROFILE_ID = NoellesRolesCore.id("spring_trap_mode");

    private SpringTrapPsychoHandler() {
    }

    public static void init() {
        ItemStack colorfulAxe = ModItems.COLORFUL_AXE.getDefaultStack();

        PsychoModeProfile profile = PsychoModeProfile.copyOf(PsychoModeApi.createDefaultProfile(), PROFILE_ID)
                .nameTranslationKey("psycho_mode.noellesroles.spring_trap")
                .shieldNameTranslationKey("psycho_shield.noellesroles.spring_trap")
                .endEventId(Wathe.id("psycho_mode_end"))
                .durationTicks(SpringTrapConstants.SPRING_TRAP_DURATION_TICKS)
                .armour(SpringTrapConstants.SPRING_TRAP_SHIELD_LAYERS)
                .grantedItems(java.util.List.of(colorfulAxe))
                .meleeKill(false, NoellesDeathReasons.DEATH_REASON_AXE)
                .shieldSourceId(NoellesEventIds.SPRING_TRAP_SHIELD_SOURCE)
                .hitSound(WatheSounds.ITEM_BAT_HIT)
                .shieldSound(WatheSounds.ITEM_PSYCHO_ARMOUR)
                .backgroundSound(NoellesRolesSounds.AMBIENT_SPRING_TRAP, true)
                .visualSettings(PsychoVisualSettings.skin(
                        NoellesRolesCore.id("textures/entity/spring_trap.png"),
                        NoellesRolesCore.id("textures/entity/spring_trap_thin.png"),
                        true
                ))
                .build();
        PsychoModeApi.registerProfile(profile);

        PsychoModeApi.registerShieldRule(NoellesRolesCore.id("spring_trap/non_fatal_root"), PsychoModeApi.DEFAULT_PRIORITY + 200, context -> {
            if (!PROFILE_ID.equals(context.profile().id())) {
                return PsychoShieldResult.PASS;
            }
            if (GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(context.deathReason())) {
                return PsychoShieldResult.BYPASS;
            }

            rootSpringTrap(context.victim());
            return PsychoShieldResult.BLOCK;
        });
    }

    public static boolean startSpringTrapMode(@NotNull PlayerEntity player) {
        if (player.getItemCooldownManager().isCoolingDown(ModItems.SPRING_TRAP)) {
            return false;
        }
        boolean started = PsychoModeApi.start(player, PROFILE_ID);
        if (started) {
            player.getItemCooldownManager().set(ModItems.SPRING_TRAP, SpringTrapConstants.SPRING_TRAP_COOLDOWN_TICKS);
        }
        return started;
    }

    public static boolean extendSpringTrapMode(@NotNull PlayerEntity player) {
        if (!PsychoModeApi.isActive(player, PROFILE_ID)) {
            return false;
        }

        PlayerPsychoComponent component = PlayerPsychoComponent.KEY.get(player);
        int remaining = component.getPsychoTicks();
        int next = remaining + SpringTrapConstants.SPRING_TRAP_ADD_TIME_TICKS;
        if (next > SpringTrapConstants.SPRING_TRAP_DURATION_TICKS) {
            return false;
        }
        component.setPsychoTicks(next);
        return true;
    }

    public static boolean isSpringTrapActive(PlayerEntity player) {
        return PsychoModeApi.isActive(player, PROFILE_ID);
    }

    private static void rootSpringTrap(PlayerEntity victim) {
        StunnedPlayerComponent.KEY.get(victim).stun(
                SpringTrapConstants.SPRING_TRAP_ROOT_TICKS,
                NoellesEventIds.SPRING_TRAP_UNROOTED_EVENT
        );
        if (victim instanceof ServerPlayerEntity serverVictim) {
            NbtCompound extra = new NbtCompound();
            extra.putUuid("victim", victim.getUuid());
            GameRecordManager.recordGlobalEvent(serverVictim.getServerWorld(), NoellesEventIds.SPRING_TRAP_ROOTED_EVENT, null, extra);
        }
    }
}
