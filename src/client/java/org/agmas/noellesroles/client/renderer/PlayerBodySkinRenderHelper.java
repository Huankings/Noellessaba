package org.agmas.noellesroles.client.renderer;

import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.model.WatheModelLayers;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 专门负责尸体外观解析。
 *
 * <p>普通玩家渲染现在会经过 {@link DisguiseRenderHelper}，那里允许 Morphling / Coroner /
 * Controller 等职业把“当前显示皮肤”改成目标玩家。尸体不能直接复用那条链路：
 * 尸体代表的是死亡玩家本人的身份快照，如果继续读取“当前显示皮肤”，就会出现变形者死亡后
 * 尸体仍然跟着伪装目标变化，或者客户端缓存刷新后尸体皮肤突然跳变的问题。</p>
 *
 * <p>所以这里固定按尸体绑定的 owner UUID 解析“原始皮肤来源”，并且只在灵术师出窍时做
 * 本地视角覆盖。这个覆盖不改服务端尸体 UUID / NBT，因此只影响灵术师自己看到的画面，
 * 不会污染其他玩家看到的尸体。</p>
 */
public final class PlayerBodySkinRenderHelper {
    private static final UUID FALLBACK_BODY_SKIN_UUID = UUID.fromString("25adae11-cd98-48f4-990b-9fe1b2ee0886");
    private static PlayerEntityModel<PlayerBodyEntity> classicBodyModel;
    private static PlayerEntityModel<PlayerBodyEntity> slimBodyModel;

    private PlayerBodySkinRenderHelper() {
    }

    /**
     * 缓存 Wathe 尸体用的宽/细两套玩家模型。
     *
     * <p>Wathe 原本会在 EntityRenderDispatcher 里按缓存中的皮肤模型选择渲染器实例。
     * 但那份缓存可能短暂过期，且不认识灵术师的尸体伪装视角规则。这里在尸体渲染器构造时
     * 自己缓存两套模型，后续每次渲染都按本 helper 最终解析出的 SkinTextures 切换。</p>
     */
    public static void initializeBodyModels(EntityRendererFactory.Context context) {
        if (classicBodyModel == null) {
            classicBodyModel = new PlayerEntityModel<>(context.getPart(WatheModelLayers.PLAYER_BODY), false);
        }
        if (slimBodyModel == null) {
            slimBodyModel = new PlayerEntityModel<>(context.getPart(WatheModelLayers.PLAYER_BODY_SLIM), true);
        }
    }

    /**
     * 解析尸体最终应该显示的 SkinTextures。
     *
     * <p>正常情况下返回尸体 owner 的原始皮肤；如果本地玩家是正在灵魂出窍的灵术师，
     * 所有尸体都返回本地玩家自己的原始皮肤，避免灵术师通过尸体皮肤识别玩家信息。</p>
     */
    public static SkinTextures resolveBodySkinTextures(PlayerBodyEntity body) {
        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer != null && SpiritualistPlayerComponent.KEY.get(localPlayer).isProjecting()) {
            return resolveLocalOriginalSkinTextures(localPlayer);
        }

        return resolveOriginalSkinTextures(body.getPlayerUuid());
    }

    public static Identifier resolveBodyTexture(PlayerBodyEntity body, Identifier fallbackTexture) {
        SkinTextures skinTextures = resolveBodySkinTextures(body);
        if (skinTextures != null && skinTextures.texture() != null) {
            return skinTextures.texture();
        }
        return fallbackTexture;
    }

    @Nullable
    public static PlayerEntityModel<PlayerBodyEntity> getBodyModel(@Nullable SkinTextures skinTextures) {
        if (skinTextures == null) {
            return null;
        }

        return skinTextures.model() == SkinTextures.Model.SLIM ? slimBodyModel : classicBodyModel;
    }

    /**
     * 解析本地玩家自己的原始皮肤。
     *
     * <p>这里优先使用 NoellesrolesClient 缓存的 LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES。
     * 这份缓存只会在本地玩家没有外观覆盖时刷新，能避免“自己正在变形/附身时，把临时皮肤
     * 误写成原始皮肤”的问题。</p>
     */
    private static SkinTextures resolveLocalOriginalSkinTextures(ClientPlayerEntity localPlayer) {
        if (NoellesrolesClient.LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES != null) {
            return NoellesrolesClient.LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES;
        }

        return resolveOriginalSkinTextures(localPlayer.getUuid());
    }

    /**
     * 只按 UUID 解析玩家原始皮肤，不读取世界中玩家实体的 getSkinTextures()。
     *
     * <p>这是本修复的关键：世界中玩家实体的 getSkinTextures() 已经被伪装系统接管，
     * 适合“活人当前看起来是谁”，不适合“尸体本来属于谁”。尸体必须避开这条可变入口。</p>
     */
    private static SkinTextures resolveOriginalSkinTextures(@Nullable UUID ownerUuid) {
        if (ownerUuid == null) {
            return DefaultSkinHelper.getSkinTextures(FALLBACK_BODY_SKIN_UUID);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity localPlayer = client.player;

        if (localPlayer != null
                && ownerUuid.equals(localPlayer.getUuid())
                && NoellesrolesClient.LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES != null) {
            return NoellesrolesClient.LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES;
        }

        if (localPlayer != null && localPlayer.networkHandler != null) {
            PlayerListEntry entry = localPlayer.networkHandler.getPlayerListEntry(ownerUuid);
            if (entry != null) {
                return entry.getSkinTextures();
            }
        }

        if (WatheClient.PLAYER_ENTRIES_CACHE != null) {
            PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(ownerUuid);
            if (cachedEntry != null) {
                return cachedEntry.getSkinTextures();
            }
        }

        return DefaultSkinHelper.getSkinTextures(ownerUuid);
    }
}
