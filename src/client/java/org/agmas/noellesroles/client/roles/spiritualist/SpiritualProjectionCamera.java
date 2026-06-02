package org.agmas.noellesroles.client.roles.spiritualist;

import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.cca.MapVariablesWorldComponent;
import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.ServerLinks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.client.mixin.roles.spiritualist.SpiritualistKeyBindingAccessor;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistConstants;

import java.util.Collections;
import java.util.UUID;

/**
 * 灵术师灵魂出窍使用的本地假玩家相机。
 *
 * <p>它只存在于客户端，不参与任何服务器判定。
 * 服务端真正记录的仍然是灵术师本体站在原地的状态。</p>
 */
public class SpiritualProjectionCamera extends ClientPlayerEntity {

    private double bodyX;
    private double bodyY;
    private double bodyZ;

    private static final ClientPlayNetworkHandler DUMMY_HANDLER = new ClientPlayNetworkHandler(
            SpiritualistClientController.client(),
            SpiritualistClientController.client().getNetworkHandler().getConnection(),
            new ClientConnectionState(
                    new GameProfile(UUID.randomUUID(), "SpiritualProjectionCamera"),
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
            // 灵魂相机只是本地渲染载体，不应该向服务器发任何移动包。
        }
    };

    public SpiritualProjectionCamera(int id) {
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
        setPose(EntityPose.SWIMMING);
        getAbilities().flying = true;
        this.input = new KeyboardInput(SpiritualistClientController.client().options) {
            @Override
            public void tick(boolean slowDown, float slowDownFactor) {
                super.tick(slowDown, slowDownFactor);
                this.jumping = isProjectionJumpPhysicallyPressed();
            }
        };
    }

    public void applyPosition(Entity entity) {
        double y = getSwimmingY(entity);
        refreshPositionAndAngles(entity.getX(), y, entity.getZ(), entity.getYaw(), entity.getPitch());
        renderPitch = getPitch();
        renderYaw = getYaw();
        lastRenderPitch = renderPitch;
        lastRenderYaw = renderYaw;
    }

    private static double getSwimmingY(Entity entity) {
        if (entity.getPose() == EntityPose.SWIMMING) {
            return entity.getY();
        }
        return entity.getY() - entity.getEyeHeight(EntityPose.SWIMMING) + entity.getEyeHeight(entity.getPose());
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

    public void setBodyPosition(double x, double y, double z) {
        this.bodyX = x;
        this.bodyY = y;
        this.bodyZ = z;
    }

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
    }

    @Override
    public float getHandSwingProgress(float tickDelta) {
        return SpiritualistClientController.client().player.getHandSwingProgress(tickDelta);
    }

    @Override
    public int getItemUseTimeLeft() {
        return SpiritualistClientController.client().player.getItemUseTimeLeft();
    }

    @Override
    public boolean isUsingItem() {
        return SpiritualistClientController.client().player.isUsingItem();
    }

    @Override
    public boolean isClimbing() {
        return false;
    }

    @Override
    public boolean isTouchingWater() {
        return false;
    }

    @Override
    public StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> effect) {
        return SpiritualistClientController.client().player.getStatusEffect(effect);
    }

    @Override
    public PistonBehavior getPistonBehavior() {
        return PistonBehavior.IGNORE;
    }

    @Override
    public boolean collidesWith(Entity other) {
        return false;
    }

    @Override
    public void setPose(EntityPose pose) {
        super.setPose(EntityPose.SWIMMING);
    }

    @Override
    public boolean shouldSlowDown() {
        return false;
    }

    @Override
    protected boolean updateWaterSubmersionState() {
        this.isSubmergedInWater = this.isSubmergedIn(FluidTags.WATER);
        return this.isSubmergedInWater;
    }

    @Override
    protected void onSwimmingStart() {
    }

    @Override
    public void tickMovement() {
        getAbilities().flying = true;
        getAbilities().setFlySpeed(getWatheWalkSpeed());

        super.tickMovement();

        getAbilities().flying = true;
        setOnGround(false);
        clampPosition();
    }

    /**
     * 让灵魂飞行速度尽量接近 spark 版的体感速度。
     *
     * <p>这里没有继续强依赖 wathe 的地图增强组件，
     * 是因为当前项目映射下客户端并不能稳定访问那套类。
     * 先改成固定常量可以保证灵术师功能编译和运行稳定，
     * 后续如果你确认了当前 wathe 暴露的正式接口，再接回动态速度也很容易。</p>
     */
    private float getWatheWalkSpeed() {
        return 0.07f * (0.09f / 0.4f);
    }

    /**
     * 灵魂活动范围沿用 spark 版的体验：
     * 既不会飞出地图边界，也不会离肉身太远。
     */
    private void clampPosition() {
        double newX = getX();
        double newY = getY();
        double newZ = getZ();
        boolean clamped = false;

        try {
            MapVariablesWorldComponent mapVariables = MapVariablesWorldComponent.KEY.get(SpiritualistClientController.client().world);
            Box playArea = mapVariables.getPlayArea();
            if (playArea != null) {
                double cx = MathHelper.clamp(newX, playArea.minX, playArea.maxX);
                double cy = MathHelper.clamp(newY, playArea.minY, playArea.maxY);
                double cz = MathHelper.clamp(newZ, playArea.minZ, playArea.maxZ);
                if (cx != newX || cy != newY || cz != newZ) {
                    newX = cx;
                    newY = cy;
                    newZ = cz;
                    clamped = true;
                }
            }
        } catch (Exception ignored) {
        }

        double dx = newX - this.bodyX;
        double dy = newY - this.bodyY;
        double dz = newZ - this.bodyZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > SpiritualistConstants.PROJECTION_MAX_RADIUS) {
            double scale = SpiritualistConstants.PROJECTION_MAX_RADIUS / distance;
            newX = this.bodyX + dx * scale;
            newY = this.bodyY + dy * scale;
            newZ = this.bodyZ + dz * scale;
            clamped = true;
        }

        if (clamped) {
            setPosition(newX, newY, newZ);
        }
    }

    /**
     * 只在灵魂相机自身这里绕过 wathe 的 jumpKey 封锁。
     *
     * <p>这样大厅 / 正常玩家 / 其他职业都不会受影响，
     * 只有灵术师出窍时的假玩家相机可以读取真实按键并向上飞。</p>
     */
    private boolean isProjectionJumpPhysicallyPressed() {
        InputUtil.Key jumpBoundKey =
                ((SpiritualistKeyBindingAccessor) SpiritualistClientController.client().options.jumpKey).noellesroles$getBoundKey();
        long windowHandle = SpiritualistClientController.client().getWindow().getHandle();

        if (jumpBoundKey.getCategory() == InputUtil.Type.KEYSYM || jumpBoundKey.getCategory() == InputUtil.Type.SCANCODE) {
            return InputUtil.isKeyPressed(windowHandle, jumpBoundKey.getCode());
        }
        if (jumpBoundKey.getCategory() == InputUtil.Type.MOUSE) {
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(windowHandle, jumpBoundKey.getCode()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        }
        return false;
    }
}
