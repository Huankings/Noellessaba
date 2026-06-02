package org.agmas.noellesroles.client.roles.spiritualist;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.ServerLinks;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.UUID;

/**
 * 灵术师附身时使用的本地假玩家相机。
 *
 * <p>和灵魂出窍相机不同，它不负责自己飞行，
 * 而是专门拿来承接两件事：</p>
 * <p>1. 本地即时转头，不再直接把视角绑死在远端宿主实体上；</p>
 * <p>2. 用宿主皮肤和宿主手臂模型来渲染第一人称手臂。</p>
 *
 * <p>它本身不会向服务器发送任何移动包，
 * 真正有效的控制仍然只由灵术师的 C2S 输入包驱动服务端宿主。</p>
 */
public class SpiritualPossessionCamera extends ClientPlayerEntity {

    private EntityPose authoritativePose = EntityPose.STANDING;
    private float authoritativeEyeHeight = 1.62f;

    private static final ClientPlayNetworkHandler DUMMY_HANDLER = new ClientPlayNetworkHandler(
            SpiritualistClientController.client(),
            SpiritualistClientController.client().getNetworkHandler().getConnection(),
            new ClientConnectionState(
                    new GameProfile(UUID.randomUUID(), "SpiritualPossessionCamera"),
                    SpiritualistClientController.client().getTelemetryManager().createWorldSession(false, null, null),
                    DynamicRegistryManager.Immutable.EMPTY,
                    FeatureSet.empty(),
                    null,
                    SpiritualistClientController.client().getCurrentServerEntry(),
                    SpiritualistClientController.client().currentScreen,
                    Collections.emptyMap(),
                    SpiritualistClientController.client().inGameHud.getChatHud().toChatState(),
                    false,
                    Collections.emptyMap(),
                    ServerLinks.EMPTY
            )
    ) {
        @Override
        public void sendPacket(Packet<?> packet) {
            // 本地附身相机只是渲染与视角载体，绝不能自己参与网络同步。
        }
    };

    public SpiritualPossessionCamera(int id) {
        super(
                SpiritualistClientController.client(),
                SpiritualistClientController.client().world,
                DUMMY_HANDLER,
                SpiritualistClientController.client().player.getStatHandler(),
                SpiritualistClientController.client().player.getRecipeBook(),
                false,
                false
        );
        setId(id);
        setPose(EntityPose.STANDING);
        this.input = new Input();
        this.noClip = true;
    }

    public void applyPosition(Entity entity) {
        this.authoritativePose = entity.getPose();
        this.authoritativeEyeHeight = entity.getEyeHeight(this.authoritativePose);
        applyPossessionPose(this.authoritativePose);
        refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch());
        renderPitch = getPitch();
        renderYaw = getYaw();
        lastRenderPitch = renderPitch;
        lastRenderYaw = renderYaw;
    }

    public void applyViewState(
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            float yaw,
            float pitch,
            float headYaw,
            float bodyYaw,
            EntityPose pose,
            float eyeHeight,
            boolean sprinting,
            boolean sneaking,
            boolean onGround
    ) {
        this.prevX = getX();
        this.prevY = getY();
        this.prevZ = getZ();
        this.lastRenderX = this.prevX;
        this.lastRenderY = this.prevY;
        this.lastRenderZ = this.prevZ;
        this.prevYaw = getYaw();
        this.prevPitch = getPitch();
        this.prevHeadYaw = getHeadYaw();
        this.prevBodyYaw = getBodyYaw();
        this.lastRenderYaw = this.renderYaw;
        this.lastRenderPitch = this.renderPitch;

        setPosition(x, y, z);
        setYaw(yaw);
        setPitch(pitch);
        setHeadYaw(headYaw);
        setBodyYaw(bodyYaw);
        this.authoritativePose = pose;
        this.authoritativeEyeHeight = eyeHeight;
        setSprinting(sprinting);
        setSneaking(sneaking);
        applyPossessionPose(pose);
        setOnGround(onGround);
        setVelocity(velocityX, velocityY, velocityZ);

        this.renderYaw = yaw;
        this.renderPitch = pitch;
    }

    /**
     * 附身相机本身也是一个本地假玩家实体，
     * 仅仅 setSneaking(true) 还不足以让第一人称眼高真的切进蹲下高度。
     *
     * <p>这里直接同步宿主的真实 pose，
     * 这样潜行、一格高爬行、木板门压低、游泳等所有低姿态都会原样带进本地附身相机。</p>
     */
    private void applyPossessionPose(@Nullable EntityPose pose) {
        EntityPose targetPose = pose == null ? EntityPose.STANDING : pose;
        if (getPose() == targetPose) {
            return;
        }

        setPose(targetPose);
        calculateDimensions();
    }

    public float getAuthoritativeEyeHeight() {
        return this.authoritativeEyeHeight;
    }

    /**
     * 每 tick 同步一次灵术师本地“镜像出来的宿主视觉状态”。
     *
     * <p>这样第一人称手里拿着的物品、血量/经验关联的使用状态等，
     * 都继续沿用灵术师自己那份已经被服务端镜像好的数据。</p>
     */
    public void syncVisualStateFromLocalPlayer() {
        ClientPlayerEntity localPlayer = SpiritualistClientController.client().player;
        if (localPlayer == null) {
            return;
        }

        getInventory().clone(localPlayer.getInventory());
        getInventory().selectedSlot = localPlayer.getInventory().selectedSlot;
        setHealth(localPlayer.getHealth());
        setAbsorptionAmount(localPlayer.getAbsorptionAmount());
        experienceLevel = localPlayer.experienceLevel;
        totalExperience = localPlayer.totalExperience;
        experienceProgress = localPlayer.experienceProgress;
    }

    public void spawn() {
        if (clientWorld != null) {
            clientWorld.addEntity(this);
        }
    }

    public void despawn() {
        if (clientWorld != null && clientWorld.getEntityById(getId()) != null) {
            clientWorld.removeEntity(getId(), RemovalReason.DISCARDED);
        }
    }

    @Override
    public SkinTextures getSkinTextures() {
        AbstractClientPlayerEntity host = SpiritualistClientController.getPossessionHost();
        if (host != null) {
            return host.getSkinTextures();
        }
        ClientPlayerEntity localPlayer = SpiritualistClientController.client().player;
        return localPlayer != null ? localPlayer.getSkinTextures() : super.getSkinTextures();
    }

    @Override
    public float getHandSwingProgress(float tickDelta) {
        AbstractClientPlayerEntity host = SpiritualistClientController.getPossessionHost();
        if (host != null) {
            return host.getHandSwingProgress(tickDelta);
        }
        ClientPlayerEntity localPlayer = SpiritualistClientController.client().player;
        return localPlayer != null ? localPlayer.getHandSwingProgress(tickDelta) : super.getHandSwingProgress(tickDelta);
    }

    @Override
    public int getItemUseTimeLeft() {
        AbstractClientPlayerEntity host = SpiritualistClientController.getPossessionHost();
        if (host != null) {
            return host.getItemUseTimeLeft();
        }
        ClientPlayerEntity localPlayer = SpiritualistClientController.client().player;
        return localPlayer != null ? localPlayer.getItemUseTimeLeft() : super.getItemUseTimeLeft();
    }

    @Override
    public boolean isUsingItem() {
        AbstractClientPlayerEntity host = SpiritualistClientController.getPossessionHost();
        if (host != null) {
            return host.isUsingItem();
        }
        ClientPlayerEntity localPlayer = SpiritualistClientController.client().player;
        return localPlayer != null ? localPlayer.isUsingItem() : super.isUsingItem();
    }

    @Override
    public ItemStack getActiveItem() {
        AbstractClientPlayerEntity host = SpiritualistClientController.getPossessionHost();
        if (host != null) {
            return host.getActiveItem();
        }
        ClientPlayerEntity localPlayer = SpiritualistClientController.client().player;
        return localPlayer != null ? localPlayer.getActiveItem() : ItemStack.EMPTY;
    }

    @Override
    public Hand getActiveHand() {
        AbstractClientPlayerEntity host = SpiritualistClientController.getPossessionHost();
        if (host != null) {
            return host.getActiveHand();
        }
        ClientPlayerEntity localPlayer = SpiritualistClientController.client().player;
        return localPlayer != null ? localPlayer.getActiveHand() : super.getActiveHand();
    }

    @Override
    public boolean collidesWith(Entity other) {
        return false;
    }

    @Override
    public void tick() {
        this.age++;
    }

    @Override
    public void tickMovement() {
        // 附身相机的位置完全由外部同步层驱动，这里故意不跑原版移动逻辑。
        // 但保留服务器同步过来的速度值，方便第一人称跳跃/落地表现更接近原版。
    }

    /**
     * 宿主相机模式下，这个本地壳实体只承担第一人称手臂渲染。
     *
     * <p>因此它的朝向必须始终跟随当前真正相机的朝向，
     * 否则 HeldItemRenderer 会把旧 yaw/pitch 当成手臂的基础旋转，
     * 导致你测试里那种“一转头，手臂跟着横着飞出去”的错位。</p>
     */
    public void syncFacingFromCamera(float yaw, float pitch) {
        this.prevYaw = getYaw();
        this.prevPitch = getPitch();
        this.prevHeadYaw = getHeadYaw();
        this.prevBodyYaw = getBodyYaw();
        this.lastRenderYaw = this.renderYaw;
        this.lastRenderPitch = this.renderPitch;

        setYaw(yaw);
        setPitch(pitch);
        setHeadYaw(yaw);
        setBodyYaw(yaw);
        this.renderYaw = yaw;
        this.renderPitch = pitch;
        this.lastRenderYaw = yaw;
        this.lastRenderPitch = pitch;
    }
}
