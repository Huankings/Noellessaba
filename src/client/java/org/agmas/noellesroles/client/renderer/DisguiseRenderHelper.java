package org.agmas.noellesroles.client.renderer;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.roles.controller.ControllerPlayerComponent;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 统一处理“伪装后应该显示谁的皮肤/模型”这件事。
 * <p>
 * 这里单独抽成工具类有两个目的：
 * 1. 让 Morphling / Controller / Coroner 三个职业共用完全一致的回退顺序，
 *    避免某个职业只支持活人皮肤，另一个职业却支持死人皮肤，表现不一致。
 * 2. 把“根据 SkinTextures 决定 Alex(SLIM) / Steve(CLASSIC) 模型”也放在同一处，
 *    这样后面维护时只需要看一个文件，不容易把纹理和模型切换写散。
 */
public final class DisguiseRenderHelper {

    /**
     * 这两个模型实例会在 PlayerEntityRenderer 构造完成后初始化一次，
     * 之后三个职业都直接复用，避免每次渲染都重新 new 模型对象。
     */
    private static PlayerEntityModel<AbstractClientPlayerEntity> classicModel;
    private static PlayerEntityModel<AbstractClientPlayerEntity> slimModel;

    private DisguiseRenderHelper() {
    }

    /**
     * 缓存经典 Steve 模型和纤细 Alex 模型。
     * <p>
     * 这里严格使用原版 PlayerEntityRenderer 初始化时使用的同一套模型层，
     * 只是在运行时根据伪装目标的皮肤模型类型动态切换，尽量不碰别的渲染逻辑，
     * 这样能把对现有“死人变形/防嵌套崩溃”修复的影响压到最低。
     */
    public static void initializePlayerModels(EntityRendererFactory.Context context) {
        if (classicModel == null) {
            classicModel = new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false);
        }
        if (slimModel == null) {
            slimModel = new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER_SLIM), true);
        }
    }

    /**
     * 按当前实际要显示的皮肤模型类型，返回对应的玩家模型。
     * <p>
     * 如果这里返回 null，调用方就保持原来的 renderer.model，不额外改动，
     * 这样即便初始化阶段出现意外，也不会因为这里直接抛错把客户端弄崩。
     */
    @Nullable
    public static PlayerEntityModel<AbstractClientPlayerEntity> getPlayerModel(@Nullable SkinTextures skinTextures) {
        if (skinTextures == null) {
            return null;
        }

        return skinTextures.model() == SkinTextures.Model.SLIM ? slimModel : classicModel;
    }

    /**
     * 按“Tab 玩家列表 -> Wathe 全局缓存 -> 默认皮肤”的顺序解析皮肤。
     * <p>
     * 这里故意不再直接读取“目标实体自身的 getSkinTextures()”，
     * 因为一旦把 AbstractClientPlayerEntity#getSkinTextures 本身也接入伪装系统后，
     * 再去读别的实体的 getSkinTextures() 就会把对方当前伪装状态一起套进来，
     * 甚至可能重新引出你之前已经修好的互相变形嵌套问题。
     *
     * <p>所以这里统一只读原始外观来源：
     * - Tab 玩家列表：优先拿正常同步的真实皮肤；
     * - Wathe 全局缓存：补齐死人、离开可见世界后的皮肤回退；
     * - 默认皮肤：极端情况下至少仍然能稳定给出一个模型类型，不至于返回 null 断渲染。
     */
    @Nullable
    public static SkinTextures resolveSkinTexturesFromUuid(AbstractClientPlayerEntity renderedPlayer,
                                                           @Nullable UUID targetUuid,
                                                           boolean allowWatheCacheFallback) {
        if (targetUuid == null) {
            return null;
        }

        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer != null
                && targetUuid.equals(localPlayer.getUuid())
                && NoellesrolesClient.LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES != null) {
            // 这里必须优先兜住“目标就是本地玩家自己”的情况。
            // 否则当 A 变形成 B、B 又变形成 A 时，
            // B 在 A 客户端里取到的就会是“A 当前已经显示成的 B 皮肤”，
            // 而不是“A 这个 UUID 原本该有的皮肤”，于是双方会互相看错。
            return NoellesrolesClient.LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES;
        }

        // 优先从 Tab 玩家列表读取真实皮肤。
        // 这不会带入“目标当前可能也在伪装中”的二次覆盖，因此更安全。
        if (localPlayer != null && localPlayer.networkHandler != null) {
            PlayerListEntry playerListEntry = localPlayer.networkHandler.getPlayerListEntry(targetUuid);
            if (playerListEntry != null) {
                return playerListEntry.getSkinTextures();
            }
        }

        // 第二层兜底：沿用 Wathe 客户端保存的玩家外观缓存，补齐“死人皮肤”的显示。
        if (allowWatheCacheFallback
                && WatheClient.PLAYER_ENTRIES_CACHE != null) {
            // 这里不能只 containsKey 就直接 get().getSkinTextures()。
            // 断线后重新进服的瞬间，Wathe 的缓存表可能已经提前放入了 key，
            // 但 value 还没重新补齐完成，此时 get(targetUuid) 会返回 null。
            // 之前第二次进服务器立刻崩溃，日志里正是炸在这一行。
            PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(targetUuid);
            if (cachedEntry != null) {
                return cachedEntry.getSkinTextures();
            }
        }

        // 最后一层兜底：哪怕目标已经完全不在缓存里，也至少给一个稳定的默认皮肤。
        return DefaultSkinHelper.getSkinTextures(targetUuid);
    }

    /**
     * 疯狂观察者效果下会把玩家随机洗牌成别人的外观。
     * 这里把那条逻辑也统一成 SkinTextures 解析，方便后面直接共用模型切换。
     */
    @Nullable
    public static SkinTextures resolveShuffledSkinTextures(AbstractClientPlayerEntity renderedPlayer) {
        if (NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE == null) {
            return null;
        }

        UUID shuffledUuid = NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.get(renderedPlayer.getUuid());
        return resolveSkinTexturesFromUuid(renderedPlayer, shuffledUuid, true);
    }

    /**
     * 解析“当前这个玩家是否有外观覆盖，如果有，应该覆盖成哪张皮肤”。
     *
     * <p>返回 null 表示当前没有任何伪装/洗牌覆盖，调用方应继续走玩家原始皮肤逻辑。</p>
     */
    @Nullable
    public static SkinTextures resolveAppearanceOverrideSkinTextures(AbstractClientPlayerEntity player) {
        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer != null) {
            SpiritualistPlayerComponent spiritualistComponent = SpiritualistPlayerComponent.KEY.get(localPlayer);

            /*
             * 灵魂出窍时，灵术师眼中的“其他所有玩家”都会被改写成自己当前原始皮肤。
             * 由于这里直接改的是 getSkinTextures 入口，披风、鞘翅和 Alex/Steve 手臂模型
             * 也会随之一起统一成灵术师自己的外观。
             */
            if (spiritualistComponent.isProjecting() && player != localPlayer) {
                if (NoellesrolesClient.LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES != null) {
                    return NoellesrolesClient.LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES;
                }
                return resolveSkinTexturesFromUuid(localPlayer, localPlayer.getUuid(), true);
            }

            /*
             * 灵魂附身时，本地第一人称手臂需要显示成宿主皮肤。
             * 这里仅在“本地玩家自己”这一侧临时替换，不会影响其他客户端看到的真实外观。
             */
            if (spiritualistComponent.isPossessing()
                    && player == localPlayer
                    && spiritualistComponent.possessionTarget != null) {
                return resolveSkinTexturesFromUuid(player, spiritualistComponent.possessionTarget, true);
            }
        }

        // 先处理疯狂观察者造成的“随机看到别人皮肤”效果，
        // 这样模型类型也会跟着被随机到的皮肤一起变化，不会只换贴图不换身形。
        if (WatheClient.moodComponent != null) {
            ConfigWorldComponent configComp = ConfigWorldComponent.KEY.get(player.getWorld());
            if (configComp != null
                    && configComp.insaneSeesMorphs
                    && WatheClient.moodComponent.isLowerThanDepressed()
                    && NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE != null
                    && NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.containsKey(player.getUuid())) {
                return resolveShuffledSkinTextures(player);
            }
        }

        // 下面三个职业理论上在正常对局里是互斥的。
        // 这里仍然全部判一次，是为了保证旧存档/异常同步边界下也有稳定兜底。
        MorphlingPlayerComponent morphComp = MorphlingPlayerComponent.KEY.get(player);
        if (morphComp.getMorphTicks() > 0) {
            return resolveSkinTexturesFromUuid(player, morphComp.disguise, true);
        }

        ControllerPlayerComponent controllerComp = ControllerPlayerComponent.KEY.get(player);
        UUID controllerDisguiseUuid = controllerComp.getDisguiseTarget();
        if (controllerDisguiseUuid != null) {
            return resolveSkinTexturesFromUuid(player, controllerDisguiseUuid, true);
        }

        CoronerPlayerComponent coronerComp = CoronerPlayerComponent.KEY.get(player);
        if (coronerComp.getMorphTicks() > 0) {
            return resolveSkinTexturesFromUuid(player, coronerComp.disguise, true);
        }

        return null;
    }

    /**
     * 解析“当前这个玩家最终应该显示成哪张皮肤”。
     *
     * <p>这和单纯读取某个职业组件里的 disguise UUID 不同：
     * 这里会把疯狂观察者的随机洗牌效果、Morphling、Controller、Coroner
     * 的真实显示优先级统一揉成一条链。
     *
     * <p>这样一来：
     * 1. EntityRenderDispatcher 在更上游挑选 slim/classic 渲染器时，
     *    可以直接复用这条链；
     * 2. PlayerEntityRenderer 内部切换 model 时，也能用同一份结果；
     * 3. 后面如果再有新的“外观覆盖职业”，只需要补这一处即可。
     */
    @Nullable
    public static SkinTextures resolveDisplayedSkinTextures(AbstractClientPlayerEntity player) {
        SkinTextures overrideSkin = resolveAppearanceOverrideSkinTextures(player);
        if (overrideSkin != null) {
            return overrideSkin;
        }

        // 正常情况下，这里应该回到玩家自己的原始皮肤。
        // 但断线重连初期如果缓存尚未恢复，仍然允许返回默认皮肤兜底，
        // 绝不能因为外观缓存短暂缺失而让整个渲染线程崩掉。
        return resolveSkinTexturesFromUuid(player, player.getUuid(), true);
    }
}
