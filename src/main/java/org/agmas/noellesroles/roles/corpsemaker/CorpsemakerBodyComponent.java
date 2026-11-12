package org.agmas.noellesroles.roles.corpsemaker;

import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

/**
 * 造尸怪伪造尸体自己的来源标记。
 *
 * <p>Wathe 的 {@code PlayerBodyEntity#isSynthetic()} 还会被魔术师等其它机制使用，
 * 不能拿它直接判断“这是造尸怪伪尸”。因此在 NoellesRoles 内单独保存一个稳定标记，
 * 并冻结尸体生成时目标是否属于当前对局，供客户端观察者本能决定是否显示白色描边。</p>
 */
public final class CorpsemakerBodyComponent implements AutoSyncedComponent {
    public static final ComponentKey<CorpsemakerBodyComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "corpsemaker_body"),
            CorpsemakerBodyComponent.class
    );

    private final PlayerBodyEntity body;
    private boolean forgedByCorpsemaker;
    private boolean whiteObserverInstinct;

    public CorpsemakerBodyComponent(PlayerBodyEntity body) {
        this.body = body;
    }

    /**
     * 在尸体生成到世界之前写入。此时 CCA 初始同步会把两个字段一起发给开始追踪该实体的客户端。
     */
    public void markForged(boolean targetParticipatedInRound) {
        this.forgedByCorpsemaker = true;
        this.whiteObserverInstinct = !targetParticipatedInRound;
    }

    public boolean shouldUseWhiteObserverInstinct() {
        return this.forgedByCorpsemaker && this.whiteObserverInstinct;
    }

    public void sync() {
        KEY.sync(this.body);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("ForgedByCorpsemaker", this.forgedByCorpsemaker);
        tag.putBoolean("WhiteObserverInstinct", this.whiteObserverInstinct);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.forgedByCorpsemaker = tag.getBoolean("ForgedByCorpsemaker");
        this.whiteObserverInstinct = tag.getBoolean("WhiteObserverInstinct");
    }
}
