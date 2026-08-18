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
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
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
    /**
     * 记录幻觉注剂当前是否仍属于梦者开局 30 秒冷却。
     *
     * <p>ItemCooldownManager 只会同步剩余比例，客户端无法仅凭比例判断
     * 当前是 30 秒开局冷却还是 45 秒普通冷却，因此这里单独同步来源状态。</p>
     */
    private int delusionSyringeStartCooldownTicks = 0;
    public int dreamerRequired = 0;
    public int dreamerCounts = 0;

    public DreamerKillerComponent(@NotNull PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void serverTick() {
        boolean startCooldownExpired = false;
        if (this.delusionSyringeStartCooldownTicks > 0) {
            this.delusionSyringeStartCooldownTicks--;
            startCooldownExpired = this.delusionSyringeStartCooldownTicks == 0;
        }

        if (this.hasBecomeKiller || this.dreamerCounts <= 0) {
            if (startCooldownExpired) {
                sync();
            }
            return;
        }

        if (GameWorldComponent.KEY.get(this.player.getWorld()).getRole(this.player) == null) {
            reset();
            return;
        }

        if (this.dreamerRequired > 0 && this.dreamerCounts >= this.dreamerRequired) {
            triggerBecomeKiller();
        } else if (startCooldownExpired) {
            sync();
        }
    }

    public boolean hasBecomeKiller() {
        return this.hasBecomeKiller;
    }

    /**
     * 梦者分配身份时启动幻觉注剂的 30 秒开局冷却来源标记。
     *
     * <p>真正的物品禁止使用仍由 DreamerRoleAssignedHandler 写入
     * ItemCooldownManager；本方法只负责让客户端 tooltip 知道应显示 30 秒总长。</p>
     */
    public void startDelusionSyringeRoundCooldown() {
        this.delusionSyringeStartCooldownTicks = DreamerConstants.DELUSION_SYRINGE_START_COOLDOWN_TICKS;
        sync();
    }

    /**
     * 判断幻觉注剂当前是否仍处于梦者开局冷却来源。
     */
    public boolean isUsingDelusionSyringeStartCooldown(@NotNull Item item) {
        return item == ModItems.DELUSION_SYRINGE && this.delusionSyringeStartCooldownTicks > 0;
    }

    /**
     * 正常注射会把开局 30 秒冷却替换为普通 45 秒冷却。
     * 在写入普通冷却前必须清掉来源标记，否则客户端会继续按 30 秒换算倒计时。
     */
    public void clearDelusionSyringeStartCooldown() {
        if (this.delusionSyringeStartCooldownTicks <= 0) {
            return;
        }

        this.delusionSyringeStartCooldownTicks = 0;
        sync();
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
        clearDreamerStartSyringe(serverPlayer);

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

    private void clearDreamerStartSyringe(@NotNull ServerPlayerEntity serverPlayer) {
        /*
         * 幻觉注剂是梦者开局发放的推进道具；梦者达标转成杀手职业后，
         * 这件道具已经完成它的职业阶段使命，继续留在原玩家背包里只会占格子。
         *
         * 这里只清理“转职成功的梦者本人”背包内的幻觉注剂，不影响其它玩家通过偷取、
         * 指令或创造模式获得的同款物品；这些物品仍按通用针剂规则正常可用。
         */
        serverPlayer.getInventory().remove(
                stack -> stack.isOf(ModItems.DELUSION_SYRINGE),
                Integer.MAX_VALUE,
                serverPlayer.getInventory()
        );
        serverPlayer.getItemCooldownManager().remove(ModItems.DELUSION_SYRINGE);
        serverPlayer.getInventory().markDirty();
    }

    private void clearCounts() {
        this.hasBecomeKiller = true;
        this.dreamerCounts = 0;
        sync();
    }

    public void reset() {
        this.hasBecomeKiller = false;
        this.delusionSyringeStartCooldownTicks = 0;
        this.dreamerRequired = 0;
        this.dreamerCounts = 0;
        this.player.getItemCooldownManager().remove(ModItems.DELUSION_SYRINGE);
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
        buf.writeBoolean(this.delusionSyringeStartCooldownTicks > 0);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.hasBecomeKiller = buf.readBoolean();
        this.dreamerRequired = buf.readInt();
        this.dreamerCounts = buf.readInt();
        this.delusionSyringeStartCooldownTicks = buf.readBoolean() ? 1 : 0;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("hasBecomeKiller", this.hasBecomeKiller);
        tag.putInt("dreamerRequired", this.dreamerRequired);
        tag.putInt("dreamerCounts", this.dreamerCounts);
        tag.putInt("delusionSyringeStartCooldownTicks", this.delusionSyringeStartCooldownTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.hasBecomeKiller = tag.contains("hasBecomeKiller") && tag.getBoolean("hasBecomeKiller");
        this.dreamerRequired = tag.contains("dreamerRequired") ? tag.getInt("dreamerRequired") : 0;
        this.dreamerCounts = tag.contains("dreamerCounts") ? tag.getInt("dreamerCounts") : 0;
        this.delusionSyringeStartCooldownTicks = tag.contains("delusionSyringeStartCooldownTicks")
                ? tag.getInt("delusionSyringeStartCooldownTicks")
                : 0;
    }
}
