package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.bomber.BomberPlayerComponent;

/**
 * 定时炸弹物品。
 * 1. 炸弹客手持时，对准目标玩家右键，可以把一枚未启动的定时炸弹放到对方身上。
 * 2. 已经携带正在倒计时的玩家手持该物品时，对准其他玩家右键，可以继续把炸弹传出去。
 */
public class TimedBombItem extends Item {

    public TimedBombItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        World world = user.getWorld();

        // 只有玩家才能成为定时炸弹的目标
        if (!(entity instanceof PlayerEntity target)) {
            return ActionResult.PASS;
        }

        if (!world.isClient) {
            // 物品仍在冷却时，不允许继续放置或传递
            if (user.getItemCooldownManager().isCoolingDown(this)) {
                return ActionResult.PASS;
            }

            // 目标必须仍然是存活中的游戏玩家
            if (!GameFunctions.isPlayerAliveAndSurvival(target)) {
                return ActionResult.PASS;
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            BomberPlayerComponent userComponent = BomberPlayerComponent.KEY.get(user);
            BomberPlayerComponent targetComponent = BomberPlayerComponent.KEY.get(target);

            // 如果使用者自己正拿着一枚已经开始滴滴响的炸弹，那么这次操作视为“传递”
            if (userComponent.isBeeping()) {
                if (userComponent.canTransfer(target)) {
                    userComponent.transferBomb(target);
                    if (user instanceof ServerPlayerEntity serverUser && target instanceof ServerPlayerEntity serverTarget) {
                        GameRecordManager.event(dev.doctor4t.wathe.record.GameRecordTypes.ITEM_USE)
                                .world(serverUser.getServerWorld())
                                .actor(serverUser)
                                .target(serverTarget)
                                .put("item", net.minecraft.registry.Registries.ITEM.getId(this).toString())
                                .putBool("timed_bomb_activated", true)
                                .record();
                    }
                    return ActionResult.CONSUME;
                }
                return ActionResult.PASS;
            }

            // 只有炸弹客本人，才能把未启动的定时炸弹放到其他玩家身上
            if (gameWorld.isRole(user, Noellesroles.BOMBER)) {
                if (targetComponent.hasBomb()) {
                    return ActionResult.PASS;
                }

                targetComponent.placeBomb(user);
                if (user instanceof ServerPlayerEntity serverUser && target instanceof ServerPlayerEntity serverTarget) {
                    GameRecordManager.event(dev.doctor4t.wathe.record.GameRecordTypes.ITEM_USE)
                            .world(serverUser.getServerWorld())
                            .actor(serverUser)
                            .target(serverTarget)
                            .put("item", net.minecraft.registry.Registries.ITEM.getId(this).toString())
                            .putBool("timed_bomb_activated", false)
                            .record();
                }
                stack.decrementUnlessCreative(1, user);
                return ActionResult.CONSUME;
            }
        }

        return ActionResult.PASS;
    }
}
