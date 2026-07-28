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
import org.agmas.noellesroles.roles.timekeeper.TimekeeperConstants;

/**
 * NoellesRoles 调试/配置指令入口。
 */
public final class NoellesRolesCommand {

    private static final SimpleCommandExceptionType SAME_PLAYER_EXCEPTION =
            new SimpleCommandExceptionType(Text.translatable("commands.noellesroles.set_lovers.same_player"));
    private static final SimpleCommandExceptionType SAME_DUAL_PERSONALITY_PLAYER_EXCEPTION =
            new SimpleCommandExceptionType(Text.translatable("commands.noellesroles.set_dual_personality.same_player"));

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
                .then(CommandManager.literal("setTime")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(NoellesRolesCommand::setTimeCurrency))))
                .then(CommandManager.literal("constants")
                        .then(CommandManager.literal("minplayerspawn")
                                .then(CommandManager.literal("dual_personality")
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(2))
                                                .executes(NoellesRolesCommand::setDualPersonalityMinPlayerSpawn)))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.literal("lovers")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(NoellesRolesCommand::removeLovers)))));
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
}
