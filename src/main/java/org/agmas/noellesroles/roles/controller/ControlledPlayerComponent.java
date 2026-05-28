package org.agmas.noellesroles.roles.controller;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

public class ControlledPlayerComponent implements AutoSyncedComponent,ServerTickingComponent{
    public static final ComponentKey<ControlledPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "controlled_player"),
            ControlledPlayerComponent.class
    );

    private final PlayerEntity player;

    public UUID controller = null;
    public boolean isControlled = false;
    public double controllerX = 0;
    public double controllerY = 0;
    public double controllerZ = 0;
    public float controllerYaw = 0;
    public float controllerPitch = 0;

    // 新增：控制持续时间计时器
    public int controlledTicks = 0;
    public static final int MAX_CONTROLLED_TICKS = 70 * 20; // 70秒

    public ControlledPlayerComponent(PlayerEntity player) {
        this.player = player;
    }
    public void reset() {
        this.clearControlled();
    }


    public void setControlled(UUID controller, double x, double y, double z, float yaw, float pitch) {
        this.controller = controller;
        this.isControlled = true;
        this.controllerX = x;
        this.controllerY = y;
        this.controllerZ = z;
        this.controllerYaw = yaw;
        this.controllerPitch = pitch;
        this.controlledTicks = 0; // 重置计时器
        this.sync();
    }

    public void clearControlled() {
        this.controller = null;
        this.isControlled = false;
        this.controllerX = 0;
        this.controllerY = 0;
        this.controllerZ = 0;
        this.controllerYaw = 0;
        this.controllerPitch = 0;
        this.controlledTicks = 0;
        this.sync();
    }

    @Override
    public void serverTick() {
        if (this.isControlled) {
            // 检查玩家是否进入旁观/创造模式
            if (this.player.isSpectator() || this.player.isCreative()) {
                this.clearControlled();
                return;
            }

            // 增加计时器
            this.controlledTicks++;

            // 如果超过最大时间，自动解除控制
            if (this.controlledTicks >= MAX_CONTROLLED_TICKS) {
                this.clearControlled();
                return;
            }

            // 检查控制器是否还在线且有效
            if (this.controller != null) {
                PlayerEntity controllerPlayer = player.getWorld().getPlayerByUuid(this.controller);
                if (controllerPlayer == null || controllerPlayer.isSpectator() || controllerPlayer.isCreative()) {
                    this.clearControlled();
                    return;
                }

                // 检查附体师是否还在附体状态
                ControllerPlayerComponent controllerComp = ControllerPlayerComponent.KEY.get(controllerPlayer);
                // 附体师的伪装/附体状态已经统一收口在 ControllerPlayerComponent 内部，
                // 这里直接检查“是否还在有效附体中”即可，避免和外部职业组件产生耦合。
                if (controllerComp == null || !controllerComp.isPossessing() ||
                        !player.getUuid().equals(controllerComp.controlledTarget)) {
                    this.clearControlled();
                    return;
                }
            }

        }
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (controller != null) {
            tag.putUuid("controller", controller);
        }
        tag.putBoolean("isControlled", isControlled);
        tag.putDouble("controllerX", controllerX);
        tag.putDouble("controllerY", controllerY);
        tag.putDouble("controllerZ", controllerZ);
        tag.putFloat("controllerYaw", controllerYaw);
        tag.putFloat("controllerPitch", controllerPitch);
        tag.putInt("controlledTicks", controlledTicks); // 保存计时器
    }

    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (tag.containsUuid("controller")) {
            this.controller = tag.getUuid("controller");
        }
        this.isControlled = tag.getBoolean("isControlled");
        this.controllerX = tag.getDouble("controllerX");
        this.controllerY = tag.getDouble("controllerY");
        this.controllerZ = tag.getDouble("controllerZ");
        this.controllerYaw = tag.getFloat("controllerYaw");
        this.controllerPitch = tag.getFloat("controllerPitch");
        this.controlledTicks = tag.getInt("controlledTicks"); // 读取计时器
    }
}
