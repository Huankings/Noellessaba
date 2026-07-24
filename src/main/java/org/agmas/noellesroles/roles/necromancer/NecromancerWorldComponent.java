package org.agmas.noellesroles.roles.necromancer;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/**
 * 死灵法师世界级复活次数。
 *
 * <p>StupidExpress 原逻辑是“每死亡一名杀手阵营玩家，全局增加一次可复活次数”。
 * 因为这个次数不属于某一个死灵法师玩家，而属于当前世界/当前对局，所以继续使用世界组件保存。</p>
 */
public class NecromancerWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<NecromancerWorldComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "necromancer"),
            NecromancerWorldComponent.class
    );

    private final World world;
    private int availableRevives = 0;

    public NecromancerWorldComponent(@NotNull World world) {
        this.world = world;
    }

    public int getAvailableRevives() {
        return this.availableRevives;
    }

    public void reset() {
        this.availableRevives = 0;
        sync();
    }

    public void increaseAvailableRevives() {
        this.availableRevives++;
        sync();
    }

    public void decreaseAvailableRevives() {
        this.availableRevives = Math.max(0, this.availableRevives - 1);
        sync();
    }

    public void sync() {
        KEY.sync(this.world);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("available_revivals", this.availableRevives);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.availableRevives = tag.contains("available_revivals") ? tag.getInt("available_revivals") : 0;
    }
}
