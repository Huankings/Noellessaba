package org.agmas.noellesroles.client.roles.insane_damned_paranoid_killer;

import net.minecraft.text.Text;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.insane_damned_paranoid_killer.InsaneDamnedKillerPlayerComponent;

/**
 * 亡语杀手右下角尸体伪装 HUD。
 */
public final class InsaneDamnedKillerStatusHud {
    private InsaneDamnedKillerStatusHud() {
    }

    public static void register() {
        NoellesHudSupport.registerAliveRole(
                "roles/insane_damned_paranoid_killer/corpse_status",
                NoellesRoleRegistry.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES,
                context -> {
                    if (NoellesrolesClient.abilityBind == null) {
                        return;
                    }

                    /*
                     * HUD 只给存活且当前职业为亡语杀手的本人显示。
                     * 这里直接读同步组件里的开关，用一行提示复刻 spark 版“按 G 开/关尸体伪装”的手感。
                     */
                    boolean corpseMode = InsaneDamnedKillerPlayerComponent.KEY.get(context.player()).isCorpseMode();
                    Text line = Text.translatable(
                            corpseMode
                                    ? "hud.noellesroles.the_insane_damned_paranoid_killer.corpse_active"
                                    : "hud.noellesroles.the_insane_damned_paranoid_killer.corpse_hint",
                            NoellesrolesClient.abilityBind.getBoundKeyLocalizedText()
                    );

                    NoellesHudSupport.drawBottomRightLine(
                            context,
                            line,
                            NoellesRoleRegistry.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES.color()
                    );
                }
        );
    }
}
