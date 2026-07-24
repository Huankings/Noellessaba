package org.agmas.noellesroles.roles.hacker;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.util.WatheItemTooltips;
import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * 黑客手机语音组状态。
 *
 * <p>这个组件只同步给手机持有者本人，用于客户端切换手机模型；
 * 服务端每 tick 也会校验玩家是否仍然有资格留在杀手语音组。</p>
 */
public class HackerPhoneComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<HackerPhoneComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "hacker_phone"),
            HackerPhoneComponent.class
    );

    private final PlayerEntity player;
    public boolean groupKiller = false;

    public HackerPhoneComponent(@NotNull PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void serverTick() {
        if (this.groupKiller) {
            quitKillerGroupIfInvalid();
        }
    }

    private void quitKillerGroupIfInvalid() {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (gameWorld.canUseKillerFeatures(this.player) || gameWorld.isRole(this.player, NoellesRoleRegistry.HACKER)) {
            return;
        }

        TrainVoicePlugin.resetPlayer(this.player.getUuid());
        reset();
    }

    public ItemStack createPhoneStack() {
        ItemStack stack = ModItems.PHONE.getDefaultStack();
        Text tooltipText = Text.translatable("item.noellesroles.phone.tooltip")
                .setStyle(Style.EMPTY.withColor(WatheItemTooltips.REGULAR_TOOLTIP_COLOR).withItalic(false));
        List<Text> loreLines = new ArrayList<>();
        loreLines.add(tooltipText);
        stack.set(DataComponentTypes.LORE, new LoreComponent(loreLines));
        return stack;
    }

    public void reset() {
        this.groupKiller = false;
        sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return this.player.equals(player);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.groupKiller);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.groupKiller = buf.readBoolean();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("groupKiller", this.groupKiller);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.groupKiller = tag.contains("groupKiller") && tag.getBoolean("groupKiller");
    }
}
