package org.agmas.noellesroles.tray;

import dev.doctor4t.wathe.api.tray.TrayEffectHandler;
import dev.doctor4t.wathe.api.tray.TrayEffectRegistry;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.framing.DelusionPlayerComponent;
import org.agmas.noellesroles.roles.bartender.BartenderPlayerComponent;
import org.agmas.noellesroles.roles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.roles.coward.SedativePlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * noellesroles 的托盘扩展效果注册入口。
 *
 * <p>把“防御试剂 / 幻觉试剂”正式注册到 wathe 的托盘扩展接口里，
 * 彻底取代过去基于 fake poison 的兼容写法。</p>
 */
public final class NoellesRolesTrayEffects {
    private NoellesRolesTrayEffects() {
    }

    public static void register() {
        TrayEffectRegistry.register(new TrayEffectHandler() {
            @Override
            public net.minecraft.util.Identifier effectId() {
                return Noellesroles.DEFENSE_TRAY_EFFECT;
            }

            @Override
            public net.minecraft.item.Item additiveItem() {
                return ModItems.DEFENSE_VIAL;
            }

            @Override
            public boolean canApply(BeveragePlateBlockEntity plate, ServerPlayerEntity player, ItemStack heldStack) {
                return true;
            }

            @Override
            public void applyToTray(BeveragePlateBlockEntity plate, ServerPlayerEntity player, ItemStack heldStack, BlockPos pos) {
                /**
                 * 防御试剂保留旧设计：可以覆盖托盘当前的毒药状态，相当于“先解掉旧毒，再挂上护盾效果”。
                 */
                TrayEffectRegistry.applyStandardEffect(player, heldStack, plate, pos, Noellesroles.DEFENSE_TRAY_EFFECT, true, true);
            }

            @Override
            public void onConsume(ServerPlayerEntity player, ItemStack consumedSnapshot, String consumeType, @Nullable UUID applierUuid) {
                BartenderPlayerComponent.KEY.get(player).giveArmor();
            }
        });

        TrayEffectRegistry.register(new TrayEffectHandler() {
            @Override
            public net.minecraft.util.Identifier effectId() {
                return Noellesroles.DELUSION_TRAY_EFFECT;
            }

            @Override
            public net.minecraft.item.Item additiveItem() {
                return ModItems.DELUSION_VIAL;
            }

            @Override
            public void applyToTray(BeveragePlateBlockEntity plate, ServerPlayerEntity player, ItemStack heldStack, BlockPos pos) {
                TrayEffectRegistry.applyStandardEffect(player, heldStack, plate, pos, Noellesroles.DELUSION_TRAY_EFFECT, false, false);
            }

            @Override
            public void onConsume(ServerPlayerEntity player, ItemStack consumedSnapshot, String consumeType, @Nullable UUID applierUuid) {
                DelusionPlayerComponent.KEY.get(player).startDelusion(player, applierUuid);
            }
        });

        TrayEffectRegistry.register(new TrayEffectHandler() {
            @Override
            public net.minecraft.util.Identifier effectId() {
                return Noellesroles.SEDATIVE_TRAY_EFFECT;
            }

            @Override
            public net.minecraft.item.Item additiveItem() {
                return ModItems.SEDATIVE;
            }

            @Override
            public void applyToTray(BeveragePlateBlockEntity plate, ServerPlayerEntity player, ItemStack heldStack, BlockPos pos) {
                TrayEffectRegistry.applyStandardEffect(player, heldStack, plate, pos, Noellesroles.SEDATIVE_TRAY_EFFECT, false, false);
            }

            @Override
            public void onConsume(ServerPlayerEntity player, ItemStack consumedSnapshot, String consumeType, @Nullable UUID applierUuid) {
                SedativePlayerComponent.KEY.get(player).startSedative(player, applierUuid);
            }
        });

        TrayEffectRegistry.register(new TrayEffectHandler() {
            @Override
            public net.minecraft.util.Identifier effectId() {
                return Noellesroles.TIMED_BOMB_TRAY_EMBEDDED_EVENT;
            }

            @Override
            public net.minecraft.item.Item additiveItem() {
                return ModItems.TIMED_BOMB;
            }

            @Override
            public boolean canApply(BeveragePlateBlockEntity plate, ServerPlayerEntity player, ItemStack heldStack) {
                return GameFunctions.isPlayerAliveAndSurvival(player)
                        && plate.getPoisoner() == null
                        && plate.getTrayEffect() == null
                        && !BomberPlayerComponent.KEY.get(player).hasBomb()
                        && !player.getItemCooldownManager().isCoolingDown(ModItems.TIMED_BOMB);
            }

            @Override
            public void applyToTray(BeveragePlateBlockEntity plate, ServerPlayerEntity player, ItemStack heldStack, BlockPos pos) {
                NbtCompound extra = new NbtCompound();
                extra.putString("placement", "tray");
                TrayEffectRegistry.applyStandardEffect(
                        player,
                        heldStack,
                        plate,
                        pos,
                        Noellesroles.TIMED_BOMB_TRAY_EMBEDDED_EVENT,
                        false,
                        false,
                        extra
                );
            }

            @Override
            public void onTakeFromTray(ServerPlayerEntity player, ItemStack takenStack, @Nullable UUID applierUuid, NbtCompound replayExtra) {
                replayExtra.putBoolean("timed_bomb_tray_take", true);
                if (applierUuid != null) {
                    replayExtra.putUuid("bomber", applierUuid);
                }

                // 定时炸弹托盘效果是“拿起就挂炸弹”，而不是“吃下去才触发”。
                // 因此这里在成功拿取时立刻把炸弹挂到玩家身上，并清掉物品上的托盘效果标记，
                // 避免后续真正食用该食物/饮品时又再次走一遍扩展消费链。
                if (!BomberPlayerComponent.KEY.get(player).hasBomb()) {
                    BomberPlayerComponent.KEY.get(player).placeBomb(applierUuid);
                }
                takenStack.remove(WatheDataComponentTypes.TRAY_EFFECT);
                takenStack.remove(WatheDataComponentTypes.TRAY_EFFECT_OWNER);
            }

            @Override
            public void onConsume(ServerPlayerEntity player, ItemStack consumedSnapshot, String consumeType, @Nullable UUID applierUuid) {
                // 已在 onTakeFromTray 阶段触发，不再在食用时重复处理。
            }
        });
    }
}
