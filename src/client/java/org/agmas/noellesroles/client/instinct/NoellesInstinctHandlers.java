package org.agmas.noellesroles.client.instinct;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.framing.DelusionPlayerComponent;
import org.agmas.noellesroles.roles.angel.AngelPlayerComponent;
import org.agmas.noellesroles.roles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.roles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.roles.executioner.ExecutionerPlayerComponent;
import org.agmas.noellesroles.roles.winder.WindMarkPlayerComponent;

import java.awt.Color;

public final class NoellesInstinctHandlers {
    private static final int PRIORITY_HIGH_INSTINCT_COLOR = 100;
    private static final int PRIORITY_ABILITY_MARK = 100;

    private NoellesInstinctHandlers() {
    }

    public static void register() {
        registerJesterAvailability();
        registerAbilityMarks();
        registerJesterInstinctColor();
        registerBomberBombColor();
        registerKillerSpecialTargetColors();
        registerExecutionerInstinctColor();
    }

    private static void registerJesterAvailability() {
        InstinctApi.registerAvailability(id("jester_instinct_availability"), InstinctApi.DEFAULT_PRIORITY, viewer -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(viewer.getWorld());
            if (gameWorldComponent.isRole(viewer, Noellesroles.JESTER) && WatheClient.isInstinctInputActive()) {
                /*
                 * Jester 不是杀手，但它拥有一套自己的本能键透视资格。
                 * priority 保持 0，表示它和 Wathe 默认杀手本能平级；
                 * Convener 的变形压制会以更高 priority 返回 DISABLE，所以仍能压住这里。
                 */
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });
    }

    private static void registerAbilityMarks() {
        InstinctApi.registerHighlight(id("ability_marks"), PRIORITY_ABILITY_MARK, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(viewer.getWorld());
            if (!GameFunctions.isPlayerSpectatingOrCreative(targetPlayer)) {
                WindMarkPlayerComponent windMark = WindMarkPlayerComponent.KEY.get(targetPlayer);
                BartenderPlayerComponent bartender = BartenderPlayerComponent.KEY.get(targetPlayer);
                DelusionPlayerComponent delusion = DelusionPlayerComponent.KEY.get(targetPlayer);
                PlayerPoisonComponent poison = PlayerPoisonComponent.KEY.get(targetPlayer);

                /*
                 * 这些是职业能力产生的目标标记，不依赖本能键是否开启。
                 * 因此它们不调用 WatheClient.isInstinctEnabled()，Convener 只压制本能链路时也不会误关掉它们。
                 */
                if (gameWorldComponent.isRole(viewer, Noellesroles.WINDER)
                        && WatheClient.isPlayerAliveAndInSurvival()
                        && windMark.hasActiveMark()
                        && GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                        && Wathe.isSkyVisibleAdjacent(target)) {
                    return InstinctApi.HighlightResult.color(Noellesroles.WINDER.color());
                }

                InstinctApi.HighlightResult bartenderResult = InstinctApi.HighlightResult.pass();
                if (gameWorldComponent.isRole(viewer, Noellesroles.BARTENDER) && bartender.glowTicks > 0) {
                    bartenderResult = InstinctApi.HighlightResult.color(Color.GREEN.getRGB());
                }
                if (gameWorldComponent.isRole(viewer, Noellesroles.BARTENDER) && bartender.armor > 0) {
                    return InstinctApi.HighlightResult.color(Color.BLUE.getRGB());
                }
                if (gameWorldComponent.isRole(viewer, Noellesroles.BARTENDER)
                        && (poison.poisonTicks > 0 || delusion.isActive())) {
                    bartenderResult = InstinctApi.HighlightResult.color(Color.RED.getRGB());
                }

                if (gameWorldComponent.isRole(viewer, Noellesroles.ANGEL)
                        && WatheClient.isPlayerAliveAndInSurvival()) {
                    AngelPlayerComponent angel = AngelPlayerComponent.KEY.get(viewer);
                    if (angel.getGuardedTarget() != null && angel.getGuardedTarget().equals(target.getUuid())) {
                        return InstinctApi.HighlightResult.color(Noellesroles.ANGEL.color());
                    }
                }
                if (bartenderResult.action() != InstinctApi.HighlightResult.Action.PASS) {
                    return bartenderResult;
                }
            }

            if (gameWorldComponent.isRole(viewer, Noellesroles.EXECUTIONER)) {
                ExecutionerPlayerComponent executioner = ExecutionerPlayerComponent.KEY.get(viewer);
                if (executioner.target != null && executioner.target.equals(target.getUuid())) {
                    return InstinctApi.HighlightResult.color(Color.YELLOW.getRGB());
                }
            }

            return InstinctApi.HighlightResult.pass();
        });
    }

    private static void registerJesterInstinctColor() {
        InstinctApi.registerHighlight(id("jester_instinct_color"), InstinctApi.DEFAULT_PRIORITY, (viewer, target) -> {
            if (target instanceof PlayerEntity
                    && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, Noellesroles.JESTER)
                    && WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.color(Color.PINK.getRGB());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }

    private static void registerBomberBombColor() {
        InstinctApi.registerHighlight(id("bomber_bomb_color"), PRIORITY_HIGH_INSTINCT_COLOR, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(viewer.getWorld());
            if (!GameFunctions.isPlayerSpectatingOrCreative(targetPlayer)
                    && gameWorldComponent.isRole(viewer, Noellesroles.BOMBER)
                    && WatheClient.isInstinctEnabled()
                    && BomberPlayerComponent.KEY.get(targetPlayer).hasBomb()) {
                /*
                 * 被 Bomber 塞炸弹的人需要压过 Wathe 默认杀手/心情色，
                 * 所以这里使用高于 0 的 priority。
                 */
                return InstinctApi.HighlightResult.color(Noellesroles.BOMBER.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }

    private static void registerKillerSpecialTargetColors() {
        InstinctApi.registerHighlight(id("killer_special_targets"), PRIORITY_HIGH_INSTINCT_COLOR, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer) || GameFunctions.isPlayerSpectatingOrCreative(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }
            if (!WatheClient.isInstinctEnabled() || !WatheClient.isKiller() || !WatheClient.isPlayerAliveAndInSurvival()) {
                return InstinctApi.HighlightResult.pass();
            }

            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(viewer.getWorld());
            if (gameWorldComponent.isRole(targetPlayer, Noellesroles.MIMIC)) {
                return InstinctApi.HighlightResult.color(MathHelper.hsvToRgb(0.0F, 1.0F, 0.6F));
            }

            Role role = gameWorldComponent.getRole(targetPlayer);
            if (role == null) {
                return InstinctApi.HighlightResult.pass();
            }
            if (Noellesroles.KILLER_SIDED_NEUTRALS.contains(role)) {
                return InstinctApi.HighlightResult.color(role.color());
            }
            if (!role.isInnocent() && !role.canUseKiller()) {
                return InstinctApi.HighlightResult.color(5168437);
            }
            return InstinctApi.HighlightResult.pass();
        });
    }

    private static void registerExecutionerInstinctColor() {
        InstinctApi.registerHighlight(id("executioner_instinct_color"), InstinctApi.DEFAULT_PRIORITY, (viewer, target) -> {
            if (target instanceof PlayerEntity
                    && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, Noellesroles.EXECUTIONER)
                    && WatheClient.isPlayerAliveAndInSurvival()
                    && WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.color(Noellesroles.EXECUTIONER.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }

    private static Identifier id(String path) {
        return Identifier.of(Noellesroles.MOD_ID, "instinct/" + path);
    }
}
