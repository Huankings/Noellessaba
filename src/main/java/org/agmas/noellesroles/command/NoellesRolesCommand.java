package org.agmas.noellesroles.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.modifiers.dual_personality.ForcedDualPersonalityManager;
import org.agmas.noellesroles.modifiers.lovers.ForcedLoversManager;
import org.agmas.noellesroles.roles.shadow_jester.ForcedShadowJesterManager;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperConstants;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;

/**
 * NoellesRoles 调试/配置指令入口。
 */
public final class NoellesRolesCommand {

    private static final SimpleCommandExceptionType SAME_PLAYER_EXCEPTION =
            new SimpleCommandExceptionType(Text.translatable("commands.noellesroles.set_lovers.same_player"));
    private static final SimpleCommandExceptionType SAME_DUAL_PERSONALITY_PLAYER_EXCEPTION =
            new SimpleCommandExceptionType(Text.translatable("commands.noellesroles.set_dual_personality.same_player"));
    private static final SimpleCommandExceptionType SAME_SHADOW_JESTER_PLAYER_EXCEPTION =
            new SimpleCommandExceptionType(Text.translatable("commands.noellesroles.set_shadow_jester.same_player"));

    private NoellesRolesCommand() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        /*
         * 用户明确要求只保留 Noelles 风格根命令：
         * /noellesroles
         *
         * 因此这里不再注册 stupidexpress / stupid_express 兼容根，避免迁移后脚本继续误用旧 mod 命名空间。
         */
        dispatcher.register(CommandManager.literal("noellesroles")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("setlovers")
                        .then(CommandManager.argument("first", EntityArgumentType.player())
                                .then(CommandManager.argument("second", EntityArgumentType.player())
                                        .executes(NoellesRolesCommand::setLovers))))
                .then(CommandManager.literal("setdual_personality")
                        .then(CommandManager.argument("main", EntityArgumentType.player())
                                .then(CommandManager.argument("sub", EntityArgumentType.player())
                                        // main/sub 的顺序有意义：main 是开局活跃的主人格，sub 是开局休眠的副人格。
                                        .executes(NoellesRolesCommand::setDualPersonality))))
                .then(CommandManager.literal("setshadow_jester")
                        .then(CommandManager.argument("first", EntityArgumentType.player())
                                .then(CommandManager.argument("second", EntityArgumentType.player())
                                        .executes(NoellesRolesCommand::setShadowJester))))
                .then(CommandManager.literal("setTime")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(NoellesRolesCommand::setTimeCurrency))))
                .then(CommandManager.literal("timekeeper")
                        .then(CommandManager.literal("finish_rift")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(NoellesRolesCommand::finishTimekeeperRift))))
                .then(CommandManager.literal("constants")
                        .then(CommandManager.literal("minplayerspawn")
                                .then(CommandManager.literal("dual_personality")
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(2))
                                                .executes(NoellesRolesCommand::setDualPersonalityMinPlayerSpawn)))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.literal("lovers")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(NoellesRolesCommand::removeLovers)))
                        .then(CommandManager.literal("shadow_jester")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(NoellesRolesCommand::removeShadowJester)))));
    }

    private static int setLovers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity first = EntityArgumentType.getPlayer(context, "first");
        ServerPlayerEntity second = EntityArgumentType.getPlayer(context, "second");
        if (first.getUuid().equals(second.getUuid())) {
            throw SAME_PLAYER_EXCEPTION.create();
        }

        ForcedLoversManager.setPendingPair(first, second);
        context.getSource().sendFeedback(
                () -> Text.translatable(
                        "commands.noellesroles.set_lovers.success",
                        first.getDisplayName(),
                        second.getDisplayName()
                ),
                true
        );
        return 1;
    }

    private static int setTimeCurrency(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int amount = IntegerArgumentType.getInteger(context, "amount");
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");

        /*
         * 光阴是 Wathe 多货币系统里的 noellesroles:time，不是普通金币 balance。
         * 测试指令使用“设置为指定数值”而不是“追加数值”，方便管理员复现边界：
         * 例如直接设置为 119 测光阴不足，设置为 120 测刚好可以回溯。
         */
        PlayerShopComponent.KEY.get(player).setCurrencyAmount(TimekeeperConstants.TIME_CURRENCY_ID, amount);

        context.getSource().sendFeedback(
                () -> Text.translatable(
                        "commands.noellesroles.set_time.success",
                        player.getDisplayName(),
                        amount,
                        Text.literal("§f" + TimekeeperConstants.TIME_CURRENCY_ICON + "§r")
                ),
                true
        );
        return 1;
    }

    private static int finishTimekeeperRift(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        TimekeeperPlayerComponent component = TimekeeperPlayerComponent.KEY.get(player);
        if (!component.isInTimeRift()) {
            context.getSource().sendError(Text.translatable(
                    "commands.noellesroles.timekeeper.finish_rift.not_in_rift",
                    player.getDisplayName()
            ));
            return 0;
        }

        /*
         * 时间回溯播放会逐帧恢复 TimekeeperPlayerComponent 和 Wathe 的生命状态组件。
         * 如果此时允许测试指令手动结束狭缝，下一帧快照可能又把 inTimeRift、
         * aliveOverride、背包和游戏模式覆盖回历史值，导致命令结果和回溯落点互相打架。
         * 因此回溯期间只拒绝执行，等回溯结束后再按目标快照决定玩家是复活还是继续狭缝。
         */
        if (TimekeeperWorldComponent.KEY.get(player.getServerWorld()).isRewinding()) {
            context.getSource().sendError(Text.translatable(
                    "commands.noellesroles.timekeeper.finish_rift.rewinding",
                    player.getDisplayName()
            ));
            return 0;
        }

        /*
         * 这里必须走 TimekeeperPlayerComponent 的正式“自然结束狭缝”出口，
         * 不能只手动清 inTimeRift/timeRiftTicksLeft：
         * - finishTimeRift() 会清掉 Wathe 的特殊存活旁观授权 aliveOverride；
         * - 会把玩家维持为普通 SPECTATOR，而不是仍被胜利结算当作存活；
         * - 会把玩家重新加入死亡语音频道并同步组件给客户端 HUD/外观/输入限制。
         *
         * 也不能调用 finishTimeRiftAsRewoundAlive()，那条出口只给时间回溯复活使用，
         * 不会把玩家切回普通死亡旁观。
         */
        component.finishTimeRift();

        context.getSource().sendFeedback(
                () -> Text.translatable(
                        "commands.noellesroles.timekeeper.finish_rift.success",
                        player.getDisplayName()
                ),
                true
        );
        return 1;
    }

    private static int setDualPersonality(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity main = EntityArgumentType.getPlayer(context, "main");
        ServerPlayerEntity sub = EntityArgumentType.getPlayer(context, "sub");
        if (main.getUuid().equals(sub.getUuid())) {
            throw SAME_DUAL_PERSONALITY_PLAYER_EXCEPTION.create();
        }

        /*
         * 指令只写入“下一局强制队列”。
         * 真正给玩家加词条、清随机残留和写世界组件，会在 Harpy 分配词条时由 ForcedDualPersonalityManager 消费。
         */
        ForcedDualPersonalityManager.setPendingPair(main, sub);
        context.getSource().sendFeedback(
                () -> Text.translatable(
                        "commands.noellesroles.set_dual_personality.success",
                        main.getDisplayName(),
                        sub.getDisplayName()
                ),
                true
        );
        return 1;
    }

    private static int setShadowJester(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity first = EntityArgumentType.getPlayer(context, "first");
        ServerPlayerEntity second = EntityArgumentType.getPlayer(context, "second");
        if (first.getUuid().equals(second.getUuid())) {
            throw SAME_SHADOW_JESTER_PLAYER_EXCEPTION.create();
        }

        /*
         * 指令只写下一局 pending 队列。
         * 真正覆盖职业、建立配对和发任务都在 Harpy 职业分配阶段完成，避免当前局半路改身份破坏状态机。
         */
        ForcedShadowJesterManager.setPendingPair(first, second);
        context.getSource().sendFeedback(
                () -> Text.translatable(
                        "commands.noellesroles.set_shadow_jester.success",
                        first.getDisplayName(),
                        second.getDisplayName()
                ),
                true
        );
        return 1;
    }

    private static int setDualPersonalityMinPlayerSpawn(CommandContext<ServerCommandSource> context) {
        int amount = IntegerArgumentType.getInteger(context, "amount");
        // 这个配置会在每局 assignModifiers 开始时重新读取，立即影响之后开的新局。
        NoellesRolesConfig.HANDLER.instance().dualPersonalityMinPlayerSpawn = amount;
        NoellesRolesConfig.HANDLER.save();
        context.getSource().sendFeedback(
                () -> Text.translatable("commands.noellesroles.constants.minplayerspawn.dual_personality.success", amount),
                true
        );
        return 1;
    }

    private static int removeLovers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        ServerWorld playerWorld = player.getServerWorld();
        ForcedLoversManager.RemovedPair removedPair = ForcedLoversManager.removePendingOrActivePair(playerWorld, player);
        if (removedPair == null) {
            context.getSource().sendError(Text.translatable(
                    "commands.noellesroles.remove_lovers.not_found",
                    player.getDisplayName()
            ));
            return 0;
        }

        Text resolvedPartnerName = Text.literal(ForcedLoversManager.describePlayer(removedPair.partner()));
        ServerPlayerEntity onlinePartner = context.getSource().getServer().getPlayerManager().getPlayer(removedPair.partner());
        if (onlinePartner != null) {
            resolvedPartnerName = onlinePartner.getDisplayName();
        }
        Text partnerName = resolvedPartnerName;

        context.getSource().sendFeedback(
                () -> Text.translatable(
                        removedPair.pending()
                                ? "commands.noellesroles.remove_lovers.pending_success"
                                : "commands.noellesroles.remove_lovers.active_success",
                        player.getDisplayName(),
                        partnerName
                ),
                true
        );
        return 1;
    }

    private static int removeShadowJester(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        ForcedShadowJesterManager.RemovedPair removedPair = ForcedShadowJesterManager.removePendingPair(player);
        if (removedPair == null) {
            context.getSource().sendError(Text.translatable(
                    "commands.noellesroles.remove_shadow_jester.not_found",
                    player.getDisplayName()
            ));
            return 0;
        }

        Text resolvedPartnerName = Text.literal(ForcedShadowJesterManager.describePlayer(removedPair.partner()));
        ServerPlayerEntity onlinePartner = context.getSource().getServer().getPlayerManager().getPlayer(removedPair.partner());
        if (onlinePartner != null) {
            resolvedPartnerName = onlinePartner.getDisplayName();
        }
        Text partnerName = resolvedPartnerName;

        context.getSource().sendFeedback(
                () -> Text.translatable(
                        "commands.noellesroles.remove_shadow_jester.pending_success",
                        player.getDisplayName(),
                        partnerName
                ),
                true
        );
        return 1;
    }
}
