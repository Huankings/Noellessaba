package org.agmas.noellesroles.client.roles.party_animal;

import dev.doctor4t.wathe.api.client.hud.HudOverlayContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.client.input.NoellesClientKeybindings;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.party_animal.HeliumBuzzPlayerComponent;
import org.agmas.noellesroles.roles.party_animal.PartyAnimalConstants;
import org.agmas.noellesroles.roles.party_animal.PartyAnimalPlayerComponent;

/** 派对狂右下角能力提示。 */
public final class PartyAnimalStatusHud {
    private PartyAnimalStatusHud() {}

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/party_animal/status", NoellesRoleRegistry.PARTY_ANIMAL, PartyAnimalStatusHud::render);
    }

    private static void render(HudOverlayContext context) {
        ClientPlayerEntity player = context.player();
        if (NoellesClientKeybindings.getAbilityBind() == null) return;
        PartyAnimalPlayerComponent state = PartyAnimalPlayerComponent.KEY.get(player);
        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        Text type = Text.translatable(state.getSelectedType() < 0
                ? "hud.noellesroles.party_animal.pitch_down"
                : "hud.noellesroles.party_animal.pitch_up");
        if (ability.cooldown > 0) {
            draw(context, Text.translatable("hud.noellesroles.party_animal.cooldown", Math.max(0, (ability.cooldown + 19) / 20)));
            return;
        }
        if (state.hasMarkedTarget()) {
            draw(context, Text.translatable("hud.noellesroles.party_animal.confirm", NoellesClientKeybindings.getAbilityBind().getBoundKeyLocalizedText(), type, Math.max(0, (state.getMarkTicksRemaining() + 19) / 20)));
            return;
        }
        PlayerEntityTarget target = PlayerEntityTarget.resolve(player);
        if (target.player != null) {
            int level = HeliumBuzzPlayerComponent.KEY.get(target.player).getEffectiveLevel();
            if (org.agmas.noellesroles.roles.party_animal.PartyAnimalAbility.calculateNextLevel(level, state.getSelectedType()) == Integer.MIN_VALUE) {
                draw(context, Text.translatable("hud.noellesroles.party_animal.target_max", type, PartyAnimalConstants.MAX_LEVEL));
            } else {
                draw(context, Text.translatable("hud.noellesroles.party_animal.mark_hint", NoellesClientKeybindings.getAbilityBind().getBoundKeyLocalizedText()));
            }
            return;
        }
        int selfLevel = HeliumBuzzPlayerComponent.KEY.get(player).getEffectiveLevel();
        if (org.agmas.noellesroles.roles.party_animal.PartyAnimalAbility.calculateNextLevel(selfLevel, state.getSelectedType()) == Integer.MIN_VALUE) {
            draw(context, Text.translatable("hud.noellesroles.party_animal.self_max", type, PartyAnimalConstants.MAX_LEVEL));
        } else {
            draw(context, Text.translatable("hud.noellesroles.party_animal.self_hint", NoellesClientKeybindings.getAbilityBind().getBoundKeyLocalizedText(), type));
        }
    }

    private static void draw(HudOverlayContext context, Text text) {
        NoellesHudSupport.drawBottomRightLine(context, text, PartyAnimalConstants.ROLE_COLOR);
    }

    private record PlayerEntityTarget(net.minecraft.entity.player.PlayerEntity player) {
        private static PlayerEntityTarget resolve(ClientPlayerEntity player) {
            HitResult result = ProjectileUtil.getCollision(player, entity -> entity instanceof net.minecraft.entity.player.PlayerEntity, (float) PartyAnimalConstants.TARGET_RANGE_BLOCKS);
            if (result instanceof EntityHitResult hit
                    && hit.getEntity() instanceof net.minecraft.entity.player.PlayerEntity target
                    && target != player
                    && dev.doctor4t.wathe.game.GameFunctions.isPlayerAliveAndSurvival(target)
                    && target.distanceTo(player) <= PartyAnimalConstants.TARGET_RANGE_BLOCKS) return new PlayerEntityTarget(target);
            return new PlayerEntityTarget(null);
        }
    }
}
