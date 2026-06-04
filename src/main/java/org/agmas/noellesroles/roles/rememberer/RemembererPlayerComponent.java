package org.agmas.noellesroles.roles.rememberer;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 追忆者组件。
 *
 * <p>它主要负责两件事：</p>
 * <p>1. 记住“追忆技能当前是否仍处于开局 30 秒冷却”，供客户端准心进度条判断总时长；</p>
 * <p>2. 管理狙击枪的三种冷却来源：开局冷却、部署冷却、开火冷却。</p>
 *
 * <p>之所以不只依赖 ItemCooldownManager，是因为客户端 tooltip / HUD 只能拿到“还剩多少比例”，
 * 但用户要求部署 2 秒、开局 30 秒、开火 4 秒都要能区分开来。
 * 所以这里额外同步一个“当前冷却来源”的轻量状态给客户端。</p>
 */
public class RemembererPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {

    public static final ComponentKey<RemembererPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "rememberer"),
            RemembererPlayerComponent.class
    );

    public static final byte SNIPER_COOLDOWN_NONE = 0;
    public static final byte SNIPER_COOLDOWN_START = 1;
    public static final byte SNIPER_COOLDOWN_DEPLOY = 2;
    public static final byte SNIPER_COOLDOWN_SHOT = 3;

    private final PlayerEntity player;
    private int abilityStartCooldownTicks = 0;
    private int sniperStartCooldownTicks = 0;
    private int sniperDeployCooldownTicks = 0;
    private int sniperShotCooldownTicks = 0;
    private byte sniperCooldownSource = SNIPER_COOLDOWN_NONE;
    private int lastSelectedSniperSlot = -1;

    public RemembererPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    /**
     * 回合开始时同时写入：
     * 1. 技能开局 30 秒；
     * 2. 狙击枪开局 30 秒。
     */
    public void startRoundCooldowns() {
        this.abilityStartCooldownTicks = RemembererConstants.RECALL_START_COOLDOWN_TICKS;
        this.sniperStartCooldownTicks = RemembererConstants.SNIPER_START_COOLDOWN_TICKS;
        this.sniperDeployCooldownTicks = 0;
        this.sniperShotCooldownTicks = 0;
        this.sniperCooldownSource = SNIPER_COOLDOWN_START;
        this.lastSelectedSniperSlot = -1;
        sync();
    }

    /**
     * 追忆技能一旦真正成功发动，就不再处于“开局冷却来源”。
     */
    public void clearAbilityStartCooldown() {
        if (this.abilityStartCooldownTicks <= 0) {
            return;
        }
        this.abilityStartCooldownTicks = 0;
        sync();
    }

    /**
     * 记录一次真正成功的狙击开火。
     */
    public void startSniperShotCooldown() {
        this.sniperShotCooldownTicks = RemembererConstants.SNIPER_SHOT_COOLDOWN_TICKS;
        this.sniperDeployCooldownTicks = 0;
        this.sniperCooldownSource = SNIPER_COOLDOWN_SHOT;
        sync();
    }

    public boolean isUsingAbilityStartCooldown() {
        return this.abilityStartCooldownTicks > 0;
    }

    public byte getSniperCooldownSource() {
        return this.sniperCooldownSource;
    }

    /**
     * 当前客户端应该把这次狙击枪冷却视作哪一种总时长。
     */
    public int getDisplayedSniperCooldownTotalTicks() {
        return switch (this.sniperCooldownSource) {
            case SNIPER_COOLDOWN_START -> RemembererConstants.SNIPER_START_COOLDOWN_TICKS;
            case SNIPER_COOLDOWN_DEPLOY -> RemembererConstants.SNIPER_DEPLOY_COOLDOWN_TICKS;
            case SNIPER_COOLDOWN_SHOT -> RemembererConstants.SNIPER_SHOT_COOLDOWN_TICKS;
            default -> 0;
        };
    }

    /**
     * 统一清空追忆者的专属状态。
     */
    public void reset() {
        this.abilityStartCooldownTicks = 0;
        this.sniperStartCooldownTicks = 0;
        this.sniperDeployCooldownTicks = 0;
        this.sniperShotCooldownTicks = 0;
        this.sniperCooldownSource = SNIPER_COOLDOWN_NONE;
        this.lastSelectedSniperSlot = -1;
        this.player.getItemCooldownManager().remove(ModItems.SNIPER_RIFLE);
        sync();
    }

    /**
     * 在服务端已经明确知道“当前选中的就是哪个快捷栏格子”时，立刻同步一次狙击枪部署状态。
     *
     * <p>这个入口专门给：
     * 1. 服务端收到切槽包时的即时处理；
     * 2. 狙击枪开火包到达前的兜底校验。
     *
     * <p>这样部署冷却就不再依赖下一次 {@link #serverTick()} 才建立，
     * 从根上堵住“切到狙击枪同一瞬间立刻右键，抢在下一 tick 前瞬发”的空窗。</p>
     */
    public void syncSniperSelectionStateNow(int selectedSlot) {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        boolean changed = updateSniperSelectionState(serverPlayer, selectedSlot);
        changed |= refreshSniperCooldownSource();
        if (changed) {
            sync();
        }
    }

    /**
     * 使用当前服务端已经记录下来的选槽状态，立即同步一次狙击枪部署逻辑。
     *
     * <p>这主要给“真正执行开火前”的兜底校验使用：
     * 如果切槽包已经先到达，这里会读到最新选槽并确认部署冷却；
     * 如果切槽包还没到达，那么当前主手还不是狙击枪，开火也会自然失败。</p>
     */
    public void syncSniperSelectionStateNow() {
        if (!(this.player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        syncSniperSelectionStateNow(serverPlayer.getInventory().selectedSlot);
    }

    @Override
    public void serverTick() {
        boolean changed = false;

        if (this.abilityStartCooldownTicks > 0) {
            this.abilityStartCooldownTicks--;
            if (this.abilityStartCooldownTicks == 0) {
                changed = true;
            }
        }

        if (this.sniperStartCooldownTicks > 0) {
            this.sniperStartCooldownTicks--;
            if (this.sniperStartCooldownTicks == 0 && this.sniperCooldownSource == SNIPER_COOLDOWN_START) {
                changed = true;
            }
        }

        if (this.sniperShotCooldownTicks > 0) {
            this.sniperShotCooldownTicks--;
            if (this.sniperShotCooldownTicks == 0 && this.sniperCooldownSource == SNIPER_COOLDOWN_SHOT) {
                changed = true;
            }
        }

        if (this.sniperDeployCooldownTicks > 0) {
            this.sniperDeployCooldownTicks--;
            if (this.sniperDeployCooldownTicks == 0 && this.sniperCooldownSource == SNIPER_COOLDOWN_DEPLOY) {
                changed = true;
            }
        }

        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            changed |= updateSniperSelectionState(serverPlayer, serverPlayer.getInventory().selectedSlot);
        }
        changed |= refreshSniperCooldownSource();

        if (changed) {
            sync();
        }
    }

    /**
     * 追忆者的部署冷却只在“活着并手持狙击枪”时有意义。
     *
     * <p>这里刻意把“切到狙击枪这一瞬间”的逻辑统一放在组件里做，
     * 这样：</p>
     * <p>1. 服务端真正禁止使用的时机和客户端 tooltip/准心来源保持一致；</p>
     * <p>2. 开火冷却如果还长于部署冷却，就继续保留更长那段；</p>
     * <p>3. 开火冷却如果已经比部署更短，重新切回时则会按用户要求重新走部署 2 秒。</p>
     */
    private boolean updateSniperSelectionState(ServerPlayerEntity serverPlayer, int selectedSlot) {
        boolean changed = false;

        if (!GameFunctions.isPlayerAliveAndSurvival(serverPlayer)) {
            this.lastSelectedSniperSlot = -1;
            if (this.sniperDeployCooldownTicks > 0) {
                this.sniperDeployCooldownTicks = 0;
                changed = true;
            }
            if (serverPlayer.getItemCooldownManager().isCoolingDown(ModItems.SNIPER_RIFLE)) {
                serverPlayer.getItemCooldownManager().remove(ModItems.SNIPER_RIFLE);
            }
            return changed;
        }

        /*
         * 这里刻意优先看“这次被选中的快捷栏格子里装的是什么”，
         * 而不是只看当前主手。
         *
         * 原因是 onUpdateSelectedSlot 的调用目标就是“服务端刚收到切槽包”的这一刻，
         * 我们要尽量把判断建立在这条数据本身上，避免再次退回到等待下一 tick 才能看见主手变化。
         */
        boolean selectedSlotHoldsSniper = selectedSlot >= 0
                && selectedSlot < 9
                && serverPlayer.getInventory().getStack(selectedSlot).isOf(ModItems.SNIPER_RIFLE);
        if (!selectedSlotHoldsSniper) {
            this.lastSelectedSniperSlot = -1;
            if (this.sniperDeployCooldownTicks > 0) {
                this.sniperDeployCooldownTicks = 0;
                changed = true;
            }
            return changed;
        }

        if (selectedSlot == this.lastSelectedSniperSlot) {
            return changed;
        }

        this.lastSelectedSniperSlot = selectedSlot;
        int selectionCooldown = applySelectionCooldownSource();
        if (selectionCooldown > 0) {
            serverPlayer.getItemCooldownManager().set(ModItems.SNIPER_RIFLE, selectionCooldown);
        }
        return true;
    }

    /**
     * 根据“重新切到狙击枪这一刻”剩余的冷却长度，选择本次真正应该生效的来源。
     */
    private int applySelectionCooldownSource() {
        int persistentCooldown = Math.max(this.sniperStartCooldownTicks, this.sniperShotCooldownTicks);
        if (persistentCooldown > RemembererConstants.SNIPER_DEPLOY_COOLDOWN_TICKS) {
            this.sniperDeployCooldownTicks = 0;
            this.sniperCooldownSource = this.sniperStartCooldownTicks >= this.sniperShotCooldownTicks
                    ? SNIPER_COOLDOWN_START
                    : SNIPER_COOLDOWN_SHOT;
            return persistentCooldown;
        }

        this.sniperDeployCooldownTicks = RemembererConstants.SNIPER_DEPLOY_COOLDOWN_TICKS;
        this.sniperCooldownSource = SNIPER_COOLDOWN_DEPLOY;
        return this.sniperDeployCooldownTicks;
    }

    /**
     * 当倒计时自然结束后，刷新给客户端看的“当前冷却来源”。
     */
    private boolean refreshSniperCooldownSource() {
        byte oldSource = this.sniperCooldownSource;

        if (this.sniperCooldownSource == SNIPER_COOLDOWN_START && this.sniperStartCooldownTicks <= 0) {
            this.sniperCooldownSource = SNIPER_COOLDOWN_NONE;
        } else if (this.sniperCooldownSource == SNIPER_COOLDOWN_SHOT && this.sniperShotCooldownTicks <= 0) {
            this.sniperCooldownSource = SNIPER_COOLDOWN_NONE;
        } else if (this.sniperCooldownSource == SNIPER_COOLDOWN_DEPLOY && this.sniperDeployCooldownTicks <= 0) {
            this.sniperCooldownSource = SNIPER_COOLDOWN_NONE;
        }

        return oldSource != this.sniperCooldownSource;
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return this.player.equals(player);
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.abilityStartCooldownTicks > 0);
        buf.writeByte(this.sniperCooldownSource);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.abilityStartCooldownTicks = buf.readBoolean() ? 1 : 0;
        this.sniperCooldownSource = buf.readByte();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("abilityStartCooldownTicks", this.abilityStartCooldownTicks);
        tag.putInt("sniperStartCooldownTicks", this.sniperStartCooldownTicks);
        tag.putInt("sniperDeployCooldownTicks", this.sniperDeployCooldownTicks);
        tag.putInt("sniperShotCooldownTicks", this.sniperShotCooldownTicks);
        tag.putByte("sniperCooldownSource", this.sniperCooldownSource);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.abilityStartCooldownTicks = tag.getInt("abilityStartCooldownTicks");
        this.sniperStartCooldownTicks = tag.getInt("sniperStartCooldownTicks");
        this.sniperDeployCooldownTicks = tag.getInt("sniperDeployCooldownTicks");
        this.sniperShotCooldownTicks = tag.getInt("sniperShotCooldownTicks");
        this.sniperCooldownSource = tag.getByte("sniperCooldownSource");
    }
}
