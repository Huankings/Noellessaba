package org.agmas.noellesroles.roles.engineer;

import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class StunnedPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<StunnedPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "stunned"),
            StunnedPlayerComponent.class
    );

    private final PlayerEntity player;
    private int stunTicks = 0;
    private @Nullable Identifier stunEndEvent = null;

    public StunnedPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void stun(int ticks) {
        this.stun(ticks, Noellesroles.CAPTURE_DEVICE_RELEASED_EVENT);
    }

    public void stun(int ticks, @Nullable Identifier stunEndEvent) {
        this.stunTicks = ticks;
        this.stunEndEvent = ticks > 0 ? stunEndEvent : null;
        KEY.sync(this.player);
    }

    public boolean isStunned() {
        return stunTicks > 0;
    }

    @Override
    public void serverTick() {
        if (stunTicks > 0) {
            // 检查玩家是否死亡或处于旁观/创造模式
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
                // 立即解除定身并移除缓慢效果
                stunTicks = 0;
                stunEndEvent = null;
                player.removeStatusEffect(StatusEffects.SLOWNESS);
                KEY.sync(this.player);
                return;
            }

            stunTicks--;
            if (stunTicks == 0) {
                // 定身结束时移除缓慢效果
                player.removeStatusEffect(StatusEffects.SLOWNESS);
                if (this.stunEndEvent != null && player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                    NbtCompound extra = new NbtCompound();
                    extra.putUuid("victim", player.getUuid());
                    GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), this.stunEndEvent, null, extra);
                }
                this.stunEndEvent = null;
            }
            KEY.sync(this.player);
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("stunTicks", stunTicks);
        if (this.stunEndEvent != null) {
            tag.putString("stunEndEvent", this.stunEndEvent.toString());
        }
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        stunTicks = tag.getInt("stunTicks");
        stunEndEvent = tag.contains("stunEndEvent") ? Identifier.of(tag.getString("stunEndEvent")) : null;
    }
}
