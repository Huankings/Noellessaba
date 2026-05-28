package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.util.AdventureUsable;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.entities.CaptureDeviceEntity;

public class CaptureDeviceItem extends Item implements AdventureUsable {
    public CaptureDeviceItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getSide() == Direction.UP || context.getSide() == Direction.DOWN) {
            PlayerEntity player = context.getPlayer();
            World world = player.getWorld();
            if (!world.isClient) {
                CaptureDeviceEntity entity = NoellesRolesEntities.CAPTURE_DEVICE_ENTITY_TYPE.create(world);
                Vec3d spawnPos = context.getHitPos();
                entity.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
                entity.setYaw(player.getHeadYaw());
                entity.setOwner(player.getUuid());
                world.spawnEntity(entity);
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), null, null);
                }

                // 播放放置音效给工程师（仅自己听到）
                if (player instanceof ServerPlayerEntity sp) {
                    sp.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
                }

                if (!player.isCreative()) {
                    player.getStackInHand(context.getHand()).decrement(1);
                }
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
