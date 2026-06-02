package org.agmas.noellesroles.client.roles.spiritualist;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.packet.role.spiritualist.SpiritualistPossessionControlC2SPacket;
import org.agmas.noellesroles.packet.role.spiritualist.SpiritualistPossessionViewS2CPacket;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistConstants;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistHostComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 灵术师客户端总控。
 *
 * <p>它负责两类完全不同的本地表现：</p>
 * <p>1. 灵魂出窍时，启用自由灵体相机；</p>
 * <p>2. 灵魂附身时，把画面挂到宿主身上并持续向服务端上送操控输入。</p>
 */
public final class SpiritualistClientController {

    private static final MinecraftClient MC = MinecraftClient.getInstance();
    /**
     * 原版双击 W 触发疾跑的容错窗口。
     *
     * <p>灵术师附身时，本地玩家本体的正常移动输入会被我们截走，
     * 因此原版挂在“本地自己移动状态”上的双击 W 疾跑状态机不会再自然工作。
     * 这里用同样的短窗口在附身控制层手动补一份，保证体感和原版一致。</p>
     */
    private static final int POSSESSION_DOUBLE_TAP_SPRINT_WINDOW_TICKS = 7;

    @Nullable
    private static SpiritualProjectionCamera projectionCamera;
    private static boolean projectionActive = false;

    @Nullable
    private static SpiritualPossessionCamera possessionCamera;
    private static boolean possessionViewActive = false;
    private static boolean possessionUsingHostCamera = false;

    @Nullable
    private static Perspective rememberedPerspective;

    private static float capturedForward = 0.0f;
    private static float capturedSideways = 0.0f;
    private static boolean capturedJumping = false;
    private static boolean capturedSneaking = false;
    private static boolean capturedSprinting = false;
    private static float predictedForward = 0.0f;
    private static float predictedSideways = 0.0f;
    private static boolean predictedJumping = false;
    private static boolean predictedSneaking = false;
    private static boolean predictedSprinting = false;
    private static boolean lastForwardPressed = false;
    private static int doubleTapSprintTicksRemaining = 0;

    @Nullable
    private static UUID possessionViewTargetUuid;
    private static double possessionViewX = 0.0d;
    private static double possessionViewY = 0.0d;
    private static double possessionViewZ = 0.0d;
    private static double possessionViewVelocityX = 0.0d;
    private static double possessionViewVelocityY = 0.0d;
    private static double possessionViewVelocityZ = 0.0d;
    private static float possessionViewAuthoritativeYaw = 0.0f;
    private static float possessionViewAuthoritativePitch = 0.0f;
    private static float possessionViewAuthoritativeHeadYaw = 0.0f;
    private static float possessionViewAuthoritativeBodyYaw = 0.0f;
    private static String possessionViewAuthoritativePoseName = EntityPose.STANDING.name();
    private static float possessionViewAuthoritativeEyeHeight = 1.62f;
    private static boolean possessionViewAuthoritativeSprinting = false;
    private static boolean possessionViewAuthoritativeSneaking = false;
    private static boolean possessionViewAuthoritativeOnGround = false;
    private static boolean possessionViewStateReceived = false;

    /**
     * 附身视角的“目标朝向”缓存。
     *
     * <p>当前附身视角是直接挂在宿主这个远端玩家实体身上的，
     * 而远端实体的朝向会持续被服务端同步包覆盖。
     * 如果只在鼠标事件发生时改一次朝向，就会出现你测试时那种
     * “视角想转过去，但又被网络同步拉回来一点”的发涩感。</p>
     *
     * <p>因此这里额外缓存一份“灵术师最近一次真正想看的朝向”，
     * 然后在每个客户端 tick 里继续把它回写到宿主实体上，
     * 先把视觉上的转向迟滞尽量压低。</p>
     */
    private static float possessionLookYaw = 0.0f;
    private static float possessionLookPitch = 0.0f;
    private static boolean possessionLookInitialized = false;

    private SpiritualistClientController() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(SpiritualistClientController::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    public static MinecraftClient client() {
        return MC;
    }

    public static void reset() {
        disableProjection();
        disablePossessionView();
        capturedForward = 0.0f;
        capturedSideways = 0.0f;
        capturedJumping = false;
        capturedSneaking = false;
        capturedSprinting = false;
        predictedForward = 0.0f;
        predictedSideways = 0.0f;
        predictedJumping = false;
        predictedSneaking = false;
        predictedSprinting = false;
        lastForwardPressed = false;
        doubleTapSprintTicksRemaining = 0;
        possessionLookYaw = 0.0f;
        possessionLookPitch = 0.0f;
        possessionLookInitialized = false;
        possessionUsingHostCamera = false;
        clearPossessionViewState();
    }

    public static boolean isProjectionActive() {
        return projectionActive;
    }

    public static boolean isPossessionViewActive() {
        return possessionViewActive;
    }

    public static boolean isUsingHostPossessionCamera() {
        return possessionViewActive && possessionUsingHostCamera;
    }

    /**
     * 灵术师附身时，宿主实体在世界里仍然需要继续真实存在，
     * 否则其他玩家、碰撞、伤害与服务端判定都会出问题。
     *
     * <p>但对于灵术师本地第一人称画面来说，
     * 宿主整个人如果继续正常渲染，就会出现“镜头卡在宿主头里，看见整具身体”的问题。
     * 因此这里单独提供一个客户端判定，只在灵术师自己的附身视角中把宿主隐藏掉。</p>
     */
    public static boolean shouldHideEntityInPossessionView(@Nullable Entity entity) {
        if (!possessionViewActive || entity == null || MC.player == null) {
            return false;
        }

        if (!(entity instanceof PlayerEntity player)) {
            return false;
        }

        SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(MC.player);
        return component.isPossessing()
                && component.possessionTarget != null
                && component.possessionTarget.equals(player.getUuid());
    }

    @Nullable
    public static SpiritualProjectionCamera getProjectionCamera() {
        return projectionCamera;
    }

    @Nullable
    public static SpiritualPossessionCamera getPossessionCamera() {
        return possessionCamera;
    }

    @Nullable
    public static AbstractClientPlayerEntity getPossessionHost() {
        if (MC.player == null || MC.world == null) {
            return null;
        }

        SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(MC.player);
        if (!component.isPossessing() || component.possessionTarget == null) {
            return null;
        }

        PlayerEntity player = MC.world.getPlayerByUuid(component.possessionTarget);
        return player instanceof AbstractClientPlayerEntity clientPlayer ? clientPlayer : null;
    }

    public static boolean redirectDetachedLook(double cursorDeltaX, double cursorDeltaY) {
        if (projectionActive && projectionCamera != null) {
            projectionCamera.changeLookDirection(cursorDeltaX, cursorDeltaY);
            return true;
        }

        if (possessionViewActive) {
            if (possessionCamera != null) {
                possessionCamera.changeLookDirection(cursorDeltaX, cursorDeltaY);
                possessionLookYaw = possessionCamera.getYaw();
                possessionLookPitch = possessionCamera.getPitch();
                possessionLookInitialized = true;
                return true;
            }

            Entity cameraEntity = MC.getCameraEntity();
            if (cameraEntity != null) {
                double sensitivity = MC.options.getMouseSensitivity().getValue() * 0.6D + 0.2D;
                double multiplier = sensitivity * sensitivity * sensitivity * 8.0D;
                possessionLookYaw += (float) (cursorDeltaX * multiplier * 0.15D);
                possessionLookPitch += (float) (cursorDeltaY * multiplier * 0.15D);
                possessionLookPitch = Math.clamp(possessionLookPitch, -90.0f, 90.0f);
                possessionLookInitialized = true;
                return true;
            }
        }

        if (isLocalPlayerPossessed() && MC.player != null && matchesPossessionViewTarget(MC.player.getUuid())) {
            /*
             * 被附身者自己也会收到服务端的平滑视图状态，
             * 因此本地鼠标在被附身期间必须继续彻底失效，
             * 否则会和服务器推过来的朝向互相抢控制权。
             */
            return true;
        }

        return false;
    }

    public static void handlePossessionViewPacket(SpiritualistPossessionViewS2CPacket payload) {
        possessionViewTargetUuid = payload.targetPlayer();
        possessionViewX = payload.x();
        possessionViewY = payload.y();
        possessionViewZ = payload.z();
        possessionViewVelocityX = payload.velocityX();
        possessionViewVelocityY = payload.velocityY();
        possessionViewVelocityZ = payload.velocityZ();
        possessionViewAuthoritativeYaw = payload.yaw();
        possessionViewAuthoritativePitch = payload.pitch();
        possessionViewAuthoritativeHeadYaw = payload.headYaw();
        possessionViewAuthoritativeBodyYaw = payload.bodyYaw();
        possessionViewAuthoritativePoseName = payload.poseName();
        possessionViewAuthoritativeEyeHeight = payload.eyeHeight();
        possessionViewAuthoritativeSprinting = payload.sprinting();
        possessionViewAuthoritativeSneaking = payload.sneaking();
        possessionViewAuthoritativeOnGround = payload.onGround();
        possessionViewStateReceived = true;
    }

    /**
     * 右键交互走一个“立即上送”的快速通道。
     *
     * <p>原先只依赖每 tick 的附身控制包时，
     * 右键通常要等到下一帧控制状态送上去后，服务端才知道“这一刻开始在按 use”，
     * 于是开门、关门、使用物品都会比正常玩家慢半拍。</p>
     *
     * <p>这里在本地点击发生的当下立刻补发一次当前控制快照，
     * 但不再直接本地 swingHand。
     * 第一人称动画统一改为读取“宿主真实网络状态”，
     * 这样空按右键、普通交互、吃喝、举刀和蓄力就不会再互相串动画。</p>
     */
    public static void handleImmediatePossessionUseAttempt() {
        if (!possessionViewActive || MC.player == null) {
            return;
        }

        ClientPlayNetworking.send(createPossessionControlPacket(true, MC.options.attackKey.isPressed()));
    }

    /**
     * 在 KeyboardInput 计算完按键状态后调用。
     *
     * <p>这里先把“想控制宿主的输入”存下来，再把本地玩家本体输入清零，
     * 这样灵术师自己就不会在附身期间偷偷原地乱走。</p>
     */
    public static void capturePossessionMovement(Input input) {
        capturedForward = input.movementForward;
        capturedSideways = input.movementSideways;
        capturedJumping = input.jumping;
        /*
         * 潜行状态优先沿用 KeyboardInput 已经算好的结果，
         * 但也额外兜底一次真实按键状态。
         *
         * 这样可以避免某些本地状态在被我们清空输入后，
         * 后续又被别的逻辑读成“当前没有潜行”，导致灵术师明明按着 Shift，
         * 却始终发不出附身潜行指令。
         */
        capturedSneaking = input.sneaking || MC.options.sneakKey.isPressed();
        predictedForward = capturedForward;
        predictedSideways = capturedSideways;
        predictedJumping = capturedJumping;
        predictedSneaking = capturedSneaking;
        capturedSprinting = updatePossessionSprintPrediction(input, capturedSneaking);

        input.movementForward = 0.0f;
        input.movementSideways = 0.0f;
        input.jumping = false;
        input.sneaking = false;
        input.pressingForward = false;
        input.pressingBack = false;
        input.pressingLeft = false;
        input.pressingRight = false;
    }

    /**
     * 灵术师附身期间复刻一份“长按疾跑键 / 双击 W”两套疾跑触发方式。
     *
     * <p>这样被附身玩家既能像原版一样双击 W 起跑，
     * 也能继续使用单独的疾跑键，而不会只剩下其中一种。</p>
     */
    private static boolean updatePossessionSprintPrediction(@NotNull Input input, boolean sneaking) {
        boolean forwardPressed = input.pressingForward || input.movementForward > 0.0f;
        boolean sprintKeyPressed = MC.options.sprintKey.isPressed();
        boolean canSprint = forwardPressed && input.movementForward > 0.0f && !sneaking;

        if (doubleTapSprintTicksRemaining > 0) {
            doubleTapSprintTicksRemaining--;
        }

        if (sprintKeyPressed) {
            predictedSprinting = canSprint;
        } else if (!canSprint) {
            predictedSprinting = false;
        } else if (!predictedSprinting && forwardPressed && !lastForwardPressed) {
            if (doubleTapSprintTicksRemaining > 0) {
                predictedSprinting = true;
            }
            doubleTapSprintTicksRemaining = POSSESSION_DOUBLE_TAP_SPRINT_WINDOW_TICKS;
        }

        lastForwardPressed = forwardPressed;
        return predictedSprinting;
    }

    public static boolean shouldCapturePossessionMovement() {
        return MC.player != null && SpiritualistPlayerComponent.KEY.get(MC.player).isPossessing();
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            reset();
            return;
        }

        SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(client.player);
        if (component.isProjecting()) {
            enableProjection();
        } else {
            disableProjection();
        }

        if (component.isPossessing()) {
            enablePossessionView();
            refreshPossessionCameraPose();
            sendPossessionControlPacket();
        } else {
            disablePossessionView();
        }

        if (isLocalPlayerPossessed()) {
            refreshPossessedLocalPlayerView();
        } else if (!component.isPossessing()) {
            clearPossessionViewState();
        }

        if (projectionActive) {
            if (client.player.input instanceof KeyboardInput) {
                Input input = new Input();
                input.sneaking = client.player.input.sneaking;
                client.player.input = input;
            }
            client.gameRenderer.setRenderHand(false);
        }

        if ((projectionActive || possessionViewActive) && isThirdPerson(client.options.getPerspective())) {
            client.options.setPerspective(Perspective.FIRST_PERSON);
        }
    }

    private static void enableProjection() {
        if (projectionActive || MC.player == null || MC.world == null) {
            return;
        }

        rememberPerspectiveIfNeeded();

        MC.chunkCullingEnabled = false;
        MC.gameRenderer.setRenderHand(false);

        projectionCamera = new SpiritualProjectionCamera(SpiritualistConstants.PROJECTION_CAMERA_ENTITY_ID);
        projectionCamera.applyPosition(MC.player);

        SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(MC.player);
        projectionCamera.setBodyPosition(component.bodyAnchorX, component.bodyAnchorY, component.bodyAnchorZ);
        projectionCamera.spawn();
        MC.setCameraEntity(projectionCamera);
        projectionActive = true;
    }

    private static void disableProjection() {
        if (!projectionActive) {
            return;
        }

        if (MC.player != null && !possessionViewActive) {
            MC.setCameraEntity(MC.player);
        }
        MC.chunkCullingEnabled = true;
        MC.gameRenderer.setRenderHand(true);

        if (projectionCamera != null) {
            projectionCamera.despawn();
            projectionCamera.input = new Input();
            projectionCamera = null;
        }

        if (MC.player != null) {
            MC.player.input = new KeyboardInput(MC.options);
        }

        projectionActive = false;
        restorePerspectiveIfIdle();
    }

    private static void enablePossessionView() {
        if (possessionViewActive) {
            Entity preferredCamera = resolvePreferredPossessionCameraEntity();
            if (preferredCamera != null && MC.getCameraEntity() != preferredCamera) {
                MC.setCameraEntity(preferredCamera);
            }
            return;
        }

        if (MC.player == null || MC.world == null) {
            return;
        }

        AbstractClientPlayerEntity host = getPossessionHost();
        if (host == null && !hasPossessionViewStateForCurrentTarget()) {
            return;
        }

        rememberPerspectiveIfNeeded();
        possessionUsingHostCamera = host != null;
        possessionCamera = new SpiritualPossessionCamera(SpiritualistConstants.POSSESSION_CAMERA_ENTITY_ID);
        if (host != null) {
            /*
             * 即便真正的观看相机已经直接挂到宿主实体身上，
             * 这里也仍然保留一个“不可见的本地 ClientPlayerEntity 壳”。
             *
             * 原因是 HeldItemRenderer 的第一人称手臂渲染签名强依赖 ClientPlayerEntity，
             * 而多人客户端中的远端宿主只是 AbstractClientPlayerEntity。
             * 如果把宿主实体直接塞给第一人称手持渲染，运行时就会像本次崩溃那样发生 class_745 -> class_746 强转异常。
             *
             * 所以这层相机壳现在只负责：
             * 1. 给第一人称手臂/手持物提供合法的 ClientPlayerEntity 类型；
             * 2. 镜像宿主当前姿态、朝向和使用状态。
             *
             * 真正的世界视角位置、跳跃高度、声音监听点仍然继续以宿主实体为准。
             */
            possessionCamera.applyPosition(host);
        } else {
            possessionCamera.applyViewState(
                    possessionViewX,
                    possessionViewY,
                    possessionViewZ,
                    possessionViewVelocityX,
                    possessionViewVelocityY,
                    possessionViewVelocityZ,
                    possessionViewAuthoritativeYaw,
                    possessionViewAuthoritativePitch,
                    possessionViewAuthoritativeHeadYaw,
                    possessionViewAuthoritativeBodyYaw,
                    resolvePoseFromSerializedName(possessionViewAuthoritativePoseName),
                    possessionViewAuthoritativeEyeHeight,
                    possessionViewAuthoritativeSprinting,
                    possessionViewAuthoritativeSneaking,
                    possessionViewAuthoritativeOnGround
            );
        }
        possessionCamera.syncVisualStateFromLocalPlayer();
        possessionCamera.spawn();

        if (host != null) {
            rememberPossessionLook(host);
        } else {
            possessionLookYaw = possessionViewAuthoritativeYaw;
            possessionLookPitch = possessionViewAuthoritativePitch;
            possessionLookInitialized = true;
        }

        Entity preferredCamera = resolvePreferredPossessionCameraEntity();
        if (preferredCamera != null) {
            MC.setCameraEntity(preferredCamera);
        }
        MC.gameRenderer.setRenderHand(true);
        possessionViewActive = true;
    }

    private static void disablePossessionView() {
        if (!possessionViewActive) {
            return;
        }

        if (MC.player != null && !projectionActive) {
            MC.setCameraEntity(MC.player);
        }
        if (possessionCamera != null) {
            possessionCamera.despawn();
            possessionCamera = null;
        }
        possessionUsingHostCamera = false;
        possessionViewActive = false;
        predictedForward = 0.0f;
        predictedSideways = 0.0f;
        predictedJumping = false;
        predictedSneaking = false;
        possessionLookYaw = 0.0f;
        possessionLookPitch = 0.0f;
        possessionLookInitialized = false;
        capturedSprinting = false;
        predictedSprinting = false;
        lastForwardPressed = false;
        doubleTapSprintTicksRemaining = 0;
        restorePerspectiveIfIdle();
    }

    private static void sendPossessionControlPacket() {
        if (MC.player == null) {
            return;
        }

        ClientPlayNetworking.send(createPossessionControlPacket(
                MC.options.useKey.isPressed(),
                MC.options.attackKey.isPressed()
        ));

        capturedForward = 0.0f;
        capturedSideways = 0.0f;
        capturedJumping = false;
        capturedSneaking = false;
    }

    /**
     * 统一构建当前这一帧的附身控制快照。
     *
     * <p>这样正常的每 tick 同步和“点击时立即补发”的快速通道，
     * 都能共用同一套朝向与输入来源，避免两边数据不一致。</p>
     */
    private static SpiritualistPossessionControlC2SPacket createPossessionControlPacket(
            boolean using,
            boolean attacking
    ) {
        Entity cameraEntity = MC.getCameraEntity();
        float yaw = possessionLookInitialized
                ? possessionLookYaw
                : (cameraEntity != null ? cameraEntity.getYaw() : MC.player.getYaw());
        float pitch = possessionLookInitialized
                ? possessionLookPitch
                : (cameraEntity != null ? cameraEntity.getPitch() : MC.player.getPitch());

        return new SpiritualistPossessionControlC2SPacket(
                capturedForward,
                capturedSideways,
                yaw,
                pitch,
                predictedJumping,
                predictedSneaking,
                predictedSprinting,
                using,
                attacking
        );
    }

    /**
     * 记住宿主当前朝向，作为附身视角的初始目标角度。
     */
    private static void rememberPossessionLook(AbstractClientPlayerEntity host) {
        possessionLookYaw = host.getYaw();
        possessionLookPitch = host.getPitch();
        possessionLookInitialized = true;
    }

    /**
     * 每个客户端 tick 都更新一次本地附身相机。
     *
     * <p>这里不再直接把相机实体绑死在“远端宿主玩家实体”上，
     * 而是改成：</p>
     * <p>1. 位置使用服务端同步过来的宿主真值；</p>
     * <p>2. 朝向使用灵术师本地即时鼠标输入；</p>
     * <p>3. 第一人称手持渲染则交给本地附身相机自身。</p>
     *
     * <p>这样就能绕开“远端玩家朝向持续被网络包覆盖”带来的卡顿转头问题。</p>
     */
    private static void refreshPossessionCameraPose() {
        AbstractClientPlayerEntity host = getPossessionHost();
        if (host != null && !possessionUsingHostCamera) {
            possessionUsingHostCamera = true;
            if (possessionCamera != null) {
                possessionCamera.despawn();
                possessionCamera = null;
            }
            if (MC.getCameraEntity() != host) {
                MC.setCameraEntity(host);
            }
        } else if (host == null && possessionUsingHostCamera && possessionViewStateReceived) {
            possessionUsingHostCamera = false;
            if (possessionCamera == null) {
                possessionCamera = new SpiritualPossessionCamera(SpiritualistConstants.POSSESSION_CAMERA_ENTITY_ID);
                possessionCamera.spawn();
            }
            if (MC.getCameraEntity() != possessionCamera) {
                MC.setCameraEntity(possessionCamera);
            }
        }

        if (!possessionLookInitialized) {
            if (host != null) {
                rememberPossessionLook(host);
            } else if (possessionViewStateReceived) {
                possessionLookYaw = possessionViewAuthoritativeYaw;
                possessionLookPitch = possessionViewAuthoritativePitch;
                possessionLookInitialized = true;
            }
        }

        if (!hasPossessionViewStateForCurrentTarget() && host == null) {
            return;
        }

        if (possessionUsingHostCamera) {
            refreshHostPossessionCameraPose(host);
            return;
        }

        if (possessionCamera == null) {
            return;
        }

        /*
         * 宿主远端实体在灵术师客户端里本来就带着原版的网络平滑与动画推进，
         * 位置、跳跃抛物线、下蹲与低姿态切换通常都比我们自己用包直塞坐标更顺。
         *
         * 之前为了规避“远端朝向被服务端不断拉回”这个问题，
         * 我们把整套相机状态都改成优先读自定义包。
         * 但真正会导致转向卡顿的核心只有 yaw/pitch，
         * 位置与姿态反而更适合继续复用远端宿主实体现成的平滑结果。
         *
         * 所以现在改成：
         * 1. 朝向继续用灵术师本地预测；
         * 2. 位置/速度/姿态/是否着地优先读宿主远端实体；
         * 3. 只有宿主实体暂时不存在时，才退回自定义同步包。
         *
         * 这样能明显缓解附身视角里蹲下、起跳、落地这些上下位移的生硬感。
         */
        double viewX = host != null ? host.getX() : possessionViewX;
        double viewY = host != null ? host.getY() : possessionViewY;
        double viewZ = host != null ? host.getZ() : possessionViewZ;
        double velocityX = host != null ? host.getVelocity().x : possessionViewVelocityX;
        double velocityY = host != null ? host.getVelocity().y : possessionViewVelocityY;
        double velocityZ = host != null ? host.getVelocity().z : possessionViewVelocityZ;
        boolean onGround = host != null ? host.isOnGround() : possessionViewAuthoritativeOnGround;
        EntityPose pose = host != null ? host.getPose() : resolvePoseFromSerializedName(possessionViewAuthoritativePoseName);
        float eyeHeight = host != null ? host.getEyeHeight(pose) : possessionViewAuthoritativeEyeHeight;

        possessionCamera.syncVisualStateFromLocalPlayer();
        boolean displaySprinting = predictedSprinting || possessionViewAuthoritativeSprinting;
        boolean displaySneaking = predictedSneaking || possessionViewAuthoritativeSneaking;
        possessionCamera.applyViewState(
                viewX,
                viewY,
                viewZ,
                velocityX,
                velocityY,
                velocityZ,
                possessionLookYaw,
                possessionLookPitch,
                possessionLookYaw,
                possessionLookYaw,
                pose,
                eyeHeight,
                displaySprinting,
                displaySneaking,
                onGround
        );
    }

    private static void refreshHostPossessionCameraPose(@Nullable AbstractClientPlayerEntity host) {
        if (host == null) {
            return;
        }

        if (host instanceof ClientPlayerEntity clientHost) {
            clientHost.lastRenderYaw = clientHost.renderYaw;
            clientHost.lastRenderPitch = clientHost.renderPitch;
        }
        if (possessionLookInitialized) {
            host.setYaw(possessionLookYaw);
            host.setPitch(possessionLookPitch);
            host.setHeadYaw(possessionLookYaw);
            host.setBodyYaw(possessionLookYaw);
            if (host instanceof ClientPlayerEntity clientHost) {
                clientHost.renderYaw = possessionLookYaw;
                clientHost.renderPitch = possessionLookPitch;
            }
        }

        /*
         * 真正观看世界的是宿主实体，
         * 但第一人称手臂仍然走本地的 SpiritualPossessionCamera 壳实体。
         *
         * 如果不把这层壳实体的朝向也每 tick 同步到当前镜头朝向，
         * HeldItemRenderer 就会拿着一份“滞后的玩家朝向”去算手臂矩阵，
         * 结果就是你测试里看到的：一转视角，手臂和手持物整体向左右漂移，甚至甩出屏幕。
         *
         * 这里把壳实体朝向强制钉死到当前附身视角，
         * 让第一人称手臂坐标系与真实镜头保持完全一致。
         */
        if (possessionCamera != null) {
            possessionCamera.syncVisualStateFromLocalPlayer();
            possessionCamera.syncFacingFromCamera(possessionLookInitialized ? possessionLookYaw : host.getYaw(),
                    possessionLookInitialized ? possessionLookPitch : host.getPitch());
            possessionCamera.applyPosition(host);
            possessionCamera.syncFacingFromCamera(possessionLookInitialized ? possessionLookYaw : host.getYaw(),
                    possessionLookInitialized ? possessionLookPitch : host.getPitch());
        }

        if (MC.getCameraEntity() != host) {
            MC.setCameraEntity(host);
        }
    }

    /**
     * 被附身者自己的客户端也走本地平滑层，
     * 不再依赖 requestTeleport 每 tick 生硬拉视角。
     */
    private static void refreshPossessedLocalPlayerView() {
        ClientPlayerEntity localPlayer = MC.player;
        if (localPlayer == null || !matchesPossessionViewTarget(localPlayer.getUuid())) {
            return;
        }

        localPlayer.prevX = localPlayer.getX();
        localPlayer.prevY = localPlayer.getY();
        localPlayer.prevZ = localPlayer.getZ();
        localPlayer.lastRenderX = localPlayer.prevX;
        localPlayer.lastRenderY = localPlayer.prevY;
        localPlayer.lastRenderZ = localPlayer.prevZ;
        localPlayer.prevYaw = localPlayer.getYaw();
        localPlayer.prevPitch = localPlayer.getPitch();
        localPlayer.prevHeadYaw = localPlayer.getHeadYaw();
        localPlayer.prevBodyYaw = localPlayer.getBodyYaw();
        localPlayer.lastRenderYaw = localPlayer.renderYaw;
        localPlayer.lastRenderPitch = localPlayer.renderPitch;

        localPlayer.setPosition(possessionViewX, possessionViewY, possessionViewZ);
        localPlayer.setYaw(possessionViewAuthoritativeYaw);
        localPlayer.setPitch(possessionViewAuthoritativePitch);
        localPlayer.setHeadYaw(possessionViewAuthoritativeHeadYaw);
        localPlayer.setBodyYaw(possessionViewAuthoritativeBodyYaw);
        localPlayer.setSprinting(possessionViewAuthoritativeSprinting);
        localPlayer.setSneaking(possessionViewAuthoritativeSneaking);
        applyClientPossessionPose(localPlayer, resolvePoseFromSerializedName(possessionViewAuthoritativePoseName));
        localPlayer.setOnGround(possessionViewAuthoritativeOnGround);
        /*
         * 被附身者本地视角此前每 tick 都被硬清零速度，
         * 会让跳跃 / 下落看起来像单纯的坐标瞬移。
         * 改成沿用宿主真实服务端速度后，镜头起伏和落地感会更接近原版。
         */
        localPlayer.setVelocity(possessionViewVelocityX, possessionViewVelocityY, possessionViewVelocityZ);
        localPlayer.renderYaw = possessionViewAuthoritativeYaw;
        localPlayer.renderPitch = possessionViewAuthoritativePitch;
    }

    /**
     * 附身平滑层里我们直接手动改玩家姿态标记，
     * 但原版的蹲下视角高度与碰撞箱更新还依赖 pose 本身切换。
     *
     * <p>这里直接同步服务端裁定的真实 pose，
     * 这样潜行、一格高爬行、木板门压低、游泳等所有低姿态都会原样落到客户端视图层里。</p>
     */
    private static void applyClientPossessionPose(@Nullable ClientPlayerEntity player, @Nullable EntityPose pose) {
        if (player == null) {
            return;
        }

        EntityPose targetPose = pose == null ? EntityPose.STANDING : pose;
        if (player.getPose() == targetPose) {
            return;
        }

        player.setPose(targetPose);
        player.calculateDimensions();
    }

    /**
     * 自定义附身视图包里的姿态字段使用字符串序列化。
     *
     * <p>比直接传枚举 ordinal 更稳妥：
     * 即便未来 Mojang 调整 EntityPose 的声明顺序，旧包也不会被误读成别的姿态。</p>
     */
    private static @NotNull EntityPose resolvePoseFromSerializedName(@Nullable String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return EntityPose.STANDING;
        }
        try {
            return EntityPose.valueOf(rawName);
        } catch (IllegalArgumentException ignored) {
            return EntityPose.STANDING;
        }
    }

    private static boolean isLocalPlayerPossessed() {
        return MC.player != null && SpiritualistHostComponent.KEY.get(MC.player).possessed;
    }

    private static boolean hasPossessionViewStateForCurrentTarget() {
        if (MC.player == null) {
            return false;
        }

        SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(MC.player);
        return component.possessionTarget != null && matchesPossessionViewTarget(component.possessionTarget);
    }

    private static boolean matchesPossessionViewTarget(@Nullable UUID expectedTarget) {
        return possessionViewStateReceived
                && possessionViewTargetUuid != null
                && expectedTarget != null
                && possessionViewTargetUuid.equals(expectedTarget);
    }

    private static void clearPossessionViewState() {
        possessionViewTargetUuid = null;
        possessionViewX = 0.0d;
        possessionViewY = 0.0d;
        possessionViewZ = 0.0d;
        possessionViewVelocityX = 0.0d;
        possessionViewVelocityY = 0.0d;
        possessionViewVelocityZ = 0.0d;
        possessionViewAuthoritativeYaw = 0.0f;
        possessionViewAuthoritativePitch = 0.0f;
        possessionViewAuthoritativeHeadYaw = 0.0f;
        possessionViewAuthoritativeBodyYaw = 0.0f;
        possessionViewAuthoritativePoseName = EntityPose.STANDING.name();
        possessionViewAuthoritativeEyeHeight = 1.62f;
        possessionViewAuthoritativeSprinting = false;
        possessionViewAuthoritativeSneaking = false;
        possessionViewAuthoritativeOnGround = false;
        possessionViewStateReceived = false;
    }

    private static void rememberPerspectiveIfNeeded() {
        if (rememberedPerspective == null) {
            rememberedPerspective = MC.options.getPerspective();
        }
        if (isThirdPerson(MC.options.getPerspective())) {
            MC.options.setPerspective(Perspective.FIRST_PERSON);
        }
    }

    /**
     * 当前映射下 Perspective 没有 isThirdPerson() 便捷方法，
     * 因此直接按枚举值判断是否处于任意第三人称模式。
     */
    private static boolean isThirdPerson(Perspective perspective) {
        return perspective == Perspective.THIRD_PERSON_BACK || perspective == Perspective.THIRD_PERSON_FRONT;
    }

    private static void restorePerspectiveIfIdle() {
        if ((projectionActive || possessionViewActive) || rememberedPerspective == null) {
            return;
        }
        MC.options.setPerspective(rememberedPerspective);
        rememberedPerspective = null;
    }

    @Nullable
    private static Entity resolvePreferredPossessionCameraEntity() {
        AbstractClientPlayerEntity host = getPossessionHost();
        if (host != null) {
            possessionUsingHostCamera = true;
            return host;
        }
        possessionUsingHostCamera = false;
        return possessionCamera;
    }
}
