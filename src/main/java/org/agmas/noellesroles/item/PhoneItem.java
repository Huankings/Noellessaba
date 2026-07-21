package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.hacker.HackerPhoneComponent;
import org.agmas.noellesroles.voice.NoellesrolesVoiceChatPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * 黑客手机。
 *
 * <p>右键只在服务端切换语音组状态；客户端模型通过 HackerPhoneComponent 的同步状态改变。</p>
 */
public class PhoneItem extends Item {
    public PhoneItem(@NotNull Settings settings) {
        super(settings);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity player, @NotNull Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient && GameWorldComponent.KEY.get(player.getWorld()).getRole(player) != null) {
            HackerPhoneComponent phone = HackerPhoneComponent.KEY.get(player);
            if (phone.groupKiller) {
                TrainVoicePlugin.resetPlayer(player.getUuid());
                phone.groupKiller = false;
            } else {
                NoellesrolesVoiceChatPlugin.addKillerGroup(player.getUuid());
                phone.groupKiller = true;
            }
            phone.sync();
            player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BANJO.value(), SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
        return TypedActionResult.pass(stack);
    }
}
