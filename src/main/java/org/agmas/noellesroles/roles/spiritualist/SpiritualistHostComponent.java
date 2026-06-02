package org.agmas.noellesroles.roles.spiritualist;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.UUID;

/**
 * 被附身目标组件。
 *
 * <p>这个组件挂在“宿主”玩家身上，负责表达：
 * 1. 当前是否正被灵术师附身；
 * 2. 附身者是谁；
 * 3. 这名玩家被保护后应该被传回哪里；
 * 4. 主动结束附身后，是否还保留一层余留庇护。
 *
 * <p>把这些状态放在目标自己身上有两个直接好处：
 * 1. 受害者死亡保护链可以直接从 victim 自己身上读取；
 * 2. 客户端被附身者本地可以直接读取“我正在被附身”的同步状态，便于锁键和提示。</p>
 */
public class SpiritualistHostComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<SpiritualistHostComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "spiritualist_host"),
            SpiritualistHostComponent.class
    );

    private final PlayerEntity player;

    @Nullable
    public UUID spiritualistController;
    public boolean possessed = false;

    /**
     * 目标在受到庇护时要被传回的点。
     *
     * <p>这就是灵术师开始附身那一刻自己原本所在的位置和朝向。</p>
     */
    public double sanctuaryX;
    public double sanctuaryY;
    public double sanctuaryZ;
    public float sanctuaryYaw;
    public float sanctuaryPitch;

    @Nullable
    public UUID lingeringOwner;
    public boolean lingeringProtection = false;
    public int lingeringProtectionTicks = 0;

    public SpiritualistHostComponent(PlayerEntity player) {
        this.player = player;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void reset() {
        this.spiritualistController = null;
        this.possessed = false;
        this.sanctuaryX = 0;
        this.sanctuaryY = 0;
        this.sanctuaryZ = 0;
        this.sanctuaryYaw = 0;
        this.sanctuaryPitch = 0;
        this.lingeringOwner = null;
        this.lingeringProtection = false;
        this.lingeringProtectionTicks = 0;
        this.sync();
    }

    public void startPossession(@NotNull UUID controllerUuid, double sanctuaryX, double sanctuaryY, double sanctuaryZ, float sanctuaryYaw, float sanctuaryPitch) {
        this.spiritualistController = controllerUuid;
        this.possessed = true;
        this.sanctuaryX = sanctuaryX;
        this.sanctuaryY = sanctuaryY;
        this.sanctuaryZ = sanctuaryZ;
        this.sanctuaryYaw = sanctuaryYaw;
        this.sanctuaryPitch = sanctuaryPitch;
        this.lingeringOwner = null;
        this.lingeringProtection = false;
        this.lingeringProtectionTicks = 0;
        this.sync();
    }

    public void stopPossession() {
        this.spiritualistController = null;
        this.possessed = false;
        this.sync();
    }

    public void applyLingeringProtection(@NotNull UUID controllerUuid) {
        this.lingeringOwner = controllerUuid;
        this.lingeringProtection = true;
        this.lingeringProtectionTicks = SpiritualistConstants.LINGERING_PROTECTION_TICKS;
        this.sync();
    }

    public void consumeLingeringProtection() {
        this.lingeringOwner = null;
        this.lingeringProtection = false;
        this.lingeringProtectionTicks = 0;
        this.sync();
    }

    @Override
    public void serverTick() {
        if (this.possessed && this.spiritualistController != null) {
            PlayerEntity controller = this.player.getWorld().getPlayerByUuid(this.spiritualistController);
            if (controller == null || !controller.isAlive() || !GameFunctions.isPlayerAliveAndSurvival(controller)) {
                this.stopPossession();
            } else {
                /*
                 * 被附身者在附身期间不应该继续掉 san。
                 *
                 * 这里每 tick 都续 2 tick 的保护，是为了：
                 * 1. 不必直接侵入 wathe 的主掉 san 公式；
                 * 2. 一旦附身结束，保护也会在极短时间内自然失效，不会残留整段长 buff。
                 */
                PlayerMoodComponent.KEY.get(this.player).setMoodDrainProtectionTicks(2);
            }
        }

        if (this.lingeringProtection && this.lingeringProtectionTicks > 0) {
            this.lingeringProtectionTicks--;
            if (this.lingeringProtectionTicks <= 0) {
                consumeLingeringProtection();
            } else if (this.lingeringProtectionTicks % 20 == 0) {
                this.sync();
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        if (this.spiritualistController != null) {
            tag.putUuid("spiritualistController", this.spiritualistController);
        }
        tag.putBoolean("possessed", this.possessed);
        tag.putDouble("sanctuaryX", this.sanctuaryX);
        tag.putDouble("sanctuaryY", this.sanctuaryY);
        tag.putDouble("sanctuaryZ", this.sanctuaryZ);
        tag.putFloat("sanctuaryYaw", this.sanctuaryYaw);
        tag.putFloat("sanctuaryPitch", this.sanctuaryPitch);

        if (this.lingeringOwner != null) {
            tag.putUuid("lingeringOwner", this.lingeringOwner);
        }
        tag.putBoolean("lingeringProtection", this.lingeringProtection);
        tag.putInt("lingeringProtectionTicks", this.lingeringProtectionTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.spiritualistController = tag.containsUuid("spiritualistController")
                ? tag.getUuid("spiritualistController")
                : null;
        this.possessed = tag.getBoolean("possessed");
        this.sanctuaryX = tag.getDouble("sanctuaryX");
        this.sanctuaryY = tag.getDouble("sanctuaryY");
        this.sanctuaryZ = tag.getDouble("sanctuaryZ");
        this.sanctuaryYaw = tag.getFloat("sanctuaryYaw");
        this.sanctuaryPitch = tag.getFloat("sanctuaryPitch");
        this.lingeringOwner = tag.containsUuid("lingeringOwner") ? tag.getUuid("lingeringOwner") : null;
        this.lingeringProtection = tag.getBoolean("lingeringProtection");
        this.lingeringProtectionTicks = tag.getInt("lingeringProtectionTicks");
    }
}
