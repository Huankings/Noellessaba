package org.agmas.noellesroles.roles.phantom;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 幻灵隐身状态跟踪组件。
 *
 * <p>这里不负责“给玩家上隐身”，而是只负责追踪：
 * 1. 这次隐身是否仍在进行；
 * 2. 什么时候应该补发“隐身结束”的回放事件。</p>
 *
 * <p>这样做的原因是：
 * 单纯在 Ability 里只能记住“开始隐身”这一瞬间，
 * 但结束事件一定要依赖后续 tick 才能稳定判断。</p>
 */
public class PhantomPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<PhantomPlayerComponent> KEY =
            ComponentRegistry.getOrCreate(Identifier.of(Noellesroles.MOD_ID, "phantom_state"), PhantomPlayerComponent.class);

    private final PlayerEntity player;
    private boolean invisibilityActive = false;
    private int remainingTicks = 0;

    public PhantomPlayerComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    /**
     * 开始追踪一段新的隐身。
     *
     * <p>真正的回放“开始事件”仍然在 Ability 内记录，
     * 这里专注于后续结束检测。</p>
     */
    public void startInvisibility() {
        this.invisibilityActive = true;
        this.remainingTicks = PhantomConstants.INVISIBILITY_DURATION_TICKS;
        this.sync();
    }

    /**
     * 无声清空状态。
     *
     * <p>用于回合重置、玩家死亡、对局结束等场景。
     * 这些情况不属于“正常结束隐身”，因此不应该播报回放事件。</p>
     */
    public void reset() {
        this.invisibilityActive = false;
        this.remainingTicks = 0;
        this.sync();
    }

    @Override
    public void serverTick() {
        if (!this.invisibilityActive) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (!gameWorld.isRunning() || !GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            this.reset();
            return;
        }

        /*
         * 如果隐身效果被其他逻辑提前移除了，也应当在这里立刻补发“结束隐身”事件，
         * 避免出现局内确实现形了，但回放里一直没有结束节点的问题。
         */
        if (!this.player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
            this.finishInvisibility();
            return;
        }

        this.remainingTicks--;
        if (this.remainingTicks <= 0) {
            this.finishInvisibility();
        } else if (this.remainingTicks % 20 == 0) {
            this.sync();
        }
    }

    private void finishInvisibility() {
        if (this.player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordGlobalEvent(
                    serverPlayer.getServerWorld(),
                    Noellesroles.PHANTOM_INVISIBILITY_ENDED_EVENT,
                    serverPlayer,
                    null
            );
        }
        this.invisibilityActive = false;
        this.remainingTicks = 0;
        this.sync();
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("invisibilityActive", this.invisibilityActive);
        tag.putInt("remainingTicks", this.remainingTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.invisibilityActive = tag.getBoolean("invisibilityActive");
        this.remainingTicks = Math.max(0, tag.getInt("remainingTicks"));
    }
}
