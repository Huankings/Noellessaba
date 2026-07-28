package org.agmas.noellesroles.roles.bounty_hunter;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
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
    private int bountyPistolStartCooldownTicks = 0;
    private int bountyPistolCooldownTotalTicks = BountyHunterConstants.BOUNTY_PISTOL_FAILED_COOLDOWN_TICKS;
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
         * 否则 PlayerPsychoComponent 还保留疯魔 ticks 时，Wathe 的服务器选槽限制会继续把玩家锁住。
         */
        stopBountyMode(true);
        this.target = this.player.getUuid();
        this.bountyPistolStartCooldownTicks = 0;
        this.bountyPistolCooldownTotalTicks = BountyHunterConstants.BOUNTY_PISTOL_FAILED_COOLDOWN_TICKS;
        this.player.getItemCooldownManager().remove(ModItems.BOUNTY_PISTOL);
        this.player.getItemCooldownManager().remove(ModItems.BOUNTY_DERRINGER);
        this.player.getItemCooldownManager().remove(ModItems.BOUNTY_MODE);
        sync();
    }

    public void startRoundCooldowns() {
        this.bountyPistolStartCooldownTicks = BountyHunterConstants.START_COOLDOWN_TICKS;
        this.bountyPistolCooldownTotalTicks = BountyHunterConstants.START_COOLDOWN_TICKS;
        sync();
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

    public int getDisplayedBountyPistolCooldownTotalTicks() {
        if (this.bountyPistolStartCooldownTicks > 0) {
            return BountyHunterConstants.START_COOLDOWN_TICKS;
        }
        return this.bountyPistolCooldownTotalTicks;
    }

    public void setBountyPistolCooldownTotalTicks(int cooldownTicks) {
        this.bountyPistolCooldownTotalTicks = cooldownTicks;
        sync();
    }

    public boolean isUsingStartCooldown(Item item) {
        return item == ModItems.BOUNTY_PISTOL && this.bountyPistolStartCooldownTicks > 0;
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

        PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(serverPlayer);
        if (psycho.getPsychoTicks() > 0) {
            /*
             * 赏金模式复用 Wathe 的疯魔组件。如果玩家已经处在其他疯魔来源中，
             * 继续叠加会让护盾、皮肤、结束回放和锁槽归属混在一起，所以这里直接购买失败。
             */
            return false;
        }

        int slot = findFreeHotbarSlot();
        if (slot < 0) {
            return false;
        }

        ItemStack derringer = ModItems.BOUNTY_DERRINGER.getDefaultStack();
        derringer.set(ModItems.BOUNTY_MODE_GRANTED, true);
        this.player.getInventory().setStack(slot, derringer);
        this.player.getInventory().selectedSlot = slot;
        this.player.playerScreenHandler.sendContentUpdates();

        this.bountyModeActive = true;
        this.bountyDerringerSlot = slot;

        /*
         * 这里不调用 PlayerPsychoComponent#startPsycho，因为原方法会塞入球棒。
         * 我们只复用同一份 psychoTicks / armour 状态，让 Wathe 的疯魔皮肤、心情图标、
         * 护盾抵挡链和环境音全部继续工作，但手持物改为赏金德林加。
         *
         * 护盾层数使用赏金猎人自己的常量，避免后续 Wathe 调整疯魔模式时连带改变赏金模式强度。
         */
        psycho.setPsychoTicks(BountyHunterConstants.BOUNTY_MODE_DURATION_TICKS);
        psycho.setArmour(BountyHunterConstants.BOUNTY_MODE_SHIELD_LAYERS);
        gameWorld.setPsychosActive(gameWorld.getPsychosActive() + 1);

        this.player.getItemCooldownManager().set(ModItems.BOUNTY_MODE, BountyHunterConstants.BOUNTY_MODE_COOLDOWN_TICKS);
        sync();
        return true;
    }

    public void stopBountyMode(boolean clearPsychoState) {
        if (!this.bountyModeActive && this.bountyDerringerSlot < 0) {
            return;
        }

        this.bountyModeActive = false;
        this.bountyDerringerSlot = -1;
        removeModeGrantedDerringer();

        if (clearPsychoState) {
            PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(this.player);
            if (psycho.getPsychoTicks() > 0) {
                psycho.stopPsycho();
            }
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
        boolean changed = tickStartCooldown();

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (!gameWorld.isRole(this.player, NoellesRoleRegistry.BOUNTY_HUNTER)) {
            if (this.bountyModeActive) {
                stopBountyMode(true);
            } else if (changed) {
                sync();
            }
            return;
        }

        tickBountyMode(gameWorld);
        tickTargetSelection(gameWorld);

        if (changed) {
            sync();
        }
    }

    private boolean tickStartCooldown() {
        if (this.bountyPistolStartCooldownTicks <= 0) {
            return false;
        }
        this.bountyPistolStartCooldownTicks--;
        return this.bountyPistolStartCooldownTicks == 0;
    }

    private void tickBountyMode(GameWorldComponent gameWorld) {
        if (!this.bountyModeActive) {
            return;
        }

        PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(this.player);
        if (!GameFunctions.isPlayerAliveAndSurvival(this.player) || psycho.getPsychoTicks() <= 0) {
            /*
             * Wathe 的 PlayerPsychoComponent 会在自己的 tick 里自然结束疯魔。
             * 如果它已经归零，这里只回收模式德林加，不再二次 stopPsycho，避免重复扣 psychosActive。
             */
            stopBountyMode(false);
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

    private int findFreeHotbarSlot() {
        for (int slot = 0; slot < 9; slot++) {
            if (this.player.getInventory().getStack(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
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
        buf.writeBoolean(this.bountyPistolStartCooldownTicks > 0);
        buf.writeInt(this.bountyPistolCooldownTotalTicks);
        buf.writeBoolean(this.bountyModeActive);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.target = buf.readBoolean() ? buf.readUuid() : this.player.getUuid();
        this.bountyPistolStartCooldownTicks = buf.readBoolean() ? 1 : 0;
        this.bountyPistolCooldownTotalTicks = buf.readInt();
        this.bountyModeActive = buf.readBoolean();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putUuid("target", this.target == null ? this.player.getUuid() : this.target);
        tag.putInt("bountyPistolStartCooldownTicks", this.bountyPistolStartCooldownTicks);
        tag.putInt("bountyPistolCooldownTotalTicks", this.bountyPistolCooldownTotalTicks);
        tag.putBoolean("bountyModeActive", this.bountyModeActive);
        tag.putInt("bountyDerringerSlot", this.bountyDerringerSlot);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.target = tag.contains("target") ? tag.getUuid("target") : this.player.getUuid();
        this.bountyPistolStartCooldownTicks = tag.getInt("bountyPistolStartCooldownTicks");
        this.bountyPistolCooldownTotalTicks = tag.contains("bountyPistolCooldownTotalTicks")
                ? tag.getInt("bountyPistolCooldownTotalTicks")
                : BountyHunterConstants.BOUNTY_PISTOL_FAILED_COOLDOWN_TICKS;
        this.bountyModeActive = tag.getBoolean("bountyModeActive");
        this.bountyDerringerSlot = tag.getInt("bountyDerringerSlot");
    }
}
