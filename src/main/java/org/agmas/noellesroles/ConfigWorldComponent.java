package org.agmas.noellesroles;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

public class ConfigWorldComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<ConfigWorldComponent> KEY = ComponentRegistry.getOrCreate(Identifier.of(NoellesRolesCore.MOD_ID, "config"), ConfigWorldComponent.class);
    public boolean insaneSeesMorphs = true;
    public boolean naturalVoodoosAllowed = false;
    public int masterKeyVisibleCount = 0;
    public boolean masterKeyIsVisible = false;
    /** 客户端可读取的配置快照；这些字段不属于时停者回溯运行态。 */
    public boolean conductorDroppedItemInstinct = false;
    public boolean coronerBodyInstinct = false;
    public boolean jesterPsychoCannotAttackKiller = false;
    private final World world;

    public void reset() {
        this.sync();
    }

    public ConfigWorldComponent(World world) {
        this.world = world;
    }

    public void sync() {
        KEY.sync(this.world);
    }

    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        insaneSeesMorphs = NoellesRolesConfig.HANDLER.instance().insanePlayersSeeMorphs;
        naturalVoodoosAllowed = NoellesRolesConfig.HANDLER.instance().voodooNonKillerDeaths;
        masterKeyVisibleCount = NoellesRolesConfig.HANDLER.instance().playerCountToMakeConducterKeyVisible;
        conductorDroppedItemInstinct = NoellesRolesConfig.HANDLER.instance().conductorDroppedItemInstinct;
        coronerBodyInstinct = NoellesRolesConfig.HANDLER.instance().coronerBodyInstinct;
        jesterPsychoCannotAttackKiller = NoellesRolesConfig.HANDLER.instance().jesterPsychoCannotAttackKiller;
        tag.putBoolean("insaneSeesMorphs", this.insaneSeesMorphs);
        tag.putBoolean("naturalVoodoosAllowed", this.naturalVoodoosAllowed);
        tag.putBoolean("masterKeyIsVisible", this.masterKeyIsVisible);
        tag.putInt("masterKeyVisibleCount", this.masterKeyVisibleCount);
        tag.putBoolean("conductorDroppedItemInstinct", this.conductorDroppedItemInstinct);
        tag.putBoolean("coronerBodyInstinct", this.coronerBodyInstinct);
        tag.putBoolean("jesterPsychoCannotAttackKiller", this.jesterPsychoCannotAttackKiller);
    }



    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (tag.contains("insaneSeesMorphs"))   this.insaneSeesMorphs = tag.getBoolean("insaneSeesMorphs");
        if (tag.contains("naturalVoodoosAllowed"))   this.naturalVoodoosAllowed = tag.getBoolean("naturalVoodoosAllowed");
        if (tag.contains("masterKeyIsVisible"))   this.masterKeyIsVisible = tag.getBoolean("masterKeyIsVisible");
        if (tag.contains("masterKeyVisibleCount"))   this.masterKeyVisibleCount = tag.getInt("masterKeyVisibleCount");
        if (tag.contains("conductorDroppedItemInstinct"))   this.conductorDroppedItemInstinct = tag.getBoolean("conductorDroppedItemInstinct");
        if (tag.contains("coronerBodyInstinct"))   this.coronerBodyInstinct = tag.getBoolean("coronerBodyInstinct");
        if (tag.contains("jesterPsychoCannotAttackKiller"))   this.jesterPsychoCannotAttackKiller = tag.getBoolean("jesterPsychoCannotAttackKiller");
    }

    @Override
    public void serverTick() {
        if (NoellesRolesConfig.HANDLER.instance().playerCountToMakeConducterKeyVisible == 0) {
            masterKeyIsVisible = false;
        } else {
            if (world.getServer() != null)
                masterKeyIsVisible =  world.getServer().getPlayerManager().getCurrentPlayerCount() >= NoellesRolesConfig.HANDLER.instance().playerCountToMakeConducterKeyVisible;
        }
        this.sync();
    }
}
