package org.agmas.noellesroles.roles.dreamer;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.config.HarpyModLoaderConfig;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.Collections;

/**
 * 写在梦者本人身上的转化进度组件。
 *
 * <p>梦之印记护盾触发、或非杀手玩家进入幻觉状态时，都会给梦者增加一次计数；
 * 达到总有职玩家数的四分之一后，梦者会转成一个可用杀手职业。</p>
 */
public class DreamerKillerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<DreamerKillerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "dreamer_killer"),
            DreamerKillerComponent.class
    );

    private final PlayerEntity player;
    private boolean hasBecomeKiller = false;
    public int dreamerRequired = 0;
    public int dreamerCounts = 0;

    public DreamerKillerComponent(@NotNull PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void serverTick() {
        if (this.hasBecomeKiller || this.dreamerCounts <= 0) {
            return;
        }

        if (GameWorldComponent.KEY.get(this.player.getWorld()).getRole(this.player) == null) {
            reset();
            return;
        }

        if (this.dreamerRequired > 0 && this.dreamerCounts >= this.dreamerRequired) {
            triggerBecomeKiller();
        }
    }

    public boolean hasBecomeKiller() {
        return this.hasBecomeKiller;
    }

    public void setDreamerRequired() {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(serverPlayer.getWorld());
        int rolePlayerCount = 0;
        for (ServerPlayerEntity possiblePlayer : serverPlayer.getServer().getPlayerManager().getPlayerList()) {
            if (gameWorld.getRole(possiblePlayer) != null) {
                rolePlayerCount++;
            }
        }

        this.dreamerRequired = rolePlayerCount / DreamerConstants.REQUIRED_PLAYER_DIVISOR;
        sync();
    }

    public void addDreamerCount(@NotNull ServerPlayerEntity dreamer) {
        if (this.hasBecomeKiller) {
            return;
        }

        this.dreamerCounts++;
        recordCounts(dreamer);
        sync();
    }

    private void recordCounts(@NotNull ServerPlayerEntity dreamer) {
        NbtCompound extra = new NbtCompound();
        extra.putInt("counts", this.dreamerCounts);
        extra.putInt("required", this.dreamerRequired);
        GameRecordManager.recordGlobalEvent(dreamer.getServerWorld(), NoellesEventIds.DREAMER_COUNTS_EVENT, dreamer, extra);
    }

    private void triggerBecomeKiller() {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(serverPlayer.getWorld());
        if (!gameWorld.isRole(serverPlayer, NoellesRoleRegistry.DREAMER) || !GameFunctions.isPlayerAliveAndSurvival(serverPlayer)) {
            return;
        }

        ArrayList<Role> shuffledKillerRoles = new ArrayList<>(WatheRoles.ROLES);
        shuffledKillerRoles.removeIf(role -> Harpymodloader.VANNILA_ROLES.contains(role)
                || !role.canUseKiller()
                || HarpyModLoaderConfig.HANDLER.instance().disabled.contains(role.identifier().toString()));
        if (shuffledKillerRoles.isEmpty()) {
            shuffledKillerRoles.add(WatheRoles.KILLER);
        }
        Collections.shuffle(shuffledKillerRoles);

        Role newRole = shuffledKillerRoles.getFirst();
        gameWorld.addRole(serverPlayer, newRole);
        ModdedRoleAssigned.EVENT.invoker().assignModdedRole(serverPlayer, newRole);
        PlayerShopComponent.KEY.get(serverPlayer).addToBalance(DreamerConstants.BECOME_KILLER_REWARD_COINS);
        PlayerPoisonComponent.KEY.get(serverPlayer).reset();

        /*
         * 角色中途转化后需要重新发送欢迎文本。
         * 原版杀手和扩展杀手的公告来源不同，因此继续沿用 Harpy 的自动公告表兜底。
         */
        Role assignedRole = gameWorld.getRole(serverPlayer);
        if (Harpymodloader.VANNILA_ROLES.contains(assignedRole)) {
            ServerPlayNetworking.send(serverPlayer, new AnnounceWelcomePayload(
                    RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(WatheRoles.KILLER),
                    gameWorld.getAllKillerTeamPlayers().size(),
                    0
            ));
        } else if (Harpymodloader.autogeneratedAnnouncements.containsKey(assignedRole)) {
            ServerPlayNetworking.send(serverPlayer, new AnnounceWelcomePayload(
                    RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(Harpymodloader.autogeneratedAnnouncements.get(assignedRole)),
                    gameWorld.getAllKillerTeamPlayers().size(),
                    0
            ));
        }

        clearCounts();
    }

    private void clearCounts() {
        this.hasBecomeKiller = true;
        this.dreamerCounts = 0;
        sync();
    }

    public void reset() {
        this.hasBecomeKiller = false;
        this.dreamerRequired = 0;
        this.dreamerCounts = 0;
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
        buf.writeBoolean(this.hasBecomeKiller);
        buf.writeInt(this.dreamerRequired);
        buf.writeInt(this.dreamerCounts);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.hasBecomeKiller = buf.readBoolean();
        this.dreamerRequired = buf.readInt();
        this.dreamerCounts = buf.readInt();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("hasBecomeKiller", this.hasBecomeKiller);
        tag.putInt("dreamerRequired", this.dreamerRequired);
        tag.putInt("dreamerCounts", this.dreamerCounts);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.hasBecomeKiller = tag.contains("hasBecomeKiller") && tag.getBoolean("hasBecomeKiller");
        this.dreamerRequired = tag.contains("dreamerRequired") ? tag.getInt("dreamerRequired") : 0;
        this.dreamerCounts = tag.contains("dreamerCounts") ? tag.getInt("dreamerCounts") : 0;
    }
}
