package org.agmas.noellesroles.roles.bomber;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheParticles;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 炸弹客玩家组件。
 * 这个组件专门负责维护“某位玩家身上是否挂着一枚正在运行的定时炸弹”。
 *
 * 整体流程如下：
 * 1. 炸弹客先把一枚未启动的炸弹放到目标身上。
 * 2. 经过一段静默时间后，炸弹进入滴滴声阶段。
 * 3. 只有滴滴声开始后，持有者才会真正看见手里的定时炸弹，并且能继续传递给别人。
 * 4. 倒计时结束后直接爆炸，并使用专属死因完成击杀与结算。
 */
public class BomberPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {

    public static final ComponentKey<BomberPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "bomber"),
            BomberPlayerComponent.class
    );

    /**
     * 炸弹客开局时对定时炸弹施加的初始冷却。
     */
    public static final int BOMBER_START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);

    /**
     * 炸弹放到别人身上后，进入滴滴声之前的静默时长。
     */
    public static final int BOMB_DELAY_TICKS = GameConstants.getInTicks(0, 10);

    /**
     * 滴滴声阶段的总时长。
     */
    public static final int BEEP_DURATION_TICKS = GameConstants.getInTicks(0, 15);

    /**
     * 每次传递后，对新持有者施加的短冷却。
     * 用来避免一瞬间连续来回互传。
     */
    public static final int TRANSFER_COOLDOWN_TICKS = GameConstants.getInTicks(0, 3);

    /**
     * 滴滴声播放间隔。
     */
    public static final int BEEP_INTERVAL_TICKS = 6;

    private static final int NORMAL_BOMB_REWARD = 70;
    private static final int BREAKDOWN_BOMB_REWARD = 120;
    private static final int FELL_OUT_BOMB_REWARD = 170;
    private static final Identifier MENTAL_BREAKDOWN_REASON = Identifier.of("wathe", "mental_breakdown");
    private static final Identifier FELL_OUT_OF_TRAIN_REASON = Identifier.of("wathe", "fell_out_of_train");

    private static final SoundEvent BOMB_BEEP_SOUND = SoundEvent.of(Identifier.of(Noellesroles.MOD_ID, "item.bomb.beep"));
    private static final SoundEvent BOMB_EXPLODE_SOUND = SoundEvent.of(Identifier.of(Noellesroles.MOD_ID, "item.bomb.explode"));

    private final PlayerEntity player;

    /**
     * 当前玩家身上是否有一枚正在运行的定时炸弹。
     */
    private boolean hasBomb = false;

    /**
     * 从“放到玩家身上”到“开始滴滴响”的计时器。
     */
    private int bombTimer = 0;

    /**
     * 滴滴声阶段的剩余倒计时。
     */
    private int beepTimer = 0;

    /**
     * 是否已经进入滴滴声阶段。
     */
    private boolean isBeeping = false;

    /**
     * 记录这枚炸弹最初属于哪位炸弹客，用于后续奖励结算。
     */
    private UUID bomberUuid = null;

    /**
     * 上一次显示在 ActionBar 里的秒数。
     * 用来避免每 tick 都重复刷新提示。
     */
    private int lastDisplayedSeconds = -1;

    public BomberPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        // 只把“谁身上有炸弹、炸弹是否已经开始响”同步给炸弹客本人，
        // 这样客户端才能在本能透视里把真正正在持有活动炸弹的玩家高亮出来。
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRole(player, Noellesroles.BOMBER);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.hasBomb);
        buf.writeBoolean(this.isBeeping);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.hasBomb = buf.readBoolean();
        this.isBeeping = buf.readBoolean();
    }

    /**
     * 在回合重置或角色状态重置时，清空炸弹相关数据。
     */
    public void reset() {
        boolean hadBomb = this.hasBomb;
        if (hadBomb) {
            removeBombFromInventory(this.player);
        }
        clearBombState();
        if (hadBomb) {
            sync();
        }
    }

    /**
     * 把一枚尚未启动的炸弹挂到目标玩家身上。
     */
    public void placeBomb(PlayerEntity bomber) {
        placeBomb(bomber.getUuid());
    }

    /**
     * 按指定炸弹客 UUID 把一枚尚未启动的炸弹挂到目标玩家身上。
     * 之所以额外保留这个重载，是为了支持“托盘/床预埋炸弹”这种延迟触发场景：
     * 触发时原始炸弹客未必正站在现场，但仍然需要把归属记下来，方便后续结算。
     */
    public void placeBomb(@Nullable UUID bomberUuid) {
        this.hasBomb = true;
        this.bombTimer = BOMB_DELAY_TICKS;
        this.beepTimer = 0;
        this.isBeeping = false;
        this.bomberUuid = bomberUuid;
        this.lastDisplayedSeconds = -1;
        this.sync();
    }

    /**
     * 当前玩家是否可以把这枚活动炸弹传给目标。
     */
    public boolean canTransfer(PlayerEntity target) {
        return this.hasBomb
                && this.isBeeping
                && target != null
                && target != this.player
                && !this.player.getItemCooldownManager().isCoolingDown(ModItems.TIMED_BOMB)
                && GameFunctions.isPlayerAliveAndSurvival(target)
                && !KEY.get(target).hasBomb;
    }

    /**
     * 将一枚已经开始倒计时的炸弹传递给另一名玩家。
     */
    public void transferBomb(PlayerEntity target) {
        if (!canTransfer(target)) {
            return;
        }

        BomberPlayerComponent targetComponent = KEY.get(target);
        targetComponent.receiveTransferredBomb(this.beepTimer, this.bomberUuid);

        removeBombFromInventory(this.player);
        clearBombState();
        sync();

        this.player.getWorld().playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                SoundEvents.ENTITY_ITEM_PICKUP,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );
    }

    /**
     * 供新持有者接手一枚已经开始滴滴响的炸弹。
     */
    private void receiveTransferredBomb(int remainingBeepTicks, @Nullable UUID bomberUuid) {
        this.hasBomb = true;
        this.bombTimer = 0;
        this.beepTimer = remainingBeepTicks;
        this.isBeeping = true;
        this.bomberUuid = bomberUuid;
        this.lastDisplayedSeconds = -1;
        this.player.getItemCooldownManager().set(ModItems.TIMED_BOMB, TRANSFER_COOLDOWN_TICKS);
        giveActiveBombToSelectedHand();
        this.sync();
    }

    @Override
    public void serverTick() {
        if (!this.hasBomb) {
            return;
        }

        if (!this.isBeeping) {
            if (this.bombTimer > 0) {
                this.bombTimer--;
            } else {
                startBeeping();
            }
            return;
        }

        if (this.beepTimer > 0) {
            if (this.beepTimer % BEEP_INTERVAL_TICKS == 0) {
                this.player.getWorld().playSound(
                        null,
                        this.player.getX(),
                        this.player.getY(),
                        this.player.getZ(),
                        BOMB_BEEP_SOUND,
                        SoundCategory.PLAYERS,
                        2.0F,
                        1.0F
                );
            }

            int secondsLeft = (this.beepTimer + 19) / 20;
            if (secondsLeft != this.lastDisplayedSeconds) {
                this.lastDisplayedSeconds = secondsLeft;
                this.player.sendMessage(
                        Text.translatable("tip.bomber.bomb_warning", secondsLeft).formatted(Formatting.RED, Formatting.BOLD),
                        true
                );
            }

            this.beepTimer--;
            return;
        }

        explode();
    }

    /**
     * 滴滴声阶段开始时，才把定时炸弹真正塞进持有者手里。
     * 这样才能实现“前面看不见，开始响后才显形”的需求。
     */
    private void startBeeping() {
        this.isBeeping = true;
        this.beepTimer = BEEP_DURATION_TICKS;
        this.lastDisplayedSeconds = -1;
        this.player.getItemCooldownManager().set(ModItems.TIMED_BOMB, TRANSFER_COOLDOWN_TICKS);
        giveActiveBombToSelectedHand();
        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            NbtCompound extra = new NbtCompound();
            extra.putUuid("victim", serverPlayer.getUuid());
            GameRecordManager.recordGlobalEvent(serverPlayer.getServerWorld(), Noellesroles.TIMED_BOMB_ACTIVATED_EVENT, null, extra);
        }
        this.sync();
    }

    /**
     * 触发炸弹爆炸。
     * 这里负责声音、粒子和实际击杀；奖励结算与状态清理由死亡流程中的 mixin 统一处理。
     */
    private void explode() {
        if (!(this.player.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        serverWorld.playSound(
                null,
                this.player.getX(),
                this.player.getY(),
                this.player.getZ(),
                BOMB_EXPLODE_SOUND,
                SoundCategory.PLAYERS,
                3.0F,
                1.0F
        );

        serverWorld.spawnParticles(
                WatheParticles.BIG_EXPLOSION,
                this.player.getX(),
                this.player.getY() + 0.5,
                this.player.getZ(),
                1,
                0.0,
                0.0,
                0.0,
                0.0
        );
        serverWorld.spawnParticles(
                ParticleTypes.SMOKE,
                this.player.getX(),
                this.player.getY() + 0.5,
                this.player.getZ(),
                100,
                0.0,
                0.0,
                0.0,
                0.2
        );
        serverWorld.spawnParticles(
                new ItemStackParticleEffect(ParticleTypes.ITEM, ModItems.TIMED_BOMB.getDefaultStack()),
                this.player.getX(),
                this.player.getY() + 0.5,
                this.player.getZ(),
                100,
                0.0,
                0.0,
                0.0,
                1.0
        );

        PlayerEntity bomber = this.bomberUuid == null ? null : this.player.getWorld().getPlayerByUuid(this.bomberUuid);
        if (GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            GameFunctions.killPlayer(this.player, true, bomber, Noellesroles.DEATH_REASON_BOMB);
        } else {
            removeBombFromInventory(this.player);
            clearBombState();
            this.sync();
        }
    }

    /**
     * 在 Wathe 的 killPlayer 流程中调用。
     * 负责给炸弹客结算额外金币，并清空受害者身上的炸弹状态。
     */
    public static void handleBombCarrierDeath(PlayerEntity victim, Identifier deathReason) {
        BomberPlayerComponent component = KEY.get(victim);
        if (!component.hasBomb) {
            return;
        }

        // 无论死于爆炸、精神崩溃还是跌出列车，都要先把活动炸弹从死者身上移除
        component.removeBombFromInventory(victim);

        int reward = getRewardForDeathReason(deathReason);
        if (reward > 0 && component.bomberUuid != null) {
            PlayerEntity bomber = victim.getWorld().getPlayerByUuid(component.bomberUuid);
            if (bomber instanceof ServerPlayerEntity serverBomber
                    && GameFunctions.isPlayerAliveAndSurvival(serverBomber)
                    && GameWorldComponent.KEY.get(serverBomber.getWorld()).isRole(serverBomber, Noellesroles.BOMBER)) {
                PlayerShopComponent.KEY.get(serverBomber).addToBalance(reward);
            }
        }

        component.clearBombState();
        component.sync();
    }

    private static int getRewardForDeathReason(Identifier deathReason) {
        if (Noellesroles.DEATH_REASON_BOMB.equals(deathReason)) {
            return NORMAL_BOMB_REWARD;
        }
        // 这里改成直接写死 Identifier，避免某些开发环境里对 DeathReasons.MENTAL_BREAKDOWN 解析异常
        if (MENTAL_BREAKDOWN_REASON.equals(deathReason)) {
            return BREAKDOWN_BOMB_REWARD;
        }
        if (FELL_OUT_OF_TRAIN_REASON.equals(deathReason)) {
            return FELL_OUT_BOMB_REWARD;
        }
        return 0;
    }

    /**
     * 用于商店判断：这位炸弹客是否已经有一枚属于自己的活动炸弹正在场上运行。
     */
    public static boolean hasBombInCirculation(PlayerEntity bomber) {
        for (PlayerEntity player : bomber.getWorld().getPlayers()) {
            BomberPlayerComponent component = KEY.get(player);
            if (component.hasBomb && bomber.getUuid().equals(component.bomberUuid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把活动炸弹强制放进玩家当前选中的热栏槽位。
     * 如果当前槽位已经有别的物品，就尽量把原物品挪到其他空位。
     */
    private void giveActiveBombToSelectedHand() {
        ItemStack bombStack = ModItems.TIMED_BOMB.getDefaultStack();
        int selectedSlot = this.player.getInventory().selectedSlot;
        ItemStack selectedStack = this.player.getInventory().getStack(selectedSlot);

        if (selectedStack.isEmpty()) {
            this.player.getInventory().setStack(selectedSlot, bombStack);
            syncInventory();
            return;
        }

        for (int i = 0; i < this.player.getInventory().size(); i++) {
            if (i == selectedSlot) {
                continue;
            }
            if (this.player.getInventory().getStack(i).isEmpty()) {
                this.player.getInventory().setStack(i, selectedStack.copy());
                this.player.getInventory().setStack(selectedSlot, bombStack);
                syncInventory();
                return;
            }
        }

        // 如果背包真的已经满了，也必须保证炸弹出现在手里，因此把当前手持物直接丢出
        this.player.dropItem(selectedStack.copy(), false, false);
        this.player.getInventory().setStack(selectedSlot, bombStack);
        syncInventory();
    }

    private void syncInventory() {
        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.playerScreenHandler.sendContentUpdates();
        }
    }

    private void removeBombFromInventory(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(ModItems.TIMED_BOMB)) {
                player.getInventory().removeStack(i, 1);
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.playerScreenHandler.sendContentUpdates();
                }
                return;
            }
        }
    }

    private void clearBombState() {
        this.hasBomb = false;
        this.bombTimer = 0;
        this.beepTimer = 0;
        this.isBeeping = false;
        this.bomberUuid = null;
        this.lastDisplayedSeconds = -1;
        this.player.getItemCooldownManager().remove(ModItems.TIMED_BOMB);
    }

    public boolean hasBomb() {
        return this.hasBomb;
    }

    public boolean isBeeping() {
        return this.isBeeping;
    }

    public UUID getBomberUuid() {
        return this.bomberUuid;
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("hasBomb", this.hasBomb);
        tag.putInt("bombTimer", this.bombTimer);
        tag.putInt("beepTimer", this.beepTimer);
        tag.putBoolean("isBeeping", this.isBeeping);
        if (this.bomberUuid != null) {
            tag.putUuid("bomberUuid", this.bomberUuid);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.hasBomb = tag.getBoolean("hasBomb");
        this.bombTimer = tag.getInt("bombTimer");
        this.beepTimer = tag.getInt("beepTimer");
        this.isBeeping = tag.getBoolean("isBeeping");
        if (tag.containsUuid("bomberUuid")) {
            this.bomberUuid = tag.getUuid("bomberUuid");
        } else {
            this.bomberUuid = null;
        }
    }
}
