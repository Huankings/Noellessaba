package org.agmas.noellesroles.roles.engineer;

import dev.doctor4t.wathe.cca.WorldBlackoutComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.mixin.compat.WorldBlackoutComponentAccessor;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 工程师组件。
 * 当前主要承担“电力恢复系统”的服务器逻辑，结构上对齐 KinsWathe 的 TechnicianComponent。
 */
public class EngineerPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {

    public static final ComponentKey<EngineerPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "engineer"),
            EngineerPlayerComponent.class
    );

    private final PlayerEntity player;

    public EngineerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    /**
     * 尝试立即恢复整张地图的电力。
     * 1. 只有在真实停电中才允许购买。
     * 2. 恢复后会彻底结束停电状态，而不只是把灯点亮。
     * 3. 所有玩家都会获得一段夜视，持续时间直接取 Wathe 的 BLACKOUT_MAX_DURATION。
     */
    public static boolean tryRestorePower(@NotNull PlayerEntity player) {
        WorldBlackoutComponent blackoutComponent = WorldBlackoutComponent.KEY.get(player.getWorld());
        if (!blackoutComponent.isBlackoutActive()) {
            player.sendMessage(Text.translatable("shop.noellesroles.power_restoration_unavailable").withColor(0xAA0000), true);
            return false;
        }

        // 先设置道具冷却，避免短时间内重复触发。
        player.getItemCooldownManager().set(
                ModItems.POWER_RESTORATION,
                GameConstants.ITEM_COOLDOWNS.getOrDefault(ModItems.POWER_RESTORATION, 0)
        );

        // reset() 只会恢复灯光与黑暗详情列表，不会清空内部 ticks。
        // 这里通过 accessor 把剩余停电计时一并归零，确保“停电状态”真正结束。
        blackoutComponent.reset();
        ((WorldBlackoutComponentAccessor) blackoutComponent).noellesroles$setTicks(0);

        MinecraftServer server = player.getWorld().getServer();
        if (server == null) {
            return true;
        }

        // 夜视持续时间直接取 Wathe 里停电的最大时长，方便和原版停电节奏保持一致。
        int nightVisionDuration = GameConstants.BLACKOUT_MAX_DURATION;
        for (ServerPlayerEntity serverPlayer : server.getPlayerManager().getPlayerList()) {
            serverPlayer.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION,
                    nightVisionDuration,
                    0,
                    false,
                    false,
                    false
            ));
            serverPlayer.playSoundToPlayer(WatheSounds.BLOCK_LIGHT_TOGGLE, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.POWER_RESTORED_EVENT, serverPlayer, null);
        }

        return true;
    }

    @Override
    public void serverTick() {
        // 当前工程师组件不需要逐 tick 维护额外状态，保留空实现便于后续扩展。
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        // 当前组件只承载逻辑，不保存额外状态。
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        // 当前组件只承载逻辑，不读取额外状态。
    }
}
