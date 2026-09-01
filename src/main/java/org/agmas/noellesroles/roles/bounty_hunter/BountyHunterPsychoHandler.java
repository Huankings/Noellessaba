package org.agmas.noellesroles.roles.bounty_hunter;

import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.psycho.PsychoModeProfile;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 赏金猎人的悬赏模式 profile。
 *
 * <p>悬赏模式复用 Wathe 疯魔的皮肤、HUD、背景音和回放结束机制，
 * 但授予物品、持续时间、护盾层数和锁栏物品都属于赏金猎人自己。
 * 这些规则必须留在 bounty_hunter 包里维护，避免后续职业扩展时混进公共大类。</p>
 */
public final class BountyHunterPsychoHandler {
    public static final Identifier PROFILE_ID = NoellesRolesCore.id("bounty_hunter_bounty_mode");

    private BountyHunterPsychoHandler() {
    }

    public static void init() {
        ItemStack derringer = ModItems.BOUNTY_DERRINGER.getDefaultStack();
        derringer.set(ModItems.BOUNTY_MODE_GRANTED, true);

        PsychoModeProfile profile = PsychoModeProfile.copyOf(PsychoModeApi.createDefaultProfile(), PROFILE_ID)
                .nameTranslationKey("psycho_mode.noellesroles.bounty_hunter")
                .shieldNameTranslationKey("psycho_shield.noellesroles.bounty_hunter")
                .endEventId(Wathe.id("psycho_mode_end"))
                .durationTicks(BountyHunterConstants.BOUNTY_MODE_DURATION_TICKS)
                .armour(BountyHunterConstants.BOUNTY_MODE_SHIELD_LAYERS)
                .grantedItems(java.util.List.of(derringer))
                .meleeKill(false, GameConstants.DeathReasons.BAT)
                .build();
        PsychoModeApi.registerProfile(profile);
    }
}
