package org.agmas.noellesroles.roles.convener;

import dev.doctor4t.wathe.api.task.TaskCompletionApi;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 召集者完成任务获得反伤护盾的任务 API 接入。
 */
public final class ConvenerTaskShieldHandler {
    private static boolean initialized = false;

    private ConvenerTaskShieldHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TaskCompletionApi.AFTER_TASK_COMPLETE.register(context -> {
            if (!ConvenerConstants.COUNTER_SHIELD_ENABLED
                    || !context.gameWorld().isRunning()
                    || !GameFunctions.isPlayerAliveAndSurvival(context.player())
                    || context.role() != Noellesroles.CONVENER) {
                return;
            }

            /*
             * 任务 API 是 Wathe 的真实任务完成入口，比监听心情变化或客户端包更准确。
             * 这里只在服务端确认完成任务后累加护盾进度。
             */
            ConvenerPlayerComponent convener = ConvenerPlayerComponent.KEY.get(context.player());
            boolean gainedShield = convener.recordCompletedTask();
            convener.sync();
            if (gainedShield) {
                NbtCompound extra = new NbtCompound();
                extra.putInt("current_layers", convener.getCounterShieldLayers());
                GameRecordManager.recordGlobalEvent(
                        context.player().getServerWorld(),
                        Noellesroles.CONVENER_COUNTER_SHIELD_GAINED_EVENT,
                        context.player(),
                        extra
                );
            }
        });
    }
}
