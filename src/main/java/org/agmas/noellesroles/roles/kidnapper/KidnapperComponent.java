package org.agmas.noellesroles.roles.kidnapper;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.Set;
import java.util.UUID;

/**
 * 绑匪的“被劫持”状态挂在目标玩家身上。
 *
 * <p>这样客户端只需要看自己身上的组件就能显示黑屏和禁用按键；
 * 服务端 tick 也能始终从目标出发，把目标拉回控制者身边。</p>
 */
public class KidnapperComponent implements AutoSyncedComponent, ServerTickingComponent {
    public static final ComponentKey<KidnapperComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(NoellesRolesCore.MOD_ID, "kidnapper"),
            KidnapperComponent.class
    );

    private final PlayerEntity player;
    public UUID controllerUUID = null;
    public int controlTicks = 0;

    public KidnapperComponent(@NotNull PlayerEntity player) {
        this.player = player;
    }

    @Override
    public void serverTick() {
        if (this.controlTicks <= 0) {
            return;
        }

        this.resetWhenOutOfGame();
        if (this.controlTicks <= 0) {
            return;
        }

        /*
         * connectWithController 返回 true 表示本 tick 已经因为距离、潜行释放、死亡/旁观等原因结束控制。
         * 结束后不能再继续传送，否则会出现“刚释放又被拉回去”的一帧错位。
         */
        if (this.connectWithController()) {
            return;
        }

        this.teleportToController();
        this.notifyControllerRemainingTime();
        --this.controlTicks;
        if (this.controlTicks <= 0) {
            this.endControl(false);
            return;
        }
        this.sync();
    }

    private void resetWhenOutOfGame() {
        if (GameWorldComponent.KEY.get(this.player.getWorld()).getRole(this.player) == null) {
            this.resetAll();
        }
    }

    public void startControl(@NotNull PlayerEntity controller) {
        this.controllerUUID = controller.getUuid();
        this.controlTicks = KidnapperConstants.CONTROL_DURATION_TICKS;
        this.sync();
    }

    private boolean connectWithController() {
        if (this.controllerUUID == null) {
            return false;
        }

        PlayerEntity controller = this.player.getWorld().getPlayerByUuid(this.controllerUUID);
        if (controller == null) {
            this.endControl(false);
            return true;
        }
        if (this.player.distanceTo(controller) > KidnapperConstants.CONTROL_BREAK_DISTANCE) {
            this.releaseControlTip();
            this.endControl(false);
            return true;
        }
        if (controller.isSneaking()
                && GameFunctions.isPlayerAliveAndSurvival(controller)
                && GameFunctions.isPlayerAliveAndSurvival(this.player)) {
            this.releaseControlTip();
            this.endControl(true);
            return true;
        }
        if (GameFunctions.isPlayerSpectatingOrCreative(controller) || GameFunctions.isPlayerSpectatingOrCreative(this.player)) {
            this.releaseControlTip();
            this.endControl(false);
            return true;
        }
        return false;
    }

    private void teleportToController() {
        if (this.controllerUUID == null || this.player.getWorld().isClient) {
            return;
        }

        PlayerEntity controller = this.player.getWorld().getPlayerByUuid(this.controllerUUID);
        if (controller != null && this.player.getWorld() instanceof ServerWorld serverWorld) {
            this.player.teleport(
                    serverWorld,
                    controller.getX(),
                    controller.getY(),
                    controller.getZ(),
                    Set.of(),
                    controller.getYaw(),
                    controller.getPitch()
            );
        }
    }

    private void notifyControllerRemainingTime() {
        if (this.controllerUUID == null) {
            return;
        }

        PlayerEntity controller = this.player.getWorld().getPlayerByUuid(this.controllerUUID);
        if (controller != null && this.controlTicks / 20 >= 0) {
            controller.sendMessage(
                    Text.translatable("tip.noellesroles.kidnapper.timeleft", this.controlTicks / 20)
                            .withColor(KidnapperConstants.ROLE_COLOR),
                    true
            );
            if (this.controlTicks == 1) {
                this.releaseControlTip();
            }
        }
    }

    private void releaseControlTip() {
        if (this.controllerUUID == null) {
            return;
        }

        PlayerEntity controller = this.player.getWorld().getPlayerByUuid(this.controllerUUID);
        if (controller != null) {
            controller.sendMessage(
                    Text.translatable("tip.noellesroles.kidnapper.release").withColor(KidnapperConstants.ROLE_COLOR),
                    true
            );
            controller.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    private void endControl(boolean manualRelease) {
        PlayerEntity controller = this.controllerUUID == null ? null : this.player.getWorld().getPlayerByUuid(this.controllerUUID);
        if (manualRelease) {
            /*
             * 绑匪主动潜行放人时，回放要记录“谁提前结束了对谁的劫持”。
             * 自然结束则由目标自己作为 actor，表示“这个人的被劫持状态结束”。
             */
            if (controller instanceof ServerPlayerEntity serverController && this.player instanceof ServerPlayerEntity serverTarget) {
                GameRecordManager.recordSkillUse(serverController, NoellesEventIds.KIDNAPPER_RELEASE_EVENT, serverTarget, null);
            }
        } else if (this.player instanceof ServerPlayerEntity serverTarget) {
            GameRecordManager.recordSkillUse(serverTarget, NoellesEventIds.KIDNAPPER_RELEASE_EVENT, null, null);
        }
        this.reset();
    }

    public void reset() {
        this.resetControlState();
        this.sync();
    }

    /**
     * 回合重置或重新分配绑匪身份时，清掉控制状态与迷药的实际冷却。
     *
     * <p>普通劫持结束只应该清 controlTicks，不应该顺手移除目标玩家身上的迷药冷却；
     * 因为目标自己也可能是绑匪，释放控制时误清冷却会破坏职业物品节奏。</p>
     */
    public void resetAll() {
        this.resetControlState();
        this.player.getItemCooldownManager().remove(ModItems.KNOCKOUT_DRUG);
        this.sync();
    }

    private void resetControlState() {
        this.controllerUUID = null;
        this.controlTicks = 0;
    }

    public void sync() {
        KEY.sync(this.player);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        tag.putInt("controlTicks", this.controlTicks);
        if (this.controllerUUID != null) {
            tag.putUuid("controllerUUID", this.controllerUUID);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.@NotNull WrapperLookup registryLookup) {
        this.controlTicks = tag.contains("controlTicks") ? tag.getInt("controlTicks") : 0;
        this.controllerUUID = tag.contains("controllerUUID") ? tag.getUuid("controllerUUID") : null;
    }

    @Override
    public void writeSyncPacket(RegistryByteBuf buf, ServerPlayerEntity recipient) {
        buf.writeBoolean(this.controllerUUID != null);
        if (this.controllerUUID != null) {
            buf.writeUuid(this.controllerUUID);
        }
        buf.writeInt(this.controlTicks);
    }

    @Override
    public void applySyncPacket(RegistryByteBuf buf) {
        this.controllerUUID = buf.readBoolean() ? buf.readUuid() : null;
        this.controlTicks = buf.readInt();
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return this.player.equals(player);
    }
}
