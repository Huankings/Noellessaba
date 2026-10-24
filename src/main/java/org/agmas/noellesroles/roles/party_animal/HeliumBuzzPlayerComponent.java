package org.agmas.noellesroles.roles.party_animal;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/** 玩家当前的普通变声等级与管理员调试变声等级。 */
public final class HeliumBuzzPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<HeliumBuzzPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            NoellesRolesCore.id("helium_buzz"), HeliumBuzzPlayerComponent.class
    );

    private final PlayerEntity player;
    private int normalLevel;
    private int debugLevel;
    /** 本次死亡清理前的等级快照，供 Wathe 随后计算武器冷却。 */
    private int pendingDeathLevel;

    public HeliumBuzzPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    /** 当前有效等级：非零调试等级优先，否则读取普通变声等级。 */
    public int getEffectiveLevel() {
        return debugLevel != 0 ? debugLevel : normalLevel;
    }

    public int getNormalLevel() {
        return normalLevel;
    }

    public int getDebugLevel() {
        return debugLevel;
    }

    public boolean hasDebugOverride() {
        return debugLevel != 0;
    }

    public void setNormalLevel(int level) {
        normalLevel = clamp(level);
        sync();
    }

    public void clearNormalLevel() {
        /* 普通等级与死亡后等待消费的等级都属于本局临时状态，回合结束必须一起清掉。 */
        if (normalLevel == 0 && pendingDeathLevel == 0) return;
        normalLevel = 0;
        pendingDeathLevel = 0;
        sync();
    }

    /** 在死亡流程中保存等级并清理普通变声，避免后续武器冷却计算读到 0。 */
    public int captureDeathLevelAndClearNormal() {
        pendingDeathLevel = getEffectiveLevel();
        normalLevel = 0;
        sync();
        return pendingDeathLevel;
    }

    /** 读取并消费死亡后武器冷却需要的等级快照。 */
    public int consumeCooldownLevel() {
        int level = getEffectiveLevel();
        if (level != 0) {
            pendingDeathLevel = 0;
            return level;
        }
        if (pendingDeathLevel != 0) {
            level = pendingDeathLevel;
            pendingDeathLevel = 0;
        }
        return level;
    }

    public void setDebugLevel(int level) {
        debugLevel = clamp(level);
        sync();
    }

    /** 调试指令设置 0 时，按需求清除当前全部变声效果。 */
    public void clearAll() {
        normalLevel = 0;
        debugLevel = 0;
        pendingDeathLevel = 0;
        sync();
    }

    private static int clamp(int level) {
        return Math.max(-PartyAnimalConstants.MAX_LEVEL, Math.min(PartyAnimalConstants.MAX_LEVEL, level));
    }

    private void sync() {
        KEY.sync(player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity recipient) {
        return true;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeByte(normalLevel);
        buf.writeByte(debugLevel);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        normalLevel = buf.readByte();
        debugLevel = buf.readByte();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        tag.putByte("normalLevel", (byte) normalLevel);
        tag.putByte("debugLevel", (byte) debugLevel);
        /* 死亡流程短暂保存的等级也要纳入历史快照，避免回溯后消费未来帧残留。 */
        tag.putByte("pendingDeathLevel", (byte) pendingDeathLevel);
    }

    /**
     * 时停快照专用写入：调试等级是永久调试配置，不属于可回滚的普通时间线。
     * 因此快照只保存普通变声与死亡流程的临时等级。
     */
    public void writeTimekeeperSnapshot(@NotNull NbtCompound tag) {
        tag.putByte("normalLevel", (byte) normalLevel);
        tag.putByte("pendingDeathLevel", (byte) pendingDeathLevel);
    }

    /**
     * 时停快照专用恢复：保留当前调试等级，只倒回普通变声和死亡临时等级。
     */
    public void readTimekeeperSnapshot(@NotNull NbtCompound tag) {
        normalLevel = clamp(tag.getByte("normalLevel"));
        pendingDeathLevel = clamp(tag.getByte("pendingDeathLevel"));
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        normalLevel = clamp(tag.getByte("normalLevel"));
        debugLevel = clamp(tag.getByte("debugLevel"));
        pendingDeathLevel = clamp(tag.getByte("pendingDeathLevel"));
    }
}
