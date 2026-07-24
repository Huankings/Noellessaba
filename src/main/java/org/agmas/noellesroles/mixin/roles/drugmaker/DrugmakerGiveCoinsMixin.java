package org.agmas.noellesroles.mixin.roles.drugmaker;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerPoisonComponent.class)
public abstract class DrugmakerGiveCoinsMixin {
    @Shadow @Final private PlayerEntity player;
    @Shadow public int poisonTicks;
    @Unique private static final UUID DELUSION_MARKER = UUID.fromString("00000000-0000-0000-dead-c0de00000000");

    @Inject(method = "setDetailedPoisonTicks", at = @At("HEAD"))
    private void noellesroles$giveDrugmakerCoins(int ticks, @Nullable UUID poisoner, @NotNull Identifier source, @Nullable NbtCompound extra, CallbackInfo ci) {
        /*
         * 制毒师奖励只在“目标原本没中毒，这次真正开始中毒”时发放。
         * 这样吹矢缩短已有毒素、重复注毒或清毒都不会被误算成新的中毒事件。
         */
        if (ticks <= 0 || this.poisonTicks > 0 || GameFunctions.isPlayerSpectatingOrCreative(this.player)) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(this.player.getWorld());
        if (gameWorld.isRole(this.player, NoellesRoleRegistry.ROBOT) || !(this.player instanceof ServerPlayerEntity serverTarget)) {
            return;
        }
        /*
         * 幻觉试剂使用固定 marker 伪装 poisoner，酒保的鸡尾酒毒也不是制毒师的经济来源。
         * 迁入 NoellesRoles 后可以直接判断 BARTENDER，不再走 kinssaba 的反射兼容。
         */
        if (poisoner != null && (poisoner.equals(DELUSION_MARKER) || gameWorld.isRole(poisoner, NoellesRoleRegistry.BARTENDER))) {
            return;
        }

        for (ServerPlayerEntity serverPlayer : serverTarget.getServer().getPlayerManager().getPlayerList()) {
            if (gameWorld.isRole(serverPlayer, NoellesRoleRegistry.DRUGMAKER) && GameFunctions.isPlayerAliveAndSurvival(serverPlayer)) {
                PlayerShopComponent shop = PlayerShopComponent.KEY.get(serverPlayer);
                serverPlayer.sendMessage(
                        Text.translatable("tip.noellesroles.drugmaker.poisoned").withColor(DrugmakerConstants.ROLE_COLOR),
                        true
                );
                shop.addToBalance(DrugmakerConstants.POISON_REWARD_COINS);
            }
        }
    }
}
