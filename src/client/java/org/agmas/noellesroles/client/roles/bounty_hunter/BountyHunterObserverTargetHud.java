package org.agmas.noellesroles.client.roles.bounty_hunter;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterPlayerComponent;

import java.util.UUID;

/** 普通非存活观察者准心查看赏金猎人时显示其当前悬赏目标。 */
public final class BountyHunterObserverTargetHud {
    private BountyHunterObserverTargetHud() {
    }

    public static void register() {
        RoleNameHudApi.registerAdditionalLine(
                NoellesRolesCore.id("role_name/bounty_hunter/observer_target"),
                90,
                context -> {
                    if (!GameFunctions.isPlayerSpectatingOrCreative(context.player())
                            || !(context.targetPlayer() instanceof PlayerEntity bountyHunter)
                            || context.player().networkHandler == null) {
                        return null;
                    }

                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(bountyHunter.getWorld());
                    if (!gameWorld.isRole(bountyHunter, NoellesRoleRegistry.BOUNTY_HUNTER)) {
                        return null;
                    }

                    UUID targetUuid = BountyHunterPlayerComponent.KEY.get(bountyHunter).getTarget();
                    if (targetUuid == null || targetUuid.equals(bountyHunter.getUuid())) {
                        return null;
                    }
                    PlayerListEntry targetEntry = context.player().networkHandler.getPlayerListEntry(targetUuid);
                    if (targetEntry == null || targetEntry.getProfile() == null) {
                        // 名称资料还没有同步完成时隐藏本行，避免显示 UUID 或错误的临时名称。
                        return null;
                    }

                    return RoleNameHudApi.AdditionalLine.below(
                            Text.translatable("hud.noellesroles.bounty_hunter.observer_target",
                                    targetEntry.getProfile().getName()),
                            NoellesRoleRegistry.BOUNTY_HUNTER.color(),
                            0.9F
                    );
                }
        );
    }
}
