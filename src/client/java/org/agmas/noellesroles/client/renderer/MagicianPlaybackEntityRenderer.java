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
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.texture.AbstractTexture;
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
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.client.ui.common.PlayerHeadTextureHelper;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.Map;
import java.util.Collections;
import java.util.Optional;
import java.util.WeakHashMap;
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
 * <p>护甲层这版先不强行挂接，优先保证 1.21.1 下自定义玩家外观链稳定工作。
 * 如果你后面还想继续补到“护甲也和原版玩家完全一致”，可以在这里再追加 ArmorFeatureRenderer。</p>
 */
public class MagicianPlaybackEntityRenderer extends LivingEntityRenderer<MagicianPlaybackEntity, PlayerEntityModel<MagicianPlaybackEntity>> {

    private static final float VISUAL_SCALE = 0.96F;
    private static final float VISUAL_GROUND_OFFSET = -0.045F;
    /** 皮肤/披风缓存暂未就绪或没有返回披风时，允许再次查询的最短间隔。 */
    private static final long FETCH_RETRY_INTERVAL_MS = 30_000L;
    private static final Map<UUID, CompletableFuture<SkinTextures>> FETCHED_SKIN_TEXTURE_CACHE = new ConcurrentHashMap<>();
    /** 记录每个目标下一次可重试时间，避免暂时缺失的 CSL 离线皮肤结果被永久缓存。 */
    private static final Map<UUID, Long> FETCH_RETRY_AFTER = new ConcurrentHashMap<>();
    /** 每个皮套按渲染阶段保存最近一次诊断状态，只在状态变化时写日志，避免逐帧刷屏。 */
    private static final Map<MagicianPlaybackEntity, Map<String, String>> CAPE_DIAGNOSTIC_STATES =
            Collections.synchronizedMap(new WeakHashMap<>());
    /** EMF 通过 Mixin 注入到 PlayerEntityModel 上的独立 player_cape.jem 根部件字段名。 */
    private static final String EMF_CAPE_MODEL_FIELD_NAME = "emf$capeModelPart";
    /** 按模型运行时类型缓存 EMF 可选字段，避免每帧重复反射搜索。 */
    private static final Map<Class<?>, Optional<Field>> EMF_CAPE_MODEL_FIELDS = new ConcurrentHashMap<>();
    /** SkinLayers3D 可选 feature 的类名；以反射接入，未安装时不产生类链接依赖。 */
    private static final String SKIN_LAYERS_FEATURE_CLASS = "dev.tr7zw.skinlayers.renderlayers.CustomLayerFeatureRenderer";
    /** SkinLayers3D 生成玩家皮肤 3D 网格的核心工具类。 */
    private static final String SKIN_LAYERS_UTIL_CLASS = "dev.tr7zw.skinlayers.SkinUtil";

    private final PlayerEntityModel<MagicianPlaybackEntity> classicModel;
    private final PlayerEntityModel<MagicianPlaybackEntity> slimModel;
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
        this.vanillaCapePart = createVanillaCapePart();
        this.addFeature(new HeldItemFeatureRenderer<>(this, context.getHeldItemRenderer()));
        this.addFeature(new MagicianPlaybackCapeFeatureRenderer());
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
        logCapeDiagnostic(entity, "renderer", "renderer-enter", null, false);
        this.model = resolveSkinTextures(entity).model() == SkinTextures.Model.SLIM ? this.slimModel : this.classicModel;
        this.setModelPose(entity);
        matrices.push();
        /*
         * entity_model_features / entity_texture_features 和部分动作优化模组会对玩家模型层做额外变换，
         * 自定义 LivingEntity 复用玩家模型时容易看起来略大、脚底略悬空。
         * 这里只在渲染层轻微缩小并下压，不改变服务端 hitbox，避免影响 Wathe 武器命中判定。
         */
        matrices.translate(0.0F, VISUAL_GROUND_OFFSET, 0.0F);
        matrices.scale(VISUAL_SCALE, VISUAL_SCALE, VISUAL_SCALE);
        super.render(entity, entityYaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
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

    private void setModelPose(@NotNull MagicianPlaybackEntity entity) {
        /*
         * 原版 PlayerEntityRenderer.render() 会先调用 setModelPose，
         * 负责把“正在蓄力、举盾、拿弓、拿三叉戟/匕首、持物”等状态翻译成玩家手臂姿势。
         * 魔术师皮套不是 AbstractClientPlayerEntity，所以这里手动补同一套关键逻辑。
         */
        PlayerEntityModel<MagicianPlaybackEntity> model = this.getModel();
        model.setVisible(true);
        model.sneaking = entity.isInSneakingPose();

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
             * 2. 手持物本身是长按 UseAction 时也不补偿，交给上面的 ArmPose 处理；
             * 3. 空手、按钮/门交互、刺刀这类普通挥手才会吃到这个兜底进度。
             */
            if (shouldApplyReplaySwing(entity)) {
                this.handSwingProgress = Math.max(this.handSwingProgress, entity.getReplaySwingProgress(0.0F));
            }
            super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        }

        private static boolean shouldApplyReplaySwing(@NotNull MagicianPlaybackEntity entity) {
            if (entity.isReplayUsingItem() || entity.isUsingItem()) {
                return false;
            }

            ItemStack swingStack = entity.getStackInHand(entity.getReplaySwingHand());
            return swingStack.isEmpty() || swingStack.getUseAction() == UseAction.NONE;
        }
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
                logCapeDiagnostic(entity, "feature", "skip-invisible", null, false);
                return;
            }
            if (!shouldShowDisguiseCape(entity)) {
                logCapeDiagnostic(entity, "feature", "skip-cape-part-disabled", null, false);
                return;
            }
            Identifier capeTexture = MagicianPlaybackEntityRenderer.this.resolveSkinTextures(entity).capeTexture();
            if (capeTexture == null) {
                logCapeDiagnostic(entity, "feature", "skip-no-cape-texture", null, true);
                return;
            }
            if (entity.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                logCapeDiagnostic(entity, "feature", "skip-chest-elytra", capeTexture, true);
                return;
            }

            MagicianPlaybackPlayerEntityModel currentModel = (MagicianPlaybackPlayerEntityModel) this.getContextModel();
            ModelPart emfCapePart = resolveEmfCapePart(currentModel);
            boolean useEmfCapeModel = emfCapePart != null && hasRenderableCapeGeometry(emfCapePart);
            ModelPart capePart = useEmfCapeModel ? emfCapePart : MagicianPlaybackEntityRenderer.this.vanillaCapePart;
            RenderLayer capeLayer = RenderLayer.getEntitySolid(capeTexture);
            AbstractTexture loadedTexture = MinecraftClient.getInstance().getTextureManager().getOrDefault(capeTexture, null);
            String renderSource = useEmfCapeModel
                    ? "emf-player_cape.jem"
                    : emfCapePart == null ? "vanilla-fallback:emf-model-unavailable" : "vanilla-fallback:emf-cape-geometry-empty";

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
                if (entity.isInSneakingPose()) {
                    walkSwing += 25.0F;
                }

                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6.0F + forwardSwing / 2.0F + verticalSwing + walkSwing));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sidewaysSwing / 2.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - sidewaysSwing / 2.0F));
            }

            String renderDetails = "source=" + renderSource + ","
                    + describeCapeRenderState(capePart, capeLayer, loadedTexture, vertexConsumers);
            logCapeDiagnostic(entity, "model-before", "about-to-render-model-part", capeTexture, true, renderDetails);
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
            logCapeDiagnostic(entity, "model-after", "cape-part-render-returned", capeTexture, true, renderDetails);
            logCapeDiagnostic(entity, "feature", "draw-submitted", capeTexture, true, renderDetails);
        }

        /** 在线目标以实时玩家实体的 CAPE 位为准；目标离线时回退到服务端同步的最后快照。 */
        private boolean shouldShowDisguiseCape(@NotNull MagicianPlaybackEntity entity) {
            UUID disguiseUuid = entity.getDisguisePlayerUuid();
            if (disguiseUuid == null) {
                return entity.isDisguiseCapeVisible();
            }
            AbstractClientPlayerEntity onlinePlayer = MagicianPlaybackEntityRenderer.this.resolveOnlineDisguisePlayer(disguiseUuid);
            return onlinePlayer == null
                    ? entity.isDisguiseCapeVisible()
                    : onlinePlayer.isPartVisible(net.minecraft.entity.player.PlayerModelPart.CAPE);
        }
    }

    /**
     * 记录一次披风诊断状态。状态按皮套和阶段去重；日志可用于区分未进入渲染器、纹理缺失、
     * 披风选项关闭、鞘翅遮挡以及已向 Minecraft 顶点缓冲提交绘制等情况。
     */
    private static void logCapeDiagnostic(
            @NotNull MagicianPlaybackEntity entity,
            @NotNull String phase,
            @NotNull String outcome,
            @Nullable Identifier resolvedCapeTexture,
            boolean textureChecked
    ) {
        logCapeDiagnostic(entity, phase, outcome, resolvedCapeTexture, textureChecked, "none");
    }

    private static void logCapeDiagnostic(
            @NotNull MagicianPlaybackEntity entity,
            @NotNull String phase,
            @NotNull String outcome,
            @Nullable Identifier resolvedCapeTexture,
            boolean textureChecked,
            @NotNull String renderDetails
    ) {
        UUID targetUuid = entity.getDisguisePlayerUuid();
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerListEntry playerListEntry = null;
        if (targetUuid != null && client.player != null && client.player.networkHandler != null) {
            playerListEntry = client.player.networkHandler.getPlayerListEntry(targetUuid);
        }

        SkinTextures stableTextures = targetUuid == null
                ? null
                : PlayerHeadTextureHelper.resolveStableSkinTextures(targetUuid, playerListEntry);
        AbstractClientPlayerEntity onlinePlayer = targetUuid == null
                ? null
                : MagicianPlaybackEntityRenderer.resolveOnlinePlayer(client, targetUuid);
        SkinTextures onlineTextures = onlinePlayer == null ? null : onlinePlayer.getSkinTextures();
        CompletableFuture<SkinTextures> fetchedFuture = targetUuid == null
                ? null
                : FETCHED_SKIN_TEXTURE_CACHE.get(targetUuid);
        String fetchState = describeFetchState(fetchedFuture);
        String onlineCapePart = onlinePlayer == null
                ? "unknown"
                : Boolean.toString(onlinePlayer.isPartVisible(net.minecraft.entity.player.PlayerModelPart.CAPE));
        String chestItem = net.minecraft.registry.Registries.ITEM.getId(
                entity.getEquippedStack(EquipmentSlot.CHEST).getItem()
        ).toString();
        String resolvedCape = !textureChecked
                ? "not-checked"
                : resolvedCapeTexture == null ? "null" : resolvedCapeTexture.toString();

        String signature = "outcome=" + outcome
                + " target=" + targetUuid
                + " name=" + entity.getDisguisePlayerName()
                + " online=" + (onlinePlayer != null)
                + " playerListEntry=" + (playerListEntry != null)
                + " stableCape=" + describeCape(stableTextures)
                + " onlineCape=" + describeCape(onlineTextures)
                + " fetched=" + fetchState
                + " snapshotCapePart=" + entity.isDisguiseCapeVisible()
                + " onlineCapePart=" + onlineCapePart
                + " invisible=" + entity.isInvisible()
                + " chest=" + chestItem
                + " resolvedCape=" + resolvedCape
                + " details=" + renderDetails;

        synchronized (CAPE_DIAGNOSTIC_STATES) {
            Map<String, String> phaseStates = CAPE_DIAGNOSTIC_STATES.computeIfAbsent(entity, ignored -> new java.util.HashMap<>());
            if (signature.equals(phaseStates.put(phase, signature))) {
                return;
            }
        }

        NoellesRolesCore.LOGGER.info("[MagicianCapeTrace] phase={} entityId={} {}", phase, entity.getId(), signature);
    }

    private static String describeCapeRenderState(
            @NotNull ModelPart capePart,
            @NotNull RenderLayer capeLayer,
            @Nullable AbstractTexture loadedTexture,
            @NotNull VertexConsumerProvider vertexConsumers
    ) {
        String textureState = loadedTexture == null
                ? "not-registered"
                : loadedTexture.getClass().getName() + "#glId=" + loadedTexture.getGlId();
        return "model=" + capePart.getClass().getName()
                + ",cloakVisible=" + capePart.visible
                + ",cloakHidden=" + capePart.hidden
                + ",cloakEmpty=" + capePart.isEmpty()
                + ",cloakPivot=" + capePart.pivotX + "/" + capePart.pivotY + "/" + capePart.pivotZ
                + ",cloakRotation=" + capePart.pitch + "/" + capePart.yaw + "/" + capePart.roll
                + ",layer=" + capeLayer
                + ",texture=" + textureState
                + ",provider=" + vertexConsumers.getClass().getName();
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

    private static String describeCape(@Nullable SkinTextures textures) {
        if (textures == null) {
            return "no-skin-textures";
        }
        return textures.capeTexture() == null ? "null" : textures.capeTexture().toString();
    }

    private static String describeFetchState(@Nullable CompletableFuture<SkinTextures> future) {
        if (future == null) {
            return "not-started";
        }
        if (!future.isDone()) {
            return "pending";
        }
        try {
            return "done:" + describeCape(future.getNow(null));
        } catch (RuntimeException exception) {
            return "failed:" + exception.getClass().getSimpleName();
        }
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
