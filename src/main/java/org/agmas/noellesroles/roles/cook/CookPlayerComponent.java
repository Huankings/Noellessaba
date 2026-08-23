package org.agmas.noellesroles.roles.cook;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.stamina.PlayerStaminaApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 厨师对“吃过东西的玩家”的短暂标记状态。
 *
 * <p>状态挂在被观察者身上，而不是挂在厨师身上：
 * 因为任意玩家吃东西后，所有存活厨师都应看到同一个目标。
 * 组件同步给客户端后，再由本能 handler 判断观察者是否真的是厨师。</p>
 */
public class CookPlayerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<CookPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "cook"),
            CookPlayerComponent.class
    );

    private final PlayerEntity player;
    private int eatTicks = 0;
    private boolean psychoStaminaBonusApplied = false;
    private float staminaBonusBeforePsycho = 0.0F;

    public CookPlayerComponent(@NotNull PlayerEntity player) {
        this.player = player;
    }

    public int getEatTicks() {
        return this.eatTicks;
    }

    public boolean isMarkedByEating() {
        return this.eatTicks > 0;
    }

    public void markAteFood() {
        this.eatTicks = CookConstants.EAT_MARK_TICKS;
        this.sync();
    }

    public void reset() {
        /*
         * 玩家重置可能发生在厨师疯魔还没自然结束时。
         * 先恢复体力上限，再清厨师自己的状态，避免临时“无限体力”加成残留到下一局。
         */
        this.restorePsychoStaminaBonusIfNeeded();
        this.eatTicks = 0;
        this.sync();
    }

    public void applyPsychoStaminaBonus() {
        if (!this.psychoStaminaBonusApplied) {
            /*
             * Wathe 当前只提供一个全局 maxStaminaBonus 字段。
             * 进入厨师疯魔时先记住原值，结束时再恢复，避免把其它未来机制写入的体力修正永久覆盖。
             */
            this.staminaBonusBeforePsycho = PlayerStaminaApi.getMaxStaminaBonus(this.player);
            this.psychoStaminaBonusApplied = true;
            this.sync();
        }

        PlayerStaminaApi.setMaxStaminaBonus(
                this.player,
                this.staminaBonusBeforePsycho + CookConstants.PSYCHO_COOK_STAMINA_MAX_BONUS
        );
        PlayerStaminaApi.fillStamina(this.player);
    }

    public void restorePsychoStaminaBonusIfNeeded() {
        if (!this.psychoStaminaBonusApplied) {
            return;
        }

        PlayerStaminaApi.setMaxStaminaBonus(this.player, this.staminaBonusBeforePsycho);
        this.psychoStaminaBonusApplied = false;
        this.staminaBonusBeforePsycho = 0.0F;
        this.sync();
    }

    @Override
    public void serverTick() {
        if (this.eatTicks <= 0) {
            return;
        }

        /*
         * 回合外或玩家还没分配职业时，旧标记没有任何玩法意义。
         * 主动清掉可以避免上一把残留的同步状态影响下一把开局观感。
         */
        if (GameWorldComponent.KEY.get(this.player.getWorld()).getRole(this.player) == null) {
            this.reset();
            return;
        }

        this.eatTicks--;
        this.sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        tag.putInt("eatTicks", this.eatTicks);
        tag.putBoolean("psychoStaminaBonusApplied", this.psychoStaminaBonusApplied);
        tag.putFloat("staminaBonusBeforePsycho", this.staminaBonusBeforePsycho);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        this.eatTicks = tag.contains("eatTicks") ? tag.getInt("eatTicks") : 0;
        this.psychoStaminaBonusApplied = tag.contains("psychoStaminaBonusApplied") && tag.getBoolean("psychoStaminaBonusApplied");
        this.staminaBonusBeforePsycho = tag.contains("staminaBonusBeforePsycho") ? tag.getFloat("staminaBonusBeforePsycho") : 0.0F;
    }
}
