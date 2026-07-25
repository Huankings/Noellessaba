package org.agmas.noellesroles.modifiers.allergic;

import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 过敏患者的玩家状态组件。
 *
 * <p>过敏类型、临时透视和护盾都需要跨 tick 保存；透视又要给客户端本能 handler 读取，
 * 因此放在 CCA 组件里比散落静态表更稳。这个组件只同步给玩家本人，
 * 因为过敏类型和剩余透视时间都不该暴露给其他存活玩家。</p>
 */
public class AllergicPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<AllergicPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "allergic"),
            AllergicPlayerComponent.class
    );

    private final PlayerEntity player;
    private boolean allergic = false;
    private String allergyType = AllergicConstants.ALLERGY_TYPE_NONE;
    private int shieldLayers = 0;
    private int glowTicks = 0;

    public AllergicPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public boolean isAllergic() {
        return this.allergic;
    }

    public String getAllergyType() {
        return this.allergyType;
    }

    public int getShieldLayers() {
        return this.shieldLayers;
    }

    public int getGlowTicks() {
        return this.glowTicks;
    }

    public boolean hasAllergicInstinct() {
        return this.glowTicks > 0;
    }

    public boolean hasShield() {
        return this.shieldLayers > 0;
    }

    public void assignRandomType() {
        this.allergic = true;
        this.allergyType = this.player.getRandom().nextBoolean()
                ? AllergicConstants.ALLERGY_TYPE_FOOD
                : AllergicConstants.ALLERGY_TYPE_DRINK;
        this.sync();
    }

    public void setGlowTicks(int ticks) {
        this.glowTicks = Math.max(0, ticks);
        this.sync();
    }

    public void giveShield() {
        /*
         * 旧 Starry 实现只有 1 层 armor；这里继续用“刷新到 1 层”而不是叠加，
         * 保持同一名过敏患者无法靠连续触发堆出多层免伤。
         */
        this.shieldLayers = 1;
        this.sync();
    }

    public void consumeShield() {
        if (this.shieldLayers <= 0) {
            return;
        }
        this.shieldLayers--;
        this.sync();
    }

    public void reset() {
        this.allergic = false;
        this.allergyType = AllergicConstants.ALLERGY_TYPE_NONE;
        this.shieldLayers = 0;
        this.glowTicks = 0;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void serverTick() {
        if (this.glowTicks <= 0) {
            return;
        }

        this.glowTicks--;
        if (this.glowTicks == 0) {
            /*
             * 透视结束只在边界同步和记录一次回放。
             * 客户端只需要知道“是否仍大于 0”，不需要每 tick 精确倒计时，
             * 这样可以避免旧实现每 tick 全量同步造成的额外网络噪声。
             */
            if (this.player instanceof ServerPlayerEntity serverPlayer) {
                GameRecordManager.recordGlobalEvent(
                        serverPlayer.getServerWorld(),
                        NoellesEventIds.ALLERGIC_INSTINCT_ENDED_EVENT,
                        serverPlayer,
                        null
                );
            }
            this.sync();
        }
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return this.player.equals(player);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        tag.putBoolean("allergic", this.allergic);
        tag.putString("allergyType", this.allergyType);
        tag.putInt("shieldLayers", this.shieldLayers);
        tag.putInt("glowTicks", this.glowTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        this.allergic = tag.contains("allergic") && tag.getBoolean("allergic");
        this.allergyType = tag.contains("allergyType") ? tag.getString("allergyType") : AllergicConstants.ALLERGY_TYPE_NONE;
        this.shieldLayers = tag.contains("shieldLayers") ? Math.max(0, tag.getInt("shieldLayers")) : 0;
        this.glowTicks = tag.contains("glowTicks") ? Math.max(0, tag.getInt("glowTicks")) : 0;
    }
}
