package org.agmas.noellesroles.roles.physician;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 医师药丸提供的一次性护盾状态。
 */
public class PhysicianPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<PhysicianPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "physician"),
            PhysicianPlayerComponent.class
    );

    private final PlayerEntity player;
    private int pillArmor = 0;

    public PhysicianPlayerComponent(@NotNull PlayerEntity player) {
        this.player = player;
    }

    public boolean hasPillArmor() {
        return this.pillArmor > 0;
    }

    public void givePillArmor() {
        this.pillArmor = PhysicianConstants.PILL_ARMOR_AMOUNT;
        this.sync();
    }

    public void consumePillArmor() {
        if (this.pillArmor > 0) {
            this.pillArmor--;
        }
        this.sync();
    }

    public void playArmorSound() {
        this.player.getWorld().playSound(
                null,
                this.player.getBlockPos(),
                WatheSounds.ITEM_PSYCHO_ARMOUR,
                SoundCategory.PLAYERS,
                5.0F,
                1.0F
        );
    }

    public void reset() {
        this.pillArmor = 0;
        this.sync();
    }

    @Override
    public void serverTick() {
        if (this.pillArmor > 0 && GameWorldComponent.KEY.get(this.player.getWorld()).getRole(this.player) == null) {
            this.reset();
        }
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        tag.putInt("pillArmor", this.pillArmor);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        this.pillArmor = tag.contains("pillArmor") ? tag.getInt("pillArmor") : 0;
    }
}
