package org.agmas.noellesroles.roles.dreamer;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.Set;
import java.util.UUID;

/**
 * 写在被梦之印记标记目标身上的组件。
 *
 * <p>它保存“是谁给我上的印记”和“这层护盾还剩几次”。
 * 目标死亡保护链触发时，死亡处理器会读取这里的数据，再把目标送回梦者身边。</p>
 */
public class DreamerComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<DreamerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "dreamer"),
            DreamerComponent.class
    );

    private final PlayerEntity player;
    public UUID dreamerUuid = null;
    public int dreamArmor = 0;

    public DreamerComponent(@NotNull PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void serverTick() {
        if (this.dreamArmor <= 0) {
            return;
        }

        if (GameWorldComponent.KEY.get(this.player.getWorld()).getRole(this.player) == null) {
            reset();
            return;
        }

        connectWithDreamer();
    }

    public void imprintDreamer(@NotNull PlayerEntity dreamer) {
        this.dreamerUuid = dreamer.getUuid();
        this.dreamArmor = 1;
        sync();
    }

    private void connectWithDreamer() {
        if (this.dreamerUuid == null) {
            return;
        }

        PlayerEntity dreamer = this.player.getWorld().getPlayerByUuid(this.dreamerUuid);
        if (dreamer != null && !GameFunctions.isPlayerSpectatingOrCreative(dreamer)) {
            return;
        }

        /*
         * 梦者离线、死亡或进入旁观后，印记不再有可用归属。
         * 这里同步清掉组件，避免目标后续死亡时被传送到无效玩家位置。
         */
        if (GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            this.player.sendMessage(Text.translatable("tip.noellesroles.dreamer.disconnect").withColor(Noellesroles.DREAMER.color()), true);
            this.player.playSoundToPlayer(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
        reset();
    }

    public void teleportToDreamer() {
        if (this.dreamerUuid == null || this.player.getWorld().isClient) {
            return;
        }

        PlayerEntity dreamer = this.player.getWorld().getPlayerByUuid(this.dreamerUuid);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (!(dreamer instanceof ServerPlayerEntity serverDreamer)
                || !(this.player instanceof ServerPlayerEntity serverTarget)
                || !GameFunctions.isPlayerAliveAndSurvival(serverDreamer)
                || !GameFunctions.isPlayerAliveAndSurvival(serverTarget)) {
            return;
        }

        if (gameWorld.isRole(serverDreamer, Noellesroles.DREAMER)) {
            DreamerKillerComponent dreamerProgress = DreamerKillerComponent.KEY.get(serverDreamer);
            if (!dreamerProgress.hasBecomeKiller()) {
                dreamerProgress.addDreamerCount(serverDreamer);
            }
        }

        ServerWorld serverWorld = serverTarget.getServerWorld();
        serverWorld.spawnParticles(ParticleTypes.PORTAL, serverTarget.getX(), serverTarget.getY(), serverTarget.getZ(), 75, 0.5, 1.5, 0.5, 0.1);
        serverWorld.playSound(null, serverTarget.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);

        /*
         * kinssaba 的实现会避免把疯魔玩家强制拉回梦者身边。
         * 这里保留这个限制，防止和 Wathe 的疯魔移动/击杀节奏互相打架。
         */
        if (PlayerPsychoComponent.KEY.get(serverTarget).getPsychoTicks() <= 0) {
            serverTarget.teleport(serverWorld, serverDreamer.getX(), serverDreamer.getY(), serverDreamer.getZ(), Set.of(), serverDreamer.getYaw(), serverDreamer.getPitch());
        }
        serverDreamer.playSoundToPlayer(SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    public void reset() {
        this.dreamerUuid = null;
        this.dreamArmor = 0;
        sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        tag.putInt("dreamArmor", this.dreamArmor);
        if (this.dreamerUuid != null) {
            tag.putUuid("dreamerUuid", this.dreamerUuid);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.dreamArmor = tag.contains("dreamArmor") ? tag.getInt("dreamArmor") : 0;
        this.dreamerUuid = tag.containsUuid("dreamerUuid") ? tag.getUuid("dreamerUuid") : null;
    }
}
