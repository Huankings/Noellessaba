package org.agmas.noellesroles.roles.vulture;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.util.AnnounceWelcomePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.TypeFilter;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.config.HarpyModLoaderConfig;
import org.agmas.harpymodloader.events.ModdedRoleAssigned;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.coroner.BodyDeathReasonComponent;
import org.agmas.noellesroles.packet.role.vulture.VultureEatC2SPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VultureAbility {

    private VultureAbility() {
        // 工具类，禁止实例化
    }

    /**
     * 处理秃鹫的技能请求（吞噬尸体）
     * @param payload 客户端发送的数据包
     * @param player  技能使用玩家（秃鹫）
     */
    public static void handle(VultureEatC2SPacket payload, ServerPlayerEntity player) {
        var world = player.getServerWorld();
        var gameWorld = GameWorldComponent.KEY.get(world);
        var ability = AbilityPlayerComponent.KEY.get(player);

        // 检查角色和状态
        if (!gameWorld.isRole(player, Noellesroles.VULTURE) || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }
        if (ability.cooldown > 0) return;

        // 查找附近的尸体
        List<PlayerBodyEntity> bodies = world.getEntitiesByType(
                TypeFilter.instanceOf(PlayerBodyEntity.class),
                player.getBoundingBox().expand(10),
                body -> body.getUuid().equals(payload.playerBody())
        );

        if (bodies.isEmpty()) return;

        PlayerBodyEntity body = bodies.getFirst();
        BodyDeathReasonComponent deathReason = BodyDeathReasonComponent.KEY.get(body);

        // 尸体未被吞噬过
        if (deathReason.vultured) return;

        // 设置冷却
        ability.cooldown = GameConstants.getInTicks(0, 20); // 20秒
        ability.sync();

        // 增加吞噬计数
        var vultureComp = VulturePlayerComponent.KEY.get(player);
        vultureComp.bodiesEaten++;
        vultureComp.sync();
        NbtCompound eatenExtra = new NbtCompound();
        eatenExtra.putUuid("victim", body.getPlayerUuid());
        GameRecordManager.recordGlobalEvent(world, Noellesroles.VULTURE_PROGRESS_EVENT, player, eatenExtra);

        NbtCompound progressExtra = new NbtCompound();
        progressExtra.putInt("bodies_eaten", vultureComp.bodiesEaten);
        progressExtra.putInt("bodies_required", vultureComp.bodiesRequired);
        GameRecordManager.recordGlobalEvent(world, Noellesroles.VULTURE_PROGRESS_EVENT, player, progressExtra);

        // 播放声音和效果
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.MASTER, 1.0F, 0.5F);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2));

        // 检查是否达成进化条件
        if (vultureComp.bodiesEaten >= vultureComp.bodiesRequired) {
            // 随机选择杀手角色
            List<Role> killerRoles = new ArrayList<>(WatheRoles.ROLES);
            killerRoles.removeIf(role ->
                    Harpymodloader.VANNILA_ROLES.contains(role) ||
                            !role.canUseKiller() ||
                            HarpyModLoaderConfig.HANDLER.instance().disabled.contains(role.identifier().toString())
            );
            if (killerRoles.isEmpty()) {
                killerRoles.add(WatheRoles.KILLER);
            }
            Collections.shuffle(killerRoles);
            Role newRole = killerRoles.getFirst();

            // 转换角色
            gameWorld.addRole(player, newRole);
            ModdedRoleAssigned.EVENT.invoker().assignModdedRole(player, newRole);

            // 设置商店余额
            PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
            shop.setBalance(200);
            shop.sync();

            // 发送欢迎信息
            if (Harpymodloader.VANNILA_ROLES.contains(newRole)) {
                ServerPlayNetworking.send(player, new AnnounceWelcomePayload(
                        RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(WatheRoles.KILLER),
                        gameWorld.getAllKillerTeamPlayers().size(),
                        0
                ));
            } else {
                ServerPlayNetworking.send(player, new AnnounceWelcomePayload(
                        RoleAnnouncementTexts.ROLE_ANNOUNCEMENT_TEXTS.indexOf(
                                Harpymodloader.autogeneratedAnnouncements.get(newRole)),
                        gameWorld.getAllKillerTeamPlayers().size(),
                        0
                ));
            }
        }

        // 标记尸体为已吞噬
        deathReason.vultured = true;
        deathReason.sync();
    }
}
