package org.agmas.noellesroles.client.roles.vecna;

import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.vecna.VecnaConstants;

/** 维克那右下角能力提示。 */
public final class VecnaStatusHud {
    private VecnaStatusHud() {}
    public static void register() {
        NoellesHudSupport.registerAliveRole("roles/vecna/status", NoellesRoleRegistry.VECNA, context -> {
            if (NoellesrolesClient.abilityBind == null) return;
            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
            Text line;
            if (ability.cooldown > 0) {
                int seconds = (ability.cooldown + VecnaConstants.TICKS_PER_SECOND - 1) / VecnaConstants.TICKS_PER_SECOND;
                line = Text.translatable("tip.noellesroles.vecna.cooldown", seconds);
            } else {
                /*
                 * HUD 与能力包使用同一套 Wathe 目标选择 API：
                 * 这里仅读取客户端当前准心目标，不承担服务端授权；
                 * 服务端收到 G 包后仍会再次检查存活、距离和目标可见性。
                 */
                var hit = WeaponTargetingApi.getVisibleAlivePlayerTarget(context.player(), VecnaConstants.MARK_RANGE_BLOCKS);
                if (hit != null && hit.getEntity() instanceof PlayerEntity target
                        && target != context.player()
                        && GameFunctions.isPlayerAliveAndSurvival(target)) {
                    line = Text.translatable(
                            "tip.noellesroles.vecna.use_target",
                            NoellesrolesClient.abilityBind.getBoundKeyLocalizedText(),
                            target.getDisplayName()
                    );
                } else {
                    line = Text.translatable(
                            "tip.noellesroles.vecna.use_self",
                            NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()
                    );
                }
            }
            NoellesHudSupport.drawBottomRightLine(context, line, VecnaConstants.ROLE_COLOR);
        });
    }
}
