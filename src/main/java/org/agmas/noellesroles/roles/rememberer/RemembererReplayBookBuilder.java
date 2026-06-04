package org.agmas.noellesroles.roles.rememberer;

import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import dev.doctor4t.wathe.record.replay.ReplayGenerator;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 生成追忆者的回忆追溯录。
 *
 * <p>这本书本质上是“对指定玩家最近 3 分钟主动行为回放的二次裁剪版”：
 * 1. 仍然走 Wathe/扩展模组统一注册的 replay formatter；
 * 2. 但只保留主动行为；
 * 3. 并且强制改成“仅显示玩家名字”，避免把职业一起暴露出来。</p>
 */
public final class RemembererReplayBookBuilder {

    private static final String MEMORY_BOOK_MARKER = "rememberer_memory_book";
    private static final Set<Identifier> PASSIVE_GLOBAL_EVENTS = Set.of(
            Noellesroles.DELUSION_STARTED_EVENT,
            Noellesroles.DELUSION_ENDED_EVENT,
            Noellesroles.COWARD_DANGER_SENSED_EVENT,
            Noellesroles.COWARD_DANGER_LEFT_EVENT,
            Noellesroles.SEDATIVE_STARTED_EVENT,
            Noellesroles.SEDATIVE_ENDED_EVENT,
            Noellesroles.TIMED_BOMB_ACTIVATED_EVENT,
            Noellesroles.ROLE_MINE_DETECTED_EVENT,
            Noellesroles.ROLE_MINE_REPORT_EVENT,
            Noellesroles.CAPTURE_DEVICE_TRIGGERED_EVENT,
            Noellesroles.CAPTURE_DEVICE_REPORT_EVENT,
            Noellesroles.CAPTURE_DEVICE_EXPIRED_EVENT,
            Noellesroles.CAPTURE_DEVICE_RELEASED_EVENT,
            Noellesroles.JESTER_PSYCHO_STARTED_EVENT,
            Noellesroles.PROPHET_VOODOO_IMMUNITY_EVENT,
            Noellesroles.WINDER_WIND_MARK_EXPIRED_EVENT,
            Noellesroles.WINDER_WIND_MARK_TRIGGERED_EVENT,
            Noellesroles.WINDER_FLOAT_ENDED_EVENT,
            Noellesroles.NOISEMAKER_GLOW_ENDED_EVENT,
            Noellesroles.MORPHLING_MORPH_ENDED_EVENT,
            Noellesroles.OPERATOR_CONNECTION_ENDED_EVENT,
            Noellesroles.OPERATOR_CONNECTION_INTERRUPTED_EVENT,
            Noellesroles.OPERATOR_BROADCAST_ENDED_EVENT,
            Noellesroles.OPERATOR_BROADCAST_INTERRUPTED_EVENT,
            Noellesroles.CONTROLLER_POSSESS_ENDED_EVENT,
            Noellesroles.SPIRITUALIST_PROJECTION_ENDED_EVENT,
            Noellesroles.SPIRITUALIST_POSSESSION_ENDED_EVENT
    );

    private RemembererReplayBookBuilder() {
    }

    public static ItemStack createMemoryBook(ServerPlayerEntity rememberer, ServerPlayerEntity target) {
        ItemStack book = Items.WRITTEN_BOOK.getDefaultStack();
        markAsMemoryBook(book);

        List<RawFilteredPair<Text>> pages = buildPages(rememberer, target);
        WrittenBookContentComponent content = new WrittenBookContentComponent(
                RawFilteredPair.of(RemembererChineseTextResolver.resolveToChineseString(
                        rememberer.getServer(),
                        Text.translatable("item.noellesroles.memory_record_book")
                )),
                RemembererChineseTextResolver.resolveToChineseString(
                        rememberer.getServer(),
                        Text.translatable("announcement.role.noellesroles.rememberer")
                ),
                0,
                pages,
                true
        );
        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    public static NbtCompound createRecallReplayExtra(ServerPlayerEntity target) {
        NbtCompound extra = new NbtCompound();
        extra.putUuid("memory_target", target.getUuid());
        return extra;
    }

    public static void removeOldMemoryBooks(PlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (isMemoryBook(stack)) {
                // 这里统一走 PlayerInventory 公开的 slot 访问接口，
                // 避免再依赖 combinedInventory 这种当前版本里已不可见的内部字段。
                // size()/getStack()/setStack() 会把主背包、护甲栏、副手一并覆盖到，
                // 因此旧的追忆书无论被玩家塞到哪里，都能被完整清掉。
                player.getInventory().setStack(slot, ItemStack.EMPTY);
            }
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.playerScreenHandler.sendContentUpdates();
        }
    }

    private static List<RawFilteredPair<Text>> buildPages(ServerPlayerEntity rememberer, ServerPlayerEntity target) {
        List<String> wrappedLines = collectWrappedReplayLines(rememberer, target);
        List<RawFilteredPair<Text>> pages = new ArrayList<>();
        List<String> currentPage = new ArrayList<>();

        currentPage.add(centerLine(target.getName().getString() + " 的追溯录"));
        currentPage.add("");

        for (String line : wrappedLines) {
            if (currentPage.size() >= RemembererConstants.BOOK_LINES_PER_PAGE) {
                pages.add(buildPage(currentPage));
                currentPage = new ArrayList<>();
            }
            currentPage.add(line);
        }

        if (currentPage.isEmpty()) {
            currentPage.add(centerLine(target.getName().getString() + " 的追溯录"));
            currentPage.add("");
        }

        pages.add(buildPage(currentPage));
        return pages;
    }

    private static List<String> collectWrappedReplayLines(ServerPlayerEntity rememberer, ServerPlayerEntity target) {
        GameRecordManager.MatchRecord match = resolveReplayMatch();
        List<String> wrappedLines = new ArrayList<>();
        if (match == null) {
            wrappedLines.addAll(wrapLine(RemembererChineseTextResolver.resolveToChineseString(
                    rememberer.getServer(),
                    Text.translatable("book.noellesroles.rememberer.empty")
            )));
            return wrappedLines;
        }

        long currentTick = rememberer.getServerWorld().getTime();
        long cutoffTick = currentTick - RemembererConstants.MEMORY_LOOKBACK_TICKS;
        UUID targetUuid = target.getUuid();

        List<Text> replayLines = ReplayGenerator.generateReplayLines(
                match,
                rememberer.getServerWorld(),
                event -> shouldRememberEvent(event, targetUuid, cutoffTick),
                ReplayGenerator.PlayerDisplayMode.NAME_ONLY
        );

        if (replayLines.isEmpty()) {
            wrappedLines.addAll(wrapLine(RemembererChineseTextResolver.resolveToChineseString(
                    rememberer.getServer(),
                    Text.translatable("book.noellesroles.rememberer.empty")
            )));
            return wrappedLines;
        }

        for (Text line : replayLines) {
            // 这里不能直接 line.getString()：
            // 那会在服务端生成成书时把 translatable 文本按默认环境提前固化，
            // 实测会把回忆录内容冻结成英文。
            // 因此统一先走中文解析器，把 replay Text 主动展开成 zh_cn 字符串后再分行。
            wrappedLines.addAll(wrapLine(RemembererChineseTextResolver.resolveToChineseString(rememberer.getServer(), line)));
        }
        return wrappedLines;
    }

    private static @Nullable GameRecordManager.MatchRecord resolveReplayMatch() {
        GameRecordManager.MatchRecord current = GameRecordManager.getCurrentMatch();
        if (current != null) {
            return current;
        }
        return GameRecordManager.getLastFinishedMatch();
    }

    /**
     * 只保留“这个目标玩家主动发起的事件”。
     */
    private static boolean shouldRememberEvent(GameRecordEvent event, UUID targetUuid, long cutoffTick) {
        if (event.worldTick() < cutoffTick) {
            return false;
        }
        if (!event.data().containsUuid("actor") || !event.data().getUuid("actor").equals(targetUuid)) {
            return false;
        }

        return switch (event.type()) {
            case GameRecordTypes.SHOP_PURCHASE,
                 GameRecordTypes.ITEM_USE,
                 GameRecordTypes.ITEM_HIT,
                 GameRecordTypes.ITEM_PICKUP,
                 GameRecordTypes.PLATTER_TAKE,
                 GameRecordTypes.CONSUME_ITEM,
                 GameRecordTypes.SKILL_USE,
                 GameRecordTypes.DOOR_INTERACTION,
                 GameRecordTypes.TASK_COMPLETE -> true;
            case GameRecordTypes.GLOBAL_EVENT -> isActorInitiatedGlobalEvent(event);
            default -> false;
        };
    }

    /**
     * GLOBAL_EVENT 被太多系统复用，因此这里采取“默认放行，列出明确的被动事件黑名单”的策略。
     *
     * <p>这样做的原因是：
     * 1. 追忆书要尽量兼容其他扩展模组的主动行为事件；
     * 2. 但像“被施加幻觉”“镇静结束”这类纯被动状态变化又不应写进去。</p>
     */
    private static boolean isActorInitiatedGlobalEvent(GameRecordEvent event) {
        Identifier eventId = Identifier.tryParse(event.data().getString("event"));
        return eventId != null && !PASSIVE_GLOBAL_EVENTS.contains(eventId);
    }

    private static RawFilteredPair<Text> buildPage(List<String> lines) {
        TextColor textColor = TextColor.fromRgb(RemembererConstants.BOOK_TEXT_COLOR);
        var pageText = Text.empty();
        for (int i = 0; i < lines.size(); i++) {
            pageText.append(Text.literal(lines.get(i)).setStyle(Style.EMPTY.withColor(textColor)));
            if (i < lines.size() - 1) {
                pageText.append(Text.literal("\n"));
            }
        }
        return RawFilteredPair.of(pageText);
    }

    private static List<String> wrapLine(@NotNull String rawLine) {
        List<String> wrapped = new ArrayList<>();
        if (rawLine.isEmpty()) {
            wrapped.add("");
            return wrapped;
        }

        StringBuilder current = new StringBuilder();
        int currentWidth = 0;
        for (int i = 0; i < rawLine.length(); i++) {
            char character = rawLine.charAt(i);
            int charWidth = getDisplayWidth(character);
            if (currentWidth + charWidth > RemembererConstants.BOOK_LINE_WIDTH_UNITS && current.length() > 0) {
                wrapped.add(current.toString());
                current = new StringBuilder();
                currentWidth = 0;
            }
            current.append(character);
            currentWidth += charWidth;
        }

        if (current.length() > 0) {
            wrapped.add(current.toString());
        }
        return wrapped;
    }

    /**
     * 用非常保守的近似宽度做“看起来差不多居中”的第一行。
     *
     * <p>用户已经接受“近似居中”，这里就不去做复杂的字体像素级测量，
     * 只按中英文宽度差异做一个足够稳定的近似。</p>
     */
    private static String centerLine(String line) {
        int width = 0;
        for (int i = 0; i < line.length(); i++) {
            width += getDisplayWidth(line.charAt(i));
        }

        int remaining = Math.max(0, RemembererConstants.BOOK_TITLE_WIDTH_UNITS - width);
        int spaces = remaining / 2;
        return " ".repeat(spaces) + line;
    }

    private static int getDisplayWidth(char character) {
        if (Character.isWhitespace(character)) {
            return 1;
        }
        return character <= 0x00FF ? 1 : 2;
    }

    private static void markAsMemoryBook(ItemStack stack) {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean(MEMORY_BOOK_MARKER, true);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
    }

    private static boolean isMemoryBook(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        return customData.copyNbt().getBoolean(MEMORY_BOOK_MARKER);
    }
}
