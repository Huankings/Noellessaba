package org.agmas.noellesroles.roles.rememberer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 把回放 Text 强制按中文语言资源展开成普通字符串。
 *
 * <p>为什么这里不能直接使用 {@link Text#getString()}：</p>
 * <p>服务端在生成成书内容时，并没有客户端当前语言环境的概念。
 * 当某些文本没有被即时本地化时，直接 getString() 往往会退回成英文 fallback，
 * 于是“回忆追溯录”的标题、正文就会被提前固化成英文，玩家即便使用中文客户端也无法再翻回来。</p>
 *
 * <p>因此这里显式读取服务端资源包中的中文语言文件
 * （例如 {@code assets/<namespace>/lang/zh_cn.json}），
 * 递归解析回放里出现的 Text 结构，并把可翻译的 key 尽量转换成中文字符串。
 * 这样做的好处是：
 * 1. Wathe 本体的 replay 文案能变成中文；
 * 2. noellesroles / 其他扩展模组只要提供了 zh_cn，也会一起生效；
 * 3. 玩家名字、物品名字、颜色样式等原有拼接结构仍可保留。</p>
 */
public final class RemembererChineseTextResolver {

    private RemembererChineseTextResolver() {
    }

    public static @NotNull String resolveToChineseString(@NotNull MinecraftServer server, @NotNull Text text) {
        Map<String, String> translations = loadChineseTranslations();
        return flatten(resolveToChineseText(text, translations));
    }

    /**
     * 递归把 Text 转成“已经尽量翻译成中文”的新 Text。
     *
     * <p>保留 Text 结构再统一 flatten，
     * 是为了让带参数的 translatable 文本可以继续套用兄弟节点与样式后的最终结果，
     * 而不是简单字符串替换导致结构丢失。</p>
     */
    private static @NotNull Text resolveToChineseText(@NotNull Text text, @NotNull Map<String, String> translations) {
        MutableText resolved = MutableText.of(resolveContent(text.getContent(), translations)).setStyle(text.getStyle());
        for (Text sibling : text.getSiblings()) {
            resolved.append(resolveToChineseText(sibling, translations));
        }
        return resolved;
    }

    private static @NotNull TextContent resolveContent(@NotNull TextContent content, @NotNull Map<String, String> translations) {
        if (content instanceof TranslatableTextContent translatable) {
            Object[] resolvedArgs = Arrays.stream(translatable.getArgs())
                    .map(argument -> resolveArgument(argument, translations))
                    .toArray();

            String translated = translations.get(translatable.getKey());
            if (translated != null) {
                return PlainTextContent.of(applyFormat(translated, resolvedArgs));
            }

            if (translatable.getFallback() != null) {
                return PlainTextContent.of(applyFormat(translatable.getFallback(), resolvedArgs));
            }
        }

        return content;
    }

    private static @NotNull Object resolveArgument(@Nullable Object argument, @NotNull Map<String, String> translations) {
        if (argument instanceof Text textArgument) {
            return flatten(resolveToChineseText(textArgument, translations));
        }
        return argument == null ? "null" : argument;
    }

    /**
     * Minecraft 语言文件里的占位符是 %s / %1$s 这类 Java Formatter 语法，
     * 因此这里直接用 String#format 套入已经递归翻译后的参数即可。
     */
    private static @NotNull String applyFormat(@NotNull String template, @NotNull Object[] args) {
        try {
            return String.format(template, args);
        } catch (Exception ignored) {
            // 如果某条翻译文本参数数量异常，就回退成“模板 + 参数列表”，
            // 避免整本书因为某一条扩展事件格式化不规范而直接报错失败。
            StringBuilder builder = new StringBuilder(template);
            if (args.length > 0) {
                builder.append(" ");
            }
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    builder.append(" ");
                }
                builder.append(args[i]);
            }
            return builder.toString();
        }
    }

    private static @NotNull String flatten(@NotNull Text text) {
        // Text#getString() 本身就会把当前节点和所有兄弟节点按最终显示结果拼平，
        // 这里直接取一次最终字符串即可，避免手动递归时把兄弟节点重复拼接。
        return text.getString();
    }

    /**
     * 汇总 Fabric 已加载模组中的 zh_cn 语言文件。
     *
     * <p>前一版实现尝试从服务端 ResourceManager 读取语言资源，
     * 但服务端资源管理器主要面向数据包，并不会稳定暴露模组 assets 下的 lang 文件。
     * 这正是“标题中文、正文仍英文”的根因：标题是手写中文，而 replay 正文的翻译表根本没被读到。
     *
     * <p>因此这里改成直接遍历 FabricLoader 已加载的全部模组，
     * 对每个模组读取 {@code assets/<modid>/lang/zh_cn.json}。
     * 这样回忆录里的各种 replay key 和 item 名称，只要对应模组提供了中文翻译，
     * 都可以被这一层统一吃到。</p>
     */
    private static @NotNull Map<String, String> loadChineseTranslations() {
        Map<String, String> translations = new HashMap<>();
        for (ModContainer modContainer : FabricLoader.getInstance().getAllMods()) {
            String modId = modContainer.getMetadata().getId();
            String relativePath = "assets/" + modId + "/lang/zh_cn.json";
            modContainer.findPath(relativePath).ifPresent(path -> mergeLanguageFile(translations, path));
        }
        return translations;
    }

    private static void mergeLanguageFile(@NotNull Map<String, String> translations, @NotNull Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!(element instanceof JsonObject object)) {
                return;
            }

            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                    translations.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (IOException ignored) {
            // 单个资源包损坏不应影响整本书生成，
            // 失败时直接跳过，让其他资源包继续提供可用翻译。
        }
    }
}
