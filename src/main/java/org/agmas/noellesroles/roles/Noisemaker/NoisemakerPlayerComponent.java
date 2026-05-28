package org.agmas.noellesroles.roles.Noisemaker;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

public class NoisemakerPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<NoisemakerPlayerComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "noisemaker"), NoisemakerPlayerComponent.class);
    private final PlayerEntity player;
    private int cooldownTicks = 0;  // 冷却时间，0表示可用
    private UUID lastTarget = null;  // 上次选择的目标（可选，用于UI显示）

    public NoisemakerPlayerComponent(PlayerEntity player) {
        this.player = player;
    }
    public void sync() {
        KEY.sync(this.player);
    }

    public void reset() {
        this.cooldownTicks = 0;
        this.lastTarget = null;
        this.sync();
    }
    // 尝试使用能力
    public boolean tryUseAbility(UUID targetUuid) {
        if (this.cooldownTicks <= 0) {
            // 冷却统一走常量，后续调数值时只改一处。
            this.cooldownTicks = NoisemakerConstants.ABILITY_COOLDOWN_TICKS;
            this.lastTarget = targetUuid;
            this.sync();
            return true;
        }
        return false;
    }
    public int getCooldownTicks() {
        return this.cooldownTicks;
    }

    public float getCooldownProgress() {
        if (this.cooldownTicks <= 0) return 0f;
        return (float) this.cooldownTicks / NoisemakerConstants.ABILITY_COOLDOWN_TICKS;
    }

    public UUID getLastTarget() {
        return this.lastTarget;
    }

    @Override
    public void serverTick() {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            if (this.cooldownTicks == 0) {
                this.lastTarget = null;
            }
            this.sync();
        }
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.cooldownTicks = tag.getInt("cooldownTicks");
        if (tag.contains("lastTarget")) {
            this.lastTarget = tag.getUuid("lastTarget");
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("cooldownTicks", this.cooldownTicks);
        if (this.lastTarget != null) {
            tag.putUuid("lastTarget", this.lastTarget);
        }
    }
}
