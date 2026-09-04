package org.agmas.noellesroles.roles.bounty_hunter;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 赏金猎人的玩家状态。
 *
 * <p>这个组件只同步给赏金猎人本人：悬赏目标、赏金模式锁槽和开局冷却都属于个人信息，
 * 其他客户端不需要知道，也不应该借同步数据反推出赏金猎人的目标。</p>
 */
public class BountyHunterPlayerComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<BountyHunterPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "bounty_hunter"),
            BountyHunterPlayerComponent.class
    );

    private final PlayerEntity player;
    private UUID target;
    private boolean bountyModeActive = false;
    private int bountyDerringerSlot = -1;

    public BountyHunterPlayerComponent(PlayerEntity player) {
        this.player = player;
        this.target = player.getUuid();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void reset() {
        /*
         * 回合清理时必须先停赏金模式，再清冷却。
         * 否则 Wathe 疯魔 API 还保留 profile 状态时，服务器选槽限制会继续把玩家锁住。
         */
        stopBountyMode(true);
        this.target = this.player.getUuid();
        this.player.getItemCooldownManager().remove(ModItems.BOUNTY_PISTOL);
        this.player.getItemCooldownManager().remove(ModItems.BOUNTY_DERRINGER);
        this.player.getItemCooldownManager().remove(ModItems.BOUNTY_MODE);
        sync();
    }

    public void startRoundCooldowns() {
        /* 实际条目包含完整的 30 秒总时长，tooltip 不再需要组件同步一份来源。 */
        this.player.getItemCooldownManager().set(ModItems.BOUNTY_PISTOL, BountyHunterConstants.START_COOLDOWN_TICKS);
    }

    public boolean isCurrentBountyTarget(PlayerEntity possibleTarget) {
        return possibleTarget != null && this.target != null && this.target.equals(possibleTarget.getUuid());
    }

    public UUID getTarget() {
        return this.target;
    }

    public boolean isBountyModeActive() {
        return this.bountyModeActive;
    }

    public boolean tryStartBountyMode() {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(serverPlayer.getWorld());
        if (!gameWorld.isRole(serverPlayer, NoellesRoleRegistry.BOUNTY_HUNTER)
                || !GameFunctions.isPlayerAliveAndSurvival(serverPlayer)
                || this.bountyModeActive) {
            return false;
        }

        if (PsychoModeApi.isActive(serverPlayer)) {
            /*
             * 赏金模式复用 Wathe 的疯魔组件。如果玩家已经处在其他疯魔来源中，
             * 继续叠加会让护盾、皮肤、结束回放和锁槽归属混在一起，所以这里直接购买失败。
             */
            return false;
        }

        if (!PsychoModeApi.start(serverPlayer, BountyHunterPsychoHandler.PROFILE_ID)) {
            return false;
        }

        int slot = findModeGrantedDerringerSlot();
        if (slot < 0) {
            /*
             * profile 启动成功却没找到授予的德林加，说明有其它逻辑在同 tick 改了背包。
             * 这里立即无回放回滚疯魔，避免玩家进入没有模式武器的锁栏状态。
             */
            PsychoModeApi.stop(serverPlayer, false);
            return false;
        }

        this.bountyModeActive = true;
        this.bountyDerringerSlot = slot;
        this.player.getInventory().selectedSlot = slot;
        this.player.playerScreenHandler.sendContentUpdates();

        this.player.getItemCooldownManager().set(ModItems.BOUNTY_MODE, BountyHunterConstants.BOUNTY_MODE_COOLDOWN_TICKS);
        sync();
        return true;
    }

    public void stopBountyMode(boolean clearPsychoState) {
        boolean profileActive = PsychoModeApi.isActive(this.player, BountyHunterPsychoHandler.PROFILE_ID);
        boolean hasModeWeapon = findModeGrantedDerringerSlot() >= 0;
        if (!this.bountyModeActive && this.bountyDerringerSlot < 0 && !profileActive && !hasModeWeapon) {
            return;
        }

        this.bountyModeActive = false;
        this.bountyDerringerSlot = -1;

        if (clearPsychoState && profileActive) {
            PsychoModeApi.stop(this.player);
        }
        if (!PsychoModeApi.isActive(this.player, BountyHunterPsychoHandler.PROFILE_ID)) {
            /*
             * 正常结束时 Wathe 会按 profile 标记回收临时物品。
             * 这里保留一层赏金德林加标记兜底，处理旧存档或异常背包改动留下的残留物。
             */
            removeModeGrantedDerringer();
        }
        this.player.playerScreenHandler.sendContentUpdates();
        sync();
    }

    @Override
    public void clientTick() {
        if (!this.bountyModeActive) {
            return;
        }

        /*
         * 服务端会真正拒绝切槽；客户端这里提前把选中栏拉回赏金德林加，
         * 只是为了避免滚轮/数字键按下后出现一瞬间的视觉错位。
         */
        int slot = findModeGrantedDerringerSlot();
        if (slot >= 0) {
            this.player.getInventory().selectedSlot = slot;
        }
    }

    @Override
    public void serverTick() {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (!gameWorld.isRole(this.player, NoellesRoleRegistry.BOUNTY_HUNTER)) {
            if (this.bountyModeActive) {
                stopBountyMode(true);
            }
            return;
        }

        tickBountyMode(gameWorld);
        tickTargetSelection(gameWorld);

    }

    private void tickBountyMode(GameWorldComponent gameWorld) {
        if (!this.bountyModeActive) {
            return;
        }

        boolean alive = GameFunctions.isPlayerAliveAndSurvival(this.player);
        if (!alive || !PsychoModeApi.isActive(this.player, BountyHunterPsychoHandler.PROFILE_ID)) {
            /*
             * Wathe 的疯魔 profile 会在自己的 tick 里自然结束。
             * 如果它已经归零，这里只回收赏金模式本地状态；如果玩家已不再存活，则主动收束 profile。
             */
            stopBountyMode(!alive);
            return;
        }

        int slot = findModeGrantedDerringerSlot();
        if (slot < 0) {
            /*
             * 正常情况下这把枪不能被切走；如果被其他模组或管理命令移除，
             * 赏金模式也同步结束，避免玩家保留疯魔皮肤和护盾却没有模式武器。
             */
            stopBountyMode(true);
            return;
        }

        this.bountyDerringerSlot = slot;
        if (this.player.getInventory().selectedSlot != slot) {
            this.player.getInventory().selectedSlot = slot;
        }
    }

    private void tickTargetSelection(GameWorldComponent gameWorld) {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)
                || !GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            return;
        }

        UUID previousTarget = this.target;
        PlayerEntity currentTarget = this.player.getWorld().getPlayerByUuid(this.target);
        UUID dualPersonalityPartner = DualPersonalityComponent.KEY.get(this.player.getWorld()).getPartner(this.player.getUuid());

        if (!isValidBountyTarget(gameWorld, currentTarget)
                || Objects.equals(this.target, dualPersonalityPartner)) {
            this.target = chooseNewTarget(gameWorld, dualPersonalityPartner);
        }

        if (!Objects.equals(previousTarget, this.target)) {
            recordTargetChange(serverPlayer, previousTarget, this.target);
            sync();
        }
    }

    private UUID chooseNewTarget(GameWorldComponent gameWorld, UUID dualPersonalityPartner) {
        List<UUID> validTargets = new ArrayList<>();
        WorldModifierComponent modifierComponent = WorldModifierComponent.KEY.get(this.player.getWorld());
        LoversPairComponent loversPairComponent = LoversPairComponent.KEY.get(this.player.getWorld());
        List<UUID> lovers = modifierComponent.getAllWithModifier(NoellesModifierRegistry.LOVERS);

        gameWorld.getRoles().forEach((uuid, role) -> {
            if (uuid == null || uuid.equals(this.player.getUuid())) {
                return;
            }
            /*
             * 和仇杀客一致，赏金目标不能抽到自己的恋人或双重人格 partner。
             * 这些关系本质上不是“外部猎物”，如果允许抽中会和对应词条目标发生冲突。
             */
            if (loversPairComponent.arePartnersOrFallback(this.player.getUuid(), uuid, lovers)
                    || uuid.equals(dualPersonalityPartner)) {
                return;
            }
            PlayerEntity candidate = this.player.getWorld().getPlayerByUuid(uuid);
            if (isValidBountyTarget(gameWorld, candidate, role)) {
                validTargets.add(uuid);
            }
        });

        Collections.shuffle(validTargets);
        return validTargets.isEmpty() ? this.player.getUuid() : validTargets.getFirst();
    }

    private void recordTargetChange(ServerPlayerEntity bountyHunter, UUID previousTarget, UUID newTarget) {
        boolean previousWasRealTarget = isRealTarget(previousTarget);
        boolean newIsRealTarget = isRealTarget(newTarget);
        if (!newIsRealTarget) {
            return;
        }

        if (!previousWasRealTarget) {
            ServerPlayerEntity lockedTarget = bountyHunter.getServer().getPlayerManager().getPlayer(newTarget);
            GameRecordManager.event(GameRecordTypes.GLOBAL_EVENT)
                    .world(bountyHunter.getServerWorld())
                    .actor(bountyHunter)
                    .target(lockedTarget)
                    .put("event", NoellesEventIds.BOUNTY_HUNTER_TARGET_LOCKED_EVENT.toString())
                    .putUuid("locked_target", newTarget)
                    .record();
            return;
        }

        GameRecordManager.event(GameRecordTypes.GLOBAL_EVENT)
                .world(bountyHunter.getServerWorld())
                .actor(bountyHunter)
                .put("event", NoellesEventIds.BOUNTY_HUNTER_TARGET_CHANGED_EVENT.toString())
                .putUuid("old_target", previousTarget)
                .putUuid("new_target", newTarget)
                .record();
    }

    private boolean isRealTarget(UUID uuid) {
        return uuid != null && !uuid.equals(this.player.getUuid());
    }

    private int findModeGrantedDerringerSlot() {
        for (int slot = 0; slot < this.player.getInventory().size(); slot++) {
            if (isModeGrantedDerringer(this.player.getInventory().getStack(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private void removeModeGrantedDerringer() {
        this.player.getInventory().remove(BountyHunterPlayerComponent::isModeGrantedDerringer, 1, this.player.getInventory());
    }

    public static boolean isModeGrantedDerringer(ItemStack stack) {
        return stack.isOf(ModItems.BOUNTY_DERRINGER) && stack.getOrDefault(ModItems.BOUNTY_MODE_GRANTED, false);
    }

    private static boolean isValidBountyTarget(GameWorldComponent gameWorld, PlayerEntity target) {
        if (target == null) {
            return false;
        }
        return isValidBountyTarget(gameWorld, target, gameWorld.getRole(target));
    }

    private static boolean isValidBountyTarget(GameWorldComponent gameWorld, PlayerEntity target, Role role) {
        if (target == null || role == null || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            return false;
        }
        /*
         * 悬赏目标筛选沿用仇杀客语义：
         * 平民/义警是主要猎物，独立中立也能作为独胜阵营的猎物；
         * 模仿者虽然在阵营上是平民，但对杀手侧具有特殊伪装语义，因此排除。
         */
        if (role.getFaction() == Faction.CIVILIAN || role.getFaction() == Faction.VIGILANTE) {
            return !role.equals(NoellesRoleRegistry.MIMIC);
        }
        return NoellesRoleGroups.INDEPENDENT_NEUTRALS.contains(role);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return this.player.equals(player);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        boolean hasRealTarget = isRealTarget(this.target);
        buf.writeBoolean(hasRealTarget);
        if (hasRealTarget) {
            buf.writeUuid(this.target);
        }
        buf.writeBoolean(this.bountyModeActive);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.target = buf.readBoolean() ? buf.readUuid() : this.player.getUuid();
        this.bountyModeActive = buf.readBoolean();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putUuid("target", this.target == null ? this.player.getUuid() : this.target);
        tag.putBoolean("bountyModeActive", this.bountyModeActive);
        tag.putInt("bountyDerringerSlot", this.bountyDerringerSlot);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.target = tag.contains("target") ? tag.getUuid("target") : this.player.getUuid();
        this.bountyModeActive = tag.getBoolean("bountyModeActive");
        this.bountyDerringerSlot = tag.getInt("bountyDerringerSlot");
    }
}
