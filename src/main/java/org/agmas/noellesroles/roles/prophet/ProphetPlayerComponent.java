package org.agmas.noellesroles.roles.prophet;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.UUID;

/**
 * 先知玩家组件。
 *
 * <p>这个组件会挂在所有玩家身上：
 * 1. 对先知本人来说，用来记录当前水晶球标记的目标；
 * 2. 对被揭露的玩家来说，用来记录其是否获得“巫毒免伤”。</p>
 */
public class ProphetPlayerComponent implements AutoSyncedComponent {

    public static final ComponentKey<ProphetPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "prophet"),
            ProphetPlayerComponent.class
    );

    private final PlayerEntity player;

    /**
     * 当前先知标记的目标。
     * 只有先知本人会真正使用这个字段。
     */
    private @Nullable UUID markedTarget;

    /**
     * 该玩家是否已经被先知揭露过，因此在本局内免疫巫毒魔法。
     */
    private boolean immuneToVoodooMagic;

    /**
     * 如果该玩家当前拥有“先知庇护”的巫毒免伤，
     * 这里记录授予这层庇护的先知是谁，方便后续回放准确显示来源。
     */
    private @Nullable UUID voodooImmunityProvider;

    public ProphetPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void reset() {
        this.markedTarget = null;
        this.immuneToVoodooMagic = false;
        this.voodooImmunityProvider = null;
        this.sync();
    }

    /**
     * 只清空先知自己的当前标记，不影响已经发放出去的巫毒免伤。
     * 这个方法用于“先知死亡后立即失去标记”。
     */
    public void clearMarkOnly() {
        this.markedTarget = null;
        this.sync();
    }

    public @Nullable UUID getMarkedTarget() {
        return markedTarget;
    }

    public boolean hasMarkedTarget() {
        return this.markedTarget != null;
    }

    public void setMarkedTarget(@Nullable UUID markedTarget) {
        this.markedTarget = markedTarget;
        this.sync();
    }

    public boolean isImmuneToVoodooMagic() {
        return immuneToVoodooMagic;
    }

    public void setImmuneToVoodooMagic(boolean immuneToVoodooMagic) {
        this.immuneToVoodooMagic = immuneToVoodooMagic;
        this.sync();
    }

    public @Nullable UUID getVoodooImmunityProvider() {
        return voodooImmunityProvider;
    }

    public void setVoodooImmunityProvider(@Nullable UUID voodooImmunityProvider) {
        this.voodooImmunityProvider = voodooImmunityProvider;
        this.sync();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (this.markedTarget != null) {
            tag.putUuid("marked_target", this.markedTarget);
        }
        tag.putBoolean("immune_to_voodoo_magic", this.immuneToVoodooMagic);
        if (this.voodooImmunityProvider != null) {
            tag.putUuid("voodoo_immunity_provider", this.voodooImmunityProvider);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.markedTarget = tag.containsUuid("marked_target") ? tag.getUuid("marked_target") : null;
        this.immuneToVoodooMagic = tag.getBoolean("immune_to_voodoo_magic");
        this.voodooImmunityProvider = tag.containsUuid("voodoo_immunity_provider") ? tag.getUuid("voodoo_immunity_provider") : null;
    }
}
