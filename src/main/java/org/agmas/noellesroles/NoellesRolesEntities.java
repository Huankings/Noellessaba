package org.agmas.noellesroles;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.entities.CaptureDeviceEntity;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
import org.agmas.noellesroles.entities.RoleMineEntity;
import org.agmas.noellesroles.entities.ThrowingAxeEntity;
import org.agmas.noellesroles.entities.ThrowingPanEntity;
import org.agmas.noellesroles.roles.jason.JasonThrownWeaponEntity;
import org.agmas.noellesroles.roles.magician.MagicianConstants;

public class NoellesRolesEntities {
    public static final EntityType<RoleMineEntity> ROLE_MINE_ENTITY_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(NoellesRolesCore.MOD_ID, "cube"),
            EntityType.Builder.create(RoleMineEntity::new, SpawnGroup.MISC).dimensions(0.75f, 0.75f).build("cube")
    );
    public static final EntityType<CaptureDeviceEntity> CAPTURE_DEVICE_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(NoellesRolesCore.MOD_ID, "capture_device"),
            EntityType.Builder.create(CaptureDeviceEntity::new, SpawnGroup.MISC).dimensions(0.75f, 0.75f).build("capture_device")
    );
    public static final EntityType<ThrowingAxeEntity> THROWING_AXE_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(NoellesRolesCore.MOD_ID, "throwing_axe"),
            EntityType.Builder.<ThrowingAxeEntity>create(ThrowingAxeEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f).build("throwing_axe")
    );
    public static final EntityType<ThrowingPanEntity> THROWING_PAN_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(NoellesRolesCore.MOD_ID, "throwing_pan"),
            EntityType.Builder.<ThrowingPanEntity>create(ThrowingPanEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f).build("throwing_pan")
    );
    public static final EntityType<JasonThrownWeaponEntity> JASON_THROWN_WEAPON_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(NoellesRolesCore.MOD_ID, "jason_thrown_weapon"),
            EntityType.Builder.<JasonThrownWeaponEntity>create(JasonThrownWeaponEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f).build("jason_thrown_weapon")
    );
    public static final EntityType<MagicianPlaybackEntity> MAGICIAN_PLAYBACK_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(NoellesRolesCore.MOD_ID, "magician_playback"),
            EntityType.Builder.<MagicianPlaybackEntity>create(MagicianPlaybackEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 1.8f)
                    .maxTrackingRange(MagicianConstants.PLAYBACK_TRACKING_RANGE)
                    .trackingTickInterval(1)
                    .disableSaving()
                    .build("magician_playback")
    );

    public static void init() {
        /*
         * 魔术师皮套虽然是自定义 LivingEntity，
         * 但仍然需要像尸体、怪物那样注册一份基础属性，
         * 否则服务端生成实体时会因为缺少属性容器而直接报错。
         */
        FabricDefaultAttributeRegistry.register(
                MAGICIAN_PLAYBACK_ENTITY_TYPE,
                MagicianPlaybackEntity.createAttributes()
        );
    }
}
