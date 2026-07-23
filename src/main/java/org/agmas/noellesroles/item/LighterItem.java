package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.api.win.CustomVictory;
import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.arsonist.ArsonistConstants;
import org.agmas.noellesroles.roles.arsonist.DousedPlayerComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 纵火犯打火机。
 */
public class LighterItem extends Item {
    public LighterItem(@NotNull Settings settings) {
        super(settings);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity player, @NotNull Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!(world instanceof ServerWorld serverWorld)) {
            return TypedActionResult.pass(stack);
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (!gameWorld.isRole(player, Noellesroles.ARSONIST)) {
            return TypedActionResult.pass(stack);
        }

        boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
        if (!ignoresCooldown && player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }

        List<ServerPlayerEntity> alivePlayers = serverWorld.getPlayers(GameFunctions::isPlayerAliveAndSurvival);
        List<ServerPlayerEntity> dousedPlayers = alivePlayers.stream()
                .filter(target -> DousedPlayerComponent.KEY.get(target).isDoused())
                .toList();

        if (dousedPlayers.size() >= ArsonistConstants.getRequiredDousedCount(alivePlayers.size())) {
            for (ServerPlayerEntity doused : dousedPlayers) {
                GameFunctions.killPlayer(doused, true, player, Noellesroles.ARSONIST_IGNITED_DEATH_REASON);
                DousedPlayerComponent.KEY.get(doused).reset();
                DousedPlayerComponent.KEY.get(doused).sync();
            }
            player.playSoundToPlayer(SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);

            long playersLeft = serverWorld.getPlayers(GameFunctions::isPlayerAliveAndSurvival).size();
            if (playersLeft == 1) {
                VictoryApi.endGameWithCustomVictory(
                        serverWorld,
                        CustomVictory.of(Noellesroles.ARSONIST.identifier(), Noellesroles.ARSONIST.color(), List.of(player))
                );
            }
        } else {
            player.playSoundToPlayer(SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.0F, 1.0F);
            GameFunctions.killPlayer(player, true, player, Noellesroles.ARSONIST_FAILED_IGNITE_DEATH_REASON);
        }

        return TypedActionResult.pass(stack);
    }
}
