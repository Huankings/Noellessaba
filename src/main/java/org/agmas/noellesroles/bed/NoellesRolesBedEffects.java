package org.agmas.noellesroles.bed;

import dev.doctor4t.wathe.api.bed.BedEffectHandler;
import dev.doctor4t.wathe.api.bed.BedEffectRegistry;
import dev.doctor4t.wathe.block_entity.TrimmedBedBlockEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.roles.bomber.BomberTrapUtils;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * noellesroles 的床扩展效果注册入口。
 *
 * <p>当前只接入炸弹客的定时炸弹。
 * 这样定时炸弹塞床、睡觉触发、粒子显示、回放记录都统一走 wathe 的公共床接口。</p>
 */
public final class NoellesRolesBedEffects {
    private NoellesRolesBedEffects() {
    }

    public static void register() {
        BedEffectRegistry.register(new BedEffectHandler() {
            @Override
            public net.minecraft.util.Identifier effectId() {
                return Noellesroles.TIMED_BOMB_BED_EMBEDDED_EVENT;
            }

            @Override
            public net.minecraft.item.Item additiveItem() {
                return ModItems.TIMED_BOMB;
            }

            @Override
            public boolean canApply(TrimmedBedBlockEntity bed, ServerPlayerEntity player, ItemStack heldStack) {
                return GameFunctions.isPlayerAliveAndSurvival(player)
                        && dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.BOMBER)
                        && bed.getBedEffect() == null
                        && !BomberPlayerComponent.KEY.get(player).hasBomb()
                        && !player.getItemCooldownManager().isCoolingDown(ModItems.TIMED_BOMB);
            }

            @Override
            public void applyToBed(TrimmedBedBlockEntity bed, ServerPlayerEntity player, ItemStack heldStack, BlockPos pos) {
                NbtCompound extra = new NbtCompound();
                extra.putString("placement", "bed");
                BedEffectRegistry.applyStandardEffect(
                        player,
                        heldStack,
                        bed,
                        pos,
                        Noellesroles.TIMED_BOMB_BED_EMBEDDED_EVENT,
                        false,
                        extra
                );
            }

            @Override
            public boolean onSleepTrigger(ServerPlayerEntity player, TrimmedBedBlockEntity bed, @Nullable UUID applierUuid) {
                if (!BomberTrapUtils.tryAttachTimedBomb(player, applierUuid)) {
                    return false;
                }

                NbtCompound extra = new NbtCompound();
                extra.putUuid("victim", player.getUuid());
                GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.TIMED_BOMB_BED_TRIGGERED_EVENT, null, extra);
                return true;
            }
        });
    }
}
