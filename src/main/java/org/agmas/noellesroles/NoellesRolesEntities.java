package org.agmas.noellesroles;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.entities.CaptureDeviceEntity;
import org.agmas.noellesroles.entities.RoleMineEntity;
import org.agmas.noellesroles.entities.ThrowingAxeEntity;

public class NoellesRolesEntities {
    public static final EntityType<RoleMineEntity> ROLE_MINE_ENTITY_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "cube"),
            EntityType.Builder.create(RoleMineEntity::new, SpawnGroup.MISC).dimensions(0.75f, 0.75f).build("cube")
    );
    public static final EntityType<CaptureDeviceEntity> CAPTURE_DEVICE_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "capture_device"),
            EntityType.Builder.create(CaptureDeviceEntity::new, SpawnGroup.MISC).dimensions(0.75f, 0.75f).build("capture_device")
    );
    public static final EntityType<ThrowingAxeEntity> THROWING_AXE_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Noellesroles.MOD_ID, "throwing_axe"),
            EntityType.Builder.<ThrowingAxeEntity>create(ThrowingAxeEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f).build("throwing_axe")
    );

    public static void init() {}
}
