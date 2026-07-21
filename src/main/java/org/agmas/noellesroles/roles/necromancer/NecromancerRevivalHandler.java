package org.agmas.noellesroles.roles.necromancer;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.config.HarpyModLoaderConfig;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;

import java.util.ArrayList;
import java.util.Collections;

/**
 * 死灵法师右键尸体复活逻辑。
 */
public final class NecromancerRevivalHandler {
    private static boolean initialized = false;

    private NecromancerRevivalHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity necromancer)) {
                return ActionResult.PASS;
            }
            if (!GameFunctions.isPlayerAliveAndSurvival(necromancer)) {
                return ActionResult.PASS;
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            if (!gameWorld.isRole(necromancer, Noellesroles.NECROMANCER)) {
                return ActionResult.PASS;
            }
            if (!(entity instanceof PlayerBodyEntity body)) {
                return ActionResult.PASS;
            }

            PlayerEntity revivedEntity = necromancer.getServerWorld().getPlayerByUuid(body.getPlayerUuid());
            if (!(revivedEntity instanceof ServerPlayerEntity revived)) {
                return ActionResult.PASS;
            }

            NecromancerWorldComponent necromancerWorld = NecromancerWorldComponent.KEY.get(world);
            if (necromancerWorld.getAvailableRevives() < 1) {
                return ActionResult.PASS;
            }

            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(necromancer);
            if (ability.cooldown > 0) {
                return ActionResult.PASS;
            }

            Role selectedRole = selectRevivedKillerRole();
            ability.setCooldown(NecromancerConstants.REVIVE_COOLDOWN_TICKS);
            necromancerWorld.decreaseAvailableRevives();

            /*
             * 复活顺序沿用 StupidExpress：先把玩家拉回尸体位置并切回冒险模式，
             * 再删除尸体、写入新职业和金币。只有这些步骤都完成后才记录回放。
             */
            revived.teleport(necromancer.getServerWorld(), body.getX(), body.getY(), body.getZ(), Collections.emptySet(), body.getYaw(), body.getPitch());
            revived.changeGameMode(GameMode.ADVENTURE);
            body.discard();

            gameWorld.addRole(revived, selectedRole);
            /*
             * NoellesRoles 的中途转职职业通常都要触发 ModdedRoleAssigned。
             * 这样被复活者如果随机到扩展杀手，也能拿到该职业自己的组件重置、初始物品或冷却处理。
             */
            ModdedRoleAssigned.EVENT.invoker().assignModdedRole(revived, selectedRole);

            PlayerShopComponent shop = PlayerShopComponent.KEY.get(revived);
            shop.setBalance(NecromancerConstants.REVIVED_BALANCE);
            shop.sync();

            sendWelcomeAnnouncement(revived, selectedRole, gameWorld);
            TrainVoicePlugin.resetPlayer(revived.getUuid());

            NbtCompound extra = new NbtCompound();
            extra.putUuid("revived_player", revived.getUuid());
            GameRecordManager.recordGlobalEvent(necromancer.getServerWorld(), Noellesroles.NECROMANCER_REVIVED_EVENT, necromancer, extra);

            return ActionResult.CONSUME;
        });
    }

    private static Role selectRevivedKillerRole() {
        var roles = new ArrayList<>(WatheRoles.ROLES);
        roles.remove(Noellesroles.NECROMANCER);
        roles.removeIf(role -> Harpymodloader.VANNILA_ROLES.contains(role)
                || !role.canUseKiller()
                || HarpyModLoaderConfig.HANDLER.instance().disabled.contains(role.identifier().toString()));
        if (roles.isEmpty()) {
            roles.add(WatheRoles.KILLER);
        }
        Collections.shuffle(roles);
        return roles.getFirst();
    }

    private static void sendWelcomeAnnouncement(ServerPlayerEntity revived, Role selectedRole, GameWorldComponent gameWorld) {
        int announcementIndex;
        if (Harpymodloader.VANNILA_ROLES.contains(selectedRole)) {
            announcementIndex = RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(RoleAnnouncementTexts.KILLER);
        } else if (Harpymodloader.autogeneratedAnnouncements.containsKey(selectedRole)) {
            announcementIndex = RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(Harpymodloader.autogeneratedAnnouncements.get(selectedRole));
        } else {
            /*
             * 理论上 Harpy refreshRoles 会给扩展职业生成欢迎文本。
             * 这里保留一个原版杀手兜底，避免某个联动职业公告缺失时复活流程中断。
             */
            announcementIndex = RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(RoleAnnouncementTexts.KILLER);
            revived.sendMessage(Text.translatable("message.noellesroles.necromancer.announcement_fallback"), true);
        }

        ServerPlayNetworking.send(revived, new AnnounceWelcomePayload(
                announcementIndex,
                gameWorld.getAllKillerTeamPlayers().size(),
                0
        ));
    }
}
