package org.agmas.noellesroles.client.renderer;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
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
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.RotationAxis;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.client.ui.common.PlayerHeadTextureHelper;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.Map;
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
    private static final Map<UUID, CompletableFuture<SkinTextures>> FETCHED_SKIN_TEXTURE_CACHE = new ConcurrentHashMap<>();

    private final PlayerEntityModel<MagicianPlaybackEntity> classicModel;
    private final PlayerEntityModel<MagicianPlaybackEntity> slimModel;

    public MagicianPlaybackEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new MagicianPlaybackPlayerEntityModel(context.getPart(EntityModelLayers.PLAYER), false), 0.5F);
        this.classicModel = this.getModel();
        this.slimModel = new MagicianPlaybackPlayerEntityModel(context.getPart(EntityModelLayers.PLAYER_SLIM), true);
        this.addFeature(new HeldItemFeatureRenderer<>(this, context.getHeldItemRenderer()));
        this.addFeature(new MagicianPlaybackCapeFeatureRenderer());
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
            return MinecraftClient.getInstance()
                    .getSkinProvider()
                    .fetchSkinTextures(profile)
                    .exceptionally(throwable -> null);
        });

        return fetchFuture.isDone() ? fetchFuture.getNow(null) : null;
    }

    private @Nullable SkinTextures resolveOnlinePlayerSkinTextures(@NotNull UUID playerUuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return null;
        }

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (playerUuid.equals(player.getUuid())) {
                return player.getSkinTextures();
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
     * 魔术师皮套的轻量披风渲染层。
     *
     * <p>原版 {@code CapeFeatureRenderer} 绑定的是 {@code AbstractClientPlayerEntity}，
     * 直接复用在自定义 LivingEntity 上风险很高。这里不模拟原版披风飘动，
     * 只读取皮套目标玩家的 SkinTextures.capeTexture()，再用玩家模型自带的披风部件渲染。
     * 这样能先保证“有披风的皮套能显示披风”，同时不影响播放实体的生命周期稳定性。</p>
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
            Identifier capeTexture = MagicianPlaybackEntityRenderer.this.resolveSkinTextures(entity).capeTexture();
            if (capeTexture == null) {
                return;
            }
            if (entity.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                return;
            }

            matrices.push();
            /*
             * 原版披风并不是直接贴在模型原点上，而是先往背后挪一点，
             * 再做一个基础翻转/后仰。播放体不是 AbstractClientPlayerEntity，
             * 拿不到完整的 capeX/capeY/capeZ 飘动数据，所以这里用稳定的轻量姿态：
             * 保证披风在玩家背后可见，并在潜行时稍微抬高，避免插进身体。
             */
            matrices.translate(0.0F, 0.0F, 0.125F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.isInSneakingPose() ? 31.0F : 12.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
            this.getContextModel().renderCape(
                    matrices,
                    vertexConsumers.getBuffer(RenderLayer.getEntitySolid(capeTexture)),
                    light,
                    OverlayTexture.DEFAULT_UV
            );
            matrices.pop();
        }
    }
}
