package org.agmas.noellesroles.client.roles.party_animal;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.Style;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.party_animal.HeliumBuzzPlayerComponent;
import org.agmas.noellesroles.roles.party_animal.PartyAnimalConstants;

/** 派对狂准心指向变声目标时显示的类型和等级。 */
public final class PartyAnimalTargetHud {
    private PartyAnimalTargetHud() {}

    public static void register() {
        RoleNameHudApi.registerExtraHud(NoellesRolesCore.id("role_name/party_animal/pitch"), RoleNameHudApi.DEFAULT_PRIORITY - 20,
                PartyAnimalTargetHud::render);
    }

    private static void render(RoleNameHudApi.Context context) {
        ClientPlayerEntity viewer = context.player();
        PlayerEntity target = context.targetPlayer();
        if (!GameFunctions.isPlayerAliveAndSurvival(viewer) || target == null
                || !GameFunctions.isPlayerAliveAndSurvival(target)
                || !GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.PARTY_ANIMAL)) return;
        int level = HeliumBuzzPlayerComponent.KEY.get(target).getEffectiveLevel();
        if (level == 0) return;
        Text type = Text.translatable(level > 0
                ? "hud.noellesroles.party_animal.pitch_up"
                : "hud.noellesroles.party_animal.pitch_down");
        int color = levelColor(level);
        MutableText text = type.copy().append(Text.translatable("hud.noellesroles.party_animal.type_separator")).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(PartyAnimalConstants.ROLE_COLOR)))
                .append(Text.literal(Math.abs(level) + "级").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));
        var matrices = context.drawContext().getMatrices();
        matrices.push();
        matrices.translate(context.drawContext().getScaledWindowWidth() / 2.0F, context.drawContext().getScaledWindowHeight() / 2.0F - 35.0F, 0.0F);
        matrices.scale(0.6F, 0.6F, 1.0F);
        context.drawContext().drawTextWithShadow(context.renderer(), text, -context.renderer().getWidth(text) / 2, 48, 0xFFFFFFFF);
        matrices.pop();
    }

    private static int levelColor(int level) {
        return switch (level) {
            case 1 -> PartyAnimalConstants.PITCH_UP_COLOR_1;
            case 2 -> PartyAnimalConstants.PITCH_UP_COLOR_2;
            case 3 -> PartyAnimalConstants.PITCH_UP_COLOR_3;
            case -1 -> PartyAnimalConstants.PITCH_DOWN_COLOR_1;
            case -2 -> PartyAnimalConstants.PITCH_DOWN_COLOR_2;
            default -> PartyAnimalConstants.PITCH_DOWN_COLOR_3;
        };
    }
}
