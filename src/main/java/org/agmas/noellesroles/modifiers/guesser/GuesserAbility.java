package org.agmas.noellesroles.modifiers.guesser;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheParticles;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.packet.modifiers.GuessC2SPacket;

public final class GuesserAbility {

    private GuesserAbility() {
        // 工具类，禁止实例化
    }

    /**
     * 处理猜测者的猜测请求
     * @param payload 客户端发送的数据包
     * @param player  技能使用玩家（猜测者）
     */
    public static void handle(GuessC2SPacket payload, ServerPlayerEntity player) {
        var world = player.getServerWorld();
        var gameWorld = GameWorldComponent.KEY.get(world);
        var worldModifier = WorldModifierComponent.KEY.get(world);

        // 检查是否是猜测者
        if (!worldModifier.isRole(player, Noellesroles.GUESSER)) return;

        var ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown > 0) return;

        // 目标玩家
        ServerPlayerEntity target = player.getServer().getPlayerManager().getPlayer(payload.player());

        if (target == null) {
            // 目标可能离线或不存在，只设置冷却，不发送提示
            ability.cooldown = GameConstants.getInTicks(1, 20); // 1分20秒
            ability.sync();
            return;
        }

        String guess = payload.guess();
        if (guess == null || guess.isBlank()) {
            ability.cooldown = GameConstants.getInTicks(1, 20);
            ability.sync();
            return;
        }

        boolean wrong = gameWorld.getRole(target) == null;
        String guessedRoleId = guess.toLowerCase(java.util.Locale.ROOT);
        Role guessedRole = WatheRoles.ROLES.stream()
                .filter(role -> role.identifier().getPath().equalsIgnoreCase(guessedRoleId))
                .findFirst()
                .orElse(null);

        /*
         * 猜测提交本身也需要留痕。
         * 这里单独把“猜了谁、猜成什么身份”先记录下来，
         * 后面再根据正确/错误分别补结果事件。
         */
        NbtCompound declaredExtra = new NbtCompound();
        declaredExtra.putUuid("target_player", target.getUuid());
        if (guessedRole != null) {
            declaredExtra.putString("guessed_role_id", guessedRole.identifier().toString());
        } else {
            /*
             * 如果输入的不是已注册职业，就保留一份原始文本兜底，
             * 这样回放至少还能显示玩家当时输入了什么，而不是退成“未知职业”。
             */
            declaredExtra.putString("guessed_role_fallback", guess);
        }
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.GUESSER_DECLARED_EVENT, player, declaredExtra);

        if (!wrong) {
            // 比较猜测的角色路径（忽略大小写）
            Role targetRole = gameWorld.getRole(target);
            wrong = !targetRole.identifier().getPath().equalsIgnoreCase(guess);

            // 特殊角色（如原版角色）不允许猜测
            if (Harpymodloader.SPECIAL_ROLES.contains(targetRole)) {
                wrong = true;
            }
        }

        if (!wrong) {
            // 猜测正确：目标死亡
            player.playSoundToPlayer(SoundEvents.ENTITY_PIG_DEATH, SoundCategory.MASTER, 1, 1);
            NbtCompound correctExtra = new NbtCompound();
            correctExtra.putUuid("target_player", target.getUuid());
            GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.GUESSER_CORRECT_EVENT, player, correctExtra);
            GameFunctions.killPlayer(target, true, player, Noellesroles.VOODOO_MAGIC_DEATH_REASON);
        } else {
            // 猜测错误
            player.playSoundToPlayer(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.MASTER, 1, 1);
            NbtCompound wrongExtra = new NbtCompound();
            wrongExtra.putUuid("target_player", target.getUuid());
            GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.GUESSER_WRONG_EVENT, player, wrongExtra);

            String punishment = NoellesRolesConfig.HANDLER.instance().guesserDiesAfterIncorrectGuess;
            if ("death".equalsIgnoreCase(punishment)) {
                /*
                 * 猜错自罚时同样补一个 replay_actor=self，
                 * 让死亡回放仍然能走通用 killed 文案链路，
                 * 显示成“被自己的巫毒魔法杀害”。
                 */
                NbtCompound replayDeathData = new NbtCompound();
                replayDeathData.putUuid("replay_actor", player.getUuid());
                GameFunctions.killPlayer(player, true, null, Noellesroles.VOODOO_MAGIC_DEATH_REASON, replayDeathData);
            } else if ("explode".equalsIgnoreCase(punishment)) {
                // 爆炸效果
                world.playSound(null, player.getBlockPos(), WatheSounds.ITEM_GRENADE_EXPLODE, SoundCategory.PLAYERS, 5.0F, 1.0F + player.getRandom().nextFloat() * 0.1F - 0.05F);
                world.spawnParticles(WatheParticles.BIG_EXPLOSION, player.getX(), player.getY() + 0.1F, player.getZ(), 1, 0.0F, 0.0F, 0.0F, 0.0F);
                world.spawnParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.1F, player.getZ(), 100, 0.0F, 0.0F, 0.0F, 0.2F);

                // 爆炸范围内其他玩家也可能死亡
                for (ServerPlayerEntity nearby : world.getPlayers(serverPlayer -> serverPlayer.getBoundingBox().expand(2.0F).intersects(player.getBoundingBox()) && GameFunctions.isPlayerAliveAndSurvival(serverPlayer))) {
                    if (!nearby.equals(player)) {
                        GameFunctions.killPlayer(nearby, true, player, Noellesroles.GUESS_EXPLODE_NEARBY_DEATH_REASON);
                    }
                }
                // 自己死亡
                GameFunctions.killPlayer(player, true, null, Noellesroles.GUESS_EXPLODE_DEATH_REASON);
            }
        }

        // 设置冷却
        ability.cooldown = GameConstants.getInTicks(1, 20);
        ability.sync();
    }
}
