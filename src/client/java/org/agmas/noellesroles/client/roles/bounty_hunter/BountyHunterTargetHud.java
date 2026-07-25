package org.agmas.noellesroles.client.roles.bounty_hunter;

import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterConstants;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterPlayerComponent;

/**
 * 赏金猎人左下角悬赏目标 HUD。
 */
public final class BountyHunterTargetHud {
    private BountyHunterTargetHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/bounty_hunter/target", NoellesRoleRegistry.BOUNTY_HUNTER, context -> {
            BountyHunterPlayerComponent bountyHunter = BountyHunterPlayerComponent.KEY.get(context.player());
            if (bountyHunter.getTarget() == null
                    || bountyHunter.getTarget().equals(context.player().getUuid())
                    || context.player().networkHandler == null) {
                return;
            }

            PlayerListEntry targetEntry = context.player().networkHandler.getPlayerListEntry(bountyHunter.getTarget());
            if (targetEntry == null) {
                return;
            }

            Text line = Text.translatable("hud.noellesroles.bounty_hunter.target", targetEntry.getProfile().getName());
            PlayerSkinDrawer.draw(context.drawContext(), targetEntry.getSkinTextures().texture(), 2, context.height() - 14, 12);
            context.drawContext().drawTextWithShadow(context.textRenderer(), line, 18, context.height() - 12, BountyHunterConstants.ROLE_COLOR);
        });
    }
}
