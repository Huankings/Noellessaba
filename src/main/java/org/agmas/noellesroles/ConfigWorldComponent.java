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
        refreshSnapshot();
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
        /*
         * 配置和钥匙可见性过去每 tick 都把整个世界组件广播给全服。
         * 这些值绝大多数整局不变，因此只在服务端真值实际变化时发送一次。
         */
        if (refreshSnapshot()) {
            this.sync();
        }
    }

    private boolean refreshSnapshot() {
        var config = NoellesRolesConfig.HANDLER.instance();
        boolean nextMasterKeyVisible = config.playerCountToMakeConducterKeyVisible > 0
                && this.world.getServer() != null
                && this.world.getServer().getPlayerManager().getCurrentPlayerCount() >= config.playerCountToMakeConducterKeyVisible;
        boolean changed = this.insaneSeesMorphs != config.insanePlayersSeeMorphs
                || this.naturalVoodoosAllowed != config.voodooNonKillerDeaths
                || this.masterKeyVisibleCount != config.playerCountToMakeConducterKeyVisible
                || this.masterKeyIsVisible != nextMasterKeyVisible
                || this.conductorDroppedItemInstinct != config.conductorDroppedItemInstinct
                || this.coronerBodyInstinct != config.coronerBodyInstinct
                || this.jesterPsychoCannotAttackKiller != config.jesterPsychoCannotAttackKiller;
        this.insaneSeesMorphs = config.insanePlayersSeeMorphs;
        this.naturalVoodoosAllowed = config.voodooNonKillerDeaths;
        this.masterKeyVisibleCount = config.playerCountToMakeConducterKeyVisible;
        this.masterKeyIsVisible = nextMasterKeyVisible;
        this.conductorDroppedItemInstinct = config.conductorDroppedItemInstinct;
        this.coronerBodyInstinct = config.coronerBodyInstinct;
        this.jesterPsychoCannotAttackKiller = config.jesterPsychoCannotAttackKiller;
        return changed;
    }
}
