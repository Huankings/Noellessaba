package org.agmas.noellesroles.client.roles.killer_queen;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.tray.TrayParticleRegistry;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.particle.DustParticleEffect;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenConstants;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenRules;
import org.joml.Vector3f;

/** 仅杀手皇后、杀手和杀手中立可见的酸液托盘双色粒子。 */
public final class KillerQueenTrayParticleHandler {
    private KillerQueenTrayParticleHandler() {
    }

    public static void init() {
        TrayParticleRegistry.registerProvider("noellesroles:killer_queen_acid", 300, context -> {
            if (!NoellesEventIds.KILLER_QUEEN_ACID_TRAY_EFFECT.toString().equals(context.plate().getTrayEffect())) return false;
            var role = GameWorldComponent.KEY.get(context.world()).getRole(context.viewer());
            if (!KillerQueenRules.isKillerOrKillerSidedNeutral(role)) return true;
            if (context.world().getRandom().nextInt(KillerQueenConstants.TRAY_PARTICLE_CHANCE_DENOMINATOR) != 0) return true;
            boolean blue = context.world().getRandom().nextBoolean();
            Vector3f color = blue
                    ? new Vector3f(KillerQueenConstants.TRAY_BLUE_RED, KillerQueenConstants.TRAY_BLUE_GREEN, KillerQueenConstants.TRAY_BLUE_BLUE)
                    : new Vector3f(KillerQueenConstants.TRAY_GREEN_RED, KillerQueenConstants.TRAY_GREEN_GREEN, KillerQueenConstants.TRAY_GREEN_BLUE);
            context.world().addParticle(new DustParticleEffect(color, KillerQueenConstants.TRAY_PARTICLE_SCALE),
                    context.pos().getX() + KillerQueenConstants.TRAY_PARTICLE_HORIZONTAL_OFFSET,
                    context.pos().getY() + KillerQueenConstants.TRAY_PARTICLE_Y_OFFSET,
                    context.pos().getZ() + KillerQueenConstants.TRAY_PARTICLE_HORIZONTAL_OFFSET,
                    0.0D, KillerQueenConstants.TRAY_PARTICLE_UP_SPEED, 0.0D);
            return true;
        });
    }
}
