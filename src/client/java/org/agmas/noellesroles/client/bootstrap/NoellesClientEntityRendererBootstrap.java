package org.agmas.noellesroles.client.bootstrap;

import dev.doctor4t.wathe.client.WatheClient;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.client.render.entity.WitherSkullEntityRenderer;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.client.renderer.CaptureDeviceEntityRenderer;
import org.agmas.noellesroles.client.renderer.JasonThrownWeaponEntityRenderer;
import org.agmas.noellesroles.client.renderer.MagicianPlaybackEntityRenderer;
import org.agmas.noellesroles.client.renderer.RoleMineEntityRenderer;
import org.agmas.noellesroles.client.renderer.ThrowingAxeEntityRenderer;
import org.agmas.noellesroles.client.renderer.ThrowingPanEntityRenderer;

/** 仅维护实体渲染器注册清单，渲染实现仍位于 client.renderer。 */
public final class NoellesClientEntityRendererBootstrap {
    private NoellesClientEntityRendererBootstrap() {
    }

    public static void register() {
        EntityRendererRegistry.register(NoellesRolesEntities.ROLE_MINE_ENTITY_ENTITY_TYPE, RoleMineEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.CAPTURE_DEVICE_ENTITY_TYPE, CaptureDeviceEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.THROWING_AXE_ENTITY_TYPE, ThrowingAxeEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.THROWING_PAN_ENTITY_TYPE, ThrowingPanEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.JASON_THROWN_WEAPON_ENTITY_TYPE, JasonThrownWeaponEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.MAGICIAN_PLAYBACK_ENTITY_TYPE, MagicianPlaybackEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.LICH_SKELETON_SKULL_ENTITY_TYPE, WitherSkullEntityRenderer::new);
        EntityRendererRegistry.register(NoellesRolesEntities.LICH_MAGIC_BARRIER_ENTITY_TYPE, EmptyEntityRenderer::new);
    }
}
