package org.agmas.noellesroles.roles.convener;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 保存召集者本人整局内的进度状态。
 *
 * <p>这里没有把这些值散落到召集逻辑里，是因为背包头像 UI、右下角 HUD、
 * 胜利判定和任务护盾都会读取同一份状态；集中到组件能避免不同入口各自维护一套计数。</p>
 */
public class ConvenerPlayerComponent implements AutoSyncedComponent {
    public static final ComponentKey<ConvenerPlayerComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(NoellesRolesCore.MOD_ID, "convener_player"), ConvenerPlayerComponent.class);

    private final PlayerEntity player;
    private final Set<UUID> unlockedDisguises = new LinkedHashSet<>();

    private int summonCount;
    private int requiredSummons = 1;
    private int completedTasksTowardsShield;
    private int counterShieldLayers;

    public ConvenerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    /**
     * 回合结束或玩家重置时彻底清空。
     */
    public void reset() {
        this.unlockedDisguises.clear();
        this.summonCount = 0;
        this.requiredSummons = 1;
        this.completedTasksTowardsShield = 0;
        this.counterShieldLayers = 0;
        this.sync();
    }

    /**
     * 真正分配为召集者时初始化。
     *
     * <p>开局只解锁自己的头像；其它头像必须通过对尸体发动召集逐步获得。</p>
     */
    public void initializeForRole() {
        this.unlockedDisguises.clear();
        this.unlockedDisguises.add(this.player.getUuid());
        this.summonCount = 0;
        this.requiredSummons = 1;
        this.completedTasksTowardsShield = 0;
        this.counterShieldLayers = 0;
        this.sync();
    }

    public Set<UUID> getUnlockedDisguises() {
        return this.unlockedDisguises;
    }

    public boolean hasUnlockedMorphs() {
        return this.unlockedDisguises.size() > 1;
    }

    public boolean knowsDisguise(UUID uuid) {
        return this.unlockedDisguises.contains(uuid);
    }

    public void unlockDisguise(UUID uuid) {
        this.unlockedDisguises.add(uuid);
    }

    public int getSummonCount() {
        return this.summonCount;
    }

    public void incrementSummonCount() {
        this.summonCount++;
    }

    public int getRequiredSummons() {
        return this.requiredSummons;
    }

    public void setRequiredSummons(int requiredSummons) {
        this.requiredSummons = Math.max(1, requiredSummons);
    }

    public boolean hasReachedSummonGoal() {
        return this.summonCount >= this.requiredSummons;
    }

    public int getCounterShieldLayers() {
        return this.counterShieldLayers;
    }

    public boolean hasCounterShield() {
        return this.counterShieldLayers > 0;
    }

    public int getTasksRemainingForNextShield() {
        return Math.max(0, ConvenerConstants.TASKS_PER_COUNTER_SHIELD - this.completedTasksTowardsShield);
    }

    /**
     * 记录一次真实任务完成。
     *
     * @return 这次任务是否正好换到了一层新的反伤护盾
     */
    public boolean recordCompletedTask() {
        this.completedTasksTowardsShield++;
        if (this.completedTasksTowardsShield < ConvenerConstants.TASKS_PER_COUNTER_SHIELD) {
            return false;
        }

        this.completedTasksTowardsShield -= ConvenerConstants.TASKS_PER_COUNTER_SHIELD;
        this.counterShieldLayers++;
        return true;
    }

    public boolean consumeCounterShield() {
        if (this.counterShieldLayers <= 0) {
            return false;
        }
        this.counterShieldLayers--;
        return true;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound unlockedTag = new NbtCompound();
        int index = 0;
        for (UUID uuid : this.unlockedDisguises) {
            unlockedTag.putUuid("uuid_" + index, uuid);
            index++;
        }
        unlockedTag.putInt("size", index);
        tag.put("unlocked_disguises", unlockedTag);
        tag.putInt("summon_count", this.summonCount);
        tag.putInt("required_summons", this.requiredSummons);
        tag.putInt("completed_tasks_towards_shield", this.completedTasksTowardsShield);
        tag.putInt("counter_shield_layers", this.counterShieldLayers);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.unlockedDisguises.clear();
        if (tag.contains("unlocked_disguises")) {
            NbtCompound unlockedTag = tag.getCompound("unlocked_disguises");
            int size = Math.max(0, unlockedTag.getInt("size"));
            for (int i = 0; i < size; i++) {
                String key = "uuid_" + i;
                if (unlockedTag.containsUuid(key)) {
                    this.unlockedDisguises.add(unlockedTag.getUuid(key));
                }
            }
        }

        this.summonCount = tag.contains("summon_count") ? Math.max(0, tag.getInt("summon_count")) : 0;
        this.requiredSummons = tag.contains("required_summons") ? Math.max(1, tag.getInt("required_summons")) : 1;
        this.completedTasksTowardsShield = tag.contains("completed_tasks_towards_shield")
                ? Math.max(0, tag.getInt("completed_tasks_towards_shield"))
                : 0;
        this.counterShieldLayers = tag.contains("counter_shield_layers")
                ? Math.max(0, tag.getInt("counter_shield_layers"))
                : 0;
    }
}
