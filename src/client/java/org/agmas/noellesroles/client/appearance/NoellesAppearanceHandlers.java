package org.agmas.noellesroles.client.appearance;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.agmas.noellesroles.roles.controller.ControllerPlayerComponent;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * NoellesRoles 接入 Wathe 外观 / 准心名字 API 的统一注册处。
 *
 * <p>这里按“职业/机制”分组，而不是继续把皮肤、披风、手臂、尸体、名字拆进多个 mixin。
 * 这样每个职业的优先级和覆盖范围都能在同一个地方看清楚。</p>
 */
public final class NoellesAppearanceHandlers {
    /**
     * 灵术师是纯本地视角覆盖：出窍后自己看到所有玩家和尸体都是自己。
     * 它必须高于召集者等全局变形，因为这是“灵术师客户端如何理解世界”的特殊视角。
     */
    private static final int PRIORITY_SPIRITUALIST = 2000;
    /**
     * 疯狂观察是心情造成的视觉错乱。用户确认它低于召集者，高于普通主动变形。
     */
    private static final int PRIORITY_INSANE_OBSERVER = 900;
    /**
     * 变形怪、附体师、验尸官属于普通主动变形。
     */
    private static final int PRIORITY_ACTIVE_DISGUISE = 100;
    private static final int PRIORITY_SHARED_NAME_RULES = 95;

    private NoellesAppearanceHandlers() {
    }

    public static void register() {
        registerSpiritualist();
        registerInsaneObserver();
        registerMorphling();
        registerController();
        registerCoroner();
        registerSharedNameRules();
        registerKillerSidedCohortRules();
    }

    private static void registerSpiritualist() {
        PlayerAppearanceApi.registerPlayerSkin(id("spiritualist/appearance/player"), PRIORITY_SPIRITUALIST, player -> {
            ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
            if (localPlayer == null) {
                return null;
            }

            SpiritualistPlayerComponent component = SpiritualistPlayerComponent.KEY.get(localPlayer);
            if (component.isProjecting() && player != localPlayer) {
                return resolveLocalOriginalSkin(localPlayer);
            }
            if (component.isPossessing() && player == localPlayer && component.possessionTarget != null) {
                return DisguiseRenderHelper.resolveSkinTexturesFromUuid(player, component.possessionTarget, true);
            }
            return null;
        });

        PlayerAppearanceApi.registerBodySkin(id("spiritualist/appearance/body"), PRIORITY_SPIRITUALIST, body -> {
            ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
            if (localPlayer == null || !SpiritualistPlayerComponent.KEY.get(localPlayer).isProjecting()) {
                return null;
            }
            return resolveLocalOriginalSkin(localPlayer);
        });

        RoleNameHudApi.registerRaycastSource(id("spiritualist/role_name/raycast_source"), PRIORITY_SPIRITUALIST, player -> {
            if (SpiritualistClientController.isProjectionActive() || SpiritualistClientController.isPossessionViewActive()) {
                Entity cameraEntity = MinecraftClient.getInstance().getCameraEntity();
                return cameraEntity == null ? null : cameraEntity;
            }
            return null;
        });

        RoleNameHudApi.registerPlayerTargetFilter(id("spiritualist/role_name/possession_target_filter"), PRIORITY_SPIRITUALIST, (viewer, target) ->
                SpiritualistClientController.shouldHideEntityInPossessionView(target)
                        ? RoleNameHudApi.TargetResult.DENY
                        : RoleNameHudApi.TargetResult.PASS
        );

        RoleNameHudApi.registerName(id("spiritualist/role_name/name"), PRIORITY_SPIRITUALIST, (viewer, target, originalName) ->
                SpiritualistClientController.isProjectionActive() ? viewer.getDisplayName() : null
        );
    }

    private static void registerInsaneObserver() {
        PlayerAppearanceApi.registerPlayerSkin(id("insane_observer/appearance/player"), PRIORITY_INSANE_OBSERVER, player ->
                isInsaneObserverMorphEnabled(player) ? DisguiseRenderHelper.resolveShuffledSkinTextures(player) : null
        );

        RoleNameHudApi.registerName(id("insane_observer/role_name/name"), PRIORITY_INSANE_OBSERVER, (viewer, target, originalName) ->
                target instanceof AbstractClientPlayerEntity clientTarget && isInsaneObserverMorphEnabled(clientTarget)
                        ? Text.literal("??!?!").formatted(Formatting.OBFUSCATED)
                        : null
        );
    }

    private static void registerMorphling() {
        PlayerAppearanceApi.registerPlayerSkin(id("morphling/appearance/player"), PRIORITY_ACTIVE_DISGUISE, player -> {
            MorphlingPlayerComponent component = MorphlingPlayerComponent.KEY.get(player);
            return component.getMorphTicks() > 0
                    ? DisguiseRenderHelper.resolveSkinTexturesFromUuid(player, component.disguise, true)
                    : null;
        });

        RoleNameHudApi.registerName(id("morphling/role_name/name"), PRIORITY_ACTIVE_DISGUISE, (viewer, target, originalName) -> {
            MorphlingPlayerComponent component = MorphlingPlayerComponent.KEY.get(target);
            return component.getMorphTicks() > 0 ? resolveNameFromUuid(target, component.disguise, originalName) : null;
        });
    }

    private static void registerController() {
        PlayerAppearanceApi.registerPlayerSkin(id("controller/appearance/player"), PRIORITY_ACTIVE_DISGUISE, player -> {
            UUID disguiseUuid = ControllerPlayerComponent.KEY.get(player).getDisguiseTarget();
            return disguiseUuid == null ? null : DisguiseRenderHelper.resolveSkinTexturesFromUuid(player, disguiseUuid, true);
        });

        RoleNameHudApi.registerName(id("controller/role_name/name"), PRIORITY_ACTIVE_DISGUISE, (viewer, target, originalName) -> {
            UUID disguiseUuid = ControllerPlayerComponent.KEY.get(target).getDisguiseTarget();
            return disguiseUuid == null ? null : resolveNameFromUuid(target, disguiseUuid, originalName);
        });
    }

    private static void registerCoroner() {
        PlayerAppearanceApi.registerPlayerSkin(id("coroner/appearance/player"), PRIORITY_ACTIVE_DISGUISE, player -> {
            CoronerPlayerComponent component = CoronerPlayerComponent.KEY.get(player);
            return component.getMorphTicks() > 0
                    ? DisguiseRenderHelper.resolveSkinTexturesFromUuid(player, component.disguise, true)
                    : null;
        });

        RoleNameHudApi.registerName(id("coroner/role_name/name"), PRIORITY_ACTIVE_DISGUISE, (viewer, target, originalName) -> {
            CoronerPlayerComponent component = CoronerPlayerComponent.KEY.get(target);
            return component.getMorphTicks() > 0 ? resolveNameFromUuid(target, component.disguise, originalName) : null;
        });
    }

    private static void registerSharedNameRules() {
        RoleNameHudApi.registerName(id("shared/role_name/invisible_name"), PRIORITY_SHARED_NAME_RULES, (viewer, target, originalName) ->
                target.isInvisible() ? Text.literal("") : null
        );
    }

    private static void registerKillerSidedCohortRules() {
        RoleNameHudApi.registerCohortState(id("shared/role_name/killer_sided_cohorts"), RoleNameHudApi.DEFAULT_PRIORITY, (viewer, subject, vanillaValue) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(subject.getWorld());
            if (gameWorld.isRole(subject, Noellesroles.MIMIC)) {
                return true;
            }
            return gameWorld.getRole(subject) != null && Noellesroles.KILLER_SIDED_NEUTRALS.contains(gameWorld.getRole(subject))
                    ? true
                    : null;
        });
    }

    private static boolean isInsaneObserverMorphEnabled(AbstractClientPlayerEntity player) {
        if (WatheClient.moodComponent == null || NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE == null) {
            return false;
        }

        ConfigWorldComponent config = ConfigWorldComponent.KEY.get(player.getWorld());
        return config != null
                && config.insaneSeesMorphs
                && WatheClient.moodComponent.isLowerThanDepressed()
                && NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.containsKey(player.getUuid());
    }

    private static SkinTextures resolveLocalOriginalSkin(ClientPlayerEntity localPlayer) {
        if (NoellesrolesClient.LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES != null) {
            return NoellesrolesClient.LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES;
        }
        return PlayerAppearanceApi.resolveOriginalSkinTextures(localPlayer.getUuid(), true);
    }

    private static Text resolveNameFromUuid(PlayerEntity viewer, @Nullable UUID targetUuid, Text fallback) {
        if (targetUuid == null) {
            return fallback;
        }
        if (targetUuid.equals(viewer.getUuid())) {
            return fallback;
        }

        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer != null && targetUuid.equals(localPlayer.getUuid())) {
            return localPlayer.getDisplayName();
        }

        PlayerEntity livePlayer = viewer.getWorld().getPlayerByUuid(targetUuid);
        if (livePlayer != null) {
            return livePlayer.getDisplayName();
        }

        String cachedName = PlayerAppearanceApi.resolveOriginalPlayerName(targetUuid);
        return cachedName == null ? Text.literal("Unknown Player") : Text.literal(cachedName);
    }

    private static Identifier id(String path) {
        return Identifier.of(Noellesroles.MOD_ID, path);
    }
}
