package org.agmas.noellesroles.roles.physician;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.api.event.DelusionEvents;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.UUID;

/**
 * 医师对“有人中毒/进入幻觉”的 actionbar 提示。
 */
public final class PhysicianStatusAlertHandler {
    private static final Identifier KINSWATHE_ROBOT_ID = Identifier.of("kinswathe", "robot");
    private static boolean initialized = false;

    private PhysicianStatusAlertHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DelusionEvents.STARTED.register((player, applier) -> notifyPhysicians(player, applier, false));
    }

    public static void notifyPoisoned(@Nullable ServerPlayerEntity target, @Nullable UUID poisonerUuid) {
        if (target == null) {
            return;
        }
        notifyPhysicians(target, poisonerUuid, true);
    }

    private static void notifyPhysicians(ServerPlayerEntity target, @Nullable UUID applierUuid, boolean skipBartenderPoison) {
        if (GameFunctions.isPlayerSpectatingOrCreative(target)) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(target.getWorld());
        if (isKinswatheRobot(gameWorld, target)) {
            return;
        }
        if (skipBartenderPoison && applierUuid != null && gameWorld.isRole(applierUuid, Noellesroles.BARTENDER)) {
            return;
        }

        for (ServerPlayerEntity possiblePhysician : target.getServer().getPlayerManager().getPlayerList()) {
            if (!GameFunctions.isPlayerAliveAndSurvival(possiblePhysician)) {
                continue;
            }
            if (gameWorld.isRole(possiblePhysician, Noellesroles.PHYSICIAN)) {
                possiblePhysician.sendMessage(Text.translatable("tip.noellesroles.physician.poisoned").withColor(Color.RED.getRGB()), true);
            }
        }
    }

    private static boolean isKinswatheRobot(GameWorldComponent gameWorld, ServerPlayerEntity player) {
        return gameWorld.getRole(player) != null && KINSWATHE_ROBOT_ID.equals(gameWorld.getRole(player).identifier());
    }
}
