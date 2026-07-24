package org.agmas.noellesroles.roles.hacker;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

/**
 * 黑客专用安全时间。
 *
 * <p>用户确认只需要给 Hacker 一个窄范围安全时间组件。
 * 因此这个组件只被黑客破解逻辑和黑客 HUD 读取，不参与全局死亡保护，也不改任何物品冷却。</p>
 */
public class HackerSafeTimeComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<HackerSafeTimeComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "hacker_safe"),
            HackerSafeTimeComponent.class
    );

    private static boolean initialized = false;
    private static boolean startPending = false;
    private final World world;
    private boolean safe = false;
    private int safeTicks = 0;

    public HackerSafeTimeComponent(@NotNull World world) {
        this.world = world;
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        GameEvents.ON_GAME_START.register(gameMode -> startPending = true);
        GameEvents.ON_GAME_STOP.register(gameMode -> startPending = false);
        GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) -> {
            if (world instanceof ServerWorld serverWorld) {
                KEY.get(serverWorld).reset();
            }
        });

        ServerTickEvents.START_SERVER_TICK.register(HackerSafeTimeComponent::startIfPending);
    }

    private static void startIfPending(MinecraftServer server) {
        if (!startPending) {
            return;
        }
        startPending = false;
        KEY.get(server.getOverworld()).start();
    }

    @Override
    public void serverTick() {
        if (!this.safe) {
            return;
        }

        if (!GameWorldComponent.KEY.get(this.world).isRunning()) {
            reset();
            return;
        }

        this.safeTicks++;
        if (this.safeTicks >= getSafeDurationTicks()) {
            reset();
            return;
        }

        if (this.safeTicks % 20 == 0) {
            sync();
        }
    }

    public boolean isSafe() {
        return this.safe && this.safeTicks < getSafeDurationTicks();
    }

    public int getSafeTicks() {
        return this.safeTicks;
    }

    private void start() {
        this.safe = getSafeDurationTicks() > 0;
        this.safeTicks = 0;
        sync();
    }

    public void reset() {
        this.safe = false;
        this.safeTicks = 0;
        sync();
    }

    private static int getSafeDurationTicks() {
        return Math.max(0, NoellesRolesConfig.HANDLER.instance().generalCooldownTicks);
    }

    private void sync() {
        KEY.sync(this.world);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putBoolean("safe", this.safe);
        tag.putInt("safeTicks", this.safeTicks);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.safe = tag.contains("safe") && tag.getBoolean("safe");
        this.safeTicks = tag.contains("safeTicks") ? tag.getInt("safeTicks") : 0;
    }
}
