package org.agmas.noellesroles.roles.corpsemaker;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheEntities;
import dev.doctor4t.wathe.index.WatheParticles;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import org.agmas.noellesroles.roles.coroner.BodyDeathReasonComponent;
import org.agmas.noellesroles.packet.role.corpsemaker.CorpsemakerC2SPacket;

public final class CorpsemakerAbility {

    private CorpsemakerAbility() {
        // 工具类，禁止实例化
    }

    /**
     * 处理造尸怪的技能请求
     * @param payload 客户端发送的数据包
     * @param player  技能使用玩家（造尸怪）
     */
    public static void handle(CorpsemakerC2SPacket payload, ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);

        // 基础检查：必须是造尸怪且存活
        if (!gameWorld.isRole(player, Noellesroles.CORPSEMAKER) || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }
        if (ability.cooldown > 0) return;

        // 目标玩家检查
        ServerPlayerEntity target = player.getServer().getPlayerManager().getPlayer(payload.target());
        if (target == null) {
            player.sendMessage(Text.literal("目标玩家无效"), true);
            return;
        }

        // 角色名处理
        String inputRole = payload.roleIdentifier().trim();
        if (inputRole.isEmpty()) {
            player.sendMessage(Text.literal("身份不能为空"), true);
            return;
        }

        // 匹配角色（忽略大小写）
        Role matchedRole = null;
        for (Role role : WatheRoles.ROLES) {
            if (role.identifier().getPath().equalsIgnoreCase(inputRole)) {
                matchedRole = role;
                break;
            }
        }
        if (matchedRole == null) {
            player.sendMessage(Text.literal("无效的身份：" + inputRole), true);
            return;
        }

        // 创建尸体
        PlayerBodyEntity body = WatheEntities.PLAYER_BODY.create(target.getWorld());
        if (body == null) return;

        // 设置尸体位置（在造尸怪面前）
        Vec3d spawnPos = player.getPos().add(player.getRotationVector().normalize().multiply(1));
        float yaw = player.getYaw();

        body.setPlayerUuid(target.getUuid());
        body.refreshPositionAndAngles(spawnPos.getX(), player.getY(), spawnPos.getZ(), yaw, 0f);
        body.setYaw(yaw);
        body.setHeadYaw(yaw);
        body.prevYaw = yaw;
        body.prevHeadYaw = yaw;
        body.prevBodyYaw = yaw;
        body.bodyYaw = yaw;
        body.setPitch(0f);
        body.prevPitch = 0f;
        body.age = 0;

        target.getWorld().spawnEntity(body);

        // 保存死亡原因和伪造角色
        BodyDeathReasonComponent deathComp = BodyDeathReasonComponent.KEY.get(body);
        deathComp.deathReason = Identifier.of(payload.deathReason());
        deathComp.playerRole = matchedRole.identifier();  // 使用完整 Identifier
        deathComp.sync();

        // 如果伪造的角色是大嗓门，让尸体发光60秒
        if (matchedRole == Noellesroles.NOISEMAKER) {
            body.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 20 * 60, 0));
        }

        // 根据死亡原因从造尸怪玩家位置播放音效/效果
        ServerWorld serverWorld = player.getServerWorld();
        String deathReason = payload.deathReason();

        if ("wathe:knife_stab".equals(deathReason)) {
            // 刀刺音效 - 全局播放
            serverWorld.playSound(null, player.getBlockPos(), WatheSounds.ITEM_KNIFE_STAB, SoundCategory.PLAYERS, 1.0f, 1.0f);
        } else if ("wathe:gun_shot".equals(deathReason)) {
            // 枪击音效 - 全局播放
            serverWorld.playSound(null, player.getBlockPos(), WatheSounds.ITEM_REVOLVER_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        } else if ("wathe:grenade".equals(deathReason)) {
            // 爆炸音效+粒子 - 全局
            serverWorld.playSound(null, player.getBlockPos(), WatheSounds.ITEM_GRENADE_EXPLODE, SoundCategory.PLAYERS, 5.0F,
                    1.0F + serverWorld.random.nextFloat() * 0.1F - 0.05F);
            serverWorld.spawnParticles(WatheParticles.BIG_EXPLOSION,
                    player.getX(), player.getY() + 0.1F, player.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
            serverWorld.spawnParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 0.1F, player.getZ(),
                    100, 0.0, 0.0, 0.0, 0.2);
        } else if ("wathe:bat_hit".equals(deathReason)) {
            // 棍棒音效 - 全局播放
            serverWorld.playSound(null, player.getBlockPos(), WatheSounds.ITEM_BAT_HIT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        } else if (Noellesroles.DEATH_REASON_THROWING_AXE.toString().equals(deathReason)) {
            // 飞斧伪造死因补一层命中音效，方便尸体伪造时更贴近实际武器反馈。
            serverWorld.playSound(null, player.getBlockPos(), net.minecraft.sound.SoundEvents.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        } else if (Noellesroles.DEATH_REASON_BOMB.toString().equals(deathReason)) {
            // 定时炸弹伪造死因沿用炸弹客自己的爆炸音效与烟雾。
            serverWorld.playSound(null, player.getBlockPos(), net.minecraft.sound.SoundEvent.of(Identifier.of(Noellesroles.MOD_ID, "item.bomb.explode")), SoundCategory.PLAYERS, 5.0f, 1.0f);
            serverWorld.spawnParticles(WatheParticles.BIG_EXPLOSION,
                    player.getX(), player.getY() + 0.1F, player.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
            serverWorld.spawnParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 0.1F, player.getZ(),
                    100, 0.0, 0.0, 0.0, 0.2);
        } else {
            // 其他死因：仅造尸怪自己听到商店购买音效
            player.playSoundToPlayer(WatheSounds.UI_SHOP_BUY, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        /*
         * 这里统一记录“伪造尸体完成”事件。
         * 额外把死因和角色都存成字符串，是为了回放生成时能够独立着色和翻译。
         */
        GameRecordManager.event(GameRecordTypes.GLOBAL_EVENT)
                .actor(player)
                .put("event", Noellesroles.CORPSEMAKER_FORGED_BODY_EVENT.toString())
                .putUuid("corpse_target", target.getUuid())
                .put("death_reason_id", payload.deathReason())
                .put("fake_role_id", matchedRole.identifier().toString())
                .record();

        // 反馈
        ability.setCooldown(GameConstants.getInTicks(0, 45)); // 45秒冷却
        ability.sync();
        player.sendMessage(Text.translatable("tip.corpsemaker.success", target.getName().getString()), true);
    }
}
