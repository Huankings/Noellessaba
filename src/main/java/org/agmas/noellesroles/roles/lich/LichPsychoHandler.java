package org.agmas.noellesroles.roles.lich;

import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.api.psycho.PsychoModeProfile;
import dev.doctor4t.wathe.api.psycho.PsychoVisualSettings;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 巫妖专属疯魔 profile。
 *
 * <p>疯魔持续时间、护盾、授予疯魔法杖、锁栏、皮肤、背景音乐和近战击杀都交给 Wathe
 * {@link PsychoModeApi} 管理，避免重写已有的疯魔状态机。</p>
 */
public final class LichPsychoHandler {
    public static final Identifier PROFILE_ID = NoellesRolesCore.id("psycho_lich");

    private static boolean initialized = false;

    private LichPsychoHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        PsychoModeProfile profile = PsychoModeProfile.copyOf(PsychoModeApi.createDefaultProfile(), PROFILE_ID)
                .nameTranslationKey("psycho_mode.noellesroles.lich")
                .shieldNameTranslationKey("psycho_shield.noellesroles.lich")
                .durationTicks(LichConstants.PSYCHO_LICH_DURATION_TICKS)
                .armour(LichConstants.PSYCHO_LICH_SHIELD_COUNT)
                .grantedItems(List.of(new ItemStack(ModItems.PSYCHO_STAFF)))
                .meleeKill(true, GameConstants.DeathReasons.BAT)
                .meleeWeaponPredicate((player, stack) -> stack.isOf(ModItems.PSYCHO_STAFF))
                .hitSound(WatheSounds.ITEM_BAT_HIT)
                .backgroundSound(NoellesRolesSounds.AMBIENT_LICH, true)
                .visualSettings(PsychoVisualSettings.skin(
                        NoellesRolesCore.id("textures/entity/lich.png"),
                        NoellesRolesCore.id("textures/entity/lich_thin.png"),
                        true
                ))
                .build();
        PsychoModeApi.registerProfile(profile);
    }

    public static boolean startLichPsycho(@NotNull PlayerEntity player) {
        boolean debugPlayer = GameFunctions.isPlayerSpectatingOrCreative(player);
        if (!debugPlayer) {
            if (!GameFunctions.isPlayerAliveAndSurvival(player)
                    || !GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.LICH)
                    || player.getItemCooldownManager().isCoolingDown(ModItems.PSYCHO_LICH)) {
                return false;
            }
        }

        boolean started = PsychoModeApi.start(player, PROFILE_ID);
        if (started && !debugPlayer) {
            player.getItemCooldownManager().set(ModItems.PSYCHO_LICH, LichConstants.PSYCHO_LICH_COOLDOWN_TICKS);
        }
        return started;
    }
}
