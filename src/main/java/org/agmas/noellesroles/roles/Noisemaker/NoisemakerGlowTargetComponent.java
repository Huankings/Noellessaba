package org.agmas.noellesroles.roles.Noisemaker;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 被大嗓门点亮的目标追踪组件。
 *
 * <p>之所以单独做这个组件，而不是只依赖一次性上发光效果，
 * 是因为你要求“自然结束”和“被提前清除”都要记同一条回放事件。
 * 因此这里要持续追踪目标状态，才能在服务器端稳定补发结束事件。</p>
 */
public class NoisemakerGlowTargetComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<NoisemakerGlowTargetComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "noisemaker_glow_target"), NoisemakerGlowTargetComponent.class);

    private final PlayerEntity player;
    private boolean active = false;
    private int remainingTicks = 0;
    private UUID sourceUuid = null;

    public NoisemakerGlowTargetComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    /**
     * 开始追踪一次新的点亮效果。
     */
    public void start(UUID sourceUuid, int durationTicks) {
        this.active = true;
        this.sourceUuid = sourceUuid;
        this.remainingTicks = Math.max(0, durationTicks);
        this.sync();
    }

    /**
     * 回合重置、玩家死亡、对局结束时无声清理状态。
     *
     * <p>这些场景不是用户想看的“发光结束”，因此不能播回放。</p>
     */
    public void reset() {
        this.active = false;
        this.remainingTicks = 0;
        this.sourceUuid = null;
        this.sync();
    }

    @Override
    public void serverTick() {
        if (!this.active) {
            return;
        }

        if (!GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            this.reset();
            return;
        }

        /*
         * 只要发光效果已经被移除，不管是自然结束还是别的逻辑提前清掉，
         * 都统一在这里结算“发光效果结束”事件。
         */
        if (!this.player.hasStatusEffect(StatusEffects.GLOWING)) {
            this.finishGlow();
            return;
        }

        this.remainingTicks--;
        if (this.remainingTicks <= 0) {
            this.finishGlow();
        } else if (this.remainingTicks % 20 == 0) {
            this.sync();
        }
    }

    private void finishGlow() {
        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.event(GameRecordTypes.GLOBAL_EVENT)
                    .world(serverPlayer.getServerWorld())
                    .put("event", Noellesroles.NOISEMAKER_GLOW_ENDED_EVENT.toString())
                    .putUuid("victim", serverPlayer.getUuid())
                    .record();
        }
        this.active = false;
        this.remainingTicks = 0;
        this.sourceUuid = null;
        this.sync();
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.active = tag.getBoolean("active");
        this.remainingTicks = Math.max(0, tag.getInt("remainingTicks"));
        this.sourceUuid = tag.containsUuid("sourceUuid") ? tag.getUuid("sourceUuid") : null;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("active", this.active);
        tag.putInt("remainingTicks", this.remainingTicks);
        if (this.sourceUuid != null) {
            tag.putUuid("sourceUuid", this.sourceUuid);
        }
    }
}
