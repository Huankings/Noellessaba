package org.agmas.noellesroles.client.renderer;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.ArmorEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.client.ui.common.PlayerHeadTextureHelper;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 魔术师播放体渲染器。
 *
 * <p>这里让播放体尽量表现成“真正的玩家外观”：</p>
 * <p>1. 使用原版玩家模型骨架，复用玩家的潜行、手持、朝向姿态；</p>
 * <p>2. 根据目标皮肤动态切换 classic / slim 模型；</p>
 * <p>3. 通过手持物特性层把录制时复制出的临时武器状态可视化出来。</p>
 *
 * <p>护甲、头部装备、披风与鞘翅都通过标准 feature 阶段渲染，
 * 同时让 EMF/ETF 能继续在原版模型层和纹理入口上完成兼容处理。</p>
 */
public class MagicianPlaybackEntityRenderer extends LivingEntityRenderer<MagicianPlaybackEntity, PlayerEntityModel<MagicianPlaybackEntity>> {

    /** 原版 PlayerEntityRenderer 使用的玩家模型缩放。 */
    private static final float VISUAL_SCALE = 0.9375F;
    /**
     * EMF 玩家 JEM 以原版玩家 renderer 的两模型像素基线为前提；皮套走自定义 LivingEntity
     * renderer 时缺少这段玩家专属基线，站立脚底会下沉。两模型像素等于 2 / 16 方块。
     */
    private static final float EMF_PLAYER_STANDING_ALIGNMENT_OFFSET = 2.0F / 16.0F;
    /**
     * 原版 PlayerEntityRenderer 在完整蹲姿首帧施加的向下位置补偿。
     * 它与 JEM/原版玩家模型把双腿即时上移两模型像素的动作相互抵消，保证脚底不跳动。
     */
    private static final float PLAYER_SNEAKING_POSITION_OFFSET = -2.0F / 16.0F;
    /** 没有玩家专属鞘翅或披风纹理时使用的原版鞘翅纹理。 */
    private static final Identifier DEFAULT_ELYTRA_TEXTURE = Identifier.ofVanilla("textures/entity/elytra.png");
    /** 皮肤/披风缓存暂未就绪或没有返回披风时，允许再次查询的最短间隔。 */
    private static final long FETCH_RETRY_INTERVAL_MS = 30_000L;
    private static final Map<UUID, CompletableFuture<SkinTextures>> FETCHED_SKIN_TEXTURE_CACHE = new ConcurrentHashMap<>();
    /** 记录每个目标下一次可重试时间，避免暂时缺失的 CSL 离线皮肤结果被永久缓存。 */
    private static final Map<UUID, Long> FETCH_RETRY_AFTER = new ConcurrentHashMap<>();
    /** EMF 通过 Mixin 注入到 PlayerEntityModel 上的独立 player_cape.jem 根部件字段名。 */
    private static final String EMF_CAPE_MODEL_FIELD_NAME = "emf$capeModelPart";
    /** EMF 注入到 EntityModel 的公开自检方法名，用于区分真正的 JEM 模型与原版模型。 */
    private static final String EMF_MODEL_CHECK_METHOD_NAME = "emf$isEMFModel";
    /** 按模型运行时类型缓存 EMF 可选字段，避免每帧重复反射搜索。 */
    private static final Map<Class<?>, Optional<Field>> EMF_CAPE_MODEL_FIELDS = new ConcurrentHashMap<>();
    /** 按模型运行时类型缓存 EMF 自检方法；未安装 EMF 时缓存空结果。 */
    private static final Map<Class<?>, Optional<Method>> EMF_MODEL_CHECK_METHODS = new ConcurrentHashMap<>();
    /** SkinLayers3D 可选 feature 的类名；以反射接入，未安装时不产生类链接依赖。 */
    private static final String SKIN_LAYERS_FEATURE_CLASS = "dev.tr7zw.skinlayers.renderlayers.CustomLayerFeatureRenderer";
    /** SkinLayers3D 生成玩家皮肤 3D 网格的核心工具类。 */
    private static final String SKIN_LAYERS_UTIL_CLASS = "dev.tr7zw.skinlayers.SkinUtil";

    private final PlayerEntityModel<MagicianPlaybackEntity> classicModel;
    private final PlayerEntityModel<MagicianPlaybackEntity> slimModel;
    /** 通过原版 ELYTRA 模型层创建，安装 EMF 时仍会经过其模型层替换入口。 */
    private final MagicianPlaybackElytraModel elytraModel;
    /** 独立于资源包替换模型的原版披风兜底几何体。 */
    private final ModelPart vanillaCapePart;
    /** SkinLayers3D 为 classic 与 slim 模型分别创建的原生 feature，未安装模组时为空。 */
    @Nullable private final Object classicSkinLayers3dFeature;
    @Nullable private final Object slimSkinLayers3dFeature;
    /** SkinLayers3D 创建皮肤网格及绘制网格的入口，仅在模组存在时可用。 */
    @Nullable private final Method skinLayers3dSetupMethod;
    @Nullable private final Method skinLayers3dRenderLayersMethod;
    /** 反射调用失败后只输出一次诊断，避免渲染帧持续刷屏。 */
    private boolean skinLayers3dFailureLogged;

    public MagicianPlaybackEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new MagicianPlaybackPlayerEntityModel(context.getPart(EntityModelLayers.PLAYER), false), 0.5F);
        this.classicModel = this.getModel();
        this.slimModel = new MagicianPlaybackPlayerEntityModel(context.getPart(EntityModelLayers.PLAYER_SLIM), true);
        this.elytraModel = new MagicianPlaybackElytraModel(context.getPart(EntityModelLayers.ELYTRA));
        this.vanillaCapePart = createVanillaCapePart();

        /*
         * 当前 renderer 会根据伪装皮肤在 classic/slim 主模型之间动态切换，而原版玩家为两种皮肤
         * 分别创建 renderer。这里准备两套护甲模型，再由包装 feature 在每帧选择匹配的一套，
         * 避免细手臂皮肤穿上护甲后恢复成四像素宽手臂。
         */
        ArmorFeatureRenderer<MagicianPlaybackEntity, PlayerEntityModel<MagicianPlaybackEntity>, ArmorEntityModel<MagicianPlaybackEntity>> classicArmorFeature =
                new ArmorFeatureRenderer<>(
                        this,
                        new ArmorEntityModel<>(context.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                        new ArmorEntityModel<>(context.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                        context.getModelManager()
                );
        ArmorFeatureRenderer<MagicianPlaybackEntity, PlayerEntityModel<MagicianPlaybackEntity>, ArmorEntityModel<MagicianPlaybackEntity>> slimArmorFeature =
                new ArmorFeatureRenderer<>(
                        this,
                        new ArmorEntityModel<>(context.getPart(EntityModelLayers.PLAYER_SLIM_INNER_ARMOR)),
                        new ArmorEntityModel<>(context.getPart(EntityModelLayers.PLAYER_SLIM_OUTER_ARMOR)),
                        context.getModelManager()
                );
        this.addFeature(new MagicianPlaybackArmorFeatureRenderer(classicArmorFeature, slimArmorFeature));
        this.addFeature(new HeldItemFeatureRenderer<>(this, context.getHeldItemRenderer()));
        this.addFeature(new MagicianPlaybackCapeFeatureRenderer());
        this.addFeature(new HeadFeatureRenderer<>(this, context.getModelLoader(), context.getHeldItemRenderer()));
        this.addFeature(new MagicianPlaybackElytraFeatureRenderer());
        // SkinLayers3D 的 feature 会在构造时读取当前模型的 slim 标记，因此两种模型分别建一份。
        this.model = this.classicModel;
        Object[] classicSkinLayersFeature = createSkinLayers3dFeature(this);
        this.model = this.slimModel;
        Object[] slimSkinLayersFeature = createSkinLayers3dFeature(this);
        this.model = this.classicModel;
        this.classicSkinLayers3dFeature = classicSkinLayersFeature == null ? null : classicSkinLayersFeature[0];
        this.slimSkinLayers3dFeature = slimSkinLayersFeature == null ? null : slimSkinLayersFeature[0];
        this.skinLayers3dRenderLayersMethod = classicSkinLayersFeature == null ? null : (Method) classicSkinLayersFeature[1];
        this.skinLayers3dSetupMethod = resolveSkinLayers3dSetupMethod(this.getClass().getClassLoader());
        if (this.classicSkinLayers3dFeature != null && this.slimSkinLayers3dFeature != null
                && this.skinLayers3dRenderLayersMethod != null && this.skinLayers3dSetupMethod != null) {
            this.addFeature(new MagicianPlaybackSkinLayers3dFeatureRenderer());
        }
    }

    @Override
    public void render(
            MagicianPlaybackEntity entity,
            float entityYaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        this.model = this.resolvePlaybackModel(entity);
        this.setModelPose(entity);
        super.render(entity, entityYaw, tickDelta, matrices, vertexConsumers, light);
    }

    /**
     * 把玩家的 0.9375 缩放放回 LivingEntityRenderer 的标准 scale 阶段。
     * 这样缩放与原版玩家拥有完全相同的矩阵顺序，不会把姿态位置补偿一起缩放。
     */
    @Override
    protected void scale(MagicianPlaybackEntity entity, MatrixStack matrices, float tickDelta) {
        matrices.scale(VISUAL_SCALE, VISUAL_SCALE, VISUAL_SCALE);
    }

    /**
     * 补齐原版玩家 renderer 的脚底位置语义。
     *
     * <p>FA+Player 的 {@code sneak2} 会在蹲姿首帧把腿部上移两模型像素；真实玩家 renderer
     * 同一帧会把整个玩家向下移动 {@code 2 / 16} 方块，所以脚底保持不动。皮套此前只有前半段，
     * 因而表现为按下蹲键立即抬高。这里仅对真正被 EMF/JEM 替换的玩家模型补齐这对位移：
     * 站立基线先上移两像素消除当前下沉，蹲下时再减去同样距离抵消腿部上移。</p>
     */
    @Override
    public Vec3d getPositionOffset(MagicianPlaybackEntity entity, float tickDelta) {
        Vec3d originalOffset = super.getPositionOffset(entity, tickDelta);
        PlayerEntityModel<MagicianPlaybackEntity> playbackModel = this.resolvePlaybackModel(entity);
        if (!usesEmfCustomModel(playbackModel)) {
            return originalOffset;
        }

        float sneakingOffset = entity.isInSneakingPose() ? PLAYER_SNEAKING_POSITION_OFFSET : 0.0F;
        double verticalOffset = entity.getScale()
                * (EMF_PLAYER_STANDING_ALIGNMENT_OFFSET + sneakingOffset);
        return originalOffset.add(0.0D, verticalOffset, 0.0D);
    }

    /** 根据伪装皮肤的粗细手臂类型返回本帧应使用的玩家模型。 */
    private PlayerEntityModel<MagicianPlaybackEntity> resolvePlaybackModel(MagicianPlaybackEntity entity) {
        return resolveSkinTextures(entity).model() == SkinTextures.Model.SLIM ? this.slimModel : this.classicModel;
    }

    /**
     * 在 classic/slim 两套原版护甲 feature 之间选择当前皮肤对应的一套。
     *
     * <p>内部仍然调用原版 {@link ArmorFeatureRenderer}，因此染色、附魔光效、纹饰以及
     * EMF 对护甲 feature/模型层的兼容都不会被重新实现代码绕开。</p>
     */
    private final class MagicianPlaybackArmorFeatureRenderer
            extends FeatureRenderer<MagicianPlaybackEntity, PlayerEntityModel<MagicianPlaybackEntity>> {
        private final ArmorFeatureRenderer<MagicianPlaybackEntity, PlayerEntityModel<MagicianPlaybackEntity>, ArmorEntityModel<MagicianPlaybackEntity>> classicArmorFeature;
        private final ArmorFeatureRenderer<MagicianPlaybackEntity, PlayerEntityModel<MagicianPlaybackEntity>, ArmorEntityModel<MagicianPlaybackEntity>> slimArmorFeature;

        private MagicianPlaybackArmorFeatureRenderer(
                ArmorFeatureRenderer<MagicianPlaybackEntity, PlayerEntityModel<MagicianPlaybackEntity>, ArmorEntityModel<MagicianPlaybackEntity>> classicArmorFeature,
                ArmorFeatureRenderer<MagicianPlaybackEntity, PlayerEntityModel<MagicianPlaybackEntity>, ArmorEntityModel<MagicianPlaybackEntity>> slimArmorFeature
        ) {
            super(MagicianPlaybackEntityRenderer.this);
            this.classicArmorFeature = classicArmorFeature;
            this.slimArmorFeature = slimArmorFeature;
        }

        @Override
        public void render(
                MatrixStack matrices,
                VertexConsumerProvider vertexConsumers,
                int light,
                MagicianPlaybackEntity entity,
                float limbAngle,
                float limbDistance,
                float tickDelta,
                float animationProgress,
                float headYaw,
                float headPitch
        ) {
            ArmorFeatureRenderer<MagicianPlaybackEntity, PlayerEntityModel<MagicianPlaybackEntity>, ArmorEntityModel<MagicianPlaybackEntity>> feature =
                    MagicianPlaybackEntityRenderer.this.model == MagicianPlaybackEntityRenderer.this.slimModel
                            ? this.slimArmorFeature
                            : this.classicArmorFeature;
            feature.render(
                    matrices,
                    vertexConsumers,
                    light,
                    entity,
                    limbAngle,
                    limbDistance,
                    tickDelta,
                    animationProgress,
                    headYaw,
                    headPitch
            );
        }
    }

    /**
     * 复用 SkinLayers3D 自己的网格生成和部件绘制逻辑：目标玩家提供网格及皮肤可见部位，
     * 当前皮套模型提供已经按录制姿态更新过的身体部件变换。这里不走其完整 render()，
     * 因为该入口的距离 LOD 测量的是伪装玩家当前位置，而不是回放体的录制位置。
     */
    private void renderSkinLayers3d(
            @NotNull MagicianPlaybackEntity entity,
            @NotNull MatrixStack matrices,
            @NotNull VertexConsumerProvider vertexConsumers,
            int light
    ) {
        if (this.skinLayers3dSetupMethod == null || this.skinLayers3dRenderLayersMethod == null || entity.isInvisible()) {
            return;
        }
        UUID disguiseUuid = entity.getDisguisePlayerUuid();
        AbstractClientPlayerEntity disguisePlayer = disguiseUuid == null
                ? null
                : resolveOnlinePlayer(MinecraftClient.getInstance(), disguiseUuid);
        if (disguisePlayer == null) {
            return;
        }

        try {
            boolean slim = this.model == this.slimModel;
            Object feature = slim ? this.slimSkinLayers3dFeature : this.classicSkinLayers3dFeature;
            if (feature == null) {
                return;
            }

            // 让模组按在线目标当前皮肤和模型类型生成/复用网格；PlayerSettings 由模组 mixin 提供。
            // 这是静态方法，invoke 的第一个参数必须是 null；之后依次传入实体、PlayerSettings 实体和 slim 标记。
            Object setupResult = this.skinLayers3dSetupMethod.invoke(null, disguisePlayer, disguisePlayer, slim);
            if (!(setupResult instanceof Boolean ready) || !ready) {
                return;
            }

            // 与 SkinLayers3D 原生 feature 一样使用目标玩家皮肤的带透明通道渲染层。
            RenderLayer layer = RenderLayer.getEntityTranslucent(resolveSkinTextures(entity).texture(), true);
            VertexConsumer consumer = vertexConsumers.getBuffer(layer);
            int overlay = LivingEntityRenderer.getOverlay(entity, 0.0F);
            this.skinLayers3dRenderLayersMethod.invoke(
                    feature,
                    disguisePlayer,
                    disguisePlayer,
                    matrices,
                    consumer,
                    light,
                    overlay
            );
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException exception) {
            if (!this.skinLayers3dFailureLogged) {
                this.skinLayers3dFailureLogged = true;
                NoellesRolesCore.LOGGER.warn("[MagicianSkinLayers3D] Could not render target skin layers; keeping vanilla skin layers", exception);
            }
        }
    }

    /**
     * 在原版实体 feature 阶段绘制 3D 外层，确保实体根变换与播放器模型的局部骨骼变换仍在矩阵栈中。
     * 这样网格与皮套使用同一帧、同一位置和朝向，不会额外引入位置平滑或 tick 延迟。
     */
    private final class MagicianPlaybackSkinLayers3dFeatureRenderer
            extends FeatureRenderer<MagicianPlaybackEntity, PlayerEntityModel<MagicianPlaybackEntity>> {
        private MagicianPlaybackSkinLayers3dFeatureRenderer() {
            super(MagicianPlaybackEntityRenderer.this);
        }

        @Override
        public void render(
                MatrixStack matrices,
                VertexConsumerProvider vertexConsumers,
                int light,
                MagicianPlaybackEntity entity,
                float limbAngle,
                float limbDistance,
                float tickDelta,
                float animationProgress,
                float headYaw,
                float headPitch
        ) {
            MagicianPlaybackEntityRenderer.this.renderSkinLayers3d(entity, matrices, vertexConsumers, light);
        }
    }

    /** 通过模组原生 feature 构造器初始化可选兼容，不安装模组时不会触发类加载。 */
    private static @Nullable Object[] createSkinLayers3dFeature(@NotNull Object renderer) {
        try {
            Class<?> featureClass = Class.forName(SKIN_LAYERS_FEATURE_CLASS, false, renderer.getClass().getClassLoader());
            java.lang.reflect.Constructor<?> constructor = featureClass.getConstructors()[0];
            Object feature = constructor.newInstance(renderer);
            Method renderLayersMethod = null;
            for (Method method : featureClass.getMethods()) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (method.getName().equals("renderLayers")
                        && parameterTypes.length == 6
                        && PlayerEntity.class.isAssignableFrom(parameterTypes[0])
                        && parameterTypes[2] == MatrixStack.class
                        && VertexConsumer.class.isAssignableFrom(parameterTypes[3])
                        && parameterTypes[4] == int.class
                        && parameterTypes[5] == int.class) {
                    renderLayersMethod = method;
                    break;
                }
            }
            if (renderLayersMethod == null) {
                NoellesRolesCore.LOGGER.warn("[MagicianSkinLayers3D] SkinLayers3D was found, but its render entry point was not recognized");
                return null;
            }
            return new Object[]{feature, renderLayersMethod};
        } catch (ClassNotFoundException exception) {
            return null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            NoellesRolesCore.LOGGER.warn("[MagicianSkinLayers3D] SkinLayers3D was found, but optional integration could not initialize", exception);
            return null;
        }
    }

    /** 通过反射解析 SkinLayers3D 生成 per-player 3D 网格的入口。 */
    private static @Nullable Method resolveSkinLayers3dSetupMethod(@NotNull ClassLoader classLoader) {
        try {
            Class<?> skinUtilClass = Class.forName(SKIN_LAYERS_UTIL_CLASS, false, classLoader);
            for (Method method : skinUtilClass.getMethods()) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (method.getName().equals("setup3dLayers")
                        && parameterTypes.length == 3
                        && PlayerEntity.class.isAssignableFrom(parameterTypes[0])
                        && parameterTypes[2] == boolean.class) {
                    return method;
                }
            }
            NoellesRolesCore.LOGGER.warn("[MagicianSkinLayers3D] SkinLayers3D was found, but its mesh setup entry point was not recognized");
        } catch (ClassNotFoundException exception) {
            // optional dependency absent; vanilla skin layers remain active.
        }
        return null;
    }

    @Override
    public @NotNull Identifier getTexture(MagicianPlaybackEntity entity) {
        return resolveSkinTextures(entity).texture();
    }

    @Override
    protected boolean hasLabel(MagicianPlaybackEntity entity) {
        /*
         * Wathe 的玩家名显示已经被 RoleNameRenderer 接管。
         *
         * 这里必须在 renderer 层也关掉原版 label；只在实体类里返回
         * shouldRenderName=false 仍可能让 LivingEntityRenderer 回退显示实体类型名，
         * 于是客户端靠近时会看到 entity.noellesroles:magician_playback 这种裸 key。
         */
        return false;
    }

    /**
     * 自定义 LivingEntity renderer 不会自动执行 PlayerEntityRenderer 的游泳与滑翔根变换，
     * 因此在这里按 1.21.1 原版公式补齐。录制速度用于还原滑翔时身体朝移动方向的横向旋转。
     */
    @Override
    protected void setupTransforms(
            MagicianPlaybackEntity entity,
            MatrixStack matrices,
            float animationProgress,
            float bodyYaw,
            float tickDelta,
            float scale
    ) {
        float leaningPitch = entity.getLeaningPitch(tickDelta);
        float entityPitch = entity.getPitch(tickDelta);

        float fallFlyingTransition = entity.getReplayFallFlyingTransition(tickDelta);
        if (fallFlyingTransition > 0.0F) {
            super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta, scale);
            if (!entity.isUsingRiptide()) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                        fallFlyingTransition * (-90.0F - entityPitch)
                ));
            }

            Vec3d look = entity.getRotationVec(tickDelta);
            Vec3d velocity = entity.getVelocity();
            double velocityHorizontalSquared = velocity.horizontalLengthSquared();
            double lookHorizontalSquared = look.horizontalLengthSquared();
            if (velocityHorizontalSquared > 0.0D && lookHorizontalSquared > 0.0D) {
                double alignment = (velocity.x * look.x + velocity.z * look.z)
                        / Math.sqrt(velocityHorizontalSquared * lookHorizontalSquared);
                alignment = MathHelper.clamp(alignment, -1.0D, 1.0D);
                double cross = velocity.x * look.z - velocity.z * look.x;
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(
                        fallFlyingTransition * (float) (Math.signum(cross) * Math.acos(alignment))
                ));
            }
            return;
        }

        if (leaningPitch > 0.0F) {
            super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta, scale);
            float targetPitch = entity.isReplayTouchingWater() ? -90.0F - entityPitch : -90.0F;
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(leaningPitch, 0.0F, targetPitch)));
            if (entity.isInSwimmingPose()) {
                matrices.translate(0.0F, -1.0F, 0.3F);
            }
            return;
        }

        super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta, scale);
    }

    private void setModelPose(@NotNull MagicianPlaybackEntity entity) {
        /*
         * 原版 PlayerEntityRenderer.render() 会先调用 setModelPose，
         * 负责把“正在蓄力、举盾、拿弓、拿三叉戟/匕首、持物”等状态翻译成玩家手臂姿势。
         * 魔术师皮套不是 AbstractClientPlayerEntity，所以这里手动补同一套关键逻辑。
         */
        PlayerEntityModel<MagicianPlaybackEntity> model = this.getModel();
        model.setVisible(true);
        if (usesEmfCustomModel(model)) {
            /*
             * FA+Player 依赖原版完整蹲姿的精确骨骼值识别 var.sneak2，并用自己的
             * var.Asneak + frame_time*12 做平滑。EMF JEM 模型必须保留原版布尔输入，
             * 否则 sneak2 会在 Noelles 插值结束时才突然从 0 跳到 1。
             */
            model.sneaking = entity.isInSneakingPose();
        } else {
            /*
             * 原版模型没有资源包动画变量，改由模型末端读取 Noelles 连续进度并混合骨骼。
             */
            model.sneaking = false;
        }

        BipedEntityModel.ArmPose mainArmPose = getReplayArmPose(entity, Hand.MAIN_HAND);
        BipedEntityModel.ArmPose offArmPose = getReplayArmPose(entity, Hand.OFF_HAND);
        if (mainArmPose.isTwoHanded()) {
            offArmPose = entity.getOffHandStack().isEmpty()
                    ? BipedEntityModel.ArmPose.EMPTY
                    : BipedEntityModel.ArmPose.ITEM;
        }

        if (entity.getMainArm() == Arm.RIGHT) {
            model.rightArmPose = mainArmPose;
            model.leftArmPose = offArmPose;
        } else {
            model.rightArmPose = offArmPose;
            model.leftArmPose = mainArmPose;
        }
    }

    private static @NotNull BipedEntityModel.ArmPose getReplayArmPose(@NotNull MagicianPlaybackEntity entity, @NotNull Hand hand) {
        ItemStack stack = entity.getStackInHand(hand);
        if (stack.isEmpty()) {
            return BipedEntityModel.ArmPose.EMPTY;
        }

        if (entity.isReplayUsingItem() && entity.getReplayActiveHand() == hand) {
            UseAction useAction = stack.getUseAction();
            if (useAction == UseAction.BLOCK) {
                return BipedEntityModel.ArmPose.BLOCK;
            }
            if (useAction == UseAction.BOW) {
                return BipedEntityModel.ArmPose.BOW_AND_ARROW;
            }
            if (useAction == UseAction.SPEAR) {
                return BipedEntityModel.ArmPose.THROW_SPEAR;
            }
            if (useAction == UseAction.CROSSBOW) {
                return BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
            }
            if (useAction == UseAction.SPYGLASS) {
                return BipedEntityModel.ArmPose.SPYGLASS;
            }
            if (useAction == UseAction.TOOT_HORN) {
                return BipedEntityModel.ArmPose.TOOT_HORN;
            }
            if (useAction == UseAction.BRUSH) {
                return BipedEntityModel.ArmPose.BRUSH;
            }
        }

        if (!entity.handSwinging && stack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
            return BipedEntityModel.ArmPose.CROSSBOW_HOLD;
        }

        /*
         * noellesroles 自己给真实玩家渲染狙击枪时，会通过 PlayerEntityRenderer mixin
         * 改成 CROSSBOW_CHARGE。皮套不走 PlayerEntityRenderer，所以这里也补一份。
         */
        if (stack.isOf(ModItems.SNIPER_RIFLE)) {
            return BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
        }

        return BipedEntityModel.ArmPose.ITEM;
    }

    private @NotNull SkinTextures resolveSkinTextures(@NotNull MagicianPlaybackEntity entity) {
        UUID disguiseUuid = entity.getDisguisePlayerUuid();
        if (disguiseUuid == null) {
            return PlayerHeadTextureHelper.resolveStableSkinTextures(entity.getUuid(), null);
        }

        @Nullable PlayerListEntry playerListEntry = null;
        if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.networkHandler != null) {
            playerListEntry = MinecraftClient.getInstance().player.networkHandler.getPlayerListEntry(disguiseUuid);
        }
        SkinTextures stableTextures = PlayerHeadTextureHelper.resolveStableSkinTextures(disguiseUuid, playerListEntry);
        SkinTextures onlineTextures = resolveOnlinePlayerSkinTextures(disguiseUuid);
        if (stableTextures.capeTexture() == null) {
            SkinTextures fetchedTextures = resolveFetchedSkinTextures(disguiseUuid, entity.getDisguisePlayerName(), playerListEntry);
            if (fetchedTextures != null && fetchedTextures.capeTexture() != null) {
                return withCapeTextures(stableTextures, fetchedTextures);
            }
        }
        if (stableTextures.capeTexture() == null && onlineTextures != null && onlineTextures.capeTexture() != null) {
            /*
             * 某些客户端/重连时序下，头像用的稳定皮肤缓存会先拿到 skin，
             * 但 cape/elytra 仍停在 null。在线玩家实体自己的 SkinTextures 往往已经补全披风，
             * 这里只借用披风相关纹理，不改主体皮肤，避免泄露变形职业的实时伪装皮肤。
             */
            return withCapeTextures(stableTextures, onlineTextures);
        }
        return stableTextures;
    }

    private @Nullable SkinTextures resolveFetchedSkinTextures(
            @NotNull UUID playerUuid,
            @NotNull String playerName,
            @Nullable PlayerListEntry playerListEntry
    ) {
        CompletableFuture<SkinTextures> fetchFuture = FETCHED_SKIN_TEXTURE_CACHE.computeIfAbsent(playerUuid, uuid -> {
            /*
             * getSkinTextures(profile) 只读本地当前缓存，很多时候会先拿到 skin，
             * 但 cape/elytra 还没有异步下载完成。这里主动发起一次 fetch：
             * 1. 如果 Tab 列表里已有 GameProfile，就沿用它的 textures 属性；
             * 2. 否则用 UUID + 名字让原版 SkinProvider 自己去 session service 补全。
             *
             * 这个 future 是静态缓存，同一个皮套玩家不会每帧重复请求。
             */
            String safeName = playerName.isBlank() ? uuid.toString().substring(0, 8) : playerName;
            GameProfile profile = playerListEntry != null ? playerListEntry.getProfile() : new GameProfile(uuid, safeName);
            FETCH_RETRY_AFTER.put(uuid, System.currentTimeMillis() + FETCH_RETRY_INTERVAL_MS);
            return MinecraftClient.getInstance()
                    .getSkinProvider()
                    .fetchSkinTextures(profile)
                    .exceptionally(throwable -> null);
        });

        if (!fetchFuture.isDone()) {
            return null;
        }

        SkinTextures fetchedTextures = fetchFuture.getNow(null);
        if (fetchedTextures != null && fetchedTextures.capeTexture() != null) {
            FETCH_RETRY_AFTER.remove(playerUuid);
            return fetchedTextures;
        }

        if (System.currentTimeMillis() >= FETCH_RETRY_AFTER.getOrDefault(playerUuid, 0L)) {
            FETCHED_SKIN_TEXTURE_CACHE.remove(playerUuid, fetchFuture);
        }
        return fetchedTextures;
    }

    private @Nullable SkinTextures resolveOnlinePlayerSkinTextures(@NotNull UUID playerUuid) {
        AbstractClientPlayerEntity player = resolveOnlineDisguisePlayer(playerUuid);
        return player == null ? null : player.getSkinTextures();
    }

    private @Nullable AbstractClientPlayerEntity resolveOnlineDisguisePlayer(@NotNull UUID playerUuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return null;
        }
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (playerUuid.equals(player.getUuid())) {
                return player;
            }
        }
        return null;
    }

    private static @NotNull SkinTextures withCapeTextures(@NotNull SkinTextures base, @NotNull SkinTextures capeSource) {
        return new SkinTextures(
                base.texture(),
                base.textureUrl(),
                capeSource.capeTexture(),
                capeSource.elytraTexture(),
                base.model(),
                base.secure()
        );
    }

    private static final class MagicianPlaybackPlayerEntityModel extends PlayerEntityModel<MagicianPlaybackEntity> {
        /** 原版完整蹲姿的身体前倾角，单位为弧度。 */
        private static final float SNEAKING_BODY_PITCH = 0.5F;
        /** 原版蹲姿在基础手臂动作上增加的前摆角，单位为弧度。 */
        private static final float SNEAKING_ARM_PITCH_OFFSET = 0.4F;
        /** 原版站立双腿的 Y 轴支点。 */
        private static final float STANDING_LEG_PIVOT_Y = 12.0F;
        /** 原版完整蹲姿双腿的 Y 轴支点。 */
        private static final float SNEAKING_LEG_PIVOT_Y = 12.2F;
        /** 原版完整蹲姿双腿向后的 Z 轴支点。 */
        private static final float SNEAKING_LEG_PIVOT_Z = 4.0F;
        /** 原版站立手臂的 Y 轴支点。 */
        private static final float STANDING_ARM_PIVOT_Y = 2.0F;
        /** 原版完整蹲姿手臂的 Y 轴支点。 */
        private static final float SNEAKING_ARM_PIVOT_Y = 5.2F;
        /** 原版完整蹲姿头部的 Y 轴支点。 */
        private static final float SNEAKING_HEAD_PIVOT_Y = 4.2F;
        /** 原版完整蹲姿身体的 Y 轴支点。 */
        private static final float SNEAKING_BODY_PIVOT_Y = 3.2F;

        private MagicianPlaybackPlayerEntityModel(net.minecraft.client.model.ModelPart root, boolean thinArms) {
            super(root, thinArms);
        }

        @Override
        public void setAngles(MagicianPlaybackEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
            /*
             * LivingEntityRenderer 会用 entity.hasVehicle() 写 model.riding。
             * 播放体没有真的骑上 Wathe 的 SeatEntity，所以这里在真正计算骨骼前，
             * 用录制帧同步来的 replaySitting 强制恢复玩家坐姿腿部动画。
             */
            this.riding = entity.isReplaySitting();

            /*
             * 复杂客户端动作模组有时不会稳定播放自定义 LivingEntity 的挥手包。
             * 这里不直接改手臂角度，只把“安全的普通挥手”进度交回原版 Biped 动画系统：
             * 1. 正在举刀/蓄力/使用物品时绝不补偿，避免再次出现“先顿一下再举起”；
             * 2. 只要没有真的进入使用状态，拿着食物、药水等物品左键也必须正常挥手；
             * 3. 空手、按钮/门交互、刺刀这类普通挥手同样会吃到这个兜底进度。
             */
            if (shouldApplyReplaySwing(entity)) {
                this.handSwingProgress = Math.max(this.handSwingProgress, entity.getReplaySwingProgress(0.0F));
            }
            super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
            float tickDelta = MathHelper.clamp(animationProgress - entity.age, 0.0F, 1.0F);
            if (!usesEmfCustomModel(this)) {
                /* EMF 动作资源包使用自身 var.Asneak 平滑，不能再叠加一遍 Noelles 骨骼插值。 */
                this.applyReplaySneakingTransition(entity.getReplaySneakingEasedTransition(tickDelta));
            }
            this.applyReplaySwingFallback(entity);
        }

        /**
         * 按连续进度复刻 BipedEntityModel 的完整蹲姿骨骼，并同步所有玩家外层皮肤部件。
         */
        private void applyReplaySneakingTransition(float progress) {
            if (progress <= 0.0F) {
                return;
            }

            this.body.pitch = MathHelper.lerp(progress, this.body.pitch, SNEAKING_BODY_PITCH);
            this.rightArm.pitch += SNEAKING_ARM_PITCH_OFFSET * progress;
            this.leftArm.pitch += SNEAKING_ARM_PITCH_OFFSET * progress;
            this.rightLeg.pivotZ = SNEAKING_LEG_PIVOT_Z * progress;
            this.leftLeg.pivotZ = SNEAKING_LEG_PIVOT_Z * progress;
            this.rightLeg.pivotY = MathHelper.lerp(progress, STANDING_LEG_PIVOT_Y, SNEAKING_LEG_PIVOT_Y);
            this.leftLeg.pivotY = MathHelper.lerp(progress, STANDING_LEG_PIVOT_Y, SNEAKING_LEG_PIVOT_Y);
            this.head.pivotY = SNEAKING_HEAD_PIVOT_Y * progress;
            this.body.pivotY = SNEAKING_BODY_PIVOT_Y * progress;
            this.rightArm.pivotY = MathHelper.lerp(progress, STANDING_ARM_PIVOT_Y, SNEAKING_ARM_PIVOT_Y);
            this.leftArm.pivotY = MathHelper.lerp(progress, STANDING_ARM_PIVOT_Y, SNEAKING_ARM_PIVOT_Y);

            /*
             * PlayerEntityModel 在 super.setAngles 内已经复制过一次外层变换；此处主骨骼发生了
             * 二次插值，必须重新复制，否则袖子、裤腿、外套和帽层会滞留在上一套姿态。
             */
            this.hat.copyTransform(this.head);
            this.rightSleeve.copyTransform(this.rightArm);
            this.leftSleeve.copyTransform(this.leftArm);
            this.rightPants.copyTransform(this.rightLeg);
            this.leftPants.copyTransform(this.leftLeg);
            this.jacket.copyTransform(this.body);
        }

        /**
         * 在原版骨骼计算完成后补一层明确的手臂挥动。
         *
         * <p>这是给 EMF/动作优化模组的最终兜底：这些模组可能在 LivingEntity 路径中忽略玩家专属
         * handSwingProgress，但仍会使用 PlayerEntityModel 的手臂部件变换。这里只在回放脉冲有效且
         * 未使用物品时应用，避免干扰吃喝、举刀、拉弓和手雷蓄力。</p>
         */
        private void applyReplaySwingFallback(@NotNull MagicianPlaybackEntity entity) {
            if (!shouldApplyReplaySwing(entity)) {
                return;
            }
            float progress = entity.getReplaySwingProgress(0.0F);
            if (progress <= 0.0F) {
                return;
            }
            float swing = MathHelper.sin(progress * (float) Math.PI);
            ModelPart swingingArm = entity.getReplaySwingHand() == Hand.OFF_HAND ? this.leftArm : this.rightArm;
            swingingArm.pitch -= swing * 1.2F;
            swingingArm.roll += (entity.getReplaySwingHand() == Hand.OFF_HAND ? -1.0F : 1.0F) * swing * 0.08F;
        }

        private static boolean shouldApplyReplaySwing(@NotNull MagicianPlaybackEntity entity) {
            return !entity.isReplayUsingItem() && !entity.isUsingItem();
        }
    }

    /**
     * 为自定义 LivingEntity 皮套补齐原版玩家专属的鞘翅角度过渡。
     *
     * <p>原版 {@link ElytraEntityModel} 只在实体是 {@code AbstractClientPlayerEntity} 时使用
     * {@code elytraPitch/elytraYaw/elytraRoll} 平滑字段；其他 LivingEntity 会直接写最终角度。
     * 皮套不属于玩家实体，因此必须显式用录制过渡进度混合收拢姿态与飞行姿态。</p>
     */
    private static final class MagicianPlaybackElytraModel extends ElytraEntityModel<MagicianPlaybackEntity> {
        /** 原版站立时左翼俯仰角，单位为弧度。 */
        private static final float STANDING_PITCH = 0.2617994F;
        /** 原版站立时左翼翻滚角，单位为弧度。 */
        private static final float STANDING_ROLL = -0.2617994F;
        /** 原版潜行时左翼俯仰角，单位为弧度。 */
        private static final float SNEAKING_PITCH = 0.6981317F;
        /** 原版潜行时左翼翻滚角，单位为弧度。 */
        private static final float SNEAKING_ROLL = -0.7853982F;
        /** 原版潜行时翅膀向下移动的模型单位。 */
        private static final float SNEAKING_PIVOT_Y = 3.0F;
        /** 原版潜行时左翼向外偏转角，单位为弧度。 */
        private static final float SNEAKING_YAW = 0.08726646F;
        /** 原版滑翔展开时的最大俯仰角，单位为弧度。 */
        private static final float FLYING_PITCH = 0.34906584F;
        /** 原版滑翔展开时的最大翻滚角，单位为弧度。 */
        private static final float FLYING_ROLL = -1.5707964F;

        private final ModelPart leftWing;
        private final ModelPart rightWing;

        private MagicianPlaybackElytraModel(ModelPart root) {
            super(root);
            this.leftWing = root.getChild("left_wing");
            this.rightWing = root.getChild("right_wing");
        }

        /**
         * 使用与身体相同的帧间插值进度计算两翼姿态，保证起飞和落地时不会各自跳帧。
         */
        private void setReplayAngles(@NotNull MagicianPlaybackEntity entity, float tickDelta) {
            float transition = entity.getReplayFallFlyingTransition(tickDelta);
            float sneakingTransition = entity.getReplaySneakingEasedTransition(tickDelta);

            /*
             * 所有模型（包括 EMF/JEM）都必须先取得连续的基础翼角。
             *
             * 原版 ElytraEntityModel 只有实体属于 AbstractClientPlayerEntity 时，才会用玩家实体上的
             * elytraPitch/elytraYaw/elytraRoll 以 0.1 比例追赶目标；皮套是 LivingEntity，调用 super
             * 会直接把翼角写到蹲姿终值。FA+Player 的 elytra.jem 虽然还会用 var.sneak 叠加动画，
             * 但它读取到的基础 right_wing/left_wing 已经突变，因此视觉上仍会瞬间张开。
             * 这里统一使用皮套已经按 tick 追踪并做帧间插值的蹲姿进度；EMF 会在模型渲染阶段继续
             * 计算资源包动画，形成与真实玩家相同的“原版基础翼角缓动 + 资源包缓动”组合。
             */
            float foldedPitch = MathHelper.lerp(sneakingTransition, STANDING_PITCH, SNEAKING_PITCH);
            float foldedRoll = MathHelper.lerp(sneakingTransition, STANDING_ROLL, SNEAKING_ROLL);
            float foldedPivotY = SNEAKING_PIVOT_Y * sneakingTransition;
            float foldedYaw = SNEAKING_YAW * sneakingTransition;

            /*
             * 以下飞行目标角度复刻原版 ElytraEntityModel：向下俯冲越明显，翅膀越接近完全展开。
             */
            float velocityFactor = 1.0F;
            Vec3d velocity = entity.getVelocity();
            if (velocity.y < 0.0D) {
                Vec3d normalizedVelocity = velocity.normalize();
                velocityFactor = 1.0F - (float) Math.pow(-normalizedVelocity.y, 1.5D);
            }
            float flyingPitch = velocityFactor * FLYING_PITCH
                    + (1.0F - velocityFactor) * STANDING_PITCH;
            float flyingRoll = velocityFactor * FLYING_ROLL
                    + (1.0F - velocityFactor) * STANDING_ROLL;

            this.leftWing.pitch = MathHelper.lerp(transition, foldedPitch, flyingPitch);
            this.leftWing.roll = MathHelper.lerp(transition, foldedRoll, flyingRoll);
            this.leftWing.pivotY = MathHelper.lerp(transition, foldedPivotY, 0.0F);
            this.leftWing.yaw = MathHelper.lerp(transition, foldedYaw, 0.0F);

            this.rightWing.pitch = this.leftWing.pitch;
            this.rightWing.roll = -this.leftWing.roll;
            this.rightWing.pivotY = this.leftWing.pivotY;
            this.rightWing.yaw = -this.leftWing.yaw;
        }

        @Override
        public void setAngles(
                MagicianPlaybackEntity entity,
                float limbAngle,
                float limbDistance,
                float animationProgress,
                float headYaw,
                float headPitch
        ) {
            /*
             * 标准模型接口没有 tickDelta 参数，普通调用使用当前 tick 姿态；
             * 魔术师 feature 会调用上面的专用入口传入真实渲染 tickDelta。
             */
            this.setReplayAngles(entity, 1.0F);
        }
    }

    /**
     * 魔术师皮套的鞘翅渲染层。
     *
     * <p>原版 {@code ElytraFeatureRenderer} 只有实体是 {@code AbstractClientPlayerEntity} 时才读取
     * 玩家 elytra/cape 纹理；皮套是自定义 LivingEntity，会错误回退成默认灰色鞘翅。
     * 这里保留原版模型姿态和渲染流程，只替换纹理来源。</p>
     */
    private final class MagicianPlaybackElytraFeatureRenderer
            extends FeatureRenderer<MagicianPlaybackEntity, PlayerEntityModel<MagicianPlaybackEntity>> {
        private MagicianPlaybackElytraFeatureRenderer() {
            super(MagicianPlaybackEntityRenderer.this);
        }

        @Override
        public void render(
                MatrixStack matrices,
                VertexConsumerProvider vertexConsumers,
                int light,
                MagicianPlaybackEntity entity,
                float limbAngle,
                float limbDistance,
                float tickDelta,
                float animationProgress,
                float headYaw,
                float headPitch
        ) {
            ItemStack chestStack = entity.getEquippedStack(EquipmentSlot.CHEST);
            if (!chestStack.isOf(Items.ELYTRA)) {
                return;
            }

            SkinTextures skinTextures = MagicianPlaybackEntityRenderer.this.resolveSkinTextures(entity);
            Identifier texture = skinTextures.elytraTexture();
            if (texture == null && skinTextures.capeTexture() != null
                    && MagicianPlaybackEntityRenderer.this.isDisguiseCapeVisible(entity)) {
                texture = skinTextures.capeTexture();
            }
            if (texture == null) {
                texture = DEFAULT_ELYTRA_TEXTURE;
            }

            matrices.push();
            matrices.translate(0.0F, 0.0F, 0.125F);
            this.getContextModel().copyStateTo(MagicianPlaybackEntityRenderer.this.elytraModel);
            MagicianPlaybackEntityRenderer.this.elytraModel.setReplayAngles(entity, tickDelta);
            VertexConsumer consumer = ItemRenderer.getArmorGlintConsumer(
                    vertexConsumers,
                    RenderLayer.getArmorCutoutNoCull(texture),
                    chestStack.hasGlint()
            );
            MagicianPlaybackEntityRenderer.this.elytraModel.render(
                    matrices,
                    consumer,
                    light,
                    OverlayTexture.DEFAULT_UV
            );
            matrices.pop();
        }
    }

    /** 在线目标以实时玩家实体的 CAPE 位为准；目标离线时回退到服务端同步的最后快照。 */
    private boolean isDisguiseCapeVisible(@NotNull MagicianPlaybackEntity entity) {
        UUID disguiseUuid = entity.getDisguisePlayerUuid();
        if (disguiseUuid == null) {
            return entity.isDisguiseCapeVisible();
        }
        AbstractClientPlayerEntity onlinePlayer = this.resolveOnlineDisguisePlayer(disguiseUuid);
        return onlinePlayer == null
                ? entity.isDisguiseCapeVisible()
                : onlinePlayer.isPartVisible(net.minecraft.entity.player.PlayerModelPart.CAPE);
    }

    /**
     * 魔术师皮套的披风渲染层。
     *
     * <p>原版 {@code CapeFeatureRenderer} 绑定的是 {@code AbstractClientPlayerEntity}，
     * 直接复用在自定义 LivingEntity 上会带入玩家专属状态依赖。这里使用皮套自己的移动历史，
     * 按 1.21.1 原版披风姿态公式渲染目标纹理，不改变播放实体生命周期。</p>
     */
    private final class MagicianPlaybackCapeFeatureRenderer extends FeatureRenderer<MagicianPlaybackEntity, PlayerEntityModel<MagicianPlaybackEntity>> {
        private MagicianPlaybackCapeFeatureRenderer() {
            super(MagicianPlaybackEntityRenderer.this);
        }

        @Override
        public void render(
                MatrixStack matrices,
                VertexConsumerProvider vertexConsumers,
                int light,
                MagicianPlaybackEntity entity,
                float limbAngle,
                float limbDistance,
                float tickDelta,
                float animationProgress,
                float headYaw,
                float headPitch
        ) {
            if (entity.isInvisible()) {
                return;
            }
            if (!MagicianPlaybackEntityRenderer.this.isDisguiseCapeVisible(entity)) {
                return;
            }
            Identifier capeTexture = MagicianPlaybackEntityRenderer.this.resolveSkinTextures(entity).capeTexture();
            if (capeTexture == null) {
                return;
            }
            if (entity.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                return;
            }

            MagicianPlaybackPlayerEntityModel currentModel = (MagicianPlaybackPlayerEntityModel) this.getContextModel();
            ModelPart emfCapePart = resolveEmfCapePart(currentModel);
            boolean useEmfCapeModel = emfCapePart != null && hasRenderableCapeGeometry(emfCapePart);
            ModelPart capePart = useEmfCapeModel ? emfCapePart : MagicianPlaybackEntityRenderer.this.vanillaCapePart;
            RenderLayer capeLayer = RenderLayer.getEntitySolid(capeTexture);
            matrices.push();
            // 两种模型都从玩家背后开始；资源包和兜底模型使用相同的基础朝向。
            matrices.translate(0.0F, 0.0F, 0.125F);
            if (useEmfCapeModel) {
                /*
                 * FA+Player 的 player_cape.jem 已独立计算跑动、转向、潜行和攀爬摆动。
                 * 这里只给它与原版一致的静态后仰/翻面基准，避免再叠加本类的位置滞后和步幅摇摆。
                 */
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
            } else {
                // 没有可用 EMF 几何体时，兜底模型使用 1.21.1 原版的完整披风运动公式。
                float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw);
                double yawRadians = bodyYaw * (Math.PI / 180.0D);
                double sinYaw = Math.sin(yawRadians);
                double negativeCosYaw = -Math.cos(yawRadians);

                double capeDeltaX = entity.getCapeX(tickDelta) - MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
                double capeDeltaY = entity.getCapeY(tickDelta) - MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
                double capeDeltaZ = entity.getCapeZ(tickDelta) - MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());

                float verticalSwing = MathHelper.clamp((float) capeDeltaY * 10.0F, -6.0F, 32.0F);
                float forwardSwing = MathHelper.clamp(
                        (float) (capeDeltaX * sinYaw + capeDeltaZ * negativeCosYaw) * 100.0F,
                        0.0F,
                        150.0F
                );
                float sidewaysSwing = MathHelper.clamp(
                        (float) (capeDeltaX * negativeCosYaw - capeDeltaZ * sinYaw) * 100.0F,
                        -20.0F,
                        20.0F
                );
                float strideDistance = entity.getCapeStrideDistance(tickDelta);
                float horizontalSpeed = MathHelper.lerp(tickDelta, entity.prevHorizontalSpeed, entity.horizontalSpeed);
                float walkSwing = MathHelper.sin(horizontalSpeed * 6.0F) * 32.0F * strideDistance;
                /* 披风的潜行抬升与身体使用同一连续进度，避免蹲下时披风先瞬间跳开。 */
                walkSwing += 25.0F * entity.getReplaySneakingEasedTransition(tickDelta);

                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0F + forwardSwing / 2.0F + verticalSwing + walkSwing));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sidewaysSwing / 2.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - sidewaysSwing / 2.0F));
            }

            var vertexConsumer = vertexConsumers.getBuffer(capeLayer);
            if (useEmfCapeModel) {
                /*
                 * EMF 的原版 CapeFeatureRenderer 钩子会因皮套不是 PlayerEntity 而跳过其 JEM 部件，
                 * 但此时 player.jem 中的 vanilla cloak 已是空占位。这里直接绘制 EMF 暴露的 cape JEM 部件，
                 * 前面的矩阵已完成背面朝向变换；不能再叠加 EMF 原版特性层内部的第二次 Y 翻转和后移，
                 * 否则两次 180 度翻转会抵消，且重复后移会让披风与身体脱开。
                 */
                capePart.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
            } else {
                // 不调用 PlayerEntityModel.renderCape，避免 EMF 对空 vanilla cloak 占位部件再次拦截。
                capePart.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
            }
            matrices.pop();
        }

    }

    /**
     * 通过可选反射读取 EMF 混入 PlayerEntityModel 的 JEM 披风部件；没有 EMF 时不会加载任何 EMF 类型。
     */
    private static @Nullable ModelPart resolveEmfCapePart(@NotNull Object model) {
        Optional<Field> field = EMF_CAPE_MODEL_FIELDS.computeIfAbsent(model.getClass(), MagicianPlaybackEntityRenderer::findEmfCapeField);
        if (field.isEmpty()) {
            return null;
        }
        try {
            Object capePart = field.get().get(model);
            return capePart instanceof ModelPart modelPart ? modelPart : null;
        } catch (IllegalAccessException | RuntimeException exception) {
            return null;
        }
    }

    /**
     * 判断当前模型是否真的被 EMF JEM 替换。
     *
     * <p>仅检测“安装了 EMF”不够：资源包未提供对应 JEM 时，EMF 仍会给模型类注入接口，
     * 但 {@code emf$isEMFModel()} 会返回 false。反射接入可避免把 EMF 变成硬依赖。</p>
     */
    private static boolean usesEmfCustomModel(@NotNull Object model) {
        Optional<Method> method = EMF_MODEL_CHECK_METHODS.computeIfAbsent(
                model.getClass(),
                MagicianPlaybackEntityRenderer::findEmfModelCheckMethod
        );
        if (method.isEmpty()) {
            return false;
        }
        try {
            Object result = method.get().invoke(model);
            return result instanceof Boolean value && value;
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            return false;
        }
    }

    /** 查找 EMF 注入的公开模型自检方法；父类和接口方法均由 getMethod 覆盖。 */
    private static Optional<Method> findEmfModelCheckMethod(@NotNull Class<?> modelClass) {
        try {
            Method method = modelClass.getMethod(EMF_MODEL_CHECK_METHOD_NAME);
            return method.trySetAccessible() ? Optional.of(method) : Optional.empty();
        } catch (NoSuchMethodException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Field> findEmfCapeField(@NotNull Class<?> modelClass) {
        for (Class<?> current = modelClass; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(EMF_CAPE_MODEL_FIELD_NAME);
                return field.trySetAccessible() ? Optional.of(field) : Optional.empty();
            } catch (NoSuchFieldException ignored) {
                // Mixin 字段由 PlayerEntityModel 的父类层级注入，继续向上查找。
            } catch (RuntimeException exception) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** EMF 的 cloak 根部件可能自身无 cuboid，但真实披风盒体存在于子部件中，因此递归检查整棵部件树。 */
    private static boolean hasRenderableCapeGeometry(@NotNull ModelPart capePart) {
        return capePart.traverse().anyMatch(part -> !part.isEmpty());
    }

    /** 从 1.21.1 原版数据工厂重建独立披风部件，保留原版 cuboid UV/dilation 参数且绕开 EMF 层替换。 */
    private static ModelPart createVanillaCapePart() {
        return TexturedModelData.of(PlayerEntityModel.getTexturedModelData(Dilation.NONE, false), 64, 64)
                .createModel()
                .getChild("cloak");
    }

    private static @Nullable AbstractClientPlayerEntity resolveOnlinePlayer(
            @NotNull MinecraftClient client,
            @NotNull UUID playerUuid
    ) {
        if (client.world == null) {
            return null;
        }
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (playerUuid.equals(player.getUuid())) {
                return player;
            }
        }
        return null;
    }
}
