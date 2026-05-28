package org.agmas.noellesroles.roles.prophet;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.item.CrystalBallItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 先知能力处理类。
 *
 * <p>能力逻辑分成两部分：
 * 1. 水晶球负责“标记下一个准确揭露目标”；
 * 2. G 键能力负责真正把某位玩家的身份向其他人公开。</p>
 */
public final class ProphetAbility {

    private ProphetAbility() {
    }

    public static void handle(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.PROPHET)) {
            return;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown > 0) {
            return;
        }

        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        if (shop.balance < ProphetConstants.REVEAL_COST) {
            return;
        }

        ProphetPlayerComponent prophetComponent = ProphetPlayerComponent.KEY.get(player);
        TargetSelection selection = selectRevealTarget(player, gameWorld, prophetComponent);
        if (selection == null || selection.role == null) {
            return;
        }

        // 扣费与冷却只有在真正成功选择到目标后才生效。
        shop.balance -= ProphetConstants.REVEAL_COST;
        shop.sync();

        ability.setCooldown(ProphetConstants.REVEAL_COOLDOWN_TICKS);
        ability.sync();

        MutableText announcement = Text.translatable(
                "message.noellesroles.prophet.reveal",
                getProphetColoredPlayerName(selection.target),
                getProphetColoredPlayerName(selection.target),
                Harpymodloader.getRoleName(selection.role).copy().withColor(selection.role.color())
        ).formatted(Formatting.RESET).withColor(Noellesroles.PROPHET.color());

        for (ServerPlayerEntity other : player.getServerWorld().getPlayers()) {
            if (!other.getUuid().equals(selection.target.getUuid())) {
                other.sendMessage(announcement, true);
            }
        }

        ProphetPlayerComponent targetComponent = ProphetPlayerComponent.KEY.get(selection.target);
        if (!targetComponent.isImmuneToVoodooMagic()) {
            targetComponent.setImmuneToVoodooMagic(true);
        }
        targetComponent.setVoodooImmunityProvider(player.getUuid());

        NbtCompound revealExtra = new NbtCompound();
        revealExtra.putUuid("target_player", selection.target.getUuid());
        revealExtra.putInt("cost", ProphetConstants.REVEAL_COST);
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.PROPHET_REVEALED_EVENT, player, revealExtra);

        selection.target.sendMessage(
                Text.translatable("message.noellesroles.prophet.revealed")
                        .formatted(Formatting.RESET)
                        .withColor(Noellesroles.PROPHET.color()),
                true
        );

        // 只要这次是依靠水晶球锁定的准确目标，揭露完就清掉标记。
        if (selection.consumeMark) {
            prophetComponent.setMarkedTarget(null);
        }
    }

    /**
     * 服务端处理水晶球标记。
     */
    public static void handleCrystalBallMark(ServerPlayerEntity player, int targetId, boolean offHand) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.PROPHET)) {
            return;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }
        if (!gameWorld.isRunning()) {
            return;
        }

        if (!(player.getWorld().getEntityById(targetId) instanceof ServerPlayerEntity target)) {
            return;
        }
        if (target.getUuid().equals(player.getUuid())) {
            return;
        }
        if (gameWorld.getRole(target) == null || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            return;
        }

        // 服务端再次校验本次是否真的对准了该玩家，
        // 防止客户端伪造数据包去越距离/越目标标记别人。
        HitResult hitResult = CrystalBallItem.getCrystalBallTarget(player);
        if (!(hitResult instanceof EntityHitResult entityHitResult) || entityHitResult.getEntity().getId() != targetId) {
            return;
        }

        ProphetPlayerComponent prophetComponent = ProphetPlayerComponent.KEY.get(player);
        UUID previousMarked = prophetComponent.getMarkedTarget();

        if (target.getUuid().equals(previousMarked)) {
            player.sendMessage(
                    Text.translatable("message.noellesroles.prophet.already_marked", target.getDisplayName())
                            .formatted(Formatting.RESET)
                            .withColor(Noellesroles.PROPHET.color()),
                    true
            );
            return;
        }

        prophetComponent.setMarkedTarget(target.getUuid());

        if (previousMarked != null) {
            NbtCompound remarkExtra = new NbtCompound();
            remarkExtra.putUuid("old_target", previousMarked);
            remarkExtra.putUuid("new_target", target.getUuid());
            GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.PROPHET_REMARKED_EVENT, player, remarkExtra);
            player.sendMessage(
                    Text.translatable(
                            "message.noellesroles.prophet.remarked",
                            getPlayerNameForMessage(player, previousMarked),
                            target.getDisplayName()
                    ).formatted(Formatting.RESET).withColor(Noellesroles.PROPHET.color()),
                    true
            );
        } else {
            NbtCompound markExtra = new NbtCompound();
            markExtra.putUuid("target_player", target.getUuid());
            GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.PROPHET_MARKED_EVENT, player, markExtra);
            player.sendMessage(
                    Text.translatable("message.noellesroles.prophet.marked", target.getDisplayName())
                            .formatted(Formatting.RESET)
                            .withColor(Noellesroles.PROPHET.color()),
                    true
            );
        }

        consumeCrystalBall(player, offHand);
    }

    private static void consumeCrystalBall(ServerPlayerEntity player, boolean offHand) {
        if (offHand) {
            if (player.getOffHandStack().isOf(org.agmas.noellesroles.ModItems.CRYSTAL_BALL)) {
                player.getOffHandStack().decrementUnlessCreative(1, player);
                return;
            }
        } else {
            if (player.getMainHandStack().isOf(org.agmas.noellesroles.ModItems.CRYSTAL_BALL)) {
                player.getMainHandStack().decrementUnlessCreative(1, player);
                return;
            }
        }

        // 如果客户端发包时手别切换了，做一次兜底检查，尽量还是把正确的水晶球扣掉。
        if (player.getMainHandStack().isOf(org.agmas.noellesroles.ModItems.CRYSTAL_BALL)) {
            player.getMainHandStack().decrementUnlessCreative(1, player);
        } else if (player.getOffHandStack().isOf(org.agmas.noellesroles.ModItems.CRYSTAL_BALL)) {
            player.getOffHandStack().decrementUnlessCreative(1, player);
        }
    }

    private static TargetSelection selectRevealTarget(
            ServerPlayerEntity player,
            GameWorldComponent gameWorld,
            ProphetPlayerComponent prophetComponent
    ) {
        UUID markedTargetUuid = prophetComponent.getMarkedTarget();
        if (markedTargetUuid != null) {
            ServerPlayerEntity markedTarget = player.getServer().getPlayerManager().getPlayer(markedTargetUuid);
            Role markedRole = gameWorld.getRole(markedTargetUuid);
            if (markedTarget != null && markedRole != null) {
                return new TargetSelection(markedTarget, markedRole, true);
            }

            // 标记目标若因为极端情况失联/不存在，避免卡死在无效标记上，直接清理后回退随机。
            prophetComponent.setMarkedTarget(null);
        }

        List<ServerPlayerEntity> candidates = new ArrayList<>();
        for (ServerPlayerEntity other : player.getServerWorld().getPlayers()) {
            if (gameWorld.getRole(other) != null && GameFunctions.isPlayerAliveAndSurvival(other)) {
                candidates.add(other);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        ServerPlayerEntity randomTarget = candidates.get(player.getRandom().nextInt(candidates.size()));
        Role randomRole = gameWorld.getRole(randomTarget);
        if (randomRole == null) {
            return null;
        }

        return new TargetSelection(randomTarget, randomRole, false);
    }

    private static MutableText getProphetColoredPlayerName(PlayerEntity player) {
        return player.getDisplayName().copy().withColor(Noellesroles.PROPHET.color());
    }

    private static Text getPlayerNameForMessage(ServerPlayerEntity viewer, UUID playerUuid) {
        PlayerEntity target = viewer.getWorld().getPlayerByUuid(playerUuid);
        if (target != null) {
            return target.getDisplayName();
        }
        return Text.translatable("message.noellesroles.prophet.unknown_player");
    }

    private record TargetSelection(ServerPlayerEntity target, Role role, boolean consumeMark) {
    }
}
