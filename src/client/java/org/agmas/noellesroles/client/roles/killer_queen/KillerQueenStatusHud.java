package org.agmas.noellesroles.client.roles.killer_queen;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.client.input.NoellesClientKeybindings;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenConstants;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenRules;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenWorldComponent;

/** 杀手皇后右下角能力状态提示。 */
public final class KillerQueenStatusHud {
    private KillerQueenStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/killer_queen/status", NoellesRoleRegistry.KILLER_QUEEN, context -> {
            if (NoellesClientKeybindings.getAbilityBind() == null) return;
            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            Text line;
            if (ability.cooldown > 0) {
                line = Text.translatable("tip.noellesroles.killer_queen.cooldown",
                        (ability.cooldown + KillerQueenConstants.TICKS_PER_SECOND - 1) / KillerQueenConstants.TICKS_PER_SECOND);
            } else {
                var hit = WeaponTargetingApi.getVisibleAlivePlayerTarget(context.player(), KillerQueenConstants.ABILITY_RANGE_BLOCKS);
                if (hit == null || !(hit.getEntity() instanceof PlayerEntity target)) {
                    line = Text.translatable("tip.noellesroles.killer_queen.need_target");
                } else {
                    line = resolveTargetLine(context.player(), target);
                }
            }
            NoellesHudSupport.drawBottomRightLine(context, line, KillerQueenConstants.ROLE_COLOR);
        });
    }

    private static Text resolveTargetLine(PlayerEntity queen, PlayerEntity target) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(queen.getWorld());
        var role = gameWorld.getRole(target);
        int balance = PlayerShopComponent.KEY.get(queen).balance;
        Text key = NoellesClientKeybindings.getAbilityBind().getBoundKeyLocalizedText();
        if (role != null && role.getFaction() == Faction.KILLER) {
            return balance < KillerQueenConstants.SHUFFLE_COST_COINS
                    ? Text.translatable("tip.noellesroles.killer_queen.shuffle_money", KillerQueenConstants.SHUFFLE_COST_COINS)
                    : Text.translatable("tip.noellesroles.killer_queen.shuffle_ready", key);
        }
        if (KillerQueenRules.isGoodOrIndependent(role)) {
            KillerQueenWorldComponent state = KillerQueenWorldComponent.KEY.get(queen.getWorld());
            boolean mimic = role == NoellesRoleRegistry.MIMIC;
            if (!mimic && !state.isMarkedBy(queen.getUuid(), target.getUuid())) {
                return Text.translatable("tip.noellesroles.killer_queen.invalid_target");
            }
            if (state.getAvailableRecruitments() <= 0) {
                return Text.translatable("tip.noellesroles.killer_queen.no_recruitment");
            }
            if (!mimic && balance < KillerQueenConstants.RECRUIT_COST_COINS) {
                return Text.translatable("tip.noellesroles.killer_queen.recruit_money", KillerQueenConstants.RECRUIT_COST_COINS);
            }
            return Text.translatable("tip.noellesroles.killer_queen.recruit_ready", key);
        }
        return Text.translatable("tip.noellesroles.killer_queen.invalid_target");
    }
}
