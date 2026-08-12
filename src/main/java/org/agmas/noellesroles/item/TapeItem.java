package org.agmas.noellesroles.item;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.roles.muzzler.MuzzlerConstants;
import org.agmas.noellesroles.roles.muzzler.SilencePlayerComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 静语者胶带。
 *
 * <p>对目标玩家右键后，目标会进入“被封嘴”状态：不能语音、室外停留太久会窒息，
 * 其他玩家可以多次互动尝试撕下胶带。</p>
 */
public class TapeItem extends Item {
    private static final SoundEvent TAPE_APPLY_SOUND =
            SoundEvent.of(Identifier.of(NoellesRolesCore.MOD_ID, "item.tape.apply"));

    public TapeItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, @NotNull PlayerEntity user, @NotNull LivingEntity entity, @NotNull Hand hand) {
        if (!(entity instanceof PlayerEntity victim)) {
            return ActionResult.PASS;
        }
        /*
         * 胶带只能贴在真正可交互的活玩家身上。
         * 亡语杀手躺尸时不应因为胶带右键反馈暴露为玩家实体。
         */
        if (!TargetVisibilityApi.canInteractWithPlayer(user, victim)) {
            return ActionResult.PASS;
        }

        if (user.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(user.getWorld());
        if (!gameWorld.isRunning()
                || !gameWorld.isRole(user, NoellesRoleRegistry.MUZZLER)
                || !GameFunctions.isPlayerAliveAndSurvival(user)
                || !GameFunctions.isPlayerAliveAndSurvival(victim)
                || user.getItemCooldownManager().isCoolingDown(this)) {
            return ActionResult.PASS;
        }

        SilencePlayerComponent victimSilence = SilencePlayerComponent.KEY.get(victim);
        if (victimSilence.isSilenced()) {
            return ActionResult.PASS;
        }

        /*
         * 只有服务端成功写入静音状态后才消耗物品和进入冷却。
         * 这样客户端误点、目标非法或目标已被封嘴时，不会凭空扣掉胶带。
         */
        stack.decrementUnlessCreative(1, user);
        user.getItemCooldownManager().set(this, MuzzlerConstants.TAPE_COOLDOWN_TICKS);
        user.getWorld().playSound(null, user.getBlockPos(), TAPE_APPLY_SOUND, SoundCategory.PLAYERS, 1.0F, 1.0F);

        victimSilence.setSilenced(true);
        victimSilence.setSilencer(user.getUuid());
        victimSilence.sync();

        if (user instanceof ServerPlayerEntity serverUser && victim instanceof ServerPlayerEntity serverVictim) {
            GameRecordManager.recordItemUse(serverUser, Registries.ITEM.getId(this), serverVictim, null);
        }

        return ActionResult.CONSUME;
    }
}
