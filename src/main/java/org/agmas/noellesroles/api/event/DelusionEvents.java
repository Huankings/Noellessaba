package org.agmas.noellesroles.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 幻觉试剂的运行时事件。
 *
 * <p>旧实现里，kinswathe 的梦者 / 医师等兼容逻辑都是通过“假毒药 marker”去猜测幻觉发生了。
 * 现在幻觉试剂已经脱离 PlayerPoisonComponent，自然也要提供一套明确的运行时事件给其它扩展模组接入。</p>
 */
public final class DelusionEvents {
    private DelusionEvents() {
    }

    public static final Event<Started> STARTED = EventFactory.createArrayBacked(Started.class, listeners -> (player, applier) -> {
        for (Started listener : listeners) {
            listener.onStarted(player, applier);
        }
    });

    public static final Event<Ended> ENDED = EventFactory.createArrayBacked(Ended.class, listeners -> player -> {
        for (Ended listener : listeners) {
            listener.onEnded(player);
        }
    });

    @FunctionalInterface
    public interface Started {
        void onStarted(ServerPlayerEntity player, @Nullable UUID applier);
    }

    @FunctionalInterface
    public interface Ended {
        void onEnded(ServerPlayerEntity player);
    }
}
