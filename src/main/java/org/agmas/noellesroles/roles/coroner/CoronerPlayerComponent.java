package org.agmas.noellesroles.roles.coroner;


import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CoronerPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<CoronerPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "coroner_player"),
            CoronerPlayerComponent.class
    );
    public static final int MORPH_DURATION_TICKS = GameConstants.getInTicks(1, 0);  // 变形持续时间：1分钟
    public static final int COOLDOWN_TICKS = GameConstants.getInTicks(0, 0);       // 冷却时间：30秒
    public static final int REMOVE_COOLDOWN_TICKS = GameConstants.getInTicks(0, 0); // 卸除伪装冷却时间：15秒

    private final PlayerEntity player;
    public UUID disguise;
    public int morphTicks = 0;

    // 检查尸体相关
    public Set<UUID> examinedBodies = new HashSet<>(); // 已检查过的尸体UUID
    public int totalBodiesExamined = 0; // 总共检查的尸体数
    public int totalGoldEarned = 0; // 总共获得的金币

    // 常量
    public static final int BASE_REWARD = 30; // 基础奖励
    public static final int BONUS_REWARD = 20; // 额外奖励（正确识别）

    public void reset() {
        this.stopMorph();
        this.examinedBodies.clear();
        this.totalBodiesExamined = 0;
        this.totalGoldEarned = 0;
        this.sync();
    }

    public CoronerPlayerComponent(PlayerEntity player) {
        this.player = player;
        disguise = player.getUuid();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void clientTick() {
        // 客户端逻辑
    }

    public void serverTick() {
        if (this.morphTicks > 0 && disguise != null) {
            if (!player.isAlive() || player.isSpectator()) {
                this.stopMorph();
            }
            if (--this.morphTicks == 0) {
                this.stopMorph();
            }
            this.sync();
        }
        if (this.morphTicks < 0) {
            this.morphTicks++;
            this.sync();
        }
    }

    public boolean startMorph(UUID id) {
        setMorphTicks(MORPH_DURATION_TICKS); // 1分钟持续时间，与变形者相同
        disguise = id;
        this.sync();
        return true;
    }

    public void stopMorph() {
        this.morphTicks = -COOLDOWN_TICKS; // 30秒冷却时间，与变形者相同
        this.sync();
    }

    // 新增一个专门卸除伪装的方法
    public void removeDisguise() {
        this.morphTicks = -REMOVE_COOLDOWN_TICKS; // 使用常量
        this.disguise = null; // 清除伪装
        this.sync();
    }

    public int getMorphTicks() {
        return this.morphTicks;
    }

    public void setMorphTicks(int ticks) {
        this.morphTicks = ticks;
        this.sync();
    }

    // 检查尸体方法
    public boolean examineBody(UUID bodyUuid, boolean correctIdentification) {
        // 如果已经检查过这个尸体，不重复奖励
        if (examinedBodies.contains(bodyUuid)) {
            return false;
        }

        // 记录已检查的尸体
        examinedBodies.add(bodyUuid);
        totalBodiesExamined++;

        // 计算奖励
        int reward = BASE_REWARD;
        if (correctIdentification) {
            reward += BONUS_REWARD;
        }

        // 给予金币
        PlayerShopComponent shopComponent = PlayerShopComponent.KEY.get(player);
        shopComponent.addToBalance(reward);
        shopComponent.sync();

        totalGoldEarned += reward;

        this.sync();
        return true;
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("morphTicks", this.morphTicks);
        tag.putInt("totalBodiesExamined", this.totalBodiesExamined);
        tag.putInt("totalGoldEarned", this.totalGoldEarned);
        // 保存已检查的尸体列表
        NbtList bodiesList = new NbtList();
        for (UUID bodyId : examinedBodies) {
            bodiesList.add(NbtHelper.fromUuid(bodyId));
        }
        tag.put("examinedBodies", bodiesList);
        if (disguise == null) disguise = player.getUuid();
        tag.putUuid("disguise", this.disguise);
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.morphTicks = tag.contains("morphTicks") ? tag.getInt("morphTicks") : 0;
        this.disguise = tag.contains("disguise") ? tag.getUuid("disguise") : player.getUuid();
        this.totalBodiesExamined = tag.getInt("totalBodiesExamined");
        this.totalGoldEarned = tag.getInt("totalGoldEarned");

        // 读取已检查的尸体列表
        this.examinedBodies.clear();
        if (tag.contains("examinedBodies", NbtElement.LIST_TYPE)) {
            NbtList bodiesList = tag.getList("examinedBodies", NbtElement.INT_ARRAY_TYPE);
            for (NbtElement element : bodiesList) {
                this.examinedBodies.add(NbtHelper.toUuid(element));
            }
        }
    }
}
