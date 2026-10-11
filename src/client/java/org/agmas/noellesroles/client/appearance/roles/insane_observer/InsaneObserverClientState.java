package org.agmas.noellesroles.client.appearance.roles.insane_observer;

import com.google.common.collect.Maps;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 疯狂观察者的客户端随机伪装映射。
 *
 * <p>这是纯客户端渲染缓存，不属于游戏世界状态，也不应加入时停者回溯快照。
 * 连接切换时必须清空，避免上一局玩家列表影响下一局。</p>
 */
public final class InsaneObserverClientState {
    private static final Map<UUID, UUID> SHUFFLED_PLAYER_ENTRIES_CACHE = Maps.newHashMap();
    private static int refreshTicks;

    private InsaneObserverClientState() {
    }

    public static Map<UUID, UUID> getShuffledPlayerEntriesCache() {
        return SHUFFLED_PLAYER_ENTRIES_CACHE;
    }

    public static void tick() {
        refreshTicks++;
        if (refreshTicks < 20 * 6) {
            return;
        }
        refreshTicks = 0;
        if (WatheClient.PLAYER_ENTRIES_CACHE == null || WatheClient.PLAYER_ENTRIES_CACHE.isEmpty()) {
            SHUFFLED_PLAYER_ENTRIES_CACHE.clear();
            return;
        }
        List<UUID> keys = new ArrayList<>(WatheClient.PLAYER_ENTRIES_CACHE.keySet());
        List<UUID> originalKeys = new ArrayList<>(WatheClient.PLAYER_ENTRIES_CACHE.keySet());
        Collections.shuffle(keys);
        SHUFFLED_PLAYER_ENTRIES_CACHE.clear();
        for (int index = 0; index < originalKeys.size(); index++) {
            SHUFFLED_PLAYER_ENTRIES_CACHE.put(originalKeys.get(index), keys.get(index));
        }
    }

    public static void reset() {
        refreshTicks = 0;
        SHUFFLED_PLAYER_ENTRIES_CACHE.clear();
    }
}
